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
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.exceptions.MissingSerializedCodec
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.interfaces.LegacyValueCodecPlatformContext
import com.kotlinorm.interfaces.KronosValueCodecs
import com.kotlinorm.interfaces.StaticValueCodec
import com.kotlinorm.interfaces.StaticValueCodecContext
import com.kotlinorm.interfaces.StaticValueCodecException
import com.kotlinorm.interfaces.ValueCodec
import com.kotlinorm.interfaces.ValueCodecContext
import com.kotlinorm.interfaces.ValueCodecRegistration
import com.kotlinorm.interfaces.asStaticValueCodec
import com.kotlinorm.interfaces.asValueCodecRegistration
import com.kotlinorm.interfaces.toStaticTypeRef
import com.kotlinorm.utils.GeneratedTypeMetadataSnapshot
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KType

/**
 * Single runtime dispatch point for all semantic value conversion.
 *
 * Registrations are immutable snapshots: closing a registration removes only
 * that registration from future requests, while in-flight conversions retain
 * the snapshot selected at their start. User codecs are checked newest first,
 * then the built-in codecs are checked in their fixed order.
 */
internal object ValueCodecRegistry {
    /**
     * Registers one user codec at the highest current priority.
     *
     * Later registrations are checked first. The returned handle is idempotent;
     * closing it removes only this entry from future snapshots and does not
     * disturb older registrations or requests already in progress.
     *
     * @param codec user codec to place at the highest current priority
     * @return an idempotent registration handle scoped to this entry
     */
    fun register(codec: ValueCodec): ValueCodecRegistration =
        KronosValueCodecs.register(codec.asStaticValueCodec()).asValueCodecRegistration()

    /**
     * Converts one request through the single registry boundary.
     *
     * SQL `NULL`, serialized storage gating, user priority, built-in fallback,
     * output validation and exception context are applied once here. Codec
     * failures surface as [ValueMappingException] subtypes.
     *
     * @param request complete conversion request, including full logical KType
     * @return the validated logical result or JDBC-bindable encoded scalar
     * @throws ValueMappingException when no codec matches or conversion/output validation fails
     */
    fun convert(request: ValueConversionRequest): Any? = convert(request, null)

    /**
     * Converts one request against an explicit generated metadata snapshot.
     *
     * The snapshot parameter exists for deterministic compiler/runtime tests;
     * production calls resolve module-generated metadata lazily. It does not
     * create a second registry or another conversion pass.
     *
     * @param request complete conversion request
     * @param generatedTypes optional immutable generated metadata snapshot
     * @return the same validated result contract as [convert]
     * @throws ValueMappingException when conversion cannot produce a valid result
     */
    internal fun convert(
        request: ValueConversionRequest,
        generatedTypes: GeneratedTypeMetadataSnapshot?
    ): Any? {
        val value = request.value
        val context = request.context()

        if (value == null) return validateResult(null, request, enforceSerializedText = false)

        if (request.direction == ValueCodecDirection.ENCODE) {
            val logicalInputType = request.targetType
            if (!logicalInputType.accepts(
                    value,
                    request.sourceType,
                    request.sourceTypeInfo,
                    request.targetTypeInfo
                ) &&
                !canBuiltInCoerceEncode(value, request, context)
            ) {
                throw request.failure(
                    "encode input ${value::class.qualifiedName} is not assignable to logical target $logicalInputType"
                )
            }
        }

        if (request.storage == ValueStorage.SERIALIZED &&
            request.direction == ValueCodecDirection.DECODE &&
            request.origin == ValueCodecOrigin.MAP &&
            request.targetType.accepts(
                value,
                request.sourceType,
                request.sourceTypeInfo,
                request.targetTypeInfo
            )
        ) {
            return value
        }

        var selectedCodec: RegistryCodec? = request.selectStaticUserCodec(value, context)

        if (selectedCodec == null && request.storage == ValueStorage.NONE) {
            for (codec in builtInCodecs) {
                if (invokeCodec(request, codec, "supports") { codec.supports(value, request, context) }) {
                    selectedCodec = codec
                    break
                }
            }
        }

        val codec = selectedCodec
        if (codec != null) {
            val result = invokeCodec(request, codec, "convert") {
                codec.convert(value, request, context, generatedTypes)
            }
            return validateResult(result, request)
        }

        if (request.storage == ValueStorage.NONE) {
            if (request.targetType.accepts(
                    value,
                    request.sourceType,
                    request.sourceTypeInfo,
                    request.targetTypeInfo
                )
            ) {
                return validateResult(value, request)
            }
        }

        if (request.storage == ValueStorage.SERIALIZED) {
            throw request.missingSerializedCodec()
        }
        throw request.failure("no ValueCodec matched the conversion request")
    }

