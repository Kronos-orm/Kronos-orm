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

/**
 * Retrieves the symbol of the `kotlin.FunctionN` class for a given arity `n` in the provided `IrPluginContext`.
 *
 * @param n The arity of the function (number of parameters).
 * @return The symbol of the `kotlin.FunctionN` class.
 */
context(context: IrPluginContext)
fun kFunctionN(n: Int) = context.referenceClass(StandardNames.getFunctionClassId(n))!!

/**
 * Retrieves the symbol of the `println` function from the `kotlin.io` package in the given `IrPluginContext`.
 *
 * @return The symbol of the `println` function.
 */
@UnsafeDuringIrConstructionAPI
context(context: IrPluginContext)
val irPrintln
    get(): IrSimpleFunctionSymbol = referenceFunctions("kotlin.io", "println").single {
        it.owner.parameters.single().type == context.irBuiltIns.anyNType
    }

/**
 * Creates an `IrExpression` representing a reference to the Kotlin class (`KClass`) of the given `IrClassSymbol`.
 *
 * @receiver The `IrClassSymbol` for which to create the `KClass` reference.
 * @return An `IrExpression` representing the `KClass` reference.
 */
context(builder: IrBuilderWithScope, context: IrPluginContext)
fun IrClassSymbol.toKClass(): IrExpression {
    val classType = defaultType
    return IrClassReferenceImpl(
        startOffset = builder.startOffset, endOffset = builder.endOffset,
        type = context.irBuiltIns.kClassClass.typeWith(classType),
        symbol = this,
        classType = classType
    )
}

/**
 * Instantiates an object of the class represented by the `IrClassSymbol` by invoking its primary constructor.
 * If the primary constructor has parameters, they must either have default values or be absent.
 *
 * @receiver The `IrClassSymbol` representing the class to instantiate.
 * @return An `IrExpression` representing the instantiated object, or null if no suitable constructor is found.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(builder: IrBuilderWithScope)
fun IrClassSymbol.instantiate(): IrExpression? {
    return constructors.firstOrNull {
        it.owner.parameters.isEmpty() || it.owner.parameters.all { v -> v.defaultValue != null }
    }?.invoke()
}