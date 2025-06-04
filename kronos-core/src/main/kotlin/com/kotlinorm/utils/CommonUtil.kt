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
import com.kotlinorm.beans.transformers.TransformerManager.getValueTransformed
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.DateTimeUtil.currentDateTime
import kotlin.reflect.KClass


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
    if (timeStrategy) {
        val format = field.dateFormat ?: defaultDateFormat
        afterExecute(field, currentDateTime(format))
    } else {
        afterExecute(field, defaultValue)
    }
}

fun <T> Collection<T>.toLinkedSet(): LinkedHashSet<T> = linkedSetOf<T>().apply { addAll(this@toLinkedSet) }


/**
 * Converts a given value to a type-safe value based on the provided Kotlin type.
 *
 * @param kotlinType The target Kotlin type to convert the value to.
 * @param value The value to be converted.
 * @param superTypes A list of super types for the value's class.
 * @param dateTimeFormat An optional date-time format to use for date-time conversions.
 * @param kClassOfVal The KClass of the value.
 * @return The converted value, or null if the conversion is not possible.
 */
fun getTypeSafeValue(
    kotlinType: String,
    value: Any,
    superTypes: List<String> = listOf(),
    dateTimeFormat: String? = null,
    kClassOfVal: KClass<*> = value::class
): Any = getValueTransformed(
    kotlinType,
    value,
    superTypes,
    dateTimeFormat,
    kClassOfVal
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
 * @param kClass
 * @param superTypes
 * @param map
 * @param key
 * @param serializable
 * @return
 */
@Suppress("UNUSED")
fun getSafeValue(
    kPojo: KPojo,
    kClass: KClass<*>,
    superTypes: List<String>,
    map: Map<String, Any?>,
    key: String,
    serializable: Boolean
): Any? {
    if (strictSetValue) {
        return map[key]
    }
    return map[key]?.let { value ->
        val kClassOfVal = value::class
        if (kClass != kClassOfVal) {
            if (serializable) {
                serializeProcessor.deserialize(
                    value.toString(), kClass
                )
            } else {
                val column = kPojo.kronosColumns().find { it.name == key }!!
                getTypeSafeValue(
                    kClass.qualifiedName!!,
                    value,
                    superTypes,
                    column.dateFormat,
                    kClassOfVal
                )
            }
        } else {
            value
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

private fun Field.isTimestampOrPostgresDatetime(wrapper: KronosDataSourceWrapper): Boolean {
    return type == KColumnType.TIMESTAMP ||
            (wrapper.dbType == DBType.Postgres && type == KColumnType.DATETIME)
}

fun processParams(
    wrapper: KronosDataSourceWrapper,
    field: Field,
    value: Any?
): Any? {
    if (value == null) return null
    if (field.serializable) return serializeProcessor.serialize(value)

    return when {
        field.isTimestampOrPostgresDatetime(wrapper) ->
            getTypeSafeValue("java.sql.Timestamp", value, field.superTypes, field.dateFormat)

        wrapper.dbType == DBType.Postgres && field.type == KColumnType.BIT ->
            getTypeSafeValue("kotlin.Boolean", value, field.superTypes, field.dateFormat)

        else -> value
    }
}

fun getDefaultBoolean(
    wrapper: KronosDataSourceWrapper,
    boolean: Boolean
): Any {
    return when (wrapper.dbType) {
        DBType.Postgres -> boolean
        else -> {
            if (boolean) 1 else 0
        }
    }
}