package com.kotoframework.plugins.utils.kTableConditional

import com.kotoframework.plugins.utils.*
import com.kotoframework.plugins.utils.kTable.correspondingName
import com.kotoframework.plugins.utils.kTable.getColumnName
import com.kotoframework.plugins.utils.kTable.getTableName
import com.kotoframework.plugins.utils.kTable.propParamSymbol
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
import kotlin.jvm.internal.Intrinsics.Kotlin

/**
 * Adds IR for setting simple criteria. example: criteriaField.setCriteria(tmp)
 *
 * @receiver KotoBuildScope instance.
 * @author OUSC
 */
context(IrBlockBuilder, IrPluginContext, IrFunction)
fun setCriteriaIr() =
    applyIrCall(
        criteriaSetterSymbol, irGet(buildCriteria(body!!)!!)
    ) {
        dispatchBy(irGet(extensionReceiverParameter!!))
    }

context(IrBlockBuilder, IrPluginContext, IrFunction)
fun buildCriteria(element: IrElement, setNot: Boolean = false): IrVariable? {
    var paramName: IrExpression? = null
    var type = "ROOT"
    var not = setNot
    var value: IrExpression? = null
    val children: MutableList<IrVariable?> = mutableListOf()
    var tableName: IrExpression? = null

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
            when (funcName) {
                "isNull" -> {
                    type = funcName
                    paramName = getColumnName(element.extensionReceiver!!)
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "notNull" -> {
                    type = "isNull"
                    not = !not
                    paramName = getColumnName(element.extensionReceiver!!)
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "lt", "gt", "le", "ge" -> {
                    type = funcName
                    if (args.isEmpty()) {
                        // 形如it.<property>.lt的写法
                        val receiver = irGet(extensionReceiverParameter!!)
                        paramName = getColumnName(element.extensionReceiver!!)
                        tableName = getTableName(element.dispatchReceiver!!)
                        value = applyIrCall(
                            propParamSymbol!!,
                            irString((element.extensionReceiver!! as IrCall).correspondingName!!.asString())
                        ) {
                            dispatchBy(receiver)
                        }
                    } else {
                        val irCall = args.first()!!.asIrCall()
                        if (irCall.extensionReceiver is IrCall) {
                            // 形如it.<property> < 100的写法
                            paramName = getColumnName(irCall.extensionReceiver!!)
                            value = irCall.valueArguments.first()!!
                        } else if (irCall.extensionReceiver is IrConstImpl<*>) {
                            // 形如100 < it.<property> 的写法
                            paramName = getColumnName(irCall.valueArguments.first()!!)
                            value = irCall.extensionReceiver
                        }
                        tableName = getTableName(irCall.dispatchReceiver!!)
                    }
                }

                "equal" -> {
                    type = funcName
                    not = if (element.valueArguments.isEmpty()) !not else not
                    val index = if (args.first() is IrConstImpl<*>) 1 else 0
                    val irCall = args[index]!!.asIrCall()
                    paramName = getColumnName(irCall)
                    value = args[1 - index]
                    tableName = getTableName(irCall.dispatchReceiver!!)
                }

                "eq" -> {
                    type = "equal"
                    paramName = getColumnName(element.extensionReceiver!!)
                    value = applyIrCall(
                        propParamSymbol!!,
                        irString(element.extensionReceiver!!.asIrCall().correspondingName!!.asString())
                    ) {
                        dispatchBy(irGet(extensionReceiverParameter!!))
                    }
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "neq" -> {
                    type = "equal"
                    not = !not
                    paramName = getColumnName(element.extensionReceiver!!)
                    value = applyIrCall(
                        propParamSymbol!!,
                        irString(element.extensionReceiver!!.asIrCall().correspondingName!!.asString())
                    ) {
                        dispatchBy(irGet(extensionReceiverParameter!!))
                    }
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "between", "like" -> {
                    type = funcName
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

                "notBetween", "notLike" -> {
                    type = when (funcName) {
                        "notBetween" -> "between"
                        else -> "like"
                    }
                    not = !not
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
                    type = "like"
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
                    ){
                        dispatchBy(str)
                    }
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "matchRight" -> {
                    type = "like"
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
                    type = "like"
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
                    type = "in"
                    paramName = getColumnName(args[0]!!)
                    value = (element.extensionReceiver as IrCall)
                    tableName = getTableName(element.dispatchReceiver!!)
                }

                "asSql" -> {
                    type = "sql"
                    value = element.extensionReceiver
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

    return CriteriaIR(paramName, type, not, value, children.filterNotNull(), tableName).toIrVariable()
}