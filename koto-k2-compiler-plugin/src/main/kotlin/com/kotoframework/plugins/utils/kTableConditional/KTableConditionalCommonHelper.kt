package com.kotoframework.plugins.utils.kTableConditional

import com.kotoframework.plugins.scopes.KotoBuildScope
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.name.FqName

/**
 * Defines various Kotlin IR extensions to handle specific methods of `KTable` during the IR transformation process in Kotlin compiler plugins.
 * 定义多个 Kotlin IR 扩展，用于在 Kotlin 编译器插件的 IR 转换过程中处理 `KTable` 类的特定方法。
 */
//KTableConditional类的setCriteria函数
@OptIn(FirIncompatiblePluginAPI::class)
private val KotoBuildScope.criteriaSetterSymbol
    get() = pluginContext.referenceClass(FqName("com.kotoframework.beans.dsl.KTableConditional"))!!.getPropertySetter("criteria")!!