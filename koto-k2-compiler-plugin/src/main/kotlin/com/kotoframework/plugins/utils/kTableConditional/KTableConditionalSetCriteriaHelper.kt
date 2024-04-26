package com.kotoframework.plugins.utils.kTableConditional

import com.kotoframework.plugins.scopes.KotoBuildScope
import com.kotoframework.plugins.scopes.KotoBuildScope.Companion.dispatchBy
import com.kotoframework.plugins.utils.kTable.getColumnName
import com.kotoframework.plugins.utils.kTable.getTableName
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl

/**
 * Adds IR for setting simple criteria. example: criteriaField.setCriteria(tmp)
 *
 * @receiver KotoBuildScope instance.
 * @author OUSC
 */
fun KotoBuildScope.setCriteriaIr() =
    applyIrCall(
        criteriaSetterSymbol,
        builder.irGet(buildCriteria(function.body!!)!!)){
        dispatchBy(builder.irGet(function.extensionReceiverParameter!!))
    }

fun KotoBuildScope.buildCriteria(element: IrElement, setNot: Boolean = false): IrVariable? {
    var paramName: IrExpression? = null
    var type = "ROOT"
    var not = setNot
    var value: IrExpression? = null
    val children: MutableList<IrVariable?> = mutableListOf()
    var tableName: IrExpression? = null

    when(element) {
        is IrBlockBody -> {
            element.statements.forEach { statement ->
                children.add(buildCriteria(statement))
            }
        }

        is IrIfThenElseImpl -> {
            type = element.origin.toString()
            element.branches.forEach {
                children.add(buildCriteria(it.condition))
                children.add(buildCriteria(it.result))
            }
        }

        is IrCall -> {
            val funcName = element.funcName()
            type = funcName
            val args = element.valueArguments
            if ("not" == funcName) {
                if (args.size == 1) {
                    return buildCriteria(args[0]!!, true)
                }
                args.forEach {
                    children.add(buildCriteria(it!!))
                }
            } else {
                when (funcName) {
                    "isIn" -> {
                        value = args[0]
                        paramName = getColumnName(args[1]!!)
                        tableName = getTableName(args[1]!!)
                    }

                    "isNull" , "notNull" -> {
                        paramName = getColumnName(args[0]!!)
                        tableName = getTableName(args[0]!!)
                    }

                    "lt" , "gt" , "le" , "ge" -> {
                        val compareToIrCall = args[0] as IrCall
                        paramName = getColumnName(compareToIrCall.extensionReceiver!!)
                        value = compareToIrCall.valueArguments[0]
                        tableName = getTableName(compareToIrCall.dispatchReceiver!!)
                    }

                    "equal" , "like" , "between" -> {
                        paramName = getColumnName(args[0]!!)
                        value = args[1]
                        tableName = getTableName((args[0] as IrCall).dispatchReceiver!!)
                    }

                    "notLike" , "notBetween" -> {
                        type = funcName.replaceFirst("not" , "").replaceFirstChar { it.lowercase() }
                        not = true
                        paramName = getColumnName(args[0]!!)
                        value = args[1]
                        tableName = getTableName(args[0]!!)
                    }
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

    return CriteriaIR(paramName , type , not , value , children.filterNotNull() , tableName).toIrVariable()
}