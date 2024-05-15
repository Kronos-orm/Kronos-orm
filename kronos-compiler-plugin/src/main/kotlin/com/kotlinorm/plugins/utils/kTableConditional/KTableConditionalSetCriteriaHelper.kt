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

package com.kotlinorm.plugins.utils.kTableConditional

import com.kotlinorm.plugins.helpers.applyIrCall
import com.kotlinorm.plugins.helpers.asIrCall
import com.kotlinorm.plugins.helpers.dispatchBy
import com.kotlinorm.plugins.utils.kTable.correspondingName
import com.kotlinorm.plugins.utils.kTable.getColumnName
import com.kotlinorm.plugins.utils.kTable.getTableName
import com.kotlinorm.plugins.utils.kTable.propParamSymbol
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl

/**
 * Generates IR for setting simple criteria.
 *
 * This function applies an IR call to set the criteria using the criteriaSetterSymbol.
 * It takes the result of building the criteria using the body of the current function.
 * The criteria are then dispatched by getting the extension receiver parameter.
 *
 * @return the IR expression for setting the criteria
 * @author OUSC
 */
context(IrBlockBuilder, IrPluginContext, IrFunction)
fun setCriteriaIr() =
    applyIrCall(
        criteriaSetterSymbol, irGet(buildCriteria(body!!)!!)
    ) {
        dispatchBy(irGet(extensionReceiverParameter!!))
    }

/**
 * Builds a criteria IR variable based on the given element.
 *
 * This function recursively builds a criteria IR variable based on the given element and its children.
 * It handles different types of elements such as IrBlockBody, IrIfThenElseImpl, IrCall, IrReturn, and IrConstImpl.
 * The criteria are built based on the function name and the value arguments of the IrCall element.
 * The criteria type and not flag are determined based on the function name.
 * The column name, table name, value, and children are extracted from the element and its arguments.
 * The criteria IR variable is then returned.
 *
 * @param element The element to build the criteria IR from.
 * @param setNot Whether to set the not flag. Default is false.
 * @param noValueStrategy The strategy to use when there is no value. Default is null.
 * @return The built criteria IR variable, or null if the element is a constant.
 */
