package com.kotlinorm.plugins.helpers

import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.types.IrType

class IrGetEnumValueImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var symbol: IrEnumEntrySymbol,
) : IrGetEnumValue()