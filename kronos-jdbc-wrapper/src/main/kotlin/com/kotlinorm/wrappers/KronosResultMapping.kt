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

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.wrappers

import com.kotlinorm.Kronos
import com.kotlinorm.beans.UnsupportedTypeException
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.ResultColumnMetadata
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.database.SqlManager
import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.InvalidKPojoFactoryResult
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.ValueCodec
import com.kotlinorm.utils.decodeDatabaseValue
import com.kotlinorm.utils.isKPojoType
import java.io.InputStream
import java.io.Reader
import java.sql.Blob
import java.sql.Clob
import java.sql.ResultSet
import java.sql.SQLXML
import java.sql.Types
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability
import kotlin.reflect.typeOf

/**
 * Result of a physical JDBC reader. [Handled] may intentionally carry `null`;
 * [NotHandled] asks the registry to continue with the next reader or JDBC fallback.
 */
sealed interface KronosPhysicalReadResult {
    /**
     * Signals that this reader did not handle the physical column.
     * The registry continues with the next reader or JDBC fallback.
     */
    data object NotHandled : KronosPhysicalReadResult

    /**
     * Signals that the reader handled the column.
     *
     * @property value raw physical value; null explicitly represents SQL `NULL`
     * and must not be confused with [NotHandled]
     */
    data class Handled(val value: Any?) : KronosPhysicalReadResult
}

/**
 * Intercepts one JDBC result column before `ResultSet.getObject(position)` is called.
 *
 * Readers are ordered by registration priority and must inspect only physical JDBC
 * metadata or vendor state. Logical target conversion belongs to [ValueCodec].
 */
fun interface KronosPhysicalValueReader {
    /**
     * Reads one physical column before the default JDBC `getObject` path.
     *
     * Implementations must match only JDBC metadata/vendor state and return
     * [KronosPhysicalReadResult.NotHandled] to continue. A handled value is
     * decoded through ValueCodec later exactly once for typed results.
     *
     * @param resultSet active result set positioned on the current row
     * @param position one-based JDBC column position
     * @param context physical statement and database context
     * @return [KronosPhysicalReadResult.Handled] with the physical value, including SQL `NULL`,
     * or [KronosPhysicalReadResult.NotHandled] to continue reader selection
     */
    fun read(resultSet: ResultSet, position: Int, context: KronosStatementContext): KronosPhysicalReadResult
}

/**
 * Normalizes one non-null vendor object after the normal JDBC object has been obtained.
 *
 * Vendor readers are tried in priority order and the first non-null result wins. Returning
 * `null` always declines; unlike [KronosPhysicalValueReader], this contract cannot represent
 * a handled null. Logical target conversion remains exclusively in [ValueCodec] and is
 * performed once for typed results.
 */
fun interface KronosVendorValueReader {
    /**
     * Normalizes one non-null vendor object without applying logical target conversion.
     *
     * @param resultSet active result set positioned on the current row
     * @param position one-based JDBC column position
     * @param value non-null value returned by the driver
     * @param context physical statement and database context
     * @return normalized physical value, or `null` when this reader declines
     */
    fun read(resultSet: ResultSet, position: Int, value: Any, context: KronosStatementContext): Any?
}

/**
 * Ordered physical JDBC readers followed by vendor-object normalization.
 *
 * Physical readers run first; [KronosPhysicalReadResult.Handled] stops selection even when
 * carrying null. Otherwise `ResultSet.getObject` is called, vendor readers are tried, and
 * standard LOB/XML values are materialized. This registry never receives a logical result
 * target. Values returned here remain raw database values and typed callers decode them once
 * through [ValueCodec].
 */