    /**
     * Accepts a caller-marked final database value without codec dispatch.
     *
     * The caller must unwrap [PreparedValue] before this boundary. READY values
     * still receive nullability, physical-target and JDBC scalar validation, but
     * cannot be encoded a second time.
     *
     * @param request unwrapped READY value and its complete mapping context
     * @return the validated database value unchanged
     * @throws ValueMappingException when the prepared value violates the target contract
     */
    fun acceptPrepared(request: ValueConversionRequest): Any? {
        return validateResult(request.value, request, enforceSerializedText = false)
    }

    /**
     * Allows only the built-in scalar/temporal ENCODE coercions that preserve
     * established parameter semantics after declared-type validation fails.
     * Serialized values and user codecs never enter this exception path.
     */
    private fun canBuiltInCoerceEncode(
        value: Any,
        request: ValueConversionRequest,
        context: ValueCodecContext
    ): Boolean = request.storage == ValueStorage.NONE &&
        builtInEncodeCoercionCodecs.any { it.supports(value, request, context) }

    /**
     * Preserves contextual mapping failures and wraps arbitrary codec errors
     * with the selected codec and operation without attempting another codec.
     */
    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> invokeCodec(
        request: ValueConversionRequest,
        codec: RegistryCodec,
        operation: String,
        block: () -> T
    ): T = try {
        block()
    } catch (cause: CancellationException) {
        throw cause
    } catch (cause: ValueMappingException) {
        throw cause
    } catch (cause: Error) {
        throw cause
    } catch (cause: Throwable) {
        throw request.failure("${codec.description} failed in $operation", cause)
    }

    /**
     * Validates DECODE against the logical target KType and ENCODE against its
     * storage and optional physical target before enforcing the JDBC-bindable
     * scalar boundary.
     *
     * [enforceSerializedText] is disabled for input SQL null and explicit READY
     * values accepted by [acceptPrepared]. Neither case is the output of
     * a semantic serialization call.
     */
    private fun validateResult(
        result: Any?,
        request: ValueConversionRequest,
        enforceSerializedText: Boolean = true
    ): Any? {
        if (enforceSerializedText &&
            request.direction == ValueCodecDirection.ENCODE &&
            request.storage == ValueStorage.SERIALIZED &&
            result !is String
        ) {
            throw request.failure(
                "serialized codec returned ${result?.let { it::class.qualifiedName } ?: "null"}; " +
                    "SERIALIZED encode must produce kotlin.String"
            )
        }
        if (result == null) {
            if (!request.targetType.isMarkedNullable) {
                throw request.failure("conversion produced null for a non-null target")
            }
            return null
        }
        if (request.direction == ValueCodecDirection.DECODE) {
            if (!request.targetType.accepts(result)) {
                throw request.failure(
                    "codec returned ${result::class.qualifiedName}, which is not assignable to ${request.targetType}"
                )
            }
            return result
        }
        request.physicalTargetType?.let { physicalType ->
            if (!physicalType.accepts(result)) {
                throw request.failure(
                    "codec returned ${result::class.qualifiedName}, which is not assignable to physical target $physicalType"
                )
            }
        }
        if (!result.isBindableScalar()) {
            throw request.failure("codec returned unsupported database value ${result::class.qualifiedName}")
        }
        return result
    }
}

