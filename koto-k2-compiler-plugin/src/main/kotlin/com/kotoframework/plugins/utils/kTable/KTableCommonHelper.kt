package com.kotoframework.plugins.utils.kTable

import com.kotoframework.plugins.scopes.KotoBuildScope
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.name.FqName

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

// Obtain a reference to the `getKProperty` method of `KTable`.
// 获取 `KTable` 的 `getKProperty` 方法的引用。
internal val KotoBuildScope.getKPropertySymbol
    get() = kTableSymbol.getSimpleFunction("getKProperty")!!

// Extension property to get the name associated with a property from an `IrCall`.
// 扩展属性，用于从 `IrCall` 获取与属性相关联的名称。
internal val IrCall.correspondingName
    get() = symbol.owner.correspondingPropertySymbol?.owner?.name