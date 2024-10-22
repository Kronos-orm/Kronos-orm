package com.kotlinorm.compiler.helpers

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isVararg


context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
val listOfSymbol
    get() = referenceFunctions(
        "kotlin.collections",
        "listOf"
    ).first { it.owner.valueParameters.size == 1 && it.owner.valueParameters.first().isVararg }

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
val pairSymbol
    get() = referenceClass(
        "kotlin.Pair"
    )!!.constructors.first()

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
val mutableMapOfSymbol
    get() = referenceFunctions(
        "kotlin.collections",
        "mapOf"
    ).first { it.owner.valueParameters.size == 1 && it.owner.valueParameters.first().isVararg }


context(IrBuilderWithScope, IrPluginContext)
fun irListOf(type: IrType, elements: List<IrExpression>) =
    applyIrCall(
        listOfSymbol,
        irVararg(type, elements),
        typeArguments = arrayOf(type)
    )

context(IrBuilderWithScope, IrPluginContext)
fun irListOf(type: IrType, vararg elements: IrExpression) = irListOf(type, elements.toList())

context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun irMutableMapOf(k: IrType, v: IrType, pairs: Map<IrExpression, IrExpression>) =
    applyIrCall(
        mutableMapOfSymbol,
        irVararg(
            pairSymbol.owner.returnType,
            pairs.map {
                applyIrCall(
                    pairSymbol,
                    it.key,
                    it.value,
                    typeArguments = arrayOf(k, v)
                )
            }),
        typeArguments = arrayOf(k, v)
    )

context(IrBuilderWithScope, IrPluginContext)
fun irMutableMapOf(k: IrType, v: IrType, vararg pairs: Pair<IrExpression, IrExpression>) =
    irMutableMapOf(k, v, pairs.toMap())