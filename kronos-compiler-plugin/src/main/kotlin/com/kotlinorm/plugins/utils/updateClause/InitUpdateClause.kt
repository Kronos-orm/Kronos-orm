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

package com.kotlinorm.plugins.utils.updateClause

import com.kotlinorm.plugins.utils.*
import com.kotlinorm.plugins.utils.kTable.getColumnName
import com.kotlinorm.plugins.utils.kTable.getTableName
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.name.FqName

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val initUpdateClauseSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.orm.update.initUpdateClause"))
        .first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val initUpdateClauseListSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.orm.update.initUpdateClauseList"))
        .first()


context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val fieldSymbol
    get() = referenceClass(FqName("com.kotlinorm.beans.dsl.Field"))!!

/**
 * Initializes an update clause for the given IrCall expression.
 *
 * @param expression the [IrCall] expression representing the update clause
 * @return the initialized IrFunctionAccessExpression
 */
context(IrBuilderWithScope, IrPluginContext)
fun initUpdateClause(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.type.subType().getClass()!!
    val updateTimeStrategy =
        getValidStrategy(irClass, globalUpdateTimeSymbol, UpdateTimeFqName)
    val logicDeleteStrategy =
        getValidStrategy(irClass, globalLogicDeleteSymbol, LogicDeleteFqName)
    return applyIrCall(
        initUpdateClauseSymbol,
        expression,
        getTableName(irClass),
        updateTimeStrategy,
        logicDeleteStrategy,
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().sortedBy { it.name }.map { getColumnName(it) }
        )
    )
}

/**
 * Initializes an update clause list for the given IrCall expression.
 *
 * @param expression the [IrCall] expression representing the update clause list
 * @return the initialized IrFunctionAccessExpression
 */
context(IrBuilderWithScope, IrPluginContext)
fun initUpdateClauseList(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.type.subType().subType().getClass()!!
    val updateTimeStrategy =
        getValidStrategy(irClass, globalUpdateTimeSymbol, UpdateTimeFqName)
    val logicDeleteStrategy =
        getValidStrategy(irClass, globalLogicDeleteSymbol, LogicDeleteFqName)
    return applyIrCall(
        initUpdateClauseListSymbol,
        expression,
        getTableName(irClass),
        updateTimeStrategy,
        logicDeleteStrategy,
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().sortedBy { it.name }.map { getColumnName(it) }
        )
    )
}