class KronosColumnMapperRegistry private constructor(
    private val physicalReaders: MutableList<KronosPhysicalValueReader>,
    private val vendorReaders: MutableList<KronosVendorValueReader>
) {
    /**
     * Creates an empty physical reader registry; readers can then be registered
     * with explicit prepend/append priority.
     */
    constructor() : this(mutableListOf(), mutableListOf())

    /**
     * Registers a physical reader without changing existing reader instances.
     *
     * @param reader reader to add
     * @param prepend true to give this reader priority over existing readers
     */
    fun register(reader: KronosPhysicalValueReader, prepend: Boolean = true) {
        if (prepend) physicalReaders.add(0, reader) else physicalReaders.add(reader)
    }

    /**
     * Registers a vendor normalizer without changing existing reader instances.
     *
     * @param reader normalizer to add
     * @param prepend true to give this normalizer priority over existing readers
     */
    fun registerVendorReader(reader: KronosVendorValueReader, prepend: Boolean = true) {
        if (prepend) vendorReaders.add(0, reader) else vendorReaders.add(reader)
    }

    /**
     * Reads one raw physical value without applying logical target conversion.
     *
     * Physical readers run before the driver fallback. [KronosPhysicalReadResult.Handled]
     * stops physical reader iteration even when its value is null. After `getObject`, the
     * first vendor reader returning non-null wins; if all decline, built-in LOB/XML
     * materialization is applied.
     *
     * @param resultSet active result set positioned on the current row
     * @param position one-based JDBC column position
     * @param context physical statement and database context
     * @return raw or vendor-normalized JDBC value, including null
     */
    fun readJdbcValue(resultSet: ResultSet, position: Int, context: KronosStatementContext): Any? {
        physicalReaders.forEach { reader ->
            when (val result = reader.read(resultSet, position, context)) {
                KronosPhysicalReadResult.NotHandled -> Unit
                is KronosPhysicalReadResult.Handled -> return result.value
            }
        }
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

    /**
     * Copies current physical and vendor reader order into an independently
     * mutable registry without sharing registration lists.
     */
    fun copy(): KronosColumnMapperRegistry =
        KronosColumnMapperRegistry(physicalReaders.toMutableList(), vendorReaders.toMutableList())

    companion object {
        /**
         * Creates a registry with low-priority built-in readers.
         *
         * Oracle/DM8 numeric columns use `getBigDecimal` before `getObject`; PostgreSQL
         * `PGobject` values are unwrapped afterwards. User registrations prepend by default
         * and therefore override both built-ins.
         *
         * @return a new independently mutable registry
         */
        fun defaults(): KronosColumnMapperRegistry = KronosColumnMapperRegistry().apply {
            register(KronosPhysicalValueReader { resultSet, position, context ->
                if (context.isOracleNumberColumn(resultSet, position)) {
                    KronosPhysicalReadResult.Handled(resultSet.getBigDecimal(position))
                } else {
                    KronosPhysicalReadResult.NotHandled
                }
            }, prepend = false)
            registerVendorReader(KronosVendorValueReader { _, _, value, _ ->
                if (value.javaClass.name == "org.postgresql.util.PGobject") {
                    value.javaClass.methods.firstOrNull { it.name == "getValue" && it.parameterCount == 0 }
                        ?.invoke(value)
                } else null
            }, prepend = false)
        }
    }
}

private fun KronosStatementContext.isOracleNumberColumn(resultSet: ResultSet, position: Int): Boolean {
    if (dbType != DBType.Oracle && dbType != DBType.DM8) return false
    val metaData = resultSet.metaData
    val jdbcType = runCatching { metaData.getColumnType(position) }.getOrNull()
    val typeName = runCatching { metaData.getColumnTypeName(position) }.getOrNull()
    return jdbcType == Types.NUMERIC ||
        jdbcType == Types.DECIMAL ||
        typeName.equals("NUMBER", ignoreCase = true)
}

/**
 * Maps JDBC rows according to a query's complete [KType] and planned column metadata.
 *
 * Mapping selects exactly one mode: string-keyed map, KPojo, or scalar. Physical values are
 * read through [KronosColumnMapperRegistry]; every typed value is then decoded exactly once.
 * Untyped `Map<String, Any?>` values stay raw unless explicit column metadata is available.
 * Scalar mapping reads the first column, and KPojo mapping ignores labels with no matching field.
 */
internal object KronosResultMappers {
    /**
     * Maps all rows using the task's logical target and result-column metadata.
     *
     * @param resultSet active result set before the first row
     * @param task query execution contract
     * @param context statement and physical-reader context
     * @return mapped rows in result-set order
     */
    fun toList(
        resultSet: ResultSet,
        task: KAtomicQueryTask,
        context: KronosStatementContext
    ): List<Any?> {
        return toList(resultSet, task.targetType, task.resultColumns, context)
    }

    /**
     * Maps all rows without planner-provided column metadata.
     *
     * @param resultSet active result set before the first row
     * @param targetType complete logical result type
     * @param context statement and physical-reader context
     * @return mapped rows in result-set order
     */
    fun toList(
        resultSet: ResultSet,
        targetType: KType,
        context: KronosStatementContext
    ): List<Any?> = toList(resultSet, targetType, emptyMap(), context)

    private fun toList(
        resultSet: ResultSet,
        targetType: KType,
        resultColumns: Map<String, ResultColumnMetadata>,
        context: KronosStatementContext
    ): List<Any?> =
        when (val mapping = targetType.mapping()) {
            is ResultMapping.Map -> toMapList(resultSet, mapping.valueType, context, resultColumns)
            is ResultMapping.KPojoResult -> toKPojoList(resultSet, mapping.targetType, context, resultColumns)
            is ResultMapping.Scalar -> toObjectList(resultSet, mapping.targetType, context, resultColumns)
        }

    /**
     * Maps rows to direct Map results while enforcing their requested value KType.
     *
     * A concrete [valueType] replaces only the planner metadata type, retaining field,
     * storage and label context. A null value type represents `Any` or a star projection,
     * so planner metadata may supply the logical type and an absent entry stays raw.
     */
    private fun toMapList(
        resultSet: ResultSet,
        valueType: KType?,
        context: KronosStatementContext,
        resultColumns: Map<String, ResultColumnMetadata> = emptyMap()
    ): List<Map<String, Any?>> =
        mapRows(resultSet, context) { row ->
            val metaData = resultSet.metaData
            val values = linkedMapOf<String, Any?>()
            for (position in 1..metaData.columnCount) {
                val label = metaData.getColumnLabel(position)
                val rawValue = row.readValue(resultSet, position, context)
                val declaredColumn = resultColumns.columnMetadata(label)
                val column = when {
                    valueType != null && declaredColumn != null ->
                        declaredColumn.copy(type = valueType, columnLabel = label)
                    valueType != null -> ResultColumnMetadata(valueType, columnLabel = label)
                    declaredColumn != null -> declaredColumn.withLabel(label)
                    else -> null
                }
                values[label] = if (column == null) rawValue else rawValue.convert(column, context)
            }
            values
        }

    /**
     * Maps the first column of each row to one scalar target.
     *
     * A target of `Any` remains raw when no column metadata exists. If exactly one metadata
     * entry is supplied, it may describe the scalar even when its planned label differs from
     * the driver's label.
     */
    fun toObjectList(
        resultSet: ResultSet,
        targetType: KType,
        context: KronosStatementContext,
        resultColumns: Map<String, ResultColumnMetadata> = emptyMap()
    ): List<Any?> =
        mapRows(resultSet, context) { row ->
            val label = resultSet.metaData.getColumnLabel(1)
            val declaredColumn = resultColumns.columnMetadata(label) ?: resultColumns.values.singleOrNull()
            val rawValue = row.readValue(resultSet, 1, context)
            if (declaredColumn == null && targetType.isAnyType()) {
                return@mapRows rawValue
            }
            val logicalType = if (targetType.isAnyType()) {
                declaredColumn?.type ?: targetType
            } else {
                targetType
            }
            val column = declaredColumn
                ?.copy(type = logicalType, columnLabel = label)
                ?: ResultColumnMetadata(logicalType, columnLabel = label)
            rawValue.convert(column, context)
        }

    /**
     * Maps one result operation to fresh KPojo instances.
     *
     * A factory may be reused across operations, but returning the same instance
     * for two rows in one operation is rejected before the second row is assigned.
     * Each matched field is decoded exactly once from its physical JDBC value.
     *
     * @throws UnsupportedTypeException when field metadata is unavailable for [targetType]
     * @throws InvalidKPojoFactoryResult when a factory reuses an instance within this mapping operation
     */
    fun toKPojoList(
        resultSet: ResultSet,
        targetType: KType,
        context: KronosStatementContext,
        resultColumns: Map<String, ResultColumnMetadata> = emptyMap()
    ): List<KPojo> {
        val columns = fieldsMapCache[targetType]
            ?: throw UnsupportedTypeException("Cannot find fields in $targetType")
        val mappedInstances = Collections.newSetFromMap(IdentityHashMap<KPojo, Boolean>())
        return mapRows(resultSet, context) { row ->
            createKPojo(resultSet, targetType, columns, resultColumns, context, row, mappedInstances)
        }
    }

    private fun createKPojo(
        resultSet: ResultSet,
        targetType: KType,
        columns: Map<String, Field>,
        resultColumns: Map<String, ResultColumnMetadata>,
        context: KronosStatementContext,
        row: RowReadState,
        mappedInstances: MutableSet<KPojo>
    ): KPojo {
        val metaData = resultSet.metaData
        val result = Kronos.createKPojo(targetType)
        if (!mappedInstances.add(result)) {
            throw InvalidKPojoFactoryResult(
                targetType,
                result.__kType,
                "factory reused the same instance within one JDBC result mapping operation"
            )
        }
        return result.apply {
            for (position in 1..metaData.columnCount) {
                val label = metaData.getColumnLabel(position)
                val field = columns.columnField(label) ?: continue
                val fieldType = field.kType
                    ?: if (field.serializable) {
                        error("Serializable field '${field.name}' requires KType metadata for deserialization")
                    } else {
                        typeOf<Any?>()
                    }
                val column = resultColumns.columnMetadata(label)
                    ?.copy(type = fieldType, columnLabel = label)
                    ?: ResultColumnMetadata(fieldType, field, columnLabel = label)
                val value = row.readValue(resultSet, position, context).convert(column, context)
                this[field.name] = value
            }
        }
    }

    /**
     * Advances every row, applies the mapping callback, and records the returned row count.
     * Oracle-family `LONG`/`LONG RAW` values are prefetched when the configured workaround requires it.
     */
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
            context.config.columnMappers.readJdbcValue(resultSet, it, context)
        }
        return RowReadState(overrideValues = longValues)
    }

    private fun oracleLongColumns(resultSet: ResultSet, context: KronosStatementContext): Set<Int> {
        if (context.config.oracleLongColumnStrategy != KronosOracleLongColumnStrategy.READ_FIRST ||
            (context.dbType != DBType.Oracle && context.dbType != DBType.DM8)
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
        val overrideValues: Map<Int, Any?> = emptyMap()
    ) {
        fun readValue(resultSet: ResultSet, position: Int, context: KronosStatementContext): Any? =
            if (position in overrideValues) {
                overrideValues[position]
            } else {
                context.config.columnMappers.readJdbcValue(resultSet, position, context)
            }
    }
}

