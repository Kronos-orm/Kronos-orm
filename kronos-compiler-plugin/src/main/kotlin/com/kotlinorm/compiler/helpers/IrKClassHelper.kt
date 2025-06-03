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

import com.kotlinorm.compiler.plugin.utils.context.KotlinBuilderContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors

fun IrPluginContext.kFunctionN(n: Int): IrClassSymbol {
    return referenceClass(StandardNames.getFunctionClassId(n))!!
}

/**
 * Retrieves the symbol of the `println` function from the `kotlin.io` package in the given `IrPluginContext`.
 *
 * @return The symbol of the `println` function.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrPluginContext.irPrintln
    get(): IrSimpleFunctionSymbol = referenceFunctions("kotlin.io", "println").single {
        val parameters = it.owner.valueParameters
        parameters.size == 1 && parameters[0].type == irBuiltIns.anyNType
    }

fun KotlinBuilderContext.createKClassExpr(
    klassSymbol: IrClassSymbol
): IrExpression {
    with(pluginContext){
        with(builder){
            val classType = klassSymbol.defaultType
            return IrClassReferenceImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = irBuiltIns.kClassClass.typeWith(classType),
                symbol = klassSymbol,
                classType = classType
            )
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrBuilderWithScope.createExprNew(
    klassSymbol: IrClassSymbol
): IrExpression? {
    return klassSymbol.constructors.firstOrNull {
        it.owner.valueParameters.isEmpty() || it.owner.valueParameters.all { v -> v.defaultValue != null }
    }?.let {
        applyIrCall(it)
    }
}