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

package com.kotlinorm.beans.transformers

import com.kotlinorm.Kronos.defaultDateFormat
import com.kotlinorm.Kronos.timeZone
import com.kotlinorm.interfaces.ValueTransformer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.time.ExperimentalTime

/**
 * Transformer for DateTime type.
 *
 * @author OUSC
 */
object JvmDateTimeTransformer : ValueTransformer {
    private val dateTimeTypes = [
        "java.sql.Date",
        "java.sql.Time",
        "java.sql.Timestamp",
        "java.util.Date",
        "java.time.LocalDateTime",
        "java.time.LocalDate",
        "java.time.LocalTime",
        "java.time.Instant",
        "java.time.ZonedDateTime",
        "java.time.OffsetDateTime",
        "kotlin.time.Instant"
    ]

    override fun isMatch(targetKotlinType: KType, sourceValueClass: KClass<*>): Boolean {
        return sourceValueClass.qualifiedName in dateTimeTypes || targetKotlinType.classifierName() in dateTimeTypes
    }

    @OptIn(ExperimentalTime::class)
    override fun transform(
        targetKotlinType: KType,
        value: Any,
        dateTimeFormat: String?,
        sourceValueClass: KClass<*>
    ): Any {
        val targetTypeName = targetKotlinType.classifierName()
        val pattern = dateTimeFormat ?: defaultDateFormat
        val formatter = DateTimeFormatter.ofPattern(pattern)
        when (targetTypeName) {
            "java.time.LocalDate" -> return value.toLocalDate(formatter)
            "java.time.LocalTime" -> return value.toLocalTime(formatter)
        }
        val localDateTime = value.toLocalDateTime(formatter)
        return when (targetTypeName) {
            "java.time.LocalDateTime" -> localDateTime
            "kotlin.String" -> DateTimeFormatter.ofPattern(pattern).format(localDateTime)
            "kotlin.Long" -> localDateTime.atZone(timeZone).toInstant().toEpochMilli()
            "kotlin.time.Instant" -> kotlin.time.Instant.fromEpochMilliseconds(localDateTime.atZone(timeZone).toInstant().toEpochMilli())
            "java.time.LocalDate" -> localDateTime.toLocalDate()
            "java.time.LocalTime" -> localDateTime.toLocalTime()
            "java.time.Instant" -> localDateTime.atZone(timeZone).toInstant()
            "java.time.ZonedDateTime" -> localDateTime.atZone(timeZone)
            "java.time.OffsetDateTime" -> localDateTime.atZone(timeZone).toOffsetDateTime()
            "java.util.Date" -> java.util.Date.from(localDateTime.atZone(timeZone).toInstant())
            "java.sql.Date" -> java.sql.Date.valueOf(localDateTime.toLocalDate())
            "java.sql.Time" -> java.sql.Time.valueOf(localDateTime.toLocalTime())
            "java.sql.Timestamp" -> java.sql.Timestamp.valueOf(localDateTime)
            else -> null
        } ?: throw IllegalArgumentException("Unsupported target type: $targetKotlinType")
    }

    @OptIn(ExperimentalTime::class)
    private fun Any.toLocalDateTime(formatter: DateTimeFormatter): LocalDateTime =
        when (this) {
            is Number -> LocalDateTime.ofInstant(Instant.ofEpochMilli(toLong()), timeZone)
            is Instant -> LocalDateTime.ofInstant(this, timeZone)
            is java.sql.Date -> toLocalDate().atStartOfDay()
            is java.sql.Time -> LocalDate.ofEpochDay(0).atTime(toLocalTime())
            is java.sql.Timestamp -> LocalDateTime.ofInstant(toInstant(), timeZone)
            is java.util.Date -> LocalDateTime.ofInstant(toInstant(), timeZone)
            is LocalDateTime -> this
            is LocalDate -> atStartOfDay()
            is LocalTime -> LocalDate.ofEpochDay(0).atTime(this)
            is kotlin.time.Instant -> LocalDateTime.ofInstant(Instant.ofEpochMilli(toEpochMilliseconds()), timeZone)
            else -> parseLocalDateTime(toString(), formatter)
        }

    private fun Any.toLocalDate(formatter: DateTimeFormatter): LocalDate =
        when (this) {
            is java.sql.Date -> toLocalDate()
            is LocalDate -> this
            is LocalDateTime -> toLocalDate()
            else -> {
                val text = toString()
                try {
                    LocalDate.parse(text, formatter)
                } catch (_: DateTimeParseException) {
                    try {
                        LocalDate.parse(text)
                    } catch (_: DateTimeParseException) {
                        toLocalDateTime(formatter).toLocalDate()
                    }
                }
            }
        }

    private fun Any.toLocalTime(formatter: DateTimeFormatter): LocalTime =
        when (this) {
            is java.sql.Time -> toLocalTime()
            is LocalTime -> this
            is LocalDateTime -> toLocalTime()
            else -> {
                val text = toString()
                try {
                    LocalTime.parse(text, formatter)
                } catch (_: DateTimeParseException) {
                    try {
                        LocalTime.parse(text)
                    } catch (_: DateTimeParseException) {
                        toLocalDateTime(formatter).toLocalTime()
                    }
                }
            }
        }

    private fun parseLocalDateTime(value: String, formatter: DateTimeFormatter): LocalDateTime =
        try {
            LocalDateTime.parse(value, formatter)
        } catch (_: DateTimeParseException) {
            LocalDateTime.parse(value)
        }

    private fun KType.classifierName(): String? =
        (classifier as? KClass<*>)?.qualifiedName
}
