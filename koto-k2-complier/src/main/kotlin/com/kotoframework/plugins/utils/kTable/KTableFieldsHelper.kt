package com.kotoframework.plugins.utils.kTable

import com.kotoframework.plugins.scopes.KotoBuildScope
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.*


/**
 * Creates a list of IR expressions that represent field additions, using a predefined symbol to generate `addField` calls.
 * 使用预定义符号生成 `addField` 调用，创建表示字段添加的 IR 表达式列表。
 */
fun KotoBuildScope.addFieldList(): List<IrExpression> {
    val receiver =
        builder.irGet(function.extensionReceiverParameter!!)  // Obtain the receiver for the current function context.
    // 获取当前函数上下文的接收者。
    return addFieldsNames(function.body!!).map {
        applyIrCall(addFieldSymbol, it, receivers = KotoBuildScope.Receivers(receiver))
        // Apply the `addField` operation to each field name gathered, passing the receiver.
        // 将 `addField` 操作应用于收集到的每个字段名，传递接收者。
    }
}

/**
 * Recursively extracts field names from an IR element, handling different kinds of IR nodes.
 * 从 IR 元素递归提取字段名，处理不同类型的 IR 节点。
 */
fun KotoBuildScope.addFieldsNames(element: IrElement): MutableList<IrExpression> {
    val fieldNames = mutableListOf<IrExpression>()  // Initialize an empty list for field names.
    // 初始化字段名的空列表。
    when (element) {
        is IrBlockBody -> {
            element.statements.forEach { statement ->
                fieldNames.addAll(addFieldsNames(statement))
                // Recursively add field names from each statement in a block body.
                // 从块体中的每个声明递归添加字段名。
            }
        }

        is IrCall -> {
            if (element.origin == IrStatementOrigin.PLUS) {
                fieldNames.addAll(addFieldsNames(element.extensionReceiver!!))
                val args = element.valueArguments.filterNotNull()
                args.forEach {
                    fieldNames.addAll(addFieldsNames(it))
                    // Add field names from both the receiver and value arguments if the origin is a PLUS operation.
                    // 如果起源是 PLUS 操作，从接收器和值参数添加字段名。
                }
            }
            if (element.origin == IrStatementOrigin.GET_PROPERTY) {
                element.correspondingName?.let {
                    fieldNames.add(builder.irString(it.asString()))
                    // Add the property name directly as a string expression if the origin is GET_PROPERTY.
                    // 如果起源是 GET_PROPERTY，直接将属性名作为字符串表达式添加。
                }
            }
        }

        is IrConst<*> -> {
            fieldNames.add(element as IrExpression)
            // Add constant values directly to the field names list.
            // 直接将常量值添加到字段名列表。
        }

        is IrReturn -> {
            return addFieldsNames(element.value)
            // Handle return statements by recursively adding field names from the return value.
            // 通过递归从返回值添加字段名来处理返回语句。
        }
    }
    return fieldNames
}