package com.kotlinorm.utils

import com.kotlinorm.Kronos.defaultDateFormat
import com.kotlinorm.Kronos.timeZone
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.interfaces.ValueCodec
import com.kotlinorm.interfaces.ValueCodecContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object KotlinXDateTimeCodec : ValueCodec {
    private val instantType = typeKey<Instant>()
    private val localDateTimeType = typeKey<LocalDateTime>()
    private val localDateType = typeKey<LocalDate>()
    private val localTimeType = typeKey<LocalTime>()
    private val stringType = typeKey<String>()
    private val longType = typeKey<Long>()
    private val dateTimeTypeKeys = setOf(
        instantType,
        localDateTimeType,
        localDateType,
        localTimeType
    )

    override fun supports(value: Any, context: ValueCodecContext): Boolean =
        context.storage == ValueStorage.NONE &&
            (value.isKotlinXDateTimeValue() || context.targetType.isKotlinXDateTimeType())

    @OptIn(FormatStringsInDatetimeFormats::class, ExperimentalTime::class)
    override fun convert(value: Any, context: ValueCodecContext): Any {
        val targetType = KTypeKey.from(context.targetType, ignoreTopLevelNullability = true)
        val pattern = LocalDateTime.Format {
            byUnicodePattern(context.field?.dateFormat ?: defaultDateFormat)
        }
        val localDateTime = if (value is Number) {
            val instant = Instant.fromEpochMilliseconds(value.toLong())
            if (targetType == instantType) return instant
            instant.toLocalDateTime(TimeZone.of(timeZone.id))
        } else {
            try {
                LocalDateTime.parse(value.toString(), pattern)
            } catch (_: IllegalArgumentException) {
                LocalDateTime.parse(value.toString())
            }
        }
        return when (targetType) {
            localDateTimeType -> localDateTime
            instantType -> localDateTime.toInstant(TimeZone.of(timeZone.id))
            stringType -> localDateTime.format(pattern)
            longType ->
                localDateTime.toInstant(TimeZone.of(timeZone.id)).toEpochMilliseconds()
            localDateType -> localDateTime.date
            localTimeType -> localDateTime.time
            else -> error("Unsupported target type: ${context.targetType}")
        }
    }

    private fun KType.isKotlinXDateTimeType(): Boolean =
        KTypeKey.from(this, ignoreTopLevelNullability = true) in dateTimeTypeKeys

    private fun Any.isKotlinXDateTimeValue(): Boolean =
        this is Instant || this is LocalDateTime || this is LocalDate || this is LocalTime

    private inline fun <reified T> typeKey(): KTypeKey =
        KTypeKey.from(typeOf<T>(), ignoreTopLevelNullability = true)
}
