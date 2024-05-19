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

package com.kotlinorm.plugins.transformer

import com.kotlinorm.plugins.helpers.referenceFunctions
import com.kotlinorm.plugins.transformer.criteria.CriteriaParseReturnTransformer
import com.kotlinorm.plugins.transformer.kTable.KTableAddFieldTransformer
import com.kotlinorm.plugins.transformer.kTable.KTableAddParamTransformer
import com.kotlinorm.plugins.transformer.kTable.KTableSortableParseReturnTransformer
import com.kotlinorm.plugins.utils.kTable.KTABLE_CLASS
import com.kotlinorm.plugins.utils.kTableConditional.KTABLE_CONDITIONAL_CLASS
import com.kotlinorm.plugins.utils.kTableSortType.KTABLE_SORTABLE_CLASS
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
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
            KTABLE_CLASS -> declaration.body = transformKTable(declaration)
            KTABLE_CONDITIONAL_CLASS -> declaration.body = transformKTableConditional(declaration)
            KTABLE_SORTABLE_CLASS -> declaration.body = transformKTableSortable(declaration)
        }
        return super.visitFunctionNew(declaration)
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (declaration.superTypes.any { it.classFqName?.asString() == "com.kotlinorm.beans.dsl.KPojo" }) {
            return super.visitClassNew(declaration)
                .transform(KronosIrClassNewTransformer(pluginContext, declaration), null) as IrStatement
        }
        return super.visitClassNew(declaration)
    }

    /**
     * Transforms the given IrFunction representing a ktable declaration into an IrBlockBody.
     *
     * @param irFunction the [IrFunction] to be transformed
     * @return the transformed IrBlockBody representing the ktable declaration
     */
    private fun transformKTable(
        irFunction: IrFunction
    ): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlockBody {
            +irFunction.body!!
                .transform(KTableAddFieldTransformer(pluginContext, irFunction), null)
                .transform(KTableAddParamTransformer(pluginContext, irFunction), null)
                .statements
        }
    }

    /**
     * Transforms the given IrFunction representing a ktable conditional declaration into an IrBlockBody.
     *
     * @param irFunction the [IrFunction] to be transformed
     * @return the transformed IrBlockBody representing the ktable conditional declaration
     */
    private fun transformKTableConditional(
        irFunction: IrFunction
    ): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlockBody {
            +irFunction.body!!.transform(CriteriaParseReturnTransformer(pluginContext, irFunction), null)
                .statements
        }
    }

    private fun transformKTableSortable(
        irFunction: IrFunction
    ): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlockBody {
            +irFunction.body!!
                .transform(KTableSortableParseReturnTransformer(pluginContext, irFunction), null)
                .statements
        }
    }
}