/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.wrappers

import com.kotlinorm.Kronos.serializeProcessor
import com.kotlinorm.Kronos.strictSetValue
import com.kotlinorm.beans.UnsupportedTypeException
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.createInstance
import com.kotlinorm.utils.getTypeSafeValue
import java.io.InputStream
import java.io.Reader
import java.sql.Blob
import java.sql.Clob
import java.sql.ResultSet
import java.sql.SQLXML
import java.sql.Types
import kotlin.jvm.javaObjectType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

fun interface KronosColumnMapper {
    fun map(
        resultSet: ResultSet,
        position: Int,
        targetType: KType,
        context: KronosStatementContext
    ): Any?
}

fun interface KronosVendorValueReader {
    fun read(resultSet: ResultSet, position: Int, value: Any, context: KronosStatementContext): Any?
}

class KronosColumnMapperRegistry private constructor(
    private val mappers: MutableMap<KClass<*>, KronosColumnMapper>,
    private val vendorReaders: MutableList<KronosVendorValueReader>
) {
    constructor() : this(linkedMapOf(), mutableListOf())

    fun register(type: KClass<*>, mapper: KronosColumnMapper) {
        mappers[type] = mapper
    }

    fun registerVendorReader(reader: KronosVendorValueReader, prepend: Boolean = true) {
        if (prepend) vendorReaders.add(0, reader) else vendorReaders.add(reader)
    }

    fun map(
        resultSet: ResultSet,
        position: Int,
        targetType: KType,
        context: KronosStatementContext
    ): Any? {
        val targetClass = targetType.classifierClass()
            ?: error("Unsupported column target type: $targetType")
        val mapper = mappers[targetClass] ?: mappers[Any::class] ?: defaultMapper
        return mapper.map(resultSet, position, targetType, context)
    }

    fun readJdbcValue(resultSet: ResultSet, position: Int, context: KronosStatementContext): Any? {
        val value = resultSet.getObject(position) ?: return null
        val vendorValue = vendorReaders.firstNotNullOfOrNull { it.read(resultSet, position, value, context) }
        if (vendorValue != null) return vendorValue
        return when (value) {
            is Clob -> value.characterStream.use(Reader::readText)
            is Blob -> value.binaryStream.use(InputStream::readBytes)
            is SQLXML -> value.string
            else -> value
        }
    }

    fun copy(): KronosColumnMapperRegistry =
        KronosColumnMapperRegistry(LinkedHashMap(mappers), vendorReaders.toMutableList())

    companion object {
        fun defaults(): KronosColumnMapperRegistry = KronosColumnMapperRegistry().apply {
            register(Any::class, defaultMapper)
            registerVendorReader(KronosVendorValueReader { _, _, value, _ ->
                if (value.javaClass.name == "org.postgresql.util.PGobject") {
                    value.javaClass.methods.firstOrNull { it.name == "getValue" && it.parameterCount == 0 }
                        ?.invoke(value)
                } else null
            }, prepend = false)
        }

        private val defaultMapper = KronosColumnMapper { resultSet, position, targetType, context ->
            val targetClass = targetType.classifierClass()
                ?: error("Unsupported column target type: $targetType")
            if (targetClass == Any::class) {
                context.config.columnMappers.readJdbcValue(resultSet, position, context)
            } else if (targetClass == Boolean::class) {
                val value = context.oracleNumberValueForBoolean(resultSet, position)
                    ?: context.config.columnMappers.readJdbcValue(resultSet, position, context)
                value?.let {
                    getTypeSafeValue(
                        targetType,
                        it,
                        sourceValueClass = it::class
                    )
                }
            } else if (strictSetValue) {
                resultSet.getObject(position, javaTypeOf(targetClass))
            } else {
                val value = context.config.columnMappers.readJdbcValue(resultSet, position, context)
                value?.let {
                    getTypeSafeValue(
                        targetType,
                        it,
                        sourceValueClass = it::class
                    )
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun javaTypeOf(kClass: KClass<*>): Class<Any> =
            when (kClass) {
                Int::class -> Int::class.javaObjectType
                Long::class -> Long::class.javaObjectType
                Short::class -> Short::class.javaObjectType
                Byte::class -> Byte::class.javaObjectType
                Float::class -> Float::class.javaObjectType
                Double::class -> Double::class.javaObjectType
                Boolean::class -> Boolean::class.javaObjectType
                Char::class -> Char::class.javaObjectType
                else -> kClass.java
            } as Class<Any>

        private fun KronosStatementContext.oracleNumberValueForBoolean(
            resultSet: ResultSet,
            position: Int
        ): Any? {
            if (dbType != DBType.Oracle && dbType != DBType.DM8) return null
            val metaData = resultSet.metaData
            val jdbcType = runCatching { metaData.getColumnType(position) }.getOrNull()
            val typeName = runCatching { metaData.getColumnTypeName(position) }.getOrNull()
            val isNumberColumn = jdbcType == Types.NUMERIC ||
                jdbcType == Types.DECIMAL ||
                typeName.equals("NUMBER", ignoreCase = true)
            return if (isNumberColumn) resultSet.getBigDecimal(position) else null
        }
    }
}

internal object KronosResultMappers {
    fun toList(
        resultSet: ResultSet,
        task: KAtomicQueryTask,
        context: KronosStatementContext
    ): List<Any?> {
        return toList(resultSet, task.targetType, context)
    }

    fun toList(
        resultSet: ResultSet,
        targetType: KType,
        context: KronosStatementContext
    ): List<Any?> =
        when (val mapping = targetType.mapping()) {
            ResultMapping.Map -> toMapList(resultSet, context)
            is ResultMapping.KPojoResult -> toKPojoList(resultSet, mapping.targetType, context)
            is ResultMapping.Scalar -> toObjectList(resultSet, mapping.targetType, context)
        }

    fun toMapList(resultSet: ResultSet, context: KronosStatementContext): List<Map<String, Any?>> =
        mapRows(resultSet, context) { row ->
            val metaData = resultSet.metaData
            val values = linkedMapOf<String, Any?>()
            for (position in 1..metaData.columnCount) {
                if (position !in row.skipColumns) {
                    values[metaData.getColumnLabel(position)] =
                        context.config.columnMappers.map(resultSet, position, typeOf<Any?>(), context)
                }
            }
            row.overrideValues.forEach { (position, value) ->
                values[metaData.getColumnLabel(position)] = value
            }
            values
        }

    fun toObjectList(
        resultSet: ResultSet,
        targetType: KType,
        context: KronosStatementContext
    ): List<Any?> =
        mapRows(resultSet, context) {
            context.config.columnMappers.map(resultSet, 1, targetType, context)
        }

    @Suppress("UNCHECKED_CAST")
    fun toKPojoList(
        resultSet: ResultSet,
        targetType: KType,
        context: KronosStatementContext
    ): List<KPojo> {
        val kClass = targetType.classifierClass() as? KClass<out KPojo>
            ?: error("Unsupported KPojo query result type: $targetType")
        val columns = fieldsMapCache[kClass as KClass<KPojo>]
            ?: throw UnsupportedTypeException("Cannot find fields in ${kClass.simpleName}")
        return mapRows(resultSet, context) { row ->
            createKPojo(resultSet, kClass, columns, context, row)
        }
    }

    private fun createKPojo(
        resultSet: ResultSet,
        kClass: KClass<out KPojo>,
        columns: Map<String, Field>,
        context: KronosStatementContext,
        row: RowReadState
    ): KPojo {
        val metaData = resultSet.metaData
        return kClass.createInstance().apply {
            for (position in 1..metaData.columnCount) {
                val label = metaData.getColumnLabel(position)
                val field = columns[label] ?: continue
                val value = if (position in row.overrideValues) {
                    row.overrideValues[position]
                } else if (position in row.skipColumns) {
                    continue
                } else {
                    val targetType = field.kType ?: typeOf<Any?>()
                    val jdbcValue = context.config.columnMappers.map(
                        resultSet,
                        position,
                        targetType,
                        context
                    )
                    if (field.serializable && jdbcValue != null) {
                        val kType = field.kType
                            ?: error("Serializable field '${field.name}' requires KType metadata for deserialization")
                        serializeProcessor.deserialize(jdbcValue.toString(), kType)
                    } else {
                        jdbcValue
                    }
                }
                this[field.name] = value
            }
        }
    }

    private inline fun <T> mapRows(
        resultSet: ResultSet,
        context: KronosStatementContext,
        crossinline mapper: (RowReadState) -> T
    ): List<T> {
        val longColumns = oracleLongColumns(resultSet, context)
        val rows = mutableListOf<T>()
        while (resultSet.next()) {
            rows.add(mapper(rowState(resultSet, context, longColumns)))
        }
        context.returnedRows = rows.size
        return rows
    }

    private fun rowState(
        resultSet: ResultSet,
        context: KronosStatementContext,
        longColumns: Set<Int>
    ): RowReadState {
        if (longColumns.isEmpty()) return RowReadState()
        val longValues = longColumns.associateWith {
            context.config.columnMappers.map(resultSet, it, typeOf<Any?>(), context)
        }
        return RowReadState(skipColumns = longColumns, overrideValues = longValues)
    }

    private fun oracleLongColumns(resultSet: ResultSet, context: KronosStatementContext): Set<Int> {
        if (context.config.oracleLongColumnStrategy != KronosOracleLongColumnStrategy.READ_FIRST ||
            context.dbType != DBType.Oracle
        ) {
            return emptySet()
        }
        val metaData = resultSet.metaData
        return (1..metaData.columnCount)
            .filter {
                val typeName = metaData.getColumnTypeName(it)
                typeName.equals("LONG", ignoreCase = true) ||
                    typeName.equals("LONG RAW", ignoreCase = true)
            }
            .toSet()
    }

    private data class RowReadState(
        val skipColumns: Set<Int> = emptySet(),
        val overrideValues: Map<Int, Any?> = emptyMap()
    )
}

private fun KType.classifierClass(): KClass<*>? = classifier as? KClass<*>

private fun KType.isStringKeyMap(): Boolean {
    val kClass = classifierClass() ?: return false
    if (kClass != Map::class && kClass != MutableMap::class) return false
    val keyType = arguments.firstOrNull()?.type ?: return true
    return keyType.classifierClass() == String::class
}

private fun KType.mapping(): ResultMapping {
    if (isStringKeyMap()) return ResultMapping.Map
    val targetClass = classifierClass() ?: error("Unsupported query result type: $this")
    if (targetClass.isKPojoType()) {
        return ResultMapping.KPojoResult(this)
    }
    return ResultMapping.Scalar(this)
}

private fun KClass<*>.isKPojoType(): Boolean =
    this == KPojo::class || supertypes.any { superType ->
        (superType.classifier as? KClass<*>)?.isKPojoType() == true
    }

private sealed interface ResultMapping {
    data object Map : ResultMapping
    data class KPojoResult(val targetType: KType) : ResultMapping
    data class Scalar(val targetType: KType) : ResultMapping
}
