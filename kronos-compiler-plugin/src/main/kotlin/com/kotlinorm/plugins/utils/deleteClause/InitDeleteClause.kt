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

package com.kotlinorm.plugins.utils.deleteClause

import com.kotlinorm.plugins.utils.*
import com.kotlinorm.plugins.utils.kTable.getColumnName
import com.kotlinorm.plugins.utils.kTable.getTableName
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val initDeleteClauseSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.orm.delete.initDeleteClause"))
        .first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val initDeleteClauseListSymbol
    get() = referenceFunctions(FqName("com.kotlinorm.orm.delete.initDeleteClauseList"))
        .first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val fieldSymbol
    get() = referenceClass(FqName("com.kotlinorm.beans.dsl.Field"))!!


context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val mutableMapOfSymbol
    get() = referenceFunctions(FqName("kotlin.collections.mutableMapOf")).first()


context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val mutableMapPutSymbol
    get() = referenceClass(FqName("kotlin.collections.MutableMap"))!!.getSimpleFunction("put")!!

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val mutableListOfSymbol
    get() = referenceFunctions(FqName("kotlin.collections.mutableListOf")).first()


context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val mutableListAdd
    get() = referenceClass(FqName("kotlin.collections.MutableList"))!!.getSimpleFunction("add")!!

/**
 * Initializes a delete clause for the given IrCall expression.
 *
 * @param expression the [IrCall] expression representing the delete clause
 * @return the initialized IrFunctionAccessExpression
 */
context(IrBuilderWithScope, IrPluginContext)
fun initDeleteClause(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.type.subType().getClass()!!
    val updateTimeStrategy =
        getValidStrategy(irClass, globalUpdateTimeSymbol, UpdateTimeFqName)
    val logicDeleteStrategy =
        getValidStrategy(irClass, globalLogicDeleteSymbol, LogicDeleteFqName)
    return applyIrCall(
        initDeleteClauseSymbol,
        expression,
        getTableName(irClass),
        updateTimeStrategy,
        logicDeleteStrategy,
        pojo2Map(irClass, expression),
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().sortedBy { it.name }.map { getColumnName(it) }
        )
    )
}

/**
 * Initializes a delete clause list for the given IrCall expression.
 *
 * @param expression the [IrCall] expression representing the delete clause
 * @return the initialized IrFunctionAccessExpression
 */
context(IrBuilderWithScope, IrPluginContext)
fun initDeleteClauseList(expression: IrCall): IrFunctionAccessExpression {
    val irClass = expression.type.subType().subType().getClass()!!
    val updateTimeStrategy =
        getValidStrategy(irClass, globalUpdateTimeSymbol, UpdateTimeFqName)
    val logicDeleteStrategy =
        getValidStrategy(irClass, globalLogicDeleteSymbol, LogicDeleteFqName)
    return applyIrCall(
        initDeleteClauseListSymbol,
        expression,
        getTableName(irClass),
        updateTimeStrategy,
        logicDeleteStrategy,
        pojoList2MapList(irClass, expression),
        irVararg(
            fieldSymbol.defaultType,
            irClass.declarations.filterIsInstance<IrProperty>().sortedBy { it.name }.map { getColumnName(it) }
        )
    )
}

context(IrBuilderWithScope, IrPluginContext)
fun pojo2Map(irClass: IrClass, expression: IrCall): IrContainerExpression {
    return irBlock {
        val irVariable = irTemporary(
            applyIrCall(
                mutableMapOfSymbol
            )
        )

        irClass.properties.forEach {
            +applyIrCall(
                mutableMapPutSymbol,
                irString(it.name.asString()),
                expression.extensionReceiver
            )
        }

        +irReturn(irGet(irVariable))
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun pojoList2MapList(irClass: IrClass, expression: IrCall): IrContainerExpression {
    return irBlock {
        val irVariable = irTemporary(
            applyIrCall(
                mutableMapOfSymbol
            )
        )

        irClass.properties.forEach {
            +applyIrCall(
                mutableMapPutSymbol,
                irString(it.name.asString()),
                expression.extensionReceiver
            )
        }

        +irReturn(irGet(irVariable))
    }
}