context(IrBlockBuilder, IrPluginContext, IrFunction)
fun buildCriteria(element: IrElement, setNot: Boolean = false, noValueStrategy: IrExpression? = null): IrVariable? {
    var paramName: IrExpression? = null
    var type = "ROOT"
    var not = setNot
    var value: IrExpression? = null
    val children: MutableList<IrVariable?> = mutableListOf()
    var tableName: IrExpression? = null
    var strategy = noValueStrategy

    when (element) {
        is IrBlockBody -> {
            element.statements.forEach { statement ->
                children.add(buildCriteria(statement))
            }
        }

        is IrIfThenElseImpl -> {
            type = element.funcName(setNot)
            element.branches.forEach {
                children.add(buildCriteria(it.condition, setNot))
                children.add(buildCriteria(it.result, setNot))
            }
            irConcat()
        }

        is IrCall -> {
            val funcName = element.funcName()
            var args = element.valueArguments
            if (args.isEmpty() && element.dispatchReceiver is IrCall) {
                args = (element.dispatchReceiver as IrCall).valueArguments
            }

            if ("not" == funcName) {
                return buildCriteria(element.dispatchReceiver!!, !not)
            }

            val (conditionType, isNot) = parseConditionType(funcName)
            type = conditionType
            not = not xor isNot
            when (funcName) {
                "isNull", "notNull" -> {
                    paramName = getColumnName(element.extensionReceiver!!)
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "lt", "gt", "le", "ge" -> {
                    if (args.isEmpty()) {
                        // 形如it.<property>.lt的写法
                        paramName = getColumnName(element.extensionReceiver!!)
                        tableName = getTableName(element.dispatchReceiver!!)
                        value = applyIrCall(
                            propParamSymbol!!,
                            irString((element.extensionReceiver!! as IrCall).correspondingName!!.asString())
                        ) {
                            dispatchBy(irGet(extensionReceiverParameter!!))
                        }
                    } else {
                        val irCall = args.first()!!.asIrCall()
                        if (irCall.extensionReceiver is IrCall && null != (irCall.extensionReceiver as IrCall).origin) {
                            // 形如it.<property> < 100的写法
                            paramName = getColumnName(irCall.extensionReceiver!!)
                            value = irCall.valueArguments.first()!!
                        } else {
                            // 形如100 < it.<property> 的写法
                            paramName = getColumnName(irCall.valueArguments.first()!!)
                            value = irCall.extensionReceiver
                        }
                        tableName = getTableName(irCall.dispatchReceiver!!)
                    }
                }

                "equal" -> {
                    not = not xor element.valueArguments.isEmpty()
                    val index = if (args.first() is IrCall && null != (args.first()!! as IrCall).origin) 0 else 1
                    val irCall = args[index]!!.asIrCall()
                    paramName = getColumnName(irCall)
                    value = args[1 - index]
                    tableName = getTableName(irCall.dispatchReceiver!!)
                }

                "eq", "neq" -> {
                    paramName = getColumnName(element.extensionReceiver!!)
                    value = applyIrCall(
                        propParamSymbol!!,
                        irString(element.extensionReceiver!!.asIrCall().correspondingName!!.asString())
                    ) {
                        dispatchBy(irGet(extensionReceiverParameter!!))
                    }
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "between", "like", "notBetween", "notLike" -> {
                    paramName = getColumnName(element.extensionReceiver!!)
                    value = if (args.isEmpty()) {
                        applyIrCall(
                            propParamSymbol!!,
                            irString(element.extensionReceiver!!.asIrCall().correspondingName!!.asString())
                        ) {
                            dispatchBy(irGet(extensionReceiverParameter!!))
                        }
                    } else {
                        args[0]
                    }
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "matchLeft" -> {
                    val str = if (args.isEmpty()) {
                        applyIrCall(
                            propParamSymbol!!,
                            irString(element.extensionReceiver!!.asIrCall().correspondingName!!.asString())
                        ) {
                            dispatchBy(irGet(extensionReceiverParameter!!))
                        }
                    } else {
                        args[0]
                    }
                    paramName = getColumnName(element.extensionReceiver!!)
                    value = applyIrCall(
                        stringPlusSymbol,
                        irString("%")
                    ) {
                        dispatchBy(str)
                    }
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "matchRight" -> {
                    val str = if (args.isEmpty()) {
                        applyIrCall(
                            propParamSymbol!!,
                            irString(element.extensionReceiver!!.asIrCall().correspondingName!!.asString())
                        ) {
                            dispatchBy(irGet(extensionReceiverParameter!!))
                        }
                    } else {
                        args[0]
                    }
                    paramName = getColumnName(element.extensionReceiver!!)
                    value = applyIrCall(
                        stringPlusSymbol,
                        str
                    ) {
                        dispatchBy(irString("%"))
                    }
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "matchBoth" -> {
                    val str = if (args.isEmpty()) {
                        applyIrCall(
                            propParamSymbol!!,
                            irString(element.extensionReceiver!!.asIrCall().correspondingName!!.asString())
                        ) {
                            dispatchBy(irGet(extensionReceiverParameter!!))
                        }
                    } else {
                        args[0]
                    }
                    paramName = getColumnName(element.extensionReceiver!!)
                    value = applyIrCall(
                        stringPlusSymbol,
                        applyIrCall(
                            stringPlusSymbol,
                            irString("%")
                        ) {
                            dispatchBy(str)
                        }
                    ) {
                        dispatchBy(irString("%"))
                    }
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "contains" -> {
                    paramName = getColumnName(args[0]!!)
                    value = element.extensionReceiver
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "asSql" -> {
                    value = element.extensionReceiver
                }

                "ifNoValue" -> {
                    strategy = args[0]
                    return buildCriteria(element.extensionReceiver!!, not, strategy)
                }
            }
        }

        is IrReturn -> {
            return buildCriteria(element.value)
        }

        is IrConstImpl<*> -> {
            return null
        }

    }

    return CriteriaIR(paramName, type, not, value, children.filterNotNull(), tableName, strategy).toIrVariable()
}