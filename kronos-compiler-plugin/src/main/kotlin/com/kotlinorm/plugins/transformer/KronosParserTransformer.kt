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

import com.kotlinorm.plugins.transformer.criteria.CriteriaParseReturnTransformer
import com.kotlinorm.plugins.transformer.kTable.KTableAddFieldTransformer
import com.kotlinorm.plugins.transformer.kTable.KTableAddParamTransformer
import com.kotlinorm.plugins.utils.asIrCall
import com.kotlinorm.plugins.utils.deleteClause.initDeleteClause
import com.kotlinorm.plugins.utils.deleteClause.initDeleteClauseList
import com.kotlinorm.plugins.utils.insertClause.initInsertClause
import com.kotlinorm.plugins.utils.insertClause.initInsertClauseList
import com.kotlinorm.plugins.utils.kTableConditional.funcName
import com.kotlinorm.plugins.utils.subType
import com.kotlinorm.plugins.utils.updateClause.initUpdateClause
import com.kotlinorm.plugins.utils.updateClause.initUpdateClauseList
import com.kotlinorm.plugins.utils.upsertClause.initUpsertClause
import com.kotlinorm.plugins.utils.upsertClause.initUpsertClauseList
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.name.FqName

/**
 * Kronos Parser Transformer
 *
 * @author OUSC, Jieyao Lu
 */
class KronosParserTransformer(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
    private val kTableClass = "com.kotlinorm.beans.dsl.KTable"
    private val updateClauseClass = "com.kotlinorm.orm.update.UpdateClause"
    private val insertClauseClass = "com.kotlinorm.orm.insert.InsertClause"
    private val upsertClauseClass = "com.kotlinorm.orm.upsert.UpsertClause"
    private val deleteClauseClass = "com.kotlinorm.orm.delete.DeleteClause"
    private val kTableConditionalClass = "com.kotlinorm.beans.dsl.KTableConditional"

    /**
     * Retrieves the symbol of the `println` function from the `kotlin.io` package in the given `IrPluginContext`.
     *
     * @return The symbol of the `println` function.
     */
    @OptIn(FirIncompatiblePluginAPI::class)
    fun IrPluginContext.printlnFunc(): IrSimpleFunctionSymbol = referenceFunctions(FqName("kotlin.io.println")).single {
        val parameters = it.owner.valueParameters
        parameters.size == 1 && parameters[0].type == irBuiltIns.anyNType
    }

    /**
     * Visits a new function and performs different actions based on the extension receiver's return type.
     *
     * @param declaration the [IrFunction] being visited
     * @return the transformed function body or the result of calling the super class's implementation
     */
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        val fqName = declaration.extensionReceiverParameter?.symbol?.descriptor?.returnType?.getKotlinTypeFqName(false)
        when (declaration.extensionReceiverParameter?.symbol?.descriptor?.returnType?.getKotlinTypeFqName(false)) {
            kTableClass -> {
                declaration.body = transformKTable(declaration)
            }

            kTableConditionalClass -> {
                declaration.body = transformKTableConditional(declaration)
            }
        }
        return super.visitFunctionNew(declaration)
    }

    /**
     * Visits a call expression and returns an IrExpression.
     *
     * @param expression the [IrCall] expression to visit
     * @return the transformed IrExpression
     */
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        with(pluginContext) {
            val fqName = expression.symbol.descriptor.returnType?.getKotlinTypeFqName(false)
            when {
                fqName == updateClauseClass &&
                        expression.funcName() in listOf("update", "updateExcept") -> {
                    return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                        initUpdateClause(super.visitCall(expression).asIrCall())
                    }
                }

                fqName == insertClauseClass &&
                        expression.funcName() == "insert" -> {
                    return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                        initInsertClause(super.visitCall(expression).asIrCall())
                    }
                }

                fqName == upsertClauseClass &&
                        expression.funcName() in listOf("upsert", "upsertExcept") -> {
                    return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                        initUpsertClause(super.visitCall(expression).asIrCall())
                    }
                }

                fqName == deleteClauseClass &&
                        expression.funcName() == "delete" -> {
                    return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                        initDeleteClause(super.visitCall(expression).asIrCall())
                    }
                }

                fqName == "kotlin.collections.List" -> {
                    val subTypeFqName =
                        expression.type.subType().kotlinType?.getKotlinTypeFqName(false)
                    when {
                        subTypeFqName == updateClauseClass &&
                                expression.funcName() in listOf("update", "updateExcept") -> {
                            return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                                initUpdateClauseList(super.visitCall(expression).asIrCall())
                            }
                        }

                        subTypeFqName == insertClauseClass &&
                                expression.funcName() in listOf("insert") -> {
                            return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                                initInsertClauseList(super.visitCall(expression).asIrCall())
                            }
                        }
                        subTypeFqName == upsertClauseClass &&
                                expression.funcName() in listOf("upsert", "upsertExcept") -> {
                            return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                                initUpsertClauseList(super.visitCall(expression).asIrCall())
                            }
                        }

                        subTypeFqName == deleteClauseClass &&
                                expression.funcName() == "delete" -> {
                            return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                                initDeleteClauseList(super.visitCall(expression).asIrCall())
                            }
                        }
                    }
                }
            }
            return super.visitCall(expression)
        }
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
            +irBlock {
                +irFunction.body!!.statements
            }
                .transform(KTableAddFieldTransformer(pluginContext, irFunction), null)
                .transform(KTableAddParamTransformer(pluginContext, irFunction), null)
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
            +irBlock(resultType = irFunction.returnType) {
                +irFunction.body!!.statements
            }
                .transform(CriteriaParseReturnTransformer(pluginContext, irFunction), null)
        }
    }
}