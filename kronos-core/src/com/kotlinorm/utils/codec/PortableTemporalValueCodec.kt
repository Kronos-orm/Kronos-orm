package com.kotlinorm.utils.codec

import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.interfaces.KronosPhysicalValue
import com.kotlinorm.interfaces.StaticValueCodecContext
import com.kotlinorm.interfaces.ValueCodecContext
import com.kotlinorm.interfaces.decodePlatformTemporal
import com.kotlinorm.interfaces.encodePlatformTemporal
import com.kotlinorm.interfaces.toLogicalScalar
import com.kotlinorm.utils.GeneratedTypeMetadataSnapshot
import com.kotlinorm.utils.StaticTypeRef
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Shared temporal policy; Java/JDBC representations are handled by the JVM adapter. */
internal object PortableTemporalValueCodec : RegistryCodec {
    override val description = "portable temporal ValueCodec"

    override fun supports(value: Any, request: ValueConversionRequest, context: ValueCodecContext): Boolean {
        val target = request.effectiveTargetType()
        if (request.direction == ValueCodecDirection.ENCODE && request.targetType.classifier !in portableTemporalClasses &&
            request.targetType.classifier !in setOf(String::class, Long::class, Any::class)) return false
        val portableInput = value.isPortableTemporalRuntimeValue()
        val portableTarget = target.classifier in portableTemporalClasses
        if (request.direction == ValueCodecDirection.DECODE && request.strict && !target.accepts(value)) return false
        if (request.direction == ValueCodecDirection.DECODE && target.accepts(value)) return false
        return when {
            portableTarget -> portableInput || value is String || value.isPortableEpochNumber()
            portableInput -> target.classifier == String::class || target.classifier == Long::class ||
                (request.direction == ValueCodecDirection.ENCODE && target.classifier == Any::class)
            else -> false
        }
    }

    override fun convert(value: Any, request: ValueConversionRequest, context: ValueCodecContext,
        generatedTypes: GeneratedTypeMetadataSnapshot?): Any {
        val logical = if (request.direction == ValueCodecDirection.ENCODE && request.targetType.accepts(value)) value else {
            val target = if (request.direction == ValueCodecDirection.ENCODE) request.targetType else request.effectiveTargetType()
            val static = request.staticContext(context)
            convertLogical(value, if (target.classifier == String::class) {
                static.copy(dateFormat = request.effectiveDateFormat)
            } else static)
        }
        if (request.direction == ValueCodecDirection.DECODE) return logical
        val physical = request.physicalTargetType
        if ((physical?.classifier ?: request.targetType.classifier) == Long::class && logical.isPortableTemporalRuntimeValue()) {
            val instant = convertTemporalObject(logical, request.staticContext(context)
                .copy(targetType = StaticTypeRef("kotlin.time.Instant", false))) as Instant
            return instant.toEpochMilliseconds()
        }
        val format = if (physical?.classifier == String::class || request.targetType.classifier == String::class) {
            request.effectiveDateFormat
        } else request.dateFormat
        return encodePlatformTemporal(logical, request.staticContext(context).copy(dateFormat = format))
            ?.let { encoded -> (encoded as KronosPhysicalValue.Text).value } ?: logical
    }

    private fun convertLogical(value: Any, context: StaticValueCodecContext): Any = when {
        context.targetType.key == "kotlin.Long" && value is Instant -> value.toEpochMilliseconds()
        context.targetType.key == "kotlin.String" -> encodePlatformTemporal(value, context)
            ?.let { (it as KronosPhysicalValue.Text).value } ?: value.toString()
        value.isPortableEpochNumber() -> {
            val instant = (if (value is Byte || value is Short) (value as Number).toLong() else value)
                .toLogicalScalar(context.copy(targetType = StaticTypeRef("kotlin.time.Instant", false)))
            convertTemporalObject(instant, context)
        }
        value.isPortableTemporalRuntimeValue() -> convertTemporalObject(value, context)
        else -> decodePlatformTemporal(KronosPhysicalValue.Text(value.toString()), context)
            ?: throw IllegalArgumentException("Unsupported portable temporal conversion to ${context.targetType.key}")
    }

    /** Logical objects never pass through a physical text pattern. Missing time/date use the JVM's fixed anchors. */
    internal fun convertTemporalObject(value: Any, context: StaticValueCodecContext): Any {
        val zone = context.timeZone?.let(TimeZone::of) ?: TimeZone.currentSystemDefault()
        val local = when (value) {
            is Instant -> value.toLocalDateTime(zone)
            is LocalDateTime -> value
            is LocalDate -> value.atTime(0, 0)
            is LocalTime -> LocalDate.fromEpochDays(0).atTime(value)
            else -> error("Not a portable temporal object")
        }
        return when (context.targetType.key.removeSuffix("?")) {
            "kotlin.time.Instant", "kotlinx.datetime.Instant" -> if (value is Instant) value else local.toInstant(zone)
            "kotlinx.datetime.LocalDateTime" -> local
            "kotlinx.datetime.LocalDate" -> local.date
            "kotlinx.datetime.LocalTime" -> local.time
            "kotlin.Long" -> (if (value is Instant) value else local.toInstant(zone)).toEpochMilliseconds()
            else -> throw IllegalArgumentException("Unsupported temporal target ${context.targetType.key}")
        }
    }
}

internal val portableTemporalClasses = setOf(Instant::class, LocalDate::class, LocalDateTime::class, LocalTime::class)

internal fun Any.isPortableTemporalRuntimeValue(): Boolean = this is Instant || this is LocalDate ||
    this is LocalDateTime || this is LocalTime

private fun Any.isPortableEpochNumber(): Boolean = this is Byte || this is Short || this is Int ||
    this is Long || this is Float || this is Double
