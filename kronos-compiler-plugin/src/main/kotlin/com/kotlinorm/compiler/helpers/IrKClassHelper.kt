package com.kotlinorm.compiler.helpers

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith

context(IrBuilderWithScope, IrPluginContext)
fun createKClassExpr(
    klassSymbol: IrClassSymbol
): IrExpression {
    val classType = klassSymbol.defaultType
    return IrClassReferenceImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        type = irBuiltIns.kClassClass.typeWith(classType),
        symbol = klassSymbol,
        classType = classType
    )
}