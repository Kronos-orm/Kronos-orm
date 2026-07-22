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

package com.kotlinorm.utils

import com.kotlinorm.annotations.InternalKronosApi
import com.kotlinorm.exceptions.UnsupportedType
import com.kotlinorm.interfaces.KPojo
import kotlin.jvm.javaObjectType
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance
import kotlin.reflect.full.isSubtypeOf

/**
 * Immutable structural key for KType lookup without collapsing generic
 * arguments, variance or nested nullability. Individual registries may ignore
 * only top-level nullability when their declared contract permits it.
 *
 * @property classifier exact Kotlin classifier, including type-parameter identity
 * @property nullable whether the represented level is nullable
 * @property arguments recursively preserved type projections
 */
internal data class KTypeKey(
    val classifier: KClassifier?,
    val nullable: Boolean,
    val arguments: List<KTypeArgumentKey>
) {
    companion object {
        /**
         * Builds a structural key while optionally normalizing only the root
         * nullability marker.
         *
         * @param type complete type to preserve
         * @param ignoreTopLevelNullability whether the root nullable marker is excluded
         * @return recursive key retaining projections and nested nullability
         */
        fun from(type: KType, ignoreTopLevelNullability: Boolean = false): KTypeKey = KTypeKey(
            type.classifier,
            type.isMarkedNullable && !ignoreTopLevelNullability,
            type.arguments.map { argument ->
                KTypeArgumentKey(
                    argument.variance,
                    argument.type?.let { from(it) }
                )
            }
        )
    }
}

/**
 * Structural representation of one KType argument.
 *
 * A `null` [type] represents a star projection; [variance] is retained so
 * invariant, input and output projections do not share registry keys.
 *
 * @property variance use-site projection variance, or `null` for a star projection
 * @property type recursive argument type, or `null` for a star projection
 */
internal data class KTypeArgumentKey(
    val variance: KVariance?,
    val type: KTypeKey?
)

/**
 * Returns whether this complete declared type is [KPojo] or reaches it through
 * its declared KType supertype graph.
 *
 * Class supertypes and type-parameter upper bounds participate in the graph.
 * Type arguments do not: containing a KPojo does not make the container one.
 *
 * @receiver complete declared type to classify
 * @return `true` when this type is [KPojo] or has a declared KPojo supertype
 */
@InternalKronosApi
fun KType.isKPojoType(): Boolean = isKPojoType(mutableSetOf())

private fun KType.isKPojoType(visited: MutableSet<KClassifier>): Boolean {
    val currentClassifier = classifier ?: return false
    if (currentClassifier == KPojo::class) return true
    if (!visited.add(currentClassifier)) return false
    val declaredSupertypes = when (currentClassifier) {
        is KClass<*> -> currentClassifier.supertypes
        is KTypeParameter -> currentClassifier.upperBounds
        else -> emptyList()
    }
    return declaredSupertypes.any { superType -> superType.isKPojoType(visited) }
}

/**
 * Checks complete KType assignability using Kotlin reflection subtype rules.
 *
 * Declaration-site and use-site variance, star projections, nested
 * nullability and supertype substitution are delegated to [KType.isSubtypeOf].
 * The only fallback normalizes equivalent JVM primitive and boxed classifiers,
 * which can differ when a source type was inferred from a runtime value.
 *
 * @receiver declared or runtime-inferred source type
 * @param target complete declared target type
 * @return whether the source can safely flow to [target]
 */
internal fun KType.isStructurallyAssignableTo(target: KType): Boolean {
    if (isSubtypeOfSafely(target)) return true
    return primitiveBoxingFallbackSafely(target)
}

/**
 * Applies Kotlin reflection subtype semantics without allowing unsupported or
 * unavailable reflection metadata to escape the assignability boundary.
 *
 * Linkage failures represent unavailable type metadata, while argument and
 * cast failures represent synthetic/foreign KType implementations unsupported
 * by Kotlin reflection. Cancellation and unrelated Errors are not caught.
 *
 * @param target complete declared target type
 * @return subtype result, or `false` when reflection cannot resolve the types
 */
