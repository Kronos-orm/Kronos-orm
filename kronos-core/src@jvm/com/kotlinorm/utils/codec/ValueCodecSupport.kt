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

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.StaticTypeInfo
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Fixed built-in fallback order used only after all current user registrations.
 * Serialized storage deliberately omits this list.
 */
internal actual val builtInCodecs: List<RegistryCodec> = listOf(
    EnumValueCodec,
    PortableTemporalValueCodec,
    JvmPortableTemporalValueCodec,
    TemporalValueCodec,
    BasicValueCodec
)

/**
 * Built-ins allowed to accept a non-assignable logical input during parameter
 * ENCODE. This exception does not apply to serialized values or DECODE.
 */
internal actual val builtInEncodeCoercionCodecs: List<RegistryCodec> = listOf(
    PortableTemporalValueCodec,
    JvmPortableTemporalValueCodec,
    TemporalValueCodec,
    BasicValueCodec
)

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
internal actual fun Any.isBindableScalar(): Boolean = when (this) {
    is KPojo, is Collection<*>, is Map<*, *>, is Sequence<*>, is Enum<*>, is Unit -> false
    is Array<*>, is BooleanArray, is CharArray, is DoubleArray, is FloatArray,
    is IntArray, is LongArray, is ShortArray -> false
    else -> true
}
