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
import com.kotlinorm.plugins.helpers.referenceFunctions
import com.kotlinorm.plugins.helpers.subType
import com.kotlinorm.plugins.utils.deleteClause.DELETE_CLAUSE_CLASS
import com.kotlinorm.plugins.utils.deleteClause.DELETE_FUNCTION
import com.kotlinorm.plugins.utils.deleteClause.initDeleteClause
import com.kotlinorm.plugins.utils.deleteClause.initDeleteClauseList
import com.kotlinorm.plugins.utils.insertClause.INSERT_CLAUSE_CLASS
import com.kotlinorm.plugins.utils.insertClause.INSERT_FUNCTION
import com.kotlinorm.plugins.utils.insertClause.initInsertClause
import com.kotlinorm.plugins.utils.insertClause.initInsertClauseList
import com.kotlinorm.plugins.utils.kTable.COLLECTION_CLASSES
import com.kotlinorm.plugins.utils.kTableConditional.funcName
import com.kotlinorm.plugins.utils.selectClause.SELECT_CLAUSE_CLASS
import com.kotlinorm.plugins.utils.selectClause.SELECT_FUNCTION
import com.kotlinorm.plugins.utils.selectClause.initSelectClause
import com.kotlinorm.plugins.utils.selectClause.initSelectClauseList
import com.kotlinorm.plugins.utils.updateClause.*
import com.kotlinorm.plugins.utils.upsertClause.*
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classFqName

/**
 * Kronos Parser Transformer
 *
 * @author OUSC, Jieyao Lu
 */
class KronosIrCallParserTransformer(
    private val pluginContext: IrPluginContext
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
     * Visits a call expression and returns an IrExpression.
     *
     * @param expression the [IrCall] expression to visit
     * @return the transformed IrExpression
     */
    override fun visitCall(expression: IrCall): IrExpression {
        with(pluginContext) {
            when (expression.type.classFqName?.asString()) {
                SELECT_CLAUSE_CLASS -> {
                    if (expression.funcName() == SELECT_FUNCTION)
                    return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                        initSelectClause(super.visitCall(expression).asIrCall())
                    }
                }

                UPDATE_CLAUSE_CLASS -> {
                    if (expression.funcName() in listOf(UPDATE_FUNCTION, UPDATE_EXCEPT_FUNCTION))
                    return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                        initUpdateClause(super.visitCall(expression).asIrCall())
                    }
                }

                INSERT_CLAUSE_CLASS -> {
                    if (expression.funcName() == INSERT_FUNCTION)
                    return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                        initInsertClause(super.visitCall(expression).asIrCall())
                    }
                }

                UPSERT_CLAUSE_CLASS -> {
                    if (expression.funcName() in listOf(UPSERT_FUNCTION, UPSERT_EXCEPT_FUNCTION))
                        return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                            initUpsertClause(super.visitCall(expression).asIrCall())
                        }
                }

                DELETE_CLAUSE_CLASS -> {
                    if (expression.funcName() == DELETE_FUNCTION)
                    return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                        initDeleteClause(super.visitCall(expression).asIrCall())
                    }
                }

                in COLLECTION_CLASSES -> {
                    val subTypeFqName =
                        expression.type.subType().classFqName?.asString()
                    when (subTypeFqName) {
                        SELECT_CLAUSE_CLASS -> {
                            if (expression.funcName() == SELECT_FUNCTION)
                            return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                                    initSelectClauseList(super.visitCall(expression).asIrCall())
                            }
                        }

                        UPDATE_CLAUSE_CLASS -> {
                            if (expression.funcName() in listOf(UPDATE_FUNCTION, UPDATE_EXCEPT_FUNCTION))
                            return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                                    initUpdateClauseList(super.visitCall(expression).asIrCall())
                            }
                        }

                        INSERT_CLAUSE_CLASS -> {
                            if (expression.funcName() == INSERT_FUNCTION)
                            return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                                initInsertClauseList(super.visitCall(expression).asIrCall())
                            }
                        }

                        UPSERT_CLAUSE_CLASS -> {
                            if (expression.funcName() in listOf(UPSERT_FUNCTION, UPSERT_EXCEPT_FUNCTION))
                            return with(DeclarationIrBuilder(pluginContext, expression.symbol)) {
                                initUpsertClauseList(super.visitCall(expression).asIrCall())
                            }
                        }

                        DELETE_CLAUSE_CLASS -> {
                            if (expression.funcName() == DELETE_FUNCTION)
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