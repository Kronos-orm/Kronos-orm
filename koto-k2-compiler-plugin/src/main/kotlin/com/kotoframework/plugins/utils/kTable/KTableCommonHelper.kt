package com.kotoframework.plugins.utils.kTable

import com.kotoframework.plugins.scopes.KotoBuildScope
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

/**
 * Defines various Kotlin IR extensions to handle specific methods of `KTable` during the IR transformation process in Kotlin compiler plugins.
 * 定义多个 Kotlin IR 扩展，用于在 Kotlin 编译器插件的 IR 转换过程中处理 `KTable` 类的特定方法。
 */
@OptIn(FirIncompatiblePluginAPI::class)
// Reference to `KTable` class in the plugin context.
// 在插件上下文中引用 `KTable` 类。
private val KotoBuildScope.kTableSymbol
    get() = pluginContext.referenceClass(FqName("com.kotoframework.beans.dsl.KTable"))!!

// Obtain a reference to the `setValue` method of `KTable`.
// 获取 `KTable` 的 `setValue` 方法的引用。
internal val KotoBuildScope.setValueSymbol
    get() = kTableSymbol.getSimpleFunction("setValue")!!

// Obtain a reference to the `addField` method of `KTable`.
// 获取 `KTable` 的 `addField` 方法的引用。
internal val KotoBuildScope.addFieldSymbol
    get() = kTableSymbol.getSimpleFunction("addField")!!

// Extension property to get the name associated with a property from an `IrCall`.
// 扩展属性，用于从 `IrCall` 获取与属性相关联的名称。
internal val IrCall.correspondingName
    get() = symbol.owner.correspondingPropertySymbol?.owner?.name

@OptIn(FirIncompatiblePluginAPI::class)
internal val KotoBuildScope.fieldK2dbSymbol
    get() = pluginContext.referenceFunctions(FqName("com.kotoframework.utils.fieldK2db")).first()

@OptIn(FirIncompatiblePluginAPI::class)
internal val KotoBuildScope.tableK2dbSymbol
    get() = pluginContext.referenceFunctions(FqName("com.kotoframework.utils.tableK2db")).first()

@OptIn(FirIncompatiblePluginAPI::class)
// Reference to `KTable` class in the plugin context.
// 在插件上下文中引用 `KTable` 类。
private val KotoBuildScope.fieldSymbol
    get() = pluginContext.referenceClass(FqName("com.kotoframework.beans.dsl.Field"))!!

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun KotoBuildScope.getColumnName(expression: IrExpression): IrExpression {
    return when (expression) {
        is IrCall -> {
            val propertyName = expression.correspondingName!!.asString()
            val annotations =
                expression.dispatchReceiver!!.type.getClass()!!.properties.first { it.name.asString() == propertyName }.annotations
            val columnAnnotation =
                annotations.firstOrNull { it.symbol.descriptor.containingDeclaration.fqNameSafe == FqName("com.kotoframework.annotations.Column") }
            val columnName =columnAnnotation?.getValueArgument(0) ?: applyIrCall(fieldK2dbSymbol, builder.irString(propertyName))
            applyIrCall(fieldSymbol.constructors.first(), columnName, builder.irString(propertyName))
        }

        else -> builder.irString("")
    }
}