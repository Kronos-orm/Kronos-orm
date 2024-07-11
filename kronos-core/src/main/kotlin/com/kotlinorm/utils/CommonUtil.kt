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
import com.kotlinorm.Kronos.timeZone
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.utils.DateTimeUtil.currentDateTime
import com.kotlinorm.utils.KotlinClassMapper.kotlinBuiltInClassMap
import kotlinx.datetime.*
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
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
    deleted: Boolean = false,
    callBack: (field: Field, value: Any?) -> Unit
) {
    if (strategy.enabled) {
        if (timeStrategy) {
            val format = (strategy.config ?: defaultDateFormat).toString()
            callBack(strategy.field, currentDateTime(format))
        } else {
            callBack(strategy.field, 1.takeIf { deleted } ?: 0)
        }
    }
}

fun <T> Collection<T>.toLinkedSet(): LinkedHashSet<T> = linkedSetOf<T>().apply { addAll(this@toLinkedSet) }

@OptIn(FormatStringsInDatetimeFormats::class)
fun getTypeSafeValue(
    kotlinType: String,
    value: Any,
    superTypes: List<String> = listOf(),
    dateTimeFormat: String? = null,
    kClassOfVal: KClass<*> = value::class
): Any? {
    fun getEpochSecond(): Long {
        return when (value) {
            is Number -> value.toLong()
            else -> LocalDateTime.parse(value.toString()).toJavaLocalDateTime().atZone(timeZone.toJavaZoneId())
                .toInstant().epochSecond
        }
    }

    fun <T> Any.safeCast(fromNumber: Number.() -> T, fromStr: String.() -> T): T {
        return if (this is Number) {
            fromNumber()
        } else {
            toString().fromStr()
        }
    }

    return when (kotlinType) {
        "kotlin.Int", "kotlin.Int?" -> value.safeCast(Number::toInt, String::toInt)
        "kotlin.Long", "kotlin.Long?" -> value.safeCast(Number::toLong, String::toLong)
        "kotlin.Short", "kotlin.Short?" -> value.safeCast(Number::toShort, String::toShort)
        "kotlin.Float", "kotlin.Float?" -> value.safeCast(Number::toFloat, String::toFloat)
        "kotlin.Double", "kotlin.Double?" -> value.safeCast(Number::toDouble, String::toDouble)
        "kotlin.Byte", "kotlin.Byte?" -> value.safeCast(Number::toByte, String::toByte)
        "kotlin.Char", "kotlin.Char?" -> value.toString().firstOrNull()
        "kotlin.String", "kotlin.String?" -> {
            val typeOfValue =
                listOf(kClassOfVal.qualifiedName, *kClassOfVal.supertypes.map { it.toString() }.toTypedArray())
            when {
                typeOfValue.intersect(
                    listOf(
                        "java.util.Date", "java.time.LocalDateTime", "java.time.LocalDate", "java.time.LocalTime",
                        "kotlinx.datetime.LocalDateTime", "kotlinx.datetime.LocalDate", "kotlinx.datetime.LocalTime"
                    )
                ).isNotEmpty() -> {
                    LocalDateTime.parse(value.toString()).format(LocalDateTime.Format {
                        byUnicodePattern(dateTimeFormat ?: defaultDateFormat)
                    })
                }

                else -> value.toString()
            }
        }

        "kotlin.Boolean" -> (value is Number && value != 0) || value.toString().toBoolean()

        "java.time.LocalDateTime", "java.time.LocalDate", "java.time.LocalTime" -> {
            val localDateTime =
                java.time.Instant.ofEpochSecond(getEpochSecond()).atZone(timeZone.toJavaZoneId())
            when (kotlinType) {
                "java.time.LocalDateTime" -> localDateTime
                "java.time.LocalDate" -> localDateTime.toLocalDate()
                "java.time.LocalTime" -> localDateTime.toLocalTime()
                else -> value
            }
        }

        "kotlinx.datetime.LocalDateTime", "kotlinx.datetime.LocalDate", "kotlinx.datetime.LocalTime" -> {
            val localDateTime = Instant.parse(
                value.toString()
            ).toLocalDateTime(timeZone)
            when (kotlinType) {
                "kotlinx.datetime.LocalDateTime" -> localDateTime
                "kotlinx.datetime.LocalDate" -> localDateTime.date
                "kotlinx.datetime.LocalTime" -> localDateTime.time
                else -> value
            }
        }

        else -> {
            val typeOfProp =
                listOf(kotlinType, *superTypes.toTypedArray())
            when {
                "java.util.Date" in typeOfProp -> {
                    val constructor =
                        Class.forName(kotlinType).constructors.find { it.parameters.size == 1 && it.parameterTypes[0] == Long::class.java }!!
                    constructor.newInstance(getEpochSecond() * 1000)
                }

                else -> value

            }
        }
    }
}

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
 * @param kotlinType
 * @param superTypes
 * @param map
 * @param key
 * @param useSerializeResolver
 * @return
 */
@Suppress("UNUSED")
fun getSafeValue(
    kPojo: KPojo,
    kotlinType: String,
    superTypes: List<String>,
    map: Map<String, Any?>,
    key: String,
    useSerializeResolver: Boolean
): Any? {
    if (strictSetValue) {
        return map[key]
    }
    val column = kPojo.kronosColumns().find { it.name == key }!!

    val safeKey =
        if (map[key] != null || kPojo.kronosColumns().any { it.name == column.columnName }) key else column.columnName
    return when {
        map[safeKey] == null -> null
        else -> {
            val kClassOfVal = map[safeKey]!!::class
            if (kotlinType != kClassOfVal.qualifiedName) {
                if (useSerializeResolver) {
                    return serializeResolver.deserializeObj(
                        map[safeKey].toString(), kotlinBuiltInClassMap[kotlinType] ?: Class.forName(kotlinType).kotlin
                    )
                }
                getTypeSafeValue(kotlinType, map[safeKey]!!, superTypes, column.dateFormat, kClassOfVal)
            } else {
                map[safeKey]
            }
        }
    }
}