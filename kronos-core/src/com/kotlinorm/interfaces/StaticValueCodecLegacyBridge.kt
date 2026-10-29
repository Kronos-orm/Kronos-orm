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

package com.kotlinorm.interfaces

import com.kotlinorm.utils.StaticTypeArgument
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.utils.StaticTypeRef
import com.kotlinorm.utils.StaticTypeVariance
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KVariance

/**
 * Compatibility adapter retained for the KType-based [ValueCodec] API.
 *
 * The common registry owns ordering and registration lifetime. This bridge is
 * selected only when the KType conversion path attaches its compatibility
 * context, so Native codecs never need to resolve or carry a KType.
 */
internal class StaticValueCodecLegacyBridge(
    private val codec: ValueCodec
) : StaticValueCodec {
    override val description: String =
        "user ValueCodec ${codec::class.qualifiedName ?: codec::class.simpleName ?: "<anonymous>"}"

    override fun supports(value: Any, context: StaticValueCodecContext): Boolean {
        val legacy = context.platformContext as? LegacyValueCodecPlatformContext ?: return false
        return codec.supports(value, legacy.context)
    }

    override fun convert(value: Any, context: StaticValueCodecContext): Any? {
        val legacy = context.platformContext as? LegacyValueCodecPlatformContext
            ?: error("ValueCodec bridge was invoked without a KType conversion context")
        return codec.convert(value, legacy.context)
    }
}

/** KType compatibility context retained for [StaticValueCodecLegacyBridge]. */
internal data class LegacyValueCodecPlatformContext(
    val context: ValueCodecContext
)

internal fun ValueCodec.asStaticValueCodec(): StaticValueCodec = StaticValueCodecLegacyBridge(this)

internal fun StaticValueCodecRegistration.asValueCodecRegistration(): ValueCodecRegistration =
    object : ValueCodecRegistration {
        override fun close() = this@asValueCodecRegistration.close()
    }

/**
 * Produces a best-effort descriptor only at the KType facade boundary. Compiler
 * metadata remains authoritative when it is available, preserving generic
 * structure without asking common code to inspect KType classifiers.
 */
internal fun KType.toStaticTypeRef(typeInfo: StaticTypeInfo? = null): StaticTypeRef =
    typeInfo?.type?.copy(nullable = isMarkedNullable) ?: StaticTypeRef(
        key = (classifier as? KClass<*>)?.qualifiedName ?: toString().ifBlank { "<unknown>" },
        nullable = isMarkedNullable,
        arguments = arguments.map { argument ->
            val variance = when (argument.variance) {
                KVariance.INVARIANT -> StaticTypeVariance.INVARIANT
                KVariance.IN -> StaticTypeVariance.IN
                KVariance.OUT -> StaticTypeVariance.OUT
                null -> StaticTypeVariance.STAR
            }
            StaticTypeArgument(variance, argument.type?.toStaticTypeRef())
        }
    )
