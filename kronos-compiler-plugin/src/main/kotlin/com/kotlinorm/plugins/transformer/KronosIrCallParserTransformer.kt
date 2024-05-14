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

import com.kotlinorm.plugins.helpers.asIrCall
import com.kotlinorm.plugins.helpers.subType
import com.kotlinorm.plugins.utils.deleteClause.initDeleteClause
import com.kotlinorm.plugins.utils.deleteClause.initDeleteClauseList
import com.kotlinorm.plugins.utils.insertClause.initInsertClause
import com.kotlinorm.plugins.utils.insertClause.initInsertClauseList
import com.kotlinorm.plugins.utils.kTableConditional.funcName
import com.kotlinorm.plugins.utils.selectClause.initSelectClause
import com.kotlinorm.plugins.utils.selectClause.initSelectClauseList
import com.kotlinorm.plugins.utils.updateClause.initUpdateClause
import com.kotlinorm.plugins.utils.updateClause.initUpdateClauseList
import com.kotlinorm.plugins.utils.upsertClause.initUpsertClause
import com.kotlinorm.plugins.utils.upsertClause.initUpsertClauseList
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.name.FqName

/**
 * Kronos Parser Transformer
 *
 * @author OUSC, Jieyao Lu
 */
class KronosIrCallParserTransformer(
    private val pluginContext: IrPluginContext,
    private val irFunction: IrFunction
) : IrElementTransformerVoidWithContext() {
    private val selectClauseClass = "com.kotlinorm.orm.select.SelectClause"
    private val updateClauseClass = "com.kotlinorm.orm.update.UpdateClause"
    private val insertClauseClass = "com.kotlinorm.orm.insert.InsertClause"
    private val upsertClauseClass = "com.kotlinorm.orm.upsert.UpsertClause"
    private val deleteClauseClass = "com.kotlinorm.orm.delete.DeleteClause"

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
                fqName == selectClauseClass &&
                        expression.funcName() == "select" -> {
                    return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                        initSelectClause(super.visitCall(expression).asIrCall())
                    }
                }

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
                        subTypeFqName == selectClauseClass &&
                                expression.funcName() == "select" -> {
                            return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                                    initSelectClauseList(super.visitCall(expression).asIrCall())
                            }
                        }

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
}