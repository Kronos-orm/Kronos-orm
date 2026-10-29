package com.kotlinorm.utils.codec

import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.interfaces.ValueCodecContext
import com.kotlinorm.utils.GeneratedTypeMetadataSnapshot
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Structural Java/JDBC adapters surround the same common logical temporal conversion used by Native. */
internal object JvmPortableTemporalValueCodec : RegistryCodec {
    override val description = "Java/portable temporal ValueCodec"

    override fun supports(value: Any, request: ValueConversionRequest, context: ValueCodecContext): Boolean {
        val target = request.effectiveTargetType()
        if (request.direction == ValueCodecDirection.DECODE && request.strict && !target.accepts(value)) return false
        return (value.isJavaTemporalRepresentation() && (target.classifier in portableTemporalClasses ||
            (request.direction == ValueCodecDirection.ENCODE && request.targetType.classifier in portableTemporalClasses))) ||
            (value.isPortableTemporalRuntimeValue() && (target.temporalTargetKind() != null ||
                (request.direction == ValueCodecDirection.ENCODE && request.targetType.temporalTargetKind() != null)))
    }

    override fun convert(value: Any, request: ValueConversionRequest, context: ValueCodecContext,
        generatedTypes: GeneratedTypeMetadataSnapshot?): Any {
        // Timestamp retains epoch semantics for Instant/epoch targets and JDBC wall-clock semantics for local targets.
        val logicalTarget = if (request.direction == ValueCodecDirection.ENCODE) request.targetType else request.effectiveTargetType()
        val preserveInstant = logicalTarget.classifier == kotlin.time.Instant::class || logicalTarget.classifier == Long::class
        val portable = if (value.isPortableTemporalRuntimeValue()) value else value.toPortableJvmTemporal(preserveInstant)
        val logical = if (request.direction == ValueCodecDirection.ENCODE && request.targetType.classifier in portableTemporalClasses) {
            PortableTemporalValueCodec.convertTemporalObject(portable, request.staticContext(context))
        } else portable
        val target = request.effectiveTargetType()
        if (target.classifier in portableTemporalClasses ||
            (request.direction == ValueCodecDirection.ENCODE && request.targetType.classifier in portableTemporalClasses &&
                target.classifier in setOf(String::class, Long::class))) {
            return PortableTemporalValueCodec.convert(logical, request, context, generatedTypes)
        }
        return convertTemporalValue(logical.toJvmTemporalRepresentation(), target,
            request.timeZone?.let(ZoneId::of) ?: ZoneId.systemDefault(),
            lazy(LazyThreadSafetyMode.NONE) { DateTimeFormatter.ofPattern(request.effectiveDateFormat) })
    }
}
