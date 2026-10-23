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

package com.kotlinorm.compiler.core

import com.kotlinorm.compiler.utils.irListOf
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.AbstractIrTypeSubstitutor
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.types.Variance

private const val ARRAY_TYPE_KEY = "kotlin.Array"
private const val ITERABLE_TYPE_KEY = "kotlin.collections.Iterable"
private const val MAP_TYPE_KEY = "kotlin.collections.Map"
private const val TYPE_INFO_ELEMENT_TYPE_INDEX = 3
private const val TYPE_INFO_KPOJO_INDEX = 4
private const val TYPE_INFO_ENUM_KEY_INDEX = 5
private const val TYPE_INFO_VARIANCES_INDEX = 6

private data class StaticContainerShape(
    val entryName: String,
    val elementType: IrType? = null
)

/**
 * Builds the common static type descriptor emitted with every generated property Field.
 *
 * Keys are literals derived from compiler declarations only. No runtime class, Java type,
 * or KType rendering is used in generated metadata.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun buildStaticTypeInfo(type: IrType): IrExpression? {
    val simpleType = type as? IrSimpleType ?: return null
    val typeRef = buildStaticTypeRef(simpleType) ?: return null
    val directSupertypes = simpleType.substitutedDirectSupertypes().map {
        buildStaticTypeRef(it) ?: return null
    }
    val shape = simpleType.staticContainerShape()
    val elementType = shape.elementType?.let { buildStaticTypeRef(it) }
    val enumKey = simpleType.classOrNull
        ?.owner
        ?.takeIf { it.kind == ClassKind.ENUM_CLASS }
        ?.staticClassifierKey()
    val declarationVariances = simpleType.classOrNull
        ?.owner
        ?.typeParameters
        ?.map { builder.staticVariance(it.variance) }
        .orEmpty()

    return builder.irCall(staticTypeInfoConstructorSymbol).apply {
        arguments[0] = typeRef
        arguments[1] = irListOf(staticTypeRefClassSymbol.defaultType, directSupertypes)
        arguments[2] = builder.staticEnumValue(staticContainerKindSymbol, shape.entryName)
        arguments[TYPE_INFO_ELEMENT_TYPE_INDEX] = elementType ?: builder.irNull()
        arguments[TYPE_INFO_KPOJO_INDEX] = builder.irBoolean(simpleType.isKPojoType())
        arguments[TYPE_INFO_ENUM_KEY_INDEX] = enumKey?.let(builder::irString) ?: builder.irNull()
        arguments[TYPE_INFO_VARIANCES_INDEX] = irListOf(staticTypeVarianceSymbol.defaultType, declarationVariances)
    }
}

/**
 * Collects the complete, statically describable type graph reachable from [roots].
 *
 * Every edge uses the source type's substituted direct supertypes. Types without a
 * stable compiler declaration key are intentionally omitted instead of embedding
 * source offsets or runtime-derived identities in generated code.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun collectStaticTypeMetadataTypes(roots: Iterable<IrType>): List<IrSimpleType> {
    val collected = linkedMapOf<String, IrSimpleType>()

    fun collect(type: IrType) {
        val simpleType = type as? IrSimpleType ?: return
        val signature = simpleType.staticReferenceSignature(normalizeRootNullability = true) ?: return
        if (collected.putIfAbsent(signature, simpleType) != null) return

        simpleType.arguments.forEach { argument ->
            (argument as? IrTypeProjection)?.type?.let(::collect)
        }
        simpleType.substitutedDirectSupertypes().forEach(::collect)
    }

    roots.forEach(::collect)
    return collected.entries.sortedBy { it.key }.map { it.value }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildStaticTypeRef(type: IrType): IrExpression? {
    val simpleType = type as? IrSimpleType ?: return null
    val key = simpleType.staticClassifierKey() ?: return null
    val typeArguments = simpleType.arguments.map { argument ->
        buildStaticTypeArgument(argument) ?: return null
    }

    return builder.irCall(staticTypeRefConstructorSymbol).apply {
        this.arguments[0] = builder.irString(key)
        this.arguments[1] = builder.irBoolean(simpleType.nullability == SimpleTypeNullability.MARKED_NULLABLE)
        this.arguments[2] = irListOf(staticTypeArgumentClassSymbol.defaultType, typeArguments)
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
private fun buildStaticTypeArgument(argument: IrTypeArgument): IrExpression? {
    val entryName: String
    val typeRef: IrExpression?
    when (argument) {
        is IrStarProjection -> {
            entryName = "STAR"
            typeRef = null
        }

        is IrTypeProjection -> {
            entryName = when (argument.variance) {
                Variance.INVARIANT -> "INVARIANT"
                Variance.IN_VARIANCE -> "IN"
                Variance.OUT_VARIANCE -> "OUT"
            }
            typeRef = buildStaticTypeRef(argument.type) ?: return null
        }
    }

    return builder.irCall(staticTypeArgumentConstructorSymbol).apply {
        arguments[0] = builder.staticEnumValue(staticTypeVarianceSymbol, entryName)
        if (typeRef != null) arguments[1] = typeRef
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
private fun IrSimpleType.substitutedDirectSupertypes(): List<IrType> {
    val substitutor = AbstractIrTypeSubstitutor.forType(this)
    return classifier.superTypes().map(substitutor::substitute)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
private fun IrSimpleType.staticContainerShape(): StaticContainerShape {
    val classSymbol = classOrNull
    val primitiveElement = classSymbol?.let { symbol ->
        context.irBuiltIns.primitiveArrayElementTypes[symbol]
            ?: context.irBuiltIns.unsignedArraysElementTypes[symbol]
    }
    if (primitiveElement != null) {
        return StaticContainerShape("PRIMITIVE_ARRAY", primitiveElement)
    }
    if (classSymbol?.owner?.fqNameWhenAvailable?.asString() == ARRAY_TYPE_KEY) {
        return StaticContainerShape("ARRAY", typeArgumentOrNull(0))
    }

    findSubstitutedSupertype(MAP_TYPE_KEY)?.let {
        return StaticContainerShape("MAP")
    }
    findSubstitutedSupertype(ITERABLE_TYPE_KEY)?.let { iterableType ->
        return StaticContainerShape("ITERABLE", iterableType.typeArgumentOrNull(0))
    }
    return StaticContainerShape("NONE")
}

private fun IrSimpleType.typeArgumentOrNull(index: Int): IrType? =
    (arguments.getOrNull(index) as? IrTypeProjection)?.type

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrSimpleType.findSubstitutedSupertype(targetKey: String): IrSimpleType? {
    val visited = mutableSetOf<IrClassSymbol>()

    fun visit(current: IrSimpleType): IrSimpleType? {
        val classSymbol = current.classOrNull ?: return null
        if (classSymbol.owner.fqNameWhenAvailable?.asString() == targetKey) return current
        if (!visited.add(classSymbol)) return null

        val substitutor = AbstractIrTypeSubstitutor.forType(current)
        return classSymbol.superTypes()
            .asSequence()
            .mapNotNull { substitutor.substitute(it) as? IrSimpleType }
            .mapNotNull(::visit)
            .firstOrNull()
    }

    return visit(this)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrSimpleType.staticClassifierKey(): String? = when (val classifier = classifier) {
    is IrClassSymbol -> classifier.owner.staticClassifierKey()
    is IrTypeParameterSymbol -> classifier.owner.staticTypeParameterKey()
    else -> null
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClass.staticClassifierKey(): String? = fqNameWhenAvailable?.asString()

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrTypeParameter.staticTypeParameterKey(): String? {
    val owner = parent
    val ownerKey = when (owner) {
        is IrClass -> owner.fqNameWhenAvailable?.asString()
        is IrFunction -> owner.fqNameWhenAvailable?.asString()
        else -> null
    } ?: return null
    val index = (owner as? IrTypeParametersContainer)?.typeParameters?.indexOf(this) ?: return null
    return "type-parameter:$ownerKey:${name.asString()}:$index"
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrSimpleType.staticReferenceSignature(normalizeRootNullability: Boolean = false): String? {
    val key = staticClassifierKey() ?: return null
    return buildString {
        append(key.length)
        append(':')
        append(key)
        append(
            if (!normalizeRootNullability && nullability == SimpleTypeNullability.MARKED_NULLABLE) '?' else '!'
        )
        append('<')
        this@staticReferenceSignature.arguments.forEachIndexed { index, argument ->
            if (index > 0) append(',')
            when (argument) {
                is IrStarProjection -> append('*')
                is IrTypeProjection -> {
                    append(
                        when (argument.variance) {
                            Variance.INVARIANT -> '='
                            Variance.IN_VARIANCE -> '-'
                            Variance.OUT_VARIANCE -> '+'
                        }
                    )
                    val nestedSignature = (argument.type as? IrSimpleType)
                        ?.staticReferenceSignature()
                        ?: return null
                    append(nestedSignature)
                }
            }
        }
        append('>')
    }
}

context(context: IrPluginContext)
private fun IrBuilderWithScope.staticVariance(variance: Variance): IrExpression =
    staticEnumValue(
        staticTypeVarianceSymbol,
        when (variance) {
            Variance.INVARIANT -> "INVARIANT"
            Variance.IN_VARIANCE -> "IN"
            Variance.OUT_VARIANCE -> "OUT"
        }
    )

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrBuilderWithScope.staticEnumValue(
    enumClass: IrClassSymbol,
    entryName: String
): IrExpression {
    val entry = enumClass.owner.declarations
        .filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrEnumEntry>()
        .single { it.name.asString() == entryName }
    return IrGetEnumValueImpl(startOffset, endOffset, enumClass.defaultType, entry.symbol)
}
