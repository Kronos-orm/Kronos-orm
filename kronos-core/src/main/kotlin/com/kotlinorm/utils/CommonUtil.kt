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

import com.kotlinorm.Kronos.defaultDateFormat
import com.kotlinorm.Kronos.serializeProcessor
import com.kotlinorm.Kronos.strictSetValue
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.parser.NamedParameterUtils
import com.kotlinorm.beans.transformers.TransformerManager.getValueTransformed
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.utils.DateTimeUtil.currentDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf


/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/7 16:01
 **/

fun KronosCommonStrategy.execute(
    timeStrategy: Boolean = false,
    defaultValue: Any = 0,
    afterExecute: KronosCommonStrategy.(field: Field, value: Any?) -> Unit
) {
    afterExecute(
        field,
        if (timeStrategy) {
            currentDateTime(field.dateFormat ?: defaultDateFormat)
        } else {
            defaultValue
        }
    )
}

fun <T> Collection<T>.toLinkedSet(): LinkedHashSet<T> = LinkedHashSet(this)


/**
 * Converts a given value to a type-safe value based on the provided Kotlin type.
 *
 * @param kotlinType The target Kotlin type to convert the value to.
 * @param value The value to be converted.
 * @param dateTimeFormat An optional date-time format to use for date-time conversions.
 * @param sourceValueClass The runtime KClass of the value.
 * @return The converted value, or null if the conversion is not possible.
 */
fun getTypeSafeValue(
    kotlinType: KType,
    value: Any,
    dateTimeFormat: String? = null,
    sourceValueClass: KClass<*> = value::class
): Any = getValueTransformed(
    kotlinType,
    value,
    dateTimeFormat,
    sourceValueClass
)

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
    if (strictSetValue) {
        return map[key]
    }
    val column = kPojo.resolveRuntimeMetadata().allFields.find { it.name == key }
    return map[key]?.let { value ->
        val targetClass = kType.classifier as? KClass<*> ?: return@let value
        if (targetClass == value::class) {
            value
        } else if (serializable) {
            serializeProcessor.deserialize(value.toString(), kType)
        } else {
            getTypeSafeValue(kType, value, column?.dateFormat, value::class)
        }
    }
}

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

fun Map<String, Field>.fieldForParameter(parameterName: String): Field? =
    this[parameterName] ?: parameterName
        .substringBefore('@')
        .takeIf { it != parameterName }
        ?.let { this[it] }

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

data class TransformerSafeValue(
    val value: Any?,
    val kotlinType: KType,
    val dateTimeFormat: String? = null
)

private fun TransformerSafeValue.toTypeSafeValue(): Any? =
    value?.let { getTypeSafeValue(kotlinType, it, dateTimeFormat) }

fun toDatabaseParameterValue(
    wrapper: KronosDataSourceWrapper,
    fieldsMap: Map<String, Field>,
    parameterName: String,
    value: Any?,
    explicitParameterFields: Map<String, Field> = emptyMap()
): Any? {
    if (value is TransformerSafeValue) {
        return value.toTypeSafeValue()
    }
    val field = explicitParameterFields[parameterName] ?: fieldsMap.fieldForParameter(parameterName)
    return if (field != null && value != null) {
        toDatabaseValue(wrapper, field, value)
    } else {
        value
    }
}

fun toDatabaseValue(
    wrapper: KronosDataSourceWrapper,
    field: Field,
    value: Any?
): Any? {
    if (value == null) return null
    if (value is TransformerSafeValue) return value.toTypeSafeValue()
    if (field.serializable) {
        val kType = field.kType
            ?: error("Serializable field '${field.name}' requires KType metadata for serialization")
        return serializeProcessor.serialize(value, kType)
    }
    if (value is Boolean && !field.acceptsNativeBoolean(wrapper) && field.storesBooleanValue()) {
        return toDatabaseBooleanValue(wrapper, field, value)
    }
    if (value is Number && !field.acceptsNativeBoolean(wrapper) && field.storesBooleanValue()) {
        return value
    }

    val targetKotlinType = field.databaseTargetKType(wrapper)

    return targetKotlinType
        ?.let { getTypeSafeValue(it, value, field.dateFormat) }
        ?: field.kType?.let { getTypeSafeValue(it, value, field.dateFormat) }
        ?: value
}

private fun Field.databaseTargetKType(wrapper: KronosDataSourceWrapper): KType? {
    val dialect = wrapper.sqlDialect
    return when {
        type == KColumnType.TIMESTAMP && dialect.timestampParametersAsSqlTimestamp ->
            typeOf<java.sql.Timestamp>()

        type == KColumnType.DATETIME && dialect.datetimeParametersAsSqlTimestamp ->
            typeOf<java.sql.Timestamp>()

        acceptsNativeBoolean(wrapper) ->
            typeOf<Boolean>()

        type == KColumnType.BIT -> null

        else -> null
    }
}

private fun Field.acceptsNativeBoolean(wrapper: KronosDataSourceWrapper): Boolean =
    wrapper.sqlDialect.nativeBooleanValues && storesBooleanValue()

private fun Field.storesBooleanValue(): Boolean =
    type == KColumnType.BIT || kClass?.qualifiedName == "kotlin.Boolean"

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
