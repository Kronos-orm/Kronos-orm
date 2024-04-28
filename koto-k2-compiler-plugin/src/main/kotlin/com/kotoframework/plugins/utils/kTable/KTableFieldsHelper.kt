package com.kotoframework.plugins.utils.kTable

import com.kotoframework.plugins.utils.*
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*


/**
 * Creates a list of IR expressions that represent field additions, using a predefined symbol to generate `addField` calls.
 * 使用预定义符号生成 `addField` 调用，创建表示字段添加的 IR 表达式列表。
 */
context(IrBuilderWithScope, IrPluginContext, IrFunction)
fun addFieldList(): List<IrExpression> {
    return addFieldsNames(body!!).map {
        // Apply the `addField` operation to each field name gathered, passing the receiver.
        // 将 `addField` 操作应用于收集到的每个字段名，传递接收者。
        applyIrCall(addFieldSymbol, it) { dispatchBy(irGet(extensionReceiverParameter!!)) }
    }
}

/**
 * Recursively extracts field names from an IR element, handling different kinds of IR nodes.
 * 从 IR 元素递归提取字段名，处理不同类型的 IR 节点。
 */
context(IrBuilderWithScope, IrPluginContext, IrFunction)
fun addFieldsNames(element: IrElement): MutableList<IrExpression> {
    // Initialize an empty list for field names.
    // 初始化字段名的空列表。
    val fieldNames = mutableListOf<IrExpression>()
    when (element) {
        is IrBlockBody -> {
            element.statements.forEach { statement ->
                // Recursively add field names from each statement in a block body.
                // 从块体中的每个声明递归添加字段名。
                fieldNames.addAll(addFieldsNames(statement))
            }
        }

        is IrCall -> {
            when (element.origin) {
                is IrStatementOrigin.PLUS -> {
                    // Add field names from both the receiver and value arguments if the origin is a PLUS operation.
                    // 如果起源是 PLUS 操作，从接收器和值参数添加字段名。
                    fieldNames.addAll(addFieldsNames(element.extensionReceiver!!))
                    val args = element.valueArguments.filterNotNull()
                    args.forEach {
                        fieldNames.addAll(addFieldsNames(it))
                    }
                }

                is IrStatementOrigin.GET_PROPERTY -> {
                    getColumnName(element).let { fieldNames.add(it) }
                }
            }
        }

        is IrConst<*> -> {
            // Add constant values directly to the field names list.
            // 直接将常量值添加到字段名列表。
            fieldNames.add(element as IrExpression)
        }

        is IrReturn -> {
            // Handle return statements by recursively adding field names from the return value.
            // 通过递归从返回值添加字段名来处理返回语句。
            return addFieldsNames(element.value)
        }
    }
    return fieldNames
}