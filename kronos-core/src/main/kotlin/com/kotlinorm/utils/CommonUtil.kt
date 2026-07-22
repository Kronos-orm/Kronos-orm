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

package com.kotlinorm.utils

import com.kotlinorm.Kronos.timeZone
import com.kotlinorm.annotations.InternalKronosApi
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.parser.NamedParameterUtils
import com.kotlinorm.beans.task.ResultColumnMetadata
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.render.SqlDialect
import com.kotlinorm.utils.codec.PreparedValue
import com.kotlinorm.utils.codec.PreparedValueKind
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.codec.ValueConversionRequest
import com.kotlinorm.utils.codec.isTemporalRuntimeValue
import java.time.Clock
import java.time.LocalDateTime
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf


fun KronosCommonStrategy.execute(
    timeStrategy: Boolean = false,
    defaultValue: Any = 0,
    afterExecute: KronosCommonStrategy.(field: Field, value: Any?) -> Unit
) {
    afterExecute(
        field,
        if (timeStrategy) {
            PreparedValue(
                value = LocalDateTime.now(Clock.system(timeZone)),
                sourceType = typeOf<LocalDateTime>(),
                kind = PreparedValueKind.STRATEGY_TEMPORAL
            )
        } else {
            defaultValue
        }
    )
}

fun <T> Collection<T>.toLinkedSet(): LinkedHashSet<T> = LinkedHashSet(this)


/**
 * getSafeValue
 * 1.类型转换 Any->String,Long->Int, Short->Int, Int->Short, Int->Boolean...
 *
 * 2.日期转换 String->Date, Long->Date, String-> LocalDateTime, Long->LocalDateTime
 *
 * 3.将String使用serialize resolver转为指定类型
 *
 * 4.若columnLabel在map中的值为null，尝试查找columnName在map中的值存入KPojo
 *
 * @param kPojo
 * @param kType
 * @param map
 * @param key
 * @param serializable
 * @return
 */
@Suppress("UNUSED")
fun getSafeValue(
    kPojo: KPojo,
    kType: KType,
    map: Map<String, Any?>,
    key: String,
    serializable: Boolean
): Any? {
    val column = kPojo.resolveRuntimeMetadata().allFields.find { it.name == key }
    val storage = if (column?.serializable == true || (column == null && serializable)) {
        ValueStorage.SERIALIZED
    } else {
        ValueStorage.NONE
    }
    return ValueCodecRegistry.convert(
        ValueConversionRequest(
            value = map[key],
            direction = ValueCodecDirection.DECODE,
            origin = ValueCodecOrigin.MAP,
            targetType = kType,
            field = column,
            storage = storage,
            valueName = key
        )
    )
}

/**
 * The single semantic decode boundary for a physical database value.
 *
 * JDBC readers must pass their raw or vendor-normalized value here exactly once
 * for typed results. Untyped `Any`/`Map<String, Any?>` results without metadata
 * deliberately bypass this function and retain the physical JDBC value.
 */
@InternalKronosApi
fun decodeDatabaseValue(
    value: Any?,
    metadata: ResultColumnMetadata,
    dialect: SqlDialect? = null,
    valueName: String? = metadata.columnLabel
): Any? = ValueCodecRegistry.convert(
    ValueConversionRequest(
        value = value,
        direction = ValueCodecDirection.DECODE,
        origin = ValueCodecOrigin.DATABASE,
        targetType = metadata.type,
        field = metadata.field,
        storage = metadata.storage,
        dialect = dialect,
        valueName = valueName
    )
)

fun String.trimWhitespace(): String {
    return replace("\\s+".toRegex(), " ").trim()
}

/**
 * Extracts numbers enclosed in parentheses from the input string.
 *
 * The function looks for patterns in the format `(number[,number])` where:
 * - `number` is one or more digits.
 * - The second number (after a comma) is optional.
 *
 * @param input The input string to search for the pattern.
 * @return A `Pair` of integers:
 * - The first integer corresponds to the first number inside the parentheses.
 * - The second integer corresponds to the second number inside the parentheses, or 0 if it is not present.
 * - If no match is found, returns `(0, 0)`.
 */
fun extractNumberInParentheses(input: String): Pair<Int, Int> {
    val regex = Regex("""\s*\(\s*(\d+)\s*(?:,\s*(\d+)\s*)?\)\s*""") // Regular expression to match the pattern.
    val matchResult = regex.find(input) // Finds the first match in the input string.

    if (matchResult != null) {
        val (length, scale) = matchResult.destructured // Extracts matched groups.
        return Pair(length.toInt(), scale.toIntOrNull() ?: 0) // Converts to integers and handles optional second number.
    }
    return Pair(0, 0) // Returns default values if no match is found.
}

fun referencedParameterNames(sql: String): Set<String> =
    NamedParameterUtils.parseSqlStatement(sql).parameterNames.toSet()

