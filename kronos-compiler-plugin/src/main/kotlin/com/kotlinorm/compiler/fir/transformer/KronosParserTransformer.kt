/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.compiler.fir.transformer

import com.kotlinorm.compiler.helpers.referenceFunctions
import com.kotlinorm.compiler.fir.transformer.kTable.KTableParserForConditionTransformer
import com.kotlinorm.compiler.fir.transformer.kTable.KTableParserForSelectTransformer
import com.kotlinorm.compiler.fir.transformer.kTable.KTableParserForSetTransformer
import com.kotlinorm.compiler.fir.transformer.kTable.KTableParserForSortReturnTransformer
import com.kotlinorm.compiler.fir.utils.KPojoFqName
import com.kotlinorm.compiler.fir.utils.kTableForCondition.KTABLE_FOR_CONDITION_CLASS
import com.kotlinorm.compiler.fir.utils.kTableForSelect.KTABLE_FOR_SELECT_CLASS
import com.kotlinorm.compiler.fir.utils.kTableForSet.KTABLE_FOR_SET_CLASS
import com.kotlinorm.compiler.fir.utils.kTableForSort.KTABLE_FOR_SORT_CLASS
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.statements

/**
 * Kronos Parser Transformer
 *
 * @author OUSC, Jieyao Lu
 */
class KronosParserTransformer(
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoidWithContext() {

    /**
     * Retrieves the symbol of the `println` function from the `kotlin.io` package in the given `IrPluginContext`.
     *
     * @return The symbol of the `println` function.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrPluginContext.printlnFunc(): IrSimpleFunctionSymbol = referenceFunctions("kotlin.io", "println").single {
        val parameters = it.owner.valueParameters
        parameters.size == 1 && parameters[0].type == irBuiltIns.anyNType
    }

    /**
     * Visits a new function and performs different actions based on the extension receiver's return type.
     *
     * @param declaration the [IrFunction] being visited
     * @return the transformed function body or the result of calling the super class's implementation
     */
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        when (declaration.extensionReceiverParameter?.type?.classFqName?.asString()) {
            KTABLE_FOR_SELECT_CLASS -> declaration.body = transformKTableForSelect(declaration)
            KTABLE_FOR_SET_CLASS -> declaration.body = transformKTableForSet(declaration)
            KTABLE_FOR_CONDITION_CLASS -> declaration.body = transformKTableForCondition(declaration)
            KTABLE_FOR_SORT_CLASS -> declaration.body = transformKTableForSort(declaration)
        }
        return super.visitFunctionNew(declaration)
    }

    /**
     * Visits a new class declaration and performs transformation if it is a subclass of "com.kotlinorm.interfaces.KPojo".
     *
     * @param declaration the class declaration to visit
     * @return the transformed class declaration or the result of calling the super class's implementation
     */
    override fun visitClassNew(declaration: IrClass): IrStatement {
        with(pluginContext) {
            if (declaration.superTypes.any { it.classFqName == KPojoFqName }) {
                return super.visitClassNew(declaration)
                    .transform(KronosIrClassNewTransformer(pluginContext, declaration), null) as IrStatement
            }
        }
        return super.visitClassNew(declaration)
    }

    /**
     * Transforms the given IrFunction representing a ktable declaration into an IrBlockBody.
     *
     * @param irFunction the [IrFunction] to be transformed
     * @return the transformed IrBlockBody representing the ktable declaration
     */
    private fun transformKTableForSelect(
        irFunction: IrFunction
    ): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlockBody {
            +irBlock {
                +irFunction.body!!.statements
            }
                .transform(KTableParserForSelectTransformer(pluginContext, irFunction), null)
        }
    }

    /**
     * Transforms the given IrFunction representing a ktable declaration into an IrBlockBody.
     *
     * @param irFunction the [IrFunction] to be transformed
     * @return the transformed IrBlockBody representing the ktable declaration
     */
    private fun transformKTableForSet(
        irFunction: IrFunction
    ): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlockBody {
            +irBlock {
                +irFunction.body!!.statements
            }
                .transform(KTableParserForSetTransformer(pluginContext, irFunction), null)
        }
    }

    /**
     * Transforms the given IrFunction representing a ktable conditional declaration into an IrBlockBody.
     *
     * @param irFunction the [IrFunction] to be transformed
     * @return the transformed IrBlockBody representing the ktable conditional declaration
     */
    private fun transformKTableForCondition(
        irFunction: IrFunction
    ): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlockBody {
            +irBlock(resultType = irFunction.returnType) {
                +irFunction.body!!.statements
            }.transform(KTableParserForConditionTransformer(pluginContext, irFunction), null)
        }
    }

    /**
     * Transforms the given IrFunction representing a ktable sortable declaration into an IrBlockBody.
     *
     * @param irFunction the IrFunction to be transformed
     * @return the transformed IrBlockBody representing the ktable sortable declaration
     */
    private fun transformKTableForSort(
        irFunction: IrFunction
    ): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlockBody {
            +irBlock(resultType = irFunction.returnType) {
                +irFunction.body!!.statements
            }
                .transform(KTableParserForSortReturnTransformer(pluginContext, irFunction), null)
        }
    }
}