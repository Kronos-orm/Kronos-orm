package com.kotlinorm.compiler.helpers

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.types.withNullability
import org.jetbrains.kotlin.ir.util.defaultType

/**
 * Casts the given IrType to an IrSimpleType.
 *
 * @return The IrSimpleType representation of the IrType.
 */
internal fun IrType.asSimpleType() = this as IrSimpleType

/**
 * Returns the first type argument of the given IrType as an IrSimpleType.
 * 返回给定IrType的第一个类型参数
 *
 * @return The first type argument of the given IrType as an IrSimpleType.
 */
internal fun IrType.subType() = asSimpleType().arguments.firstOrNull()?.typeOrFail?.asSimpleType()

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrClassSymbol.nType get() = owner.defaultType.withNullability(true)