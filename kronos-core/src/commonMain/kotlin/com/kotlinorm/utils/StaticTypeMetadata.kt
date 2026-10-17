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

package com.kotlinorm.utils

/**
 * Variance attached to a generated type argument or declared type parameter.
 */
enum class StaticTypeVariance {
    INVARIANT,
    IN,
    OUT,
    STAR
}

/**
 * Explicit container classification used when runtime inheritance is unavailable.
 */
enum class StaticContainerKind {
    NONE,
    ITERABLE,
    ARRAY,
    PRIMITIVE_ARRAY,
    MAP
}

/**
 * One generated type argument. A star projection deliberately carries no type.
 */
data class StaticTypeArgument(
    val variance: StaticTypeVariance,
    val type: StaticTypeRef? = null
) {
    init {
        if (variance == StaticTypeVariance.STAR) {
            require(type == null) { "A STAR type argument must not contain a type." }
        } else {
            require(type != null) { "A $variance type argument must contain a type." }
        }
    }
}

/**
 * Platform-neutral reference to a compiler-generated stable type key.
 */
data class StaticTypeRef(
    val key: String,
    val nullable: Boolean,
    val arguments: List<StaticTypeArgument> = emptyList()
) {
    init {
        require(key.isNotBlank()) { "Static type keys must not be blank." }
    }
}

/**
 * Returns this complete type reference with only its outer nullability removed.
 *
 * Nested argument nullability remains part of the reference. Generated metadata
 * registries use this form for lookups whose contract treats `T` and `T?` as
 * the same declaration while retaining every other structural distinction.
 */
fun StaticTypeRef.withoutTopLevelNullability(): StaticTypeRef =
    if (nullable) copy(nullable = false) else this

/**
 * Generated metadata for one concrete type declaration.
 *
 * Direct supertypes are already substituted for [type]. [containerKind] and
 * [elementType] describe iterable and array shape without runtime inheritance;
 * map keys and values remain in [StaticTypeRef.arguments]. [declarationVariances]
 * records the classifier's declaration-site generic variance separately from
 * argument use-site projections.
 */
data class StaticTypeInfo(
    val type: StaticTypeRef,
    val directSupertypes: List<StaticTypeRef> = emptyList(),
    val containerKind: StaticContainerKind = StaticContainerKind.NONE,
    val elementType: StaticTypeRef? = null,
    val isKPojo: Boolean = false,
    val enumKey: String? = null,
    val declarationVariances: List<StaticTypeVariance> = emptyList()
) {
    init {
        require(containerKind != StaticContainerKind.NONE || elementType == null) {
            "A non-container type must not declare an element type."
        }
        require(containerKind != StaticContainerKind.PRIMITIVE_ARRAY || elementType != null) {
            "A primitive array type must declare its element type."
        }
        require(containerKind != StaticContainerKind.MAP || elementType == null) {
            "A map type must describe keys and values through type arguments."
        }
        require(enumKey == null || enumKey.isNotBlank()) {
            "An enum metadata key must not be blank."
        }
        require(declarationVariances.none { it == StaticTypeVariance.STAR }) {
            "Declaration-site variance cannot be STAR."
        }
    }
}

/**
 * Produces a deterministic structural signature without consulting platform reflection.
 */
fun StaticTypeRef.structuralSignature(): String = buildString {
    append(key.length)
    append(':')
    append(key)
    append(if (nullable) '?' else '!')
    append('<')
    arguments.forEachIndexed { index, argument ->
        if (index > 0) append(',')
        append(argument.structuralSignature())
    }
    append('>')
}

/**
 * Produces a deterministic signature for all generated metadata retained by this descriptor.
 */
fun StaticTypeInfo.structuralSignature(): String = buildString {
    append("type=")
    append(type.structuralSignature())
    append("|supertypes=")
    directSupertypes.forEachIndexed { index, supertype ->
        if (index > 0) append(',')
        append(supertype.structuralSignature())
    }
    append("|container=")
    append(containerKind.name)
    append("|element=")
    append(elementType?.structuralSignature() ?: "-")
    append("|kPojo=")
    append(isKPojo)
    append("|enum=")
    append(enumKey?.let { "${it.length}:$it" } ?: "-")
    append("|declarationVariances=")
    declarationVariances.joinTo(this, separator = ",") { it.name }
}

private fun StaticTypeArgument.structuralSignature(): String = when (variance) {
    StaticTypeVariance.STAR -> "*"
    StaticTypeVariance.INVARIANT -> "=${requireNotNull(type).structuralSignature()}"
    StaticTypeVariance.IN -> "-${requireNotNull(type).structuralSignature()}"
    StaticTypeVariance.OUT -> "+${requireNotNull(type).structuralSignature()}"
}
