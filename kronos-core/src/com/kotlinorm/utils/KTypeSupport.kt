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
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance

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
 * Returns whether this declared KType is a JVM [KPojo] class or a type
 * parameter whose upper bound is one.
 *
 * Runtime KType inspection is retained only for legacy JVM factory validation.
 * Typed mapping must use [StaticTypeInfo.isKPojo] instead of this compatibility
 * helper, because no static metadata is inferred from a KType at runtime.
 *
 * @receiver complete declared type to classify
 * @return `true` when this type is [KPojo] or has a declared KPojo supertype
 */
@InternalKronosApi
fun KType.isKPojoType(): Boolean = isKPojoType(mutableSetOf())

private fun KType.isKPojoType(visited: MutableSet<KClassifier>): Boolean {
    val currentClassifier = classifier ?: return false
    if (!visited.add(currentClassifier)) return false
    return when (currentClassifier) {
        is KClass<*> -> platformIsKPojoClass(currentClassifier)
        is KTypeParameter -> currentClassifier.upperBounds.any { it.isKPojoType(visited) }
        else -> false
    }
}

/**
 * Checks the narrow JVM compatibility subset of KType assignability.
 *
 * Generic relationships are deliberately not inferred from runtime KTypes.
 * Their complete relation is evaluated through
 * [StaticTypeInfo.isStructurallyAssignableTo] when compiler-generated
 * descriptors are available. This helper remains for scalar and legacy JVM
 * factory paths only.
 *
 * @receiver declared or runtime-inferred source type
 * @param target complete declared target type
 * @return whether the source can safely flow to [target]
 */
@Suppress("TooGenericExceptionCaught") // Optional JVM linkage errors are classified by the platform; other Errors propagate.
internal fun KType.isStructurallyAssignableTo(target: KType): Boolean {
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
                    platformClassAssignable(sourceClass, targetClass)
                }
            }
        }
    } catch (_: IllegalArgumentException) {
        false
    } catch (_: ClassCastException) {
        false
    } catch (failure: Error) {
        if (platformIsUnavailableTypeMetadata(failure)) false else throw failure
    }
}

/**
 * Checks complete static type assignability without consulting Kotlin's full
 * reflection implementation.
 *
 * Direct supertype edges are compiler-substituted. The generated provider must
 * therefore supply metadata for every edge reached during traversal; a missing
 * node fails closed instead of falling back to runtime superclass inspection.
 */
internal fun StaticTypeInfo.isStructurallyAssignableTo(target: StaticTypeInfo): Boolean {
    val supplied = linkedMapOf<StaticTypeRef, StaticTypeInfo>().apply {
        put(this@isStructurallyAssignableTo.type.withoutTopLevelNullability(), this@isStructurallyAssignableTo)
        put(target.type.withoutTopLevelNullability(), target)
    }
    return isStructurallyAssignableTo(target) { type ->
        supplied[type.withoutTopLevelNullability()] ?: generatedTypeMetadata().typeInfo(type)
    }
}

/**
 * Pure descriptor relation used by focused tests and platform-neutral callers.
 *
 * The [resolver] receives normalized complete references for each transitive
 * direct-supertype edge. Generic relationships without compiler-emitted
 * declaration variance are rejected unless their complete arguments are equal.
 */
internal fun StaticTypeInfo.isStructurallyAssignableTo(
    target: StaticTypeInfo,
    resolver: (StaticTypeRef) -> StaticTypeInfo?
): Boolean = StaticTypeRelation(resolver).isAssignable(type, target.type, mutableSetOf())

private class StaticTypeRelation(
    private val resolver: (StaticTypeRef) -> StaticTypeInfo?
) {
    fun isAssignable(
        source: StaticTypeRef,
        target: StaticTypeRef,
        visited: MutableSet<Pair<StaticTypeRef, StaticTypeRef>>
    ): Boolean {
        if (source.nullable && !target.nullable) return false
        val normalizedSource = source.withoutTopLevelNullability()
        val normalizedTarget = target.withoutTopLevelNullability()
        val pair = normalizedSource to normalizedTarget
        if (!visited.add(pair)) return false

        if (normalizedSource.key == normalizedTarget.key &&
            argumentsAssignable(normalizedSource, normalizedTarget, visited)
        ) {
            return true
        }

        val sourceInfo = resolver(normalizedSource) ?: return false
        return sourceInfo.directSupertypes.any { supertype ->
            val inherited = if (source.nullable && !supertype.nullable) supertype.copy(nullable = true) else supertype
            isAssignable(inherited, target, visited.toMutableSet())
        }
    }

    private fun argumentsAssignable(
        source: StaticTypeRef,
        target: StaticTypeRef,
        visited: MutableSet<Pair<StaticTypeRef, StaticTypeRef>>
    ): Boolean {
        if (source.arguments == target.arguments) return true
        if (source.arguments.size != target.arguments.size) return false
        val sourceInfo = resolver(source) ?: return false
        if (sourceInfo.declarationVariances.size != source.arguments.size) return false

        return source.arguments.indices.all { index ->
            val sourceArgument = source.arguments[index]
            val targetArgument = target.arguments[index]
            argumentAssignable(
                sourceArgument,
                targetArgument,
                sourceInfo.declarationVariances[index],
                visited
            )
        }
    }

    private fun argumentAssignable(
        source: StaticTypeArgument,
        target: StaticTypeArgument,
        declarationVariance: StaticTypeVariance,
        visited: MutableSet<Pair<StaticTypeRef, StaticTypeRef>>
    ): Boolean {
        if (target.variance == StaticTypeVariance.STAR) return true
        if (source.variance == StaticTypeVariance.STAR ||
            source.variance != StaticTypeVariance.INVARIANT
        ) {
            return false
        }
        val sourceType = requireNotNull(source.type)
        val targetType = requireNotNull(target.type)

        return when (target.variance) {
            StaticTypeVariance.OUT -> declarationVariance != StaticTypeVariance.IN &&
                isAssignable(sourceType, targetType, visited.toMutableSet())

            StaticTypeVariance.IN -> declarationVariance != StaticTypeVariance.OUT &&
                isAssignable(targetType, sourceType, visited.toMutableSet())

            StaticTypeVariance.INVARIANT -> when (declarationVariance) {
                StaticTypeVariance.OUT -> isAssignable(sourceType, targetType, visited.toMutableSet())
                StaticTypeVariance.IN -> isAssignable(targetType, sourceType, visited.toMutableSet())
                StaticTypeVariance.INVARIANT ->
                    isAssignable(sourceType, targetType, visited.toMutableSet()) &&
                        isAssignable(targetType, sourceType, visited.toMutableSet())

                StaticTypeVariance.STAR -> false
            }

            StaticTypeVariance.STAR -> true
        }
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
        is KClass<*> -> classifier.qualifiedName ?: classifier.simpleName ?: "<anonymous>"
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
    require(platformIsEnumClass(classifier) && arguments.isEmpty()) {
        "Enum metadata type must be a concrete enum: $this"
    }
    return KTypeKey.from(this, ignoreTopLevelNullability = true)
}
