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

package com.kotlinorm.beans.transformers

import com.kotlinorm.Kronos.defaultDateFormat
import com.kotlinorm.Kronos.timeZone
import com.kotlinorm.interfaces.ValueTransformer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import kotlin.reflect.KClass

/**
 * Transformer for DateTime type.
 *
 * @author OUSC
 */
object JvmDateTimeTransformer : ValueTransformer {
    private val dateTimeTypes = listOf(
        "java.sql.Date",
        "java.util.Date",
        "java.time.LocalDateTime",
        "java.time.LocalDate",
        "java.time.LocalTime",
        "java.time.Instant",
        "java.time.ZonedDateTime",
        "java.time.OffsetDateTime",
    )

    override fun isMatch(targetKotlinType: String, superTypesOfValue: List<String>, kClassOfValue: KClass<*>): Boolean {
        return kClassOfValue.qualifiedName in dateTimeTypes ||
                superTypesOfValue.intersect(dateTimeTypes).isNotEmpty() ||
                targetKotlinType in dateTimeTypes
    }

    override fun transform(
        targetKotlinType: String,
        value: Any,
        superTypesOfValue: List<String>,
        dateTimeFormat: String?,
        kClassOfValue: KClass<*>
    ): Any {
        val pattern = dateTimeFormat ?: defaultDateFormat
        val localDateTime = if (value is Number) {
            if (targetKotlinType == "java.time.Instant") {
                return java.time.Instant.ofEpochMilli(value.toLong())
            }
            LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(value.toLong()), timeZone)
        } else {
            try {
                LocalDateTime.parse(value.toString(), DateTimeFormatter.ofPattern(pattern))
            } catch (e: DateTimeParseException) {
                LocalDateTime.parse(value.toString())
            } catch (e: Exception) {
                throw e
            }
        }
        return when (targetKotlinType) {
            "java.time.LocalDateTime" -> localDateTime
            "kotlin.String" -> DateTimeFormatter.ofPattern(pattern).format(localDateTime)
            "kotlin.Long" -> localDateTime.atZone(timeZone).toInstant().toEpochMilli()
            "java.time.LocalDate" -> localDateTime.toLocalDate()
            "java.time.LocalTime" -> localDateTime.toLocalTime()
            "java.time.Instant" -> localDateTime.atZone(timeZone).toInstant()
            "java.time.ZonedDateTime" -> localDateTime.atZone(timeZone)
            "java.time.OffsetDateTime" -> localDateTime.atZone(timeZone).toOffsetDateTime()
            "java.util.Date" -> Date.from(localDateTime.atZone(timeZone).toInstant())
            "java.sql.Date" -> java.sql.Date.valueOf(localDateTime.toLocalDate())
            else -> null
        } ?: throw IllegalArgumentException("Unsupported target type: $targetKotlinType")
    }
}