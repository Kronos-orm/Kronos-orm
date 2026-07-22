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

package com.kotlinorm.exceptions

import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import kotlin.reflect.KType

/**
 * Reports one failed semantic value conversion with both declared and runtime
 * source types kept separately.
 *
 * @property direction conversion direction selected by the caller
 * @property origin mapping boundary that created the request
 * @property declaredSourceType complete source KType supplied by the caller, when known
 * @property runtimeSourceType star-projected KType observed from the non-null runtime value
 * @property targetType complete logical target KType
 * @property fieldName mapped Kotlin field name, when available
 * @property columnName mapped database column name, when available
 * @property valueName parameter or result label, when available
 * @property dialect SQL dialect family, when available
 * @property batchIndex source batch index, when available
 */
open class ValueMappingException internal constructor(
    val direction: ValueCodecDirection,
    val origin: ValueCodecOrigin,
    val declaredSourceType: KType?,
    val runtimeSourceType: KType?,
    val targetType: KType,
    val fieldName: String?,
    val columnName: String?,
    val valueName: String?,
    val dialect: String?,
    val batchIndex: Int?,
    detail: String,
    cause: Throwable? = null
) : IllegalArgumentException(
    buildString {
        append("Value mapping failed for ")
        append(direction)
        append('/')
        append(origin)
        fieldName?.let { append(" field '").append(it).append('\'') }
        columnName?.let { append(" column '").append(it).append('\'') }
        valueName?.let { append(" parameter/value '").append(it).append('\'') }
        append(": declared source ")
        append(declaredSourceType ?: "unknown")
        append(", runtime source ")
        append(runtimeSourceType ?: "unknown")
        append(" -> ")
        append(targetType)
        dialect?.let { append(" (dialect=").append(it).append(')') }
        batchIndex?.let { append(" (batchIndex=").append(it).append(')') }
        append("; ")
        append(detail)
    },
    cause
)

/**
 * Reports a serialized request for which no registered user codec matched.
 */
class MissingSerializedCodec internal constructor(
    direction: ValueCodecDirection,
    origin: ValueCodecOrigin,
    declaredSourceType: KType?,
    runtimeSourceType: KType?,
    targetType: KType,
    fieldName: String?,
    columnName: String?,
    valueName: String?,
    dialect: String?,
    batchIndex: Int?
) : ValueMappingException(
    direction,
    origin,
    declaredSourceType,
    runtimeSourceType,
    targetType,
    fieldName,
    columnName,
    valueName,
    dialect,
    batchIndex,
    "no registered ValueCodec matched SERIALIZED storage"
)

/**
 * Reports enum decoding without compiler-generated metadata for the target KType.
 */
class MissingEnumMetadata internal constructor(
    direction: ValueCodecDirection,
    origin: ValueCodecOrigin,
    declaredSourceType: KType?,
    runtimeSourceType: KType?,
    targetType: KType,
    fieldName: String?,
    columnName: String?,
    valueName: String?,
    dialect: String?,
    batchIndex: Int?
) : ValueMappingException(
    direction,
    origin,
    declaredSourceType,
    runtimeSourceType,
    targetType,
    fieldName,
    columnName,
    valueName,
    dialect,
    batchIndex,
    "no compiler-generated enum metadata was loaded for $targetType"
)

/**
 * Reports an enum name that is absent from generated metadata.
 *
 * @property rawValuePreview fixed-length safe preview of the persisted name
 */
class UnknownEnumValue internal constructor(
    direction: ValueCodecDirection,
    origin: ValueCodecOrigin,
    declaredSourceType: KType?,
    runtimeSourceType: KType?,
    targetType: KType,
    fieldName: String?,
    columnName: String?,
    valueName: String?,
    dialect: String?,
    batchIndex: Int?,
    rawValue: String
) : ValueMappingException(
    direction,
    origin,
    declaredSourceType,
    runtimeSourceType,
    targetType,
    fieldName,
    columnName,
    valueName,
    dialect,
    batchIndex,
    "unknown enum name '${rawValue.safeValuePreview()}'"
) {
    val rawValuePreview: String = rawValue.safeValuePreview()
}

/**
 * Reports a persisted enum ordinal that is not an in-range integer.
 *
 * @property rawValuePreview fixed-length safe preview of the persisted value
 */
class InvalidEnumOrdinal internal constructor(
    direction: ValueCodecDirection,
    origin: ValueCodecOrigin,
    declaredSourceType: KType?,
    runtimeSourceType: KType?,
    targetType: KType,
    fieldName: String?,
    columnName: String?,
    valueName: String?,
    dialect: String?,
    batchIndex: Int?,
    rawValue: String,
    entryCount: Int
) : ValueMappingException(
    direction,
    origin,
    declaredSourceType,
    runtimeSourceType,
    targetType,
    fieldName,
    columnName,
    valueName,
    dialect,
    batchIndex,
    "invalid enum ordinal '${rawValue.safeValuePreview()}'; expected an integer in 0 until $entryCount"
) {
    val rawValuePreview: String = rawValue.safeValuePreview()
}

class ConflictingEnumMetadata(val targetType: KType) : IllegalStateException(
    "Conflicting generated enum metadata was registered for $targetType"
)

private const val SAFE_VALUE_PREVIEW_LIMIT = 128

private fun String.safeValuePreview(): String =
    if (length <= SAFE_VALUE_PREVIEW_LIMIT) this else take(SAFE_VALUE_PREVIEW_LIMIT) + "..."