/**
 * Runtime source KTypes are intentionally not synthesized. The public
 * diagnostic field remains available for source compatibility and is `null`
 * unless a future caller provides an explicit non-reflective representation.
 */
internal val ValueConversionRequest.runtimeSourceType: KType?
    get() = null

internal fun ValueConversionRequest.context(): ValueCodecContext = ValueCodecContext(
    direction,
    origin,
    sourceType,
    targetType,
    field,
    storage,
    sourceTypeInfo,
    targetTypeInfo
)

/**
 * Resolves shared common codec registrations before JVM-only built-ins. The
 * compatibility context lets legacy KType codecs participate in the same
 * newest-registration-first ledger as descriptor-based common codecs.
 */
@Suppress("TooGenericExceptionCaught") // User codec callbacks need contextual wrapping; cancellation and errors pass through.
private fun ValueConversionRequest.selectStaticUserCodec(
    value: Any,
    legacyContext: ValueCodecContext
): RegistryCodec? {
    val staticContext = staticContext(legacyContext)
    val codec = try {
        KronosValueCodecs.findMatchingCodec(value, staticContext)
    } catch (cause: StaticValueCodecException) {
        throw failure(cause.detail, cause.cause ?: cause)
    } catch (cause: CancellationException) {
        throw cause
    } catch (cause: ValueMappingException) {
        throw cause
    } catch (cause: Error) {
        throw cause
    } catch (cause: Throwable) {
        throw failure("user StaticValueCodec failed in supports", cause)
    }
    return codec?.let { SelectedStaticValueCodec(it, staticContext) }
}

internal fun ValueConversionRequest.staticContext(legacyContext: ValueCodecContext = context()): StaticValueCodecContext = StaticValueCodecContext(
    direction = direction,
    origin = origin,
    sourceType = sourceType?.toStaticTypeRef(sourceTypeInfo),
    targetType = targetType.toStaticTypeRef(targetTypeInfo),
    field = field,
    storage = storage,
    sourceTypeInfo = sourceTypeInfo,
    targetTypeInfo = targetTypeInfo,
    timeZone = timeZone,
    dateFormat = dateFormat
).also { context ->
    context.platformContext = LegacyValueCodecPlatformContext(legacyContext)
}

/** Built-ins shape the effective physical target without carrying stale logical field metadata into its descriptor. */
internal fun ValueConversionRequest.effectiveStaticContext(legacyContext: ValueCodecContext = context()): StaticValueCodecContext {
    val effectiveInfo = if (direction == ValueCodecDirection.ENCODE && physicalTargetType != null) physicalTargetTypeInfo else targetTypeInfo
    return staticContext(legacyContext).copy(targetType = effectiveTargetType().toStaticTypeRef(effectiveInfo),
        targetTypeInfo = effectiveInfo)
}

private class SelectedStaticValueCodec(
    private val codec: StaticValueCodec,
    private val staticContext: StaticValueCodecContext
) : RegistryCodec {
    override val description: String = codec.description

    override fun supports(value: Any, request: ValueConversionRequest, context: ValueCodecContext): Boolean = true

    override fun convert(
        value: Any,
        request: ValueConversionRequest,
        context: ValueCodecContext,
        generatedTypes: GeneratedTypeMetadataSnapshot?
    ): Any? = codec.convert(value, staticContext)
}

/**
 * Creates one diagnostic carrying the full conversion location and declared
 * source/target types so nested, projected and batch failures remain actionable.
 */
internal fun ValueConversionRequest.failure(
    detail: String,
    cause: Throwable? = null
): ValueMappingException = ValueMappingException(
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
    detail,
    cause
)

/**
 * Creates the specialized failure used when serialized input has no decoder.
 *
 * Keeping this separate from [ValueConversionRequest.failure] preserves the
 * public exception subtype while carrying the same source, target, field and
 * batch context as every other registry failure.
 */
private fun ValueConversionRequest.missingSerializedCodec() = MissingSerializedCodec(
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