private fun KType.isSubtypeOfSafely(target: KType): Boolean = try {
    isSubtypeOf(target)
} catch (_: LinkageError) {
    false
} catch (_: IllegalArgumentException) {
    false
} catch (_: ClassCastException) {
    false
}

/**
 * Checks the narrow runtime primitive/boxed equivalence fallback after Kotlin
 * reflection subtype resolution has failed or returned `false`.
 *
 * @param target complete declared target type
 * @return whether both non-generic types represent the same boxed JVM class
 */
private fun KType.primitiveBoxingFallbackSafely(target: KType): Boolean {
    return try {
        when {
            arguments.isNotEmpty() || target.arguments.isNotEmpty() -> false
            isMarkedNullable && !target.isMarkedNullable -> false
            else -> {
                val sourceClass = classifier as? KClass<*>
                val targetClass = target.classifier as? KClass<*>
                if (sourceClass == null || targetClass == null) {
                    false
                } else {
                    val crossesPrimitiveBoundary = sourceClass.java.isPrimitive || targetClass.java.isPrimitive
                    crossesPrimitiveBoundary && sourceClass.javaObjectType == targetClass.javaObjectType
                }
            }
        }
    } catch (_: LinkageError) {
        false
    } catch (_: IllegalArgumentException) {
        false
    } catch (_: ClassCastException) {
        false
    }
}

/**
 * Returns a stable diagnostic signature for deterministic provider conflict
 * comparison without using rendered KType text as a registry key.
 *
 * @receiver structural type key to render
 * @return deterministic classifier, projection and nullability signature
 */
internal fun KTypeKey.stableSignature(): String = buildString {
    val classifierName = when (val classifier = classifier) {
        is KClass<*> -> classifier.qualifiedName ?: classifier.java.name
        null -> "*"
        else -> classifier.toString()
    }
    append(classifierName)
    if (arguments.isNotEmpty()) {
        arguments.joinTo(this, prefix = "<", postfix = ">") { argument ->
            val type = argument.type ?: return@joinTo "*"
            when (argument.variance) {
                KVariance.IN -> "in ${type.stableSignature()}"
                KVariance.OUT -> "out ${type.stableSignature()}"
                KVariance.INVARIANT, null -> type.stableSignature()
            }
        }
    }
    if (nullable) append('?')
}

/**
 * Validates and normalizes a concrete, currently non-generic KPojo lookup key.
 *
 * @receiver requested KPojo type
 * @return exact structural key ignoring only top-level nullability
 * @throws UnsupportedType when the classifier is not a supported KPojo
 */
internal fun KType.normalizedKPojoType(): KTypeKey {
    if (classifier !is KClass<*>) throw UnsupportedType(this, "classifier must be a concrete class")
    if (!isKPojoType()) throw UnsupportedType(this, "classifier must implement KPojo")
    if (arguments.isNotEmpty()) throw UnsupportedType(this, "generic KPojo types are not supported yet")
    return KTypeKey.from(this, ignoreTopLevelNullability = true)
}

/**
 * Validates and normalizes one concrete enum metadata lookup key.
 *
 * @receiver requested enum type
 * @return exact structural key ignoring only top-level nullability
 * @throws IllegalArgumentException when the classifier is not a concrete enum
 */
internal fun KType.normalizedEnumType(): KTypeKey {
    val classifier = classifier as? KClass<*> ?: throw IllegalArgumentException(
        "Enum metadata type must have a concrete classifier: $this"
    )
    require(classifier.java.isEnum && arguments.isEmpty()) {
        "Enum metadata type must be a concrete enum: $this"
    }
    return KTypeKey.from(this, ignoreTopLevelNullability = true)
}
