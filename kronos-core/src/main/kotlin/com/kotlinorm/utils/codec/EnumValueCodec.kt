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

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.utils.codec

import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.exceptions.MissingEnumMetadata
import com.kotlinorm.exceptions.InvalidEnumOrdinal
import com.kotlinorm.exceptions.UnknownEnumValue
import com.kotlinorm.interfaces.ValueCodecContext
import com.kotlinorm.utils.GeneratedTypeMetadataSnapshot
import com.kotlinorm.utils.generatedTypeMetadata
import java.math.BigDecimal
import java.math.BigInteger

private val ordinalColumnTypes = setOf(
    KColumnType.TINYINT,
    KColumnType.SMALLINT,
    KColumnType.INT,
    KColumnType.MEDIUMINT,
    KColumnType.SERIAL,
    KColumnType.BIGINT
)

/**
 * Encodes ordinary enum values with [Enum.name] for string-compatible fields
 * and with the declaration ordinal for explicitly integer fields. Both forms
 * decode through compiler-generated enum metadata.
 *
 * The codec is gated to non-serialized storage. It never uses reflection or
 * `toString`; missing metadata, invalid ordinals and unknown names become
 * contextual mapping failures.
 */
internal object EnumValueCodec : RegistryCodec {
    override val description: String = "enum ValueCodec"

    /**
     * Matches an enum runtime value for ENCODE or a concrete enum target for
     * DECODE. Collection and serialized requests are not matched here.
     *
     * @param value non-null source value
     * @param request request whose direction and exact target determine matching
     * @param context logical request context
     * @return whether enum name/ordinal encoding or generated decoding applies
     */
    override fun supports(value: Any, request: ValueConversionRequest, context: ValueCodecContext): Boolean =
        (request.direction == ValueCodecDirection.ENCODE && value is Enum<*>) ||
            (request.direction == ValueCodecDirection.DECODE && request.targetType.isConcreteEnumType())

    /**
     * Encodes exactly once with `Enum.name` or `Enum.ordinal` according to the
     * physical field type, or decodes a name/ordinal through the
     * compiler-generated factory. Missing metadata, invalid ordinals and
     * unknown names are explicit errors.
     *
     * @param value enum value on encode or persisted name/ordinal on decode
     * @param request exact enum target and failure context
     * @param context logical request context
     * @param generatedTypes optional deterministic metadata snapshot
     * @return persisted enum name or generated enum entry
     * @throws MissingEnumMetadata when no generated decoder exists for the target
     * @throws UnknownEnumValue when the persisted name is not a declared entry
     * @throws InvalidEnumOrdinal when an integer field contains an invalid ordinal
     */
    override fun convert(
        value: Any,
        request: ValueConversionRequest,
        context: ValueCodecContext,
        generatedTypes: GeneratedTypeMetadataSnapshot?
    ): Any {
        if (request.direction == ValueCodecDirection.ENCODE) {
            val enumValue = value as Enum<*>
            if (request.targetType.isConcreteEnumType() &&
                !request.targetType.accepts(enumValue, request.sourceType)
            ) {
                throw request.failure(
                    "enum value ${enumValue::class.qualifiedName} is not assignable to ${request.targetType}"
                )
            }
            return if (request.usesOrdinalStorage()) enumValue.ordinal else enumValue.name
        }

        if (request.targetType.accepts(value, request.sourceType)) return value
        val metadata = (generatedTypes ?: generatedTypeMetadata()).enumMetadata(request.targetType)
            ?: throw request.missingEnumMetadata()
        if (request.usesOrdinalStorage()) {
            val ordinal = (value as? Number)?.toEnumOrdinal()
            if (ordinal == null || ordinal < 0 || ordinal >= metadata.entryNames.size) {
                throw request.invalidEnumOrdinal(value, metadata.entryNames.size)
            }
            val name = metadata.entryNames[ordinal]
            return metadata.factory.create(name)
                ?: throw request.unknownEnumValue(name)
        }
        if (value !is String) {
            throw request.failure("enum decoding requires a String source, got ${value::class.qualifiedName}")
        }
        return metadata.factory.create(value)
            ?: throw request.unknownEnumValue(value)
    }
}

private fun ValueConversionRequest.usesOrdinalStorage(): Boolean =
    field?.type in ordinalColumnTypes

/**
 * Returns an exact Int only for an integer-valued Number within Int range.
 */
private fun Number.toEnumOrdinal(): Int? = when (this) {
    is Byte -> toInt()
    is Short -> toInt()
    is Int -> this
    is Long -> toIntOrNull()
    is BigInteger -> toIntOrNull()
    is BigDecimal -> toIntOrNull()
    is Float -> toBigDecimalOrNull()?.toIntOrNull()
    is Double -> toBigDecimalOrNull()?.toIntOrNull()
    else -> runCatching { BigDecimal(toString()).toIntOrNull() }.getOrNull()
}

private fun Long.toIntOrNull(): Int? = takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()

private fun BigInteger.toIntOrNull(): Int? = runCatching { intValueExact() }.getOrNull()

private fun BigDecimal.toIntOrNull(): Int? = runCatching { toBigIntegerExact().intValueExact() }.getOrNull()

private fun Number.toBigDecimalOrNull(): BigDecimal? = when (this) {
    is Float -> takeIf { it.isFinite() }?.let { BigDecimal.valueOf(it.toDouble()) }
    is Double -> takeIf { it.isFinite() }?.let { BigDecimal.valueOf(it) }
    else -> null
}

private fun ValueConversionRequest.missingEnumMetadata() = MissingEnumMetadata(
    direction,
    origin,
    sourceType,
    runtimeSourceType,
    targetType,
    field?.name,
    field?.columnName,
    valueName,
    dialect?.family?.name,
    batchIndex
)

private fun ValueConversionRequest.unknownEnumValue(rawValue: String) = UnknownEnumValue(
    direction,
    origin,
    sourceType,
    runtimeSourceType,
    targetType,
    field?.name,
    field?.columnName,
    valueName,
    dialect?.family?.name,
    batchIndex,
    rawValue
)

private fun ValueConversionRequest.invalidEnumOrdinal(
    rawValue: Any,
    entryCount: Int
) = InvalidEnumOrdinal(
    direction,
    origin,
    sourceType,
    runtimeSourceType,
    targetType,
    field?.name,
    field?.columnName,
    valueName,
    dialect?.family?.name,
    batchIndex,
    rawValue.toString(),
    entryCount
)