fun Map<String, Field>.fieldForParameter(parameterName: String): Field? {
    this[parameterName]?.let { return it }
    val normalizedName = parameterName.substringBefore('@')
    this[normalizedName]?.let { return it }
    conditionParameterSuffixes.forEach { suffix ->
        if (normalizedName.endsWith(suffix)) {
            this[normalizedName.removeSuffix(suffix)]?.let { return it }
        }
    }
    if (normalizedName.startsWith(CURSOR_PARAMETER_PREFIX)) {
        this[normalizedName.removePrefix(CURSOR_PARAMETER_PREFIX)]?.let { return it }
    }
    return null
}

private val conditionParameterSuffixes = listOf("List", "Min", "Max")
private const val CURSOR_PARAMETER_PREFIX = "cursor_"

const val DEFAULT_LIKE_ESCAPE: Char = '\\'

fun escapeLikeLiteral(value: String, escape: Char = DEFAULT_LIKE_ESCAPE): String =
    buildString(value.length) {
        value.forEach { char ->
            if (char == escape || char == '%' || char == '_') {
                append(escape)
            }
            append(char)
        }
    }

fun toDatabaseParameterValue(
    wrapper: KronosDataSourceWrapper,
    fieldsMap: Map<String, Field>,
    parameterName: String,
    value: Any?,
    explicitParameterFields: Map<String, Field> = emptyMap(),
    expandAsList: Boolean = false,
    batchIndex: Int? = null
): Any? {
    val field = explicitParameterFields[parameterName] ?: fieldsMap.fieldForParameter(parameterName)
    if (expandAsList) {
        return value.asDatabaseListParameter().mapIndexed { elementIndex, element ->
            if (field == null) {
                prepareRawSqlParameterValue(
                    element,
                    "$parameterName[$elementIndex]",
                    batchIndex
                )
            } else {
                prepareDatabaseValue(
                    wrapper,
                    field,
                    element,
                    "$parameterName[$elementIndex]",
                    batchIndex
                )
            }
        }
    }
    return if (field != null) {
        prepareDatabaseValue(wrapper, field, value, parameterName, batchIndex)
    } else {
        prepareRawSqlParameterValue(value, parameterName, batchIndex)
    }
}

fun toDatabaseValue(
    wrapper: KronosDataSourceWrapper,
    field: Field,
    value: Any?
): Any? = prepareDatabaseValue(wrapper, field, value, field.name.ifBlank { field.columnName }, null)

private fun prepareDatabaseValue(
    wrapper: KronosDataSourceWrapper,
    field: Field,
    value: Any?,
    valueName: String,
    batchIndex: Int?
): Any? {
    val preparedValue = value as? PreparedValue
    val actualValue = if (preparedValue == null) value else preparedValue.value
    val targetType = field.kType
        ?: if (preparedValue?.kind == PreparedValueKind.STRATEGY_TEMPORAL) {
            field.currentTemporalTargetKType(wrapper)
        } else if (actualValue == null) {
            return null
        } else {
            actualValue::class.starProjectedType
        }
    if (preparedValue?.kind == PreparedValueKind.READY_DATABASE_VALUE) {
        return ValueCodecRegistry.acceptPrepared(
            ValueConversionRequest(
                value = actualValue,
                direction = ValueCodecDirection.ENCODE,
                origin = ValueCodecOrigin.PARAMETER,
                targetType = targetType,
                sourceType = preparedValue.sourceType,
                field = field,
                dialect = wrapper.sqlDialect,
                dateFormat = preparedValue.dateFormat,
                batchIndex = batchIndex,
                valueName = valueName
            )
        )
    }
    val physicalTargetType = field.databaseTargetKType(wrapper, actualValue)
        .takeUnless {
            preparedValue?.kind == PreparedValueKind.STRATEGY_TEMPORAL && field.storesTextValue()
        }

    return ValueCodecRegistry.convert(
        ValueConversionRequest(
            value = actualValue,
            direction = ValueCodecDirection.ENCODE,
            origin = ValueCodecOrigin.PARAMETER,
            targetType = targetType,
            sourceType = preparedValue?.sourceType ?: field.kType,
            field = field,
            physicalTargetType = physicalTargetType,
            dialect = wrapper.sqlDialect,
            dateFormat = preparedValue?.dateFormat,
            batchIndex = batchIndex,
            valueName = valueName
        )
    )
}

@PublishedApi
internal fun prepareRawSqlParameters(
    parameters: Map<String, Any?>,
    batchIndex: Int? = null
): Map<String, Any?> = parameters.mapValues { (name, value) ->
    prepareRawSqlParameterValue(value, name, batchIndex)
}

