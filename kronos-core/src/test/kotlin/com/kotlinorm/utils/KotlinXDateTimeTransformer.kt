package com.kotlinorm.utils

import com.kotlinorm.Kronos.defaultDateFormat
import com.kotlinorm.Kronos.timeZone
import com.kotlinorm.interfaces.ValueTransformer
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object KotlinXDateTimeTransformer : ValueTransformer {
    private val dateTimeTypes = [
        "kotlinx.datetime.Instant",
        "kotlinx.datetime.LocalDateTime",
        "kotlinx.datetime.LocalDate",
        "kotlinx.datetime.LocalTime",
    ]

    override fun isMatch(targetKotlinType: KType, sourceValueClass: KClass<*>): Boolean {
        return sourceValueClass.qualifiedName in dateTimeTypes || targetKotlinType.classifierName() in dateTimeTypes
    }

    @OptIn(FormatStringsInDatetimeFormats::class, ExperimentalTime::class)
    override fun transform(
        targetKotlinType: KType,
        value: Any,
        dateTimeFormat: String?,
        sourceValueClass: KClass<*>
    ): Any {
        val targetTypeName = targetKotlinType.classifierName()
        val pattern = LocalDateTime.Format { byUnicodePattern(dateTimeFormat ?: defaultDateFormat) }
        val localDateTime = if (value is Number) {
            val instant = Instant.fromEpochMilliseconds(value.toLong())
            if (targetTypeName == "kotlinx.datetime.Instant") {
                return instant
            }
            instant.toLocalDateTime(TimeZone.of(timeZone.id))
        } else {
            try {
                LocalDateTime.parse(value.toString(), pattern)
            } catch (_: IllegalArgumentException) {
                LocalDateTime.parse(value.toString())
            }
        }
        return when (targetTypeName) {
            "kotlinx.datetime.LocalDateTime" -> localDateTime
            "kotlinx.datetime.Instant" -> localDateTime.toInstant(TimeZone.of(timeZone.id))
            "kotlin.String" -> localDateTime.format(pattern)
            "kotlin.Long" -> localDateTime.toInstant(TimeZone.of(timeZone.id)).toEpochMilliseconds()
            "kotlinx.datetime.LocalDate" -> localDateTime.date
            "kotlinx.datetime.LocalTime" -> localDateTime.time
            else -> null
        } ?: throw IllegalArgumentException("Unsupported target type: $targetKotlinType")
    }

    private fun KType.classifierName(): String? =
        (classifier as? KClass<*>)?.qualifiedName
}