private fun KType.classifierClass(): KClass<*>? = classifier as? KClass<*>

private fun Map<String, ResultColumnMetadata>.columnMetadata(label: String): ResultColumnMetadata? =
    this[label] ?: caseInsensitiveEntry(label)?.value

/**
 * Finds an unambiguous case-insensitive label fallback.
 *
 * Multiple spellings are accepted when they map to the same value. Distinct values for the
 * same case-insensitive label are rejected rather than selecting by map iteration order.
 */
private fun <T> Map<String, T>.caseInsensitiveEntry(label: String): Map.Entry<String, T>? {
    val matches = entries.filter { it.key.equals(label, ignoreCase = true) }
    val distinctValues = matches.map { it.value }.distinct()
    return when (distinctValues.size) {
        0 -> null
        1 -> matches.first()
        else -> error(
            "Ambiguous result column metadata for label '$label': ${matches.map { it.key }.sorted()}"
        )
    }
}

private fun Map<String, Field>.columnField(label: String): Field? =
    this[label] ?: caseInsensitiveEntry(label)?.value

private fun ResultColumnMetadata.withLabel(label: String): ResultColumnMetadata =
    if (columnLabel == label) this else copy(columnLabel = label)

/**
 * Performs the single logical database-to-Kotlin conversion for one typed result value.
 *
 * @receiver raw physical JDBC value, including null
 * @param metadata logical target and result-column metadata
 * @param context active JDBC statement context
 * @return value converted to the logical target type
 */
