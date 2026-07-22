/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm.utils.codec

import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.interfaces.ValueCodecContext
import com.kotlinorm.utils.GeneratedTypeMetadataSnapshot
import java.time.format.DateTimeFormatter

/**
 * Built-in date/time codec for non-serialized values.
 *
 * Text conversion uses the request's resolved `dateFormat`; instant/local
 * conversion uses its configured time zone. Native temporal conversion avoids
 * parsing the format. Strict mode disables implicit DECODE coercion while
 * parameter and strategy ENCODE still honor the requested JDBC physical type.
 */
internal object TemporalValueCodec : RegistryCodec {
    override val description: String = "temporal ValueCodec"

    /**
     * Matches temporal source/target pairs that require logical conversion or
     * physical JDBC shaping. Strict DECODE accepts an already assignable value
     * without coercion; ENCODE still honors the requested physical target. The
     * effective target must be one exact constructible classifier, preventing
     * broad `Date` subclasses from receiving a base-class result.
     *
     * @param value non-null source value
     * @param request internal request containing physical target and date policy
     * @param context public logical type/direction context
     * @return whether this codec can produce an assignable exact target
     */
    override fun supports(value: Any, request: ValueConversionRequest, context: ValueCodecContext): Boolean {
        val targetType = request.effectiveTargetType()
        val sourceIsTemporal = context.sourceType.isTemporalSource(value)
        val targetKind = targetType.temporalTargetKind()
        val targetIsTemporal = targetKind?.temporalType == true
        if (targetKind == null || (!sourceIsTemporal && !targetIsTemporal)) return false
        if (request.direction == ValueCodecDirection.DECODE && request.strict && !targetType.accepts(value)) {
            return false
        }
        return !targetType.accepts(value) || request.physicalTargetType != null
    }

    /**
     * Converts one temporal value exactly once using the request's effective
     * target type, date format and time zone.
     *
     * The formatter is lazy so native JDBC/epoch conversions do not validate a
     * text pattern unnecessarily. The registry applies strict-mode matching,
     * null handling, physical-target validation and contextual error wrapping.
     *
     * @param value source previously accepted by [supports]
     * @param request request containing exact target, format and time zone
     * @param context logical conversion context used by the registry
     * @param generatedTypes unused generated metadata snapshot
     * @return one value for the effective exact target classifier
     */
    override fun convert(
        value: Any,
        request: ValueConversionRequest,
        context: ValueCodecContext,
        generatedTypes: GeneratedTypeMetadataSnapshot?
    ): Any {
        val formatter = lazy(LazyThreadSafetyMode.NONE) {
            DateTimeFormatter.ofPattern(request.effectiveDateFormat)
        }
        return convertTemporalValue(
            value,
            request.effectiveTargetType(),
            request.timeZone,
            formatter
        )
    }
}
