package com.kotlinorm.compiler.helpers

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable


@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrBuilderWithScope.irEnum(irClassSymbol: IrClassSymbol, enumName: String): IrExpression{
    val enumEntries = irClassSymbol.owner.declarations.filterIsInstance<IrEnumEntry>()
    val enumEntry = enumEntries.firstOrNull() { it.name.asString().equals(enumName, true)  } ?: error("Enum entry $enumName not found in ${irClassSymbol.owner.fqNameWhenAvailable!!.asString()}")
    return IrGetEnumValueImpl(startOffset, endOffset, irClassSymbol.defaultType, enumEntry.symbol)
}