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
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.ValueCodec
import com.kotlinorm.interfaces.ValueCodecContext
import com.kotlinorm.utils.GeneratedTypeMetadataSnapshot
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.utils.isStructurallyAssignableTo
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Internal adapter contract shared by user and built-in codecs.
 *
 * The registry supplies the complete request and invokes one selected codec at
 * most once; implementations must not dispatch to another conversion registry.
 */
internal interface RegistryCodec {
    /**
     * Human-readable identity included in contextual mapping failures.
     */
    val description: String

    /**
     * Matches one non-null request without changing registry or application state.
     * Matching should include direction, origin, storage and complete target type.
     *
     * @param value non-null request value
     * @param request internal conversion policy and physical target
     * @param context public logical type/direction context
     * @return whether this codec can perform the request's single conversion
     */
    fun supports(value: Any, request: ValueConversionRequest, context: ValueCodecContext): Boolean

    /**
     * Performs the single semantic conversion selected for this request.
     * The registry validates nullability and logical/physical output types.
     *
     * @param value source previously accepted by [supports]
     * @param request internal conversion policy
     * @param context public logical type/direction context
     * @param generatedTypes optional deterministic generated metadata snapshot
     * @return converted logical value or encoded database scalar
     */
    fun convert(
        value: Any,
        request: ValueConversionRequest,
        context: ValueCodecContext,
        generatedTypes: GeneratedTypeMetadataSnapshot?
    ): Any?
}

/**
 * Adapts the public [ValueCodec] contract without exposing internal request
 * fields such as physical target, date format or batch index.
 */
internal class UserRegistryCodec(private val codec: ValueCodec) : RegistryCodec {
    override val description: String =
        "user ValueCodec ${codec::class.qualifiedName ?: codec::class.simpleName ?: "<anonymous>"}"

    override fun supports(value: Any, request: ValueConversionRequest, context: ValueCodecContext): Boolean =
        codec.supports(value, context)

    override fun convert(
        value: Any,
        request: ValueConversionRequest,
        context: ValueCodecContext,
        generatedTypes: GeneratedTypeMetadataSnapshot?
    ): Any? = codec.convert(value, context)
}

/**
 * Fixed built-in fallback order used only after all current user registrations.
 * Serialized storage deliberately omits this list.
 */
internal val builtInCodecs: List<RegistryCodec> = listOf(
    EnumValueCodec,
    TemporalValueCodec,
    BasicValueCodec
)

/**
 * Built-ins allowed to accept a non-assignable logical input during parameter
 * ENCODE. This exception does not apply to serialized values or DECODE.
 */
internal val builtInEncodeCoercionCodecs: List<RegistryCodec> = listOf(
    TemporalValueCodec,
    BasicValueCodec
)

/**
 * Chooses the physical ENCODE target when present while keeping DECODE on the
 * logical Kotlin type supplied by the caller.
 *
 * @return the exact KType the selected codec must produce
 */
internal fun ValueConversionRequest.effectiveTargetType(): KType =
    if (direction == ValueCodecDirection.ENCODE) physicalTargetType ?: targetType else targetType

/**
 * Checks runtime assignability and, for known generic collection/map arguments,
 * validates each contained value against the complete target KType.
 *
 * @param value non-null runtime value to validate
 * @return whether runtime classifier and inspectable generic elements match
 */
internal fun KType.accepts(value: Any): Boolean = acceptsRuntimeValue(value)

/**
 * Combines runtime validation with declared source-to-target assignability.
 *
 * Compiler-generated descriptors carry generic variance and substituted
 * supertypes. When they are unavailable, the existing JVM KType/runtime
 * validation remains available without invoking full-reflection subtype APIs.
 * A missing source type means runtime validation is sufficient.
 *
 * @param value non-null runtime value to validate
 * @param sourceType complete declared source type when available
 * @param sourceTypeInfo compiler-generated metadata for [sourceType], when available
 * @param targetTypeInfo compiler-generated metadata for this target, when available
 * @return whether runtime contents and declared structure are assignable
 */
internal fun KType.accepts(
    value: Any,
    sourceType: KType?,
    sourceTypeInfo: StaticTypeInfo?,
    targetTypeInfo: StaticTypeInfo?
): Boolean {
    if (!acceptsRuntimeValue(value)) return false
    if (sourceType == null || sourceType == this) return true
    return if (sourceTypeInfo != null && targetTypeInfo != null) {
        sourceTypeInfo.isStructurallyAssignableTo(targetTypeInfo)
    } else {
        sourceType.isStructurallyAssignableTo(this)
    }
}

private fun KType.acceptsRuntimeValue(value: Any?): Boolean {
    if (value == null) return isMarkedNullable
    if ((classifier as? KClass<*>)?.isInstance(value) != true) return false
    val argumentTypes = arguments.map { it.type }
    return when (value) {
        is Map<*, *> -> {
            val keyType = argumentTypes.getOrNull(0)
            val valueType = argumentTypes.getOrNull(1)
            value.all { (key, element) ->
                (keyType == null || keyType.acceptsRuntimeValue(key)) &&
                    (valueType == null || valueType.acceptsRuntimeValue(element))
            }
        }
        is Iterable<*> -> argumentTypes.firstOrNull()?.let { value.all(it::acceptsRuntimeValue) } ?: true
        is Array<*> -> argumentTypes.firstOrNull()?.let { value.all(it::acceptsRuntimeValue) } ?: true
        else -> true
    }
}

/**
 * Returns whether generated static metadata identifies one concrete enum
 * declaration suitable for name-to-entry decoding.
 *
 * Generated metadata is preferred because it is platform-neutral and carries
 * the stable enum key.
 */
internal fun StaticTypeInfo.isConcreteEnumType(): Boolean = enumKey != null && type.arguments.isEmpty()

/**
 * Returns whether a concrete JVM KType can use the generated enum metadata
 * lookup when no static descriptor accompanies the request.
 *
 * This is intentionally a direct classifier check: it does not traverse a
 * reflective subtype graph or inspect generic arguments.
 */
internal fun KType.isConcreteEnumType(): Boolean = try {
    arguments.isEmpty() && (classifier as? KClass<*>)?.java?.isEnum == true
} catch (_: LinkageError) {
    false
} catch (_: IllegalArgumentException) {
    false
} catch (_: ClassCastException) {
    false
}

/**
 * Rejects logical containers, KPojo values and enum values that JDBC binding
 * must not accept implicitly. Serialized codecs must produce a scalar instead.
 *
 * @return whether this value can cross the database binding boundary directly
 */
internal fun Any.isBindableScalar(): Boolean = when (this) {
    is KPojo, is Collection<*>, is Map<*, *>, is Sequence<*>, is Enum<*>, is Unit -> false
    is Array<*>, is BooleanArray, is CharArray, is DoubleArray, is FloatArray,
    is IntArray, is LongArray, is ShortArray -> false
    else -> true
}
