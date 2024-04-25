package com.kotoframework.plugins.utils.kTable

import com.kotoframework.plugins.scopes.KotoBuildScope
import com.kotoframework.plugins.scopes.KotoBuildScope.Companion.dispatchBy
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

/**
 * Constructs a list of IR expressions by processing assignment statements and mapping them to field parameters.
 * 通过处理赋值语句并将其映射到字段参数，构造 IR 表达式列表。
 */
fun KotoBuildScope.putFieldParamMap(): List<IrExpression> {
    // Call to process the body of the function with the function's receiver.
    // 调用以使用函数的接收器处理函数的体。
    return putParamMapStatements(builder.irGet(function.extensionReceiverParameter!!), function.body!!)
}

/**
 * Recursively processes IR elements to construct expressions that represent parameter mapping for fields.
 * 递归处理 IR 元素，以构建表示字段参数映射的表达式。
 */
fun KotoBuildScope.putParamMapStatements(receiver: IrExpression, element: IrElement): MutableList<IrExpression> {
    val statements = mutableListOf<IrExpression>()  // Initialize an empty list to hold the resulting IR expressions.
    // 初始化空列表，以保存结果 IR 表达式。
    when (element) {
        is IrBlockBody -> {
            // Recursively handle each statement within a block body.
            // 递归处理块体内的每个语句。
            element.statements.forEach { statement ->
                statements.addAll(putParamMapStatements(receiver, statement))
            }
        }

        is IrCall -> {
            // Handle assignment operations (EQ origin) to map field parameters.
            // 处理赋值操作（EQ 原点）以映射字段参数。
            if (element.origin == IrStatementOrigin.EQ) { // Assignment statement
                statements.add(
                    applyIrCall(
                        setValueSymbol,
                        getColumnName(element),
                        element.valueArguments[0]){
                        dispatchBy(receiver)
                    }
                )
            }
        }
    }
    return statements
}