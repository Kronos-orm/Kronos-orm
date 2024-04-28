package com.kotoframework.plugins.utils.kTable

import com.kotoframework.plugins.utils.applyIrCall
import com.kotoframework.plugins.utils.asSimpleType
import com.kotoframework.plugins.utils.findByFqName
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

/**
 * Defines various Kotlin IR extensions to handle specific methods of `KTable` during the IR transformation process in Kotlin compiler plugins.
 * 定义多个 Kotlin IR 扩展，用于在 Kotlin 编译器插件的 IR 转换过程中处理 `KTable` 类的特定方法。
 */
context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
// Reference to `KTable` class in the plugin context.
// 在插件上下文中引用 `KTable` 类。
private val kTableSymbol
    get() = referenceClass(FqName("com.kotoframework.beans.dsl.KTable"))!!

context(IrPluginContext)
// Obtain a reference to the `setValue` method of `KTable`.
// 获取 `KTable` 的 `setValue` 方法的引用。
internal val setValueSymbol
    get() = kTableSymbol.getSimpleFunction("setValue")!!

context(IrPluginContext)
// Obtain a reference to the `addField` method of `KTable`.
// 获取 `KTable` 的 `addField` 方法的引用。
internal val addFieldSymbol
    get() = kTableSymbol.getSimpleFunction("addField")!!

context(IrPluginContext)
internal val propParamSymbol
    get() = kTableSymbol.getSimpleFunction("getValueByFieldName")

// Extension property to get the name associated with a property from an `IrCall`.
// 扩展属性，用于从 `IrCall` 获取与属性相关联的名称。
context(IrPluginContext)
internal val IrCall.correspondingName
    get() = symbol.owner.correspondingPropertySymbol?.owner?.name

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
internal val fieldK2dbSymbol
    get() = referenceFunctions(FqName("com.kotoframework.utils.fieldK2db")).first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
internal val tableK2dbSymbol
    get() = referenceFunctions(FqName("com.kotoframework.utils.tableK2db")).first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
// Reference to `KTable` class in the plugin context.
// 在插件上下文中引用 `KTable` 类。
private val fieldSymbol
    get() = referenceClass(FqName("com.kotoframework.beans.dsl.Field"))!!

context(IrBuilderWithScope, IrPluginContext, IrFunction)
fun getColumnName(expression: IrExpression): IrExpression {
    return when (expression) {
        is IrCall -> {
            val propertyName = expression.correspondingName!!.asString()
            val irProperty =
                expression.dispatchReceiver!!.type.getClass()!!.properties.first { it.name.asString() == propertyName }
            getColumnName(irProperty, propertyName)
        }

        else -> applyIrCall(fieldSymbol.constructors.first(), irString(""), irString(""))
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun getColumnName(irProperty: IrProperty, propertyName: String = irProperty.name.asString()): IrExpression {
    val columnAnnotation =
        irProperty.annotations.findByFqName(FqName("com.kotoframework.annotations.Column"))
    val columnName =
        columnAnnotation?.getValueArgument(0) ?: applyIrCall(fieldK2dbSymbol, irString(propertyName))
    return applyIrCall(fieldSymbol.constructors.first(), columnName, irString(propertyName))
}

context(IrBuilderWithScope, IrPluginContext)
fun getTableName(expression: IrExpression): IrExpression {
    val irClass = when (expression) {
        is IrGetValue -> expression.type.asSimpleType().arguments[0].typeOrFail.getClass()
        is IrCall -> expression.type.getClass()
        else -> throw IllegalStateException("Unexpected expression type: $expression")
    }!!
    val annotations = irClass.annotations
    val tableAnnotation =
        annotations.findByFqName(FqName("com.kotoframework.annotations.Table"))
    return tableAnnotation?.getValueArgument(0) ?: applyIrCall(
        tableK2dbSymbol, irString(
            irClass.name.asString()
        )
    )
}