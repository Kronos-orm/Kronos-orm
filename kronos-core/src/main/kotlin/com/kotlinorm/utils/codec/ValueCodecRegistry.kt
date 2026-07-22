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

import com.kotlinorm.Kronos
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.exceptions.MissingSerializedCodec
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.interfaces.ValueCodec
import com.kotlinorm.interfaces.ValueCodecContext
import com.kotlinorm.interfaces.ValueCodecRegistration
import com.kotlinorm.syntax.render.SqlDialect
import com.kotlinorm.utils.GeneratedTypeMetadataSnapshot
import java.time.ZoneId
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

/**
 * Marks whether a caller supplied a final database value or a temporal value
 * that still needs one registry encoding pass.
 */
internal enum class PreparedValueKind {
    /**
     * The value is already JDBC-bindable and must bypass every codec.
     */
    READY_DATABASE_VALUE,
    /**
     * The value came from a time strategy and must enter temporal conversion once.
     */
    STRATEGY_TEMPORAL
}

/**
 * Carries an explicitly prepared source value across SQL rendering boundaries.
 *
 * @property value source value, including SQL `NULL`
 * @property sourceType complete declared source type when the caller knows it
 * @property dateFormat explicit text format, which takes precedence over field and global formats
 * @property kind whether conversion is complete or still requires temporal encoding
 */
internal data class PreparedValue(
    val value: Any?,
    val sourceType: KType?,
    val dateFormat: String? = null,
    val kind: PreparedValueKind
)

/**
 * Complete internal request carried from a safe mapping boundary to the registry.
 *
 * [targetType] always describes the logical Kotlin field/result type. A JDBC
 * [physicalTargetType] may constrain ENCODE output but never replaces that
 * logical type in public codec matching. [storage] is derived from [field] when
 * one is present and conflicting explicit metadata is rejected immediately.
 *
 * @property value raw input value before any semantic conversion
 * @property direction ENCODE for parameters/delegate writes, DECODE for safe Map/JDBC/delegate reads
 * @property origin the boundary creating this request
 * @property targetType complete logical Kotlin target type, including generic arguments and nullability
 * @property sourceType complete declared source type when known; runtime fallback is created only when absent
 * @property field optional mapped field metadata such as serialization and date format
 * @property storage storage protocol gate; serialized requests never fall through to built-in scalar codecs
 * @property physicalTargetType optional JDBC binding type used only to validate/shape encoded results
 * @property dialect optional SQL dialect included in temporal behavior and error context
 * @property dateFormat explicit caller format before field/global fallback
 * @property effectiveDateFormat resolved explicit, field or global date format in priority order
 * @property timeZone zone used for instant/local temporal conversion
 * @property strict whether implicit basic/temporal DECODE coercion is disabled
 * @property batchIndex source index included in conversion failures
 * @property valueName parameter or result label included in conversion failures
 */
internal data class ValueConversionRequest(
    val value: Any?,
    val direction: ValueCodecDirection,
    val origin: ValueCodecOrigin,
    val targetType: KType,
    val sourceType: KType? = null,
    val field: Field? = null,
    val storage: ValueStorage = if (field?.serializable == true) ValueStorage.SERIALIZED else ValueStorage.NONE,
    val physicalTargetType: KType? = null,
    val dialect: SqlDialect? = null,
    val dateFormat: String? = null,
    val timeZone: ZoneId = Kronos.timeZone,
    val strict: Boolean = Kronos.strictSetValue,
    val batchIndex: Int? = null,
    val valueName: String? = null
) {
    val effectiveDateFormat: String
        get() = dateFormat ?: this.field?.dateFormat ?: Kronos.defaultDateFormat

    init {
        if (field != null) {
            val fieldStorage = if (field.serializable) ValueStorage.SERIALIZED else ValueStorage.NONE
            require(storage == fieldStorage) {
                "Value storage $storage conflicts with field '${field.name}' storage $fieldStorage"
            }
        }
    }
}

/**
 * Single runtime dispatch point for all semantic value conversion.
 *
 * Registrations are immutable snapshots: closing a registration removes only
 * that registration from future requests, while in-flight conversions retain
 * the snapshot selected at their start. User codecs are checked newest first,
 * then the built-in codecs are checked in their fixed order.
 */
internal object ValueCodecRegistry {
    private data class Entry(val id: Long, val codec: UserRegistryCodec)

    private val nextId = AtomicLong()
    private val entries = AtomicReference<List<Entry>>(emptyList())

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
    fun register(codec: ValueCodec): ValueCodecRegistration {
        val entry = Entry(nextId.incrementAndGet(), UserRegistryCodec(codec))
        entries.updateAndGet { listOf(entry) + it }
        return object : ValueCodecRegistration {
            private val closed = AtomicBoolean(false)

            override fun close() {
                if (closed.compareAndSet(false, true)) {
                    entries.updateAndGet { current -> current.filterNot { it.id == entry.id } }
                }
            }
        }
    }

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
        val userCodecs = entries.get()
        val value = request.value
        val context = request.context()

        if (value == null) return validateResult(null, request, enforceSerializedText = false)

        if (request.direction == ValueCodecDirection.ENCODE) {
            val logicalInputType = request.targetType
            if (!logicalInputType.accepts(value, request.sourceType) &&
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
            request.targetType.accepts(value, request.sourceType)
        ) {
            return value
        }

        var selectedCodec: RegistryCodec? = null
        for ((_, codec) in userCodecs) {
            if (invokeCodec(request, codec, "supports") { codec.supports(value, request, context) }) {
                selectedCodec = codec
                break
            }
        }

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
            if (request.targetType.accepts(value, request.sourceType)) {
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
    private inline fun <T> invokeCodec(
        request: ValueConversionRequest,
        codec: RegistryCodec,
        operation: String,
        block: () -> T
    ): T = try {
        block()
    } catch (cause: Throwable) {
        if (cause is Error || cause is CancellationException || cause is ValueMappingException) throw cause
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
 * Derives the runtime source KType without overwriting caller-supplied
 * declared type metadata.
 */
internal val ValueConversionRequest.runtimeSourceType: KType?
    get() = value?.let { it::class.starProjectedType }

private fun ValueConversionRequest.context(): ValueCodecContext = ValueCodecContext(
    direction,
    origin,
    sourceType ?: runtimeSourceType,
    targetType,
    field,
    storage
)

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