private fun Any?.convert(metadata: ResultColumnMetadata, context: KronosStatementContext): Any? =
    decodeDatabaseValue(
        value = this,
        metadata = metadata,
        dialect = runCatching { SqlManager.dialectOf(context.dbType) }.getOrNull(),
        valueName = metadata.columnLabel
    )

/**
 * Builds direct Map row semantics from this complete logical result type.
 *
 * JDBC always materializes a row as [LinkedHashMap], so arbitrary Map subtypes cannot be
 * returned safely and their own generic argument order cannot describe Map key/value types.
 * Direct Map declarations retain their complete key/value [KType], including nested
 * arguments and nullability. Star, Any, and Any? values intentionally remain raw.
 *
 * @receiver complete logical result type, including top-level nullability
 * @return validated Map mapping, or null when this type is outside the Map hierarchy
 * @throws UnsupportedTypeException when a Map subtype or unsupported key type is requested
 */
private fun KType.directMapMappingOrNull(): ResultMapping.Map? {
    val normalizedType = withoutTopLevelNullability()
    if (!normalizedType.isSubtypeOf(typeOf<Map<*, *>>())) return null
    if (!normalizedType.hasDirectMapDeclaration()) {
        throw UnsupportedTypeException(
            "Unsupported Map result type $this: JDBC row mapping returns LinkedHashMap " +
                "and supports only direct Map or MutableMap declarations"
        )
    }

    val keyType = normalizedType.arguments.getOrNull(0)?.type
    if (keyType != null && !keyType.equalsIgnoringTopLevelNullability(typeOf<String>())) {
        throw UnsupportedTypeException(
            "Unsupported Map key type $keyType in $this: JDBC row labels require String, String?, or *"
        )
    }
    val valueType = normalizedType.arguments.getOrNull(1)?.type?.takeUnless { it.isAnyType() }
    return ResultMapping.Map(valueType)
}

