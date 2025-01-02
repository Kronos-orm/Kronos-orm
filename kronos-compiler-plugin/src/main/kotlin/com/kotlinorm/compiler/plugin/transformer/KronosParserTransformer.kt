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

package com.kotlinorm.compiler.plugin.transformer

import com.kotlinorm.compiler.plugin.transformer.kTable.KTableParserForConditionTransformer
import com.kotlinorm.compiler.plugin.transformer.kTable.KTableParserForReferenceTransformer
import com.kotlinorm.compiler.plugin.transformer.kTable.KTableParserForSelectTransformer
import com.kotlinorm.compiler.plugin.transformer.kTable.KTableParserForSetTransformer
import com.kotlinorm.compiler.plugin.transformer.kTable.KTableParserForSortReturnTransformer
import com.kotlinorm.compiler.plugin.utils.KClassCreatorUtil.kPojoClasses
import com.kotlinorm.compiler.plugin.utils.KPojoFqName
import com.kotlinorm.compiler.plugin.utils.context.withBlock
import com.kotlinorm.compiler.plugin.utils.fqNameOfSelectFromsRegexes
import com.kotlinorm.compiler.plugin.utils.fqNameOfTypedQuery
import com.kotlinorm.compiler.plugin.utils.kTableForCondition.KTABLE_FOR_CONDITION_CLASS
import com.kotlinorm.compiler.plugin.utils.kTableForReference.KTABLE_FOR_REFERENCE_CLASS
import com.kotlinorm.compiler.plugin.utils.kTableForSelect.KTABLE_FOR_SELECT_CLASS
import com.kotlinorm.compiler.plugin.utils.kTableForSet.KTABLE_FOR_SET_CLASS
import com.kotlinorm.compiler.plugin.utils.kTableForSort.KTABLE_FOR_SORT_CLASS
import com.kotlinorm.compiler.plugin.utils.updateTypedQueryParameters
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.typeArguments
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.superTypes

/**
 * Kronos Parser Transformer
 *
 * @author OUSC, Jieyao Lu
 */
class KronosParserTransformer(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.typeArgumentsCount > 0) {
            with(pluginContext) {
                expression.typeArguments.forEach {
                    if (it != null && it.superTypes().any { it.classFqName == KPojoFqName }) {
                        kPojoClasses.add(it.getClass()!!)
                    }
                }
            }
        }
        return transformKQueryTask(expression) {
            super.visitCall(expression)
        }
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
            KTABLE_FOR_REFERENCE_CLASS -> declaration.body = transformKTableForReference(declaration)
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
                kPojoClasses.add(declaration)
                return super.visitClassNew(declaration)
                    .transform(KronosIrClassNewTransformer(pluginContext, declaration), null) as IrStatement
            }
        }
        return super.visitClassNew(declaration)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitClassReference(expression: IrClassReference): IrExpression {
        with(pluginContext) {
            val declaration = expression.classType.classOrNull?.owner
            if (declaration != null && declaration.superTypes.any { it.classFqName == KPojoFqName }) {
                kPojoClasses.add(declaration)
            }
        }
        return super.visitClassReference(expression)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        with(pluginContext) {
            val declaration = expression.type.classOrNull?.owner
            if (declaration != null && declaration.superTypes.any { it.classFqName == KPojoFqName }) {
                kPojoClasses.add(declaration)
            }
        }
        return super.visitConstructorCall(expression)
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

    /**
     * Transforms the given IrFunction representing a ktable reference declaration into an IrBlockBody.
     *
     * @param irFunction the IrFunction to be transformed
     * @return the transformed IrBlockBody representing the ktable reference declaration
     */
    private fun transformKTableForReference(
        irFunction: IrFunction
    ): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, irFunction.symbol).irBlockBody {
            +irBlock(resultType = irFunction.returnType) {
                +irFunction.body!!.statements
            }
                .transform(KTableParserForReferenceTransformer(pluginContext, irFunction), null)
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun transformKQueryTask(expression: IrCall, finally: () -> IrExpression): IrExpression {
        val fqNameOfIrCall = expression.symbol.owner.kotlinFqName
        if (
            (fqNameOfIrCall in fqNameOfTypedQuery ||
            fqNameOfSelectFromsRegexes.any { Regex(it).matches(fqNameOfIrCall.asString()) }) &&
            expression.typeArgumentsCount == 1
        ) {
            return DeclarationIrBuilder(pluginContext, expression.symbol).irBlock {
                withBlock(pluginContext) {
                    +updateTypedQueryParameters(expression)
                }
                finally()
            }
        }
        return finally()
    }
}