private fun prepareRawSqlParameterValue(
    value: Any?,
    valueName: String,
    batchIndex: Int?
): Any? = when (value) {
    is PreparedValue -> {
        val actualValue = value.value
        if (actualValue == null) {
            null
        } else if (value.kind == PreparedValueKind.READY_DATABASE_VALUE) {
            val targetType = value.sourceType ?: actualValue::class.starProjectedType
            ValueCodecRegistry.acceptPrepared(
                ValueConversionRequest(
                    value = actualValue,
                    direction = ValueCodecDirection.ENCODE,
                    origin = ValueCodecOrigin.PARAMETER,
                    targetType = targetType,
                    sourceType = value.sourceType,
                    dateFormat = value.dateFormat,
                    batchIndex = batchIndex,
                    valueName = valueName
                )
            )
        } else {
            val targetType = value.sourceType ?: actualValue::class.starProjectedType
            ValueCodecRegistry.convert(
                ValueConversionRequest(
                    value = actualValue,
                    direction = ValueCodecDirection.ENCODE,
                    origin = ValueCodecOrigin.PARAMETER,
                    targetType = targetType,
                    sourceType = value.sourceType,
                    dateFormat = value.dateFormat,
                    batchIndex = batchIndex,
                    valueName = valueName
                )
            )
        }
    }
    is Enum<*> -> {
        val enumType = value::class.starProjectedType
        ValueCodecRegistry.convert(
            ValueConversionRequest(
                value = value,
                direction = ValueCodecDirection.ENCODE,
                origin = ValueCodecOrigin.PARAMETER,
                targetType = enumType,
                sourceType = enumType,
                batchIndex = batchIndex,
                valueName = valueName
            )
        )
    }
    is Iterable<*> -> value.mapIndexed { index, element ->
        prepareRawSqlParameterValue(element, "$valueName[$index]", batchIndex)
    }
    is Array<*> -> value.mapIndexed { index, element ->
        prepareRawSqlParameterValue(element, "$valueName[$index]", batchIndex)
    }.toTypedArray()
    else -> value
}

private fun Any?.asDatabaseListParameter(): List<Any?> = when (this) {
    is Iterable<*> -> toList()
    is Array<*> -> toList()
    is BooleanArray -> toList()
    is ByteArray -> toList()
    is CharArray -> toList()
    is DoubleArray -> toList()
    is FloatArray -> toList()
    is IntArray -> toList()
    is LongArray -> toList()
    is ShortArray -> toList()
    else -> error("Expanded SQL parameter requires an iterable or array value")
}

private fun Field.currentTemporalTargetKType(wrapper: KronosDataSourceWrapper): KType =
    databaseTargetKType(wrapper)
        ?: kType
        ?: when (type) {
            KColumnType.BIGINT -> typeOf<Long>()
            KColumnType.DATE -> typeOf<java.sql.Date>()
            KColumnType.TIME -> typeOf<java.sql.Time>()
            KColumnType.DATETIME,
            KColumnType.TIMESTAMP -> typeOf<LocalDateTime>()
            else -> typeOf<String>()
        }

private fun Field.databaseTargetKType(wrapper: KronosDataSourceWrapper, value: Any? = null): KType? {
    val dialect = wrapper.sqlDialect
    return when {
        type == KColumnType.TIMESTAMP && dialect.timestampParametersAsSqlTimestamp ->
            typeOf<java.sql.Timestamp>()

        type == KColumnType.DATETIME && dialect.datetimeParametersAsSqlTimestamp ->
            typeOf<java.sql.Timestamp>()

        value?.isTemporalRuntimeValue() == true && storesTextValue() ->
            typeOf<String>()

        value is Boolean && acceptsNativeBoolean(wrapper) ->
            typeOf<Boolean>()

        value is Boolean && storesBooleanValue() ->
            typeOf<Int>()

        else -> null
    }
}

private fun Field.storesTextValue(): Boolean = type in setOf(
    KColumnType.CHAR,
    KColumnType.VARCHAR,
    KColumnType.TEXT,
    KColumnType.LONGTEXT,
    KColumnType.CLOB,
    KColumnType.NVARCHAR,
    KColumnType.NCHAR,
    KColumnType.NCLOB,
    KColumnType.MEDIUMTEXT
)

private fun Field.acceptsNativeBoolean(wrapper: KronosDataSourceWrapper): Boolean =
    wrapper.sqlDialect.nativeBooleanValues && storesBooleanValue()

private fun Field.storesBooleanValue(): Boolean =
    type == KColumnType.BIT || kType?.let {
        KTypeKey.from(it, ignoreTopLevelNullability = true) == KTypeKey.from(typeOf<Boolean>())
    } == true

fun toDatabaseBooleanValue(
    wrapper: KronosDataSourceWrapper,
    field: Field,
    boolean: Boolean
): Any {
    return if (field.acceptsNativeBoolean(wrapper)) boolean else if (boolean) 1 else 0
}

fun databaseBooleanLiteral(
    wrapper: KronosDataSourceWrapper,
    field: Field,
    boolean: Boolean
): SqlExpr = if (field.acceptsNativeBoolean(wrapper)) {
    SqlExpr.BooleanLiteral(boolean)
} else {
    SqlExpr.NumberLiteral(if (boolean) "1" else "0")
}
