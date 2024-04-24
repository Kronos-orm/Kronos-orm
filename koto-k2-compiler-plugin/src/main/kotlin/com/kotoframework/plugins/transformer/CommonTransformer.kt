package com.kotoframework.plugins.transformer

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction

/**
 * The `CommonTransformer` interface defines the common properties necessary for IR transformations within a Kotlin compiler plugin.
 * It includes the context for the plugin and a reference to the current function being transformed.
 * `CommonTransformer` 接口定义了在 Kotlin 编译器插件中进行 IR 转换所需的公共属性。
 * 它包含了插件的上下文和对当前正在转换的函数的引用。
 */
interface CommonTransformer{
    // Context for the plugin, contains global information about the compilation
    // 插件的上下文，包含编译过程中的全球信息
    val pluginContext: IrPluginContext

    // The current IR function in the transformation process
    // 转换过程中的当前 IR 函数
    val irFunction: IrFunction
}