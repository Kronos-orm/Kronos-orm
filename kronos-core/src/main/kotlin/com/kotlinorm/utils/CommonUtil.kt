/**
 * Copyright 2022-2024 kronos-orm
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
import com.kotlinorm.Kronos.serializeResolver
import com.kotlinorm.Kronos.strictSetValue
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.transformers.TransformerManager.getValueTransformed
import com.kotlinorm.utils.DateTimeUtil.currentDateTime
import kotlin.reflect.KClass


/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/7 16:01
 **/

fun setCommonStrategy(
    strategy: KronosCommonStrategy,
    timeStrategy: Boolean = false,
    defaultValue: Any = 0,
    callBack: (field: Field, value: Any?) -> Unit
) {
    if (strategy.enabled) {
        if (timeStrategy) {
            val format = (strategy.field.dateFormat ?: defaultDateFormat).toString()
            callBack(strategy.field, currentDateTime(format))
        } else {
            callBack(strategy.field, defaultValue)
        }
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
    val column = kPojo.kronosColumns().find { it.name == key }!!
    return map[key]?.let {
        val kClassOfVal = it::class
        if (kClass != kClassOfVal) {
            if (serializable) {
                serializeResolver.deserialize(
                    it.toString(), kClass
                )
            } else {
                getTypeSafeValue(
                    kClass.qualifiedName!!,
                    it,
                    superTypes,
                    column.dateFormat,
                    kClassOfVal
                )
            }
        } else {
            it
        }
    }
}

fun String.trimWhitespace(): String {
    return replace("\\s+".toRegex(), " ").trim()
}