/**
 * Confirms the concrete declaration boundary for the row container returned by JDBC mapping.
 *
 * This is intentionally the only class-level Map check: runtime construction always returns
 * [LinkedHashMap], so only the directly assignable Map and MutableMap declarations are valid.
 * Key/value semantics continue to come from the complete [KType] in [directMapMappingOrNull].
 * Kotlin type aliases resolve to their underlying declaration and therefore remain supported.
 *
 * @receiver top-level non-null logical Map type
 * @return true only for direct Map or MutableMap declarations
 */
private fun KType.hasDirectMapDeclaration(): Boolean {
    val declaration = classifierClass()
    return declaration == Map::class || declaration == MutableMap::class
}

/**
 * Checks for the complete Any type after ignoring only top-level nullability.
 *
 * @receiver logical target type
 * @return true for Any or Any?, and false for every other complete KType
 */
private fun KType.isAnyType(): Boolean = equalsIgnoringTopLevelNullability(typeOf<Any>())

/**
 * Compares complete types after removing only their top-level nullability marker.
 *
 * @receiver first type to compare
 * @param other second type to compare
 * @return true when all remaining KType structure is equal
 */
private fun KType.equalsIgnoringTopLevelNullability(other: KType): Boolean =
    withoutTopLevelNullability() == other.withoutTopLevelNullability()

/**
 * Removes the top-level nullable marker without changing nested type arguments.
 *
 * @receiver type to normalize
 * @return this type when already non-nullable, otherwise its non-nullable form
 */
private fun KType.withoutTopLevelNullability(): KType =
    if (isMarkedNullable) withNullability(false) else this

/**
 * Selects the row-mapping strategy for this complete logical result type.
 *
 * @receiver complete logical result type
 * @return Map, KPojo, or scalar mapping strategy
 */
private fun KType.mapping(): ResultMapping {
    directMapMappingOrNull()?.let { return it }
    classifierClass() ?: error("Unsupported query result type: $this")
    if (isKPojoType()) {
        return ResultMapping.KPojoResult(this)
    }
    return ResultMapping.Scalar(this)
}

private sealed interface ResultMapping {
    data class Map(val valueType: KType?) : ResultMapping
    data class KPojoResult(val targetType: KType) : ResultMapping
    data class Scalar(val targetType: KType) : ResultMapping
}
