/**
 * Copyright 2022-2025 kronos-orm
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

package com.kotlinorm.compiler.helpers

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable

/**
 * Creates an `IrExpression` representing the access to a specific enum entry of an enum class.
 *
 * @param irClassSymbol The symbol of the enum class.
 * @param enumName The name of the enum entry to access.
 * @return An `IrExpression` representing the enum entry access.
 */
@UnsafeDuringIrConstructionAPI
context(builder: IrBuilderWithScope)
fun irEnum(irClassSymbol: IrClassSymbol, enumName: String): IrExpression {
    val enumEntries = irClassSymbol.owner
        .declarations
        .filterIsInstance<IrEnumEntry>()
    val enumEntry = enumEntries.firstOrNull {
        it.name.asString().equals(enumName, true)
    } ?: error("Enum entry $enumName not found in ${irClassSymbol.owner.fqNameWhenAvailable!!.asString()}")
    return IrGetEnumValueImpl(
        builder.startOffset,
        builder.endOffset,
        irClassSymbol.defaultType,
        enumEntry.symbol
    )
}