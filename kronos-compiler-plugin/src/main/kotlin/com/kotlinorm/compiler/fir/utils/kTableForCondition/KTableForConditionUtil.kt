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

package com.kotlinorm.compiler.fir.utils.kTableForCondition

import com.kotlinorm.compiler.fir.beans.CriteriaIR
import com.kotlinorm.compiler.fir.utils.ARRAY_OR_COLLECTION_FQ_NAMES
import com.kotlinorm.compiler.fir.utils.KPojoFqName
import com.kotlinorm.compiler.fir.utils.correspondingName
import com.kotlinorm.compiler.fir.utils.findKronosColumn
import com.kotlinorm.compiler.fir.utils.funcName
import com.kotlinorm.compiler.fir.utils.getColumnOrValue
import com.kotlinorm.compiler.fir.utils.getTableName
import com.kotlinorm.compiler.fir.utils.isColumn
import com.kotlinorm.compiler.fir.utils.isKronosColumn
import com.kotlinorm.compiler.fir.utils.isKronosFunction
import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.asIrCall
import com.kotlinorm.compiler.helpers.dispatchBy
import com.kotlinorm.compiler.helpers.extensionBy
import com.kotlinorm.compiler.helpers.subType
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.superTypes

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
fun setCriteriaIr() = applyIrCall(
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
 * @param noValueStrategyType The strategy to use when there is no value. Default is null.
 * @return The built criteria IR variable, or null if the element is a constant.
 */
context(IrBlockBuilder, IrPluginContext, IrFunction)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun buildCriteria(element: IrElement, setNot: Boolean = false, noValueStrategyType: IrExpression? = null): IrVariable? {
    var paramName: IrExpression? = null
    var type = "ROOT"
    var not = setNot
    var value: IrExpression? = null
    val children: MutableList<IrVariable?> = mutableListOf()
    var tableName: IrExpression? = null
    var strategy = noValueStrategyType

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
                    paramName = getColumnOrValue(element.extensionReceiver!!)
                    tableName = getTableName(element.dispatchReceiver!!.type.subType()!!.getClass()!!)
                }

                "lt", "gt", "le", "ge" -> {
                    if (args.isEmpty()) {
                        // 形如it.<property>.lt的写法
                        // Write like it.<property>.lt with no arguments
                        paramName = getColumnOrValue(element.extensionReceiver!!)
                        tableName = getTableName(element.dispatchReceiver!!.type.subType()!!.getClass()!!)
                        value = applyIrCall(
                            getValueByFieldNameSymbol,
                            irString(element.extensionReceiver!!.asIrCall().funcName())
                        ) {
                            dispatchBy(irGet(extensionReceiverParameter!!))
                        }
                    } else {
                        // it.xxx > xx 或 xx > it.xxx
                        // it.xxx > xx or xx > it.xxx
                        val irCall = args.first()!!.asIrCall()
                        // 提供fun(a, b)形式和A.B.C形式的函数调用支持(!!属于fun(a, b))
                        // Provides support for function calls of the form fun(a, b)
                        // and of the form A.B.C (!!!). belongs to fun(a, b))
                        val (left, operator, right) = runExpressionAnalysis(
                            irCall.extensionReceiver,
                            funcName,
                            irCall.valueArguments.firstOrNull() ?: args.getOrNull(1)
                        )
                        paramName = left
                        type = operator
                        value = right
                        tableName = getTableName(irCall.findKronosColumn()!!.asIrCall().dispatchReceiver!!)
                    }
                }

                "equal" -> {
                    not = not xor element.valueArguments.isEmpty()
                    val index = when {
                        args[0].isKronosColumn() || args[0].isKronosFunction() -> 0
                        args[1].isKronosColumn() || args[0].isKronosFunction() -> 1
                        else -> {
                            type = "sql"
                            value = element
                            -1
                        }
                    }
                    if (index != -1) {
                        val irCall = args[index]!!.asIrCall()
                        val (left, _, right) = runExpressionAnalysis(
                            irCall,
                            funcName,
                            args[1 - index]
                        )
                        paramName = left
                        value = right
                        tableName = getTableName(irCall.dispatchReceiver!!)
                    }
                }

                "eq", "neq" -> {
                    val extensionReceiver = element.extensionReceiver
                    if (extensionReceiver != null && extensionReceiver.isKronosColumn()) {
                        paramName = getColumnOrValue(extensionReceiver)
                        value = applyIrCall(
                            getValueByFieldNameSymbol,
                            irString(extensionReceiver.asIrCall().correspondingName!!.asString())
                        ) {
                            dispatchBy(irGet(extensionReceiverParameter!!))
                        }
                        tableName = getTableName(extensionReceiver.asIrCall().dispatchReceiver!!)
                    } else if (extensionReceiver != null) {
                        val irClass = element.extensionReceiver?.type?.getClass()

                        fun generateEq(kPojo: IrClass, receiver: IrExpression, excludes: List<String>) {
                            kPojo.properties.forEach { prop ->
                                if (prop.isColumn() && prop.name.asString() !in excludes) {
                                    children.add(
                                        buildCriteria(
                                            applyIrCall(
                                                ComparableEq.getter!!.symbol,
                                            ) {
                                                dispatchBy(extensionReceiver)
                                                extensionBy(
                                                    irGet(
                                                        prop.backingField!!.type, receiver,
                                                        prop.getter!!.symbol
                                                    )
                                                )
                                            },
                                            setNot
                                        )
                                    )
                                }
                            }
                        }

                        if (irClass?.kotlinFqName == KPojoFqName) {
                            if (extensionReceiver is IrCallImpl && extensionReceiver.asIrCall().origin == IrStatementOrigin.MINUS) {
                                type = "AND"
                                val (kPojoClass, kPojo, excludes) = analyzeMinusExpression(element.extensionReceiver!!.asIrCall())
                                generateEq(kPojoClass, kPojo, excludes)
                            } else if (irClass.superTypes.any { it.classFqName == KPojoFqName }) {
                                type = "AND"
                                generateEq(irClass, extensionReceiver, listOf())
                            }
                        }
                    }
                }

                "between", "like", "regexp", "notBetween", "notLike", "notRegexp" -> {
                    paramName = getColumnOrValue(element.extensionReceiver!!)
                    value = if (args.isEmpty()) {
                        applyIrCall(
                            getValueByFieldNameSymbol,
                            irString(element.extensionReceiver!!.asIrCall().correspondingName!!.asString())
                        ) {
                            dispatchBy(irGet(extensionReceiverParameter!!))
                        }
                    } else {
                        getColumnOrValue(args.first())
                    }
                    tableName = getTableName(element.dispatchReceiver!!.type.subType()!!.getClass()!!)
                }

                "startsWith" -> {
                    val str = if (args.isEmpty()) {
                        applyIrCall(
                            getValueByFieldNameSymbol,
                            irString(element.extensionReceiver!!.asIrCall().correspondingName!!.asString())
                        ) {
                            dispatchBy(irGet(extensionReceiverParameter!!))
                        }
                    } else {
                        getColumnOrValue(args.first())
                    }
                    paramName = getColumnOrValue(element.extensionReceiver!!)
                    value = applyIrCall(
                        stringPlusSymbol, irString("%")
                    ) {
                        dispatchBy(str)
                    }
                    tableName = getTableName(element.dispatchReceiver!!.type.subType()!!.getClass()!!)
                }

                "endsWith" -> {
                    val str = if (args.isEmpty()) {
                        applyIrCall(
                            getValueByFieldNameSymbol,
                            irString(element.extensionReceiver!!.asIrCall().correspondingName!!.asString())
                        ) {
                            dispatchBy(irGet(extensionReceiverParameter!!))
                        }
                    } else {
                        getColumnOrValue(args.first())
                    }
                    paramName = getColumnOrValue(element.extensionReceiver!!)
                    value = applyIrCall(
                        stringPlusSymbol, str
                    ) {
                        dispatchBy(irString("%"))
                    }
                    tableName = getTableName(element.dispatchReceiver!!.type.subType()!!.getClass()!!)
                }

                "contains" -> {
                    val left = element.extensionReceiver ?: element.dispatchReceiver
                    if (left!!.type.superTypes().any { it.classFqName in ARRAY_OR_COLLECTION_FQ_NAMES }) {
                        tableName = getTableName(args.first()!!.asIrCall().dispatchReceiver!!.type.getClass()!!)
                        // 形如 it.<property> in [1, 2, 3]的写法
                        // Write like it.<property> in listOf(1, 2, 3)
                        paramName = getColumnOrValue(args.first()!!)
                        value = left
                    } else {
                        tableName = getTableName(left.asIrCall().dispatchReceiver!!.type.getClass()!!)
                        type = "like"
                        val str = if (args.isEmpty()) {
                            paramName = getColumnOrValue(left)
                            // 形如 it.<property>.contains后面不加参数的写法
                            // Write it as it.<property>.contains with no arguments after it
                            applyIrCall(
                                getValueByFieldNameSymbol,
                                irString(left.asIrCall().correspondingName!!.asString())
                            ) {
                                dispatchBy(irGet(extensionReceiverParameter!!))
                            }
                        } else {
                            paramName = getColumnOrValue(left)
                            // 形如 it.<property>.contains("xx")的写法
                            // Writes like it.<property>.contains("xx") or "xx" in it.<property>
                            getColumnOrValue(args.first())
                        }

                        value = if (str is IrConstImpl<*> && str.value is String) {
                            irString("%${str.value}%")
                        } else {
                            applyIrCall(
                                buildContainsStrSymbol, str
                            ) {
                                dispatchBy(irGet(extensionReceiverParameter!!))
                            }
                        }
                    }
                }

                "asSql" -> {
                    value = element.extensionReceiver
                }

                "ifNoValue" -> {
                    strategy = args.first()
                    return buildCriteria(element.extensionReceiver!!, not, strategy)
                }
            }
        }

        is IrReturn -> {
            return buildCriteria(element.value)
        }

        is IrConstImpl<*> -> {
            if (element.type == irBuiltIns.stringType) {
                type = "sql"
                value = element
            } else {
                return null
            }
        }

    }

    return CriteriaIR(
        paramName, type, not, value, children.filterNotNull(), tableName, strategy
    ).toIrVariable()
}

context(IrPluginContext)
fun analyzeMinusExpression(irCall: IrCall): Triple<IrClass, IrExpression, List<String>> {
    val (kPojo, properties) = getIrMinusParent(irCall)
    return Triple(
        kPojo.type.getClass()!!,
        kPojo,
        properties
    )
}

context(IrPluginContext)
fun getIrMinusParent(irCall: IrCall): Pair<IrExpression, List<String>> {
    val property = listOfNotNull(
        irCall.valueArguments.find { it is IrCallImpl && it.origin == IrStatementOrigin.GET_PROPERTY }?.funcName()
    )
    val (kPojo, properties) = if (irCall.extensionReceiver is IrCallImpl) {
        getIrMinusParent(irCall.extensionReceiver!!.asIrCall())
    } else {
        irCall.extensionReceiver!! to listOf()
    }

    return kPojo to (properties + property)
}