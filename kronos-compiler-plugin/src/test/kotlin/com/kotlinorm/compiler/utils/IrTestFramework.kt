/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.compiler.utils

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * 通用 IR 测试框架
 * 
 * 提供统一的方式来测试 IR 相关功能，包括：
 * - 编译代码并收集 IR 元素
 * - 提供访问 IrPluginContext 的能力
 * - 收集各种 IR 表达式用于测试
 * - 支持自定义验证逻辑
 * - 直接测试 utils 函数（符号解析、注解读取等）
 * 
 * 这个框架完全替代了 JSON debug 机制，提供更直接、更灵活的测试方式
 * 
 * ## 使用方式
 * 
 * ### 1. 测试 IR 收集（适用于测试 IR 转换、表达式分析等）
 * ```kotlin
 * val context = IrTestFramework.compile(
 *     IrTestFramework.source("Test.kt", """
 *         package test
 *         fun test() {
 *             val x = 1 + 2
 *         }
 *     """)
 * )
 * context.assertSuccess()
 * assertTrue(context.collector.plusCalls.size >= 1)
 * ```
 * 
 * ### 2. 测试符号解析和 Utils 函数（需要在 IR 生成阶段执行）
 * ```kotlin
 * IrTestFramework.testInIrGeneration(
 *     IrTestFramework.source("Test.kt", """
 *         package test
 *         import com.kotlinorm.interfaces.KPojo
 *         data class User(val id: Int) : KPojo
 *     """)
 * ) { ctx, pluginContext ->
 *     with(pluginContext) {
 *         val symbol = kPojoClassSymbol
 *         assertNotNull(symbol)
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalCompilerApi::class)
object IrTestFramework {

    /**
     * IR 收集器 - 收集编译后的 IR 元素
     */
    class IrCollector : IrVisitorVoid() {
        val propertyAccesses = mutableListOf<IrCall>()
        val propertyReferences = mutableListOf<IrPropertyReference>()
        val getValues = mutableListOf<IrGetValue>()
        val constants = mutableListOf<IrConst>()
        val plusCalls = mutableListOf<IrCall>()
        val minusCalls = mutableListOf<IrCall>()
        val allCalls = mutableListOf<IrCall>()
        val allExpressions = mutableListOf<IrExpression>()
        
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }
        
        override fun visitExpression(expression: IrExpression) {
            allExpressions.add(expression)
            super.visitExpression(expression)
        }
        
        override fun visitCall(expression: IrCall) {
            allCalls.add(expression)
            when (expression.origin) {
                IrStatementOrigin.GET_PROPERTY -> propertyAccesses.add(expression)
                IrStatementOrigin.PLUS -> plusCalls.add(expression)
                IrStatementOrigin.MINUS -> minusCalls.add(expression)
            }
            super.visitCall(expression)
        }
        
        override fun visitPropertyReference(expression: IrPropertyReference) {
            propertyReferences.add(expression)
            super.visitPropertyReference(expression)
        }
        
        override fun visitGetValue(expression: IrGetValue) {
            getValues.add(expression)
            super.visitGetValue(expression)
        }
        
        override fun visitConst(expression: IrConst) {
            constants.add(expression)
            super.visitConst(expression)
        }
        
        fun clear() {
            propertyAccesses.clear()
            propertyReferences.clear()
            getValues.clear()
            constants.clear()
            plusCalls.clear()
            minusCalls.clear()
            allCalls.clear()
            allExpressions.clear()
        }
    }

    /**
     * IR 测试上下文 - 包含编译结果和收集的 IR 元素
     */
    data class IrTestContext(
        val pluginContext: IrPluginContext?,
        val collector: IrCollector,
        val moduleFragment: IrModuleFragment?,
        val exitCode: KotlinCompilation.ExitCode
    ) {
        fun assertSuccess() {
            if (exitCode != KotlinCompilation.ExitCode.OK) {
                throw AssertionError("Compilation failed with exit code: $exitCode")
            }
        }
        
        fun assertFailure() {
            if (exitCode == KotlinCompilation.ExitCode.OK) {
                throw AssertionError("Expected compilation to fail, but it succeeded")
            }
        }
        
        /**
         * 获取所有 KPojo 类
         */
        @OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)
        fun getKPojoClasses(): List<IrClass> {
            requireNotNull(moduleFragment) { "Module fragment not available (compilation may have failed)" }
            val classes = mutableListOf<IrClass>()
            moduleFragment.acceptChildrenVoid(object : IrVisitorVoid() {
                override fun visitClass(declaration: IrClass) {
                    val hasKPojoSuperType = declaration.superTypes.any { superType ->
                        @OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)
                        superType.classOrNull?.owner?.name?.asString() == "KPojo"
                    }
                    if (hasKPojoSuperType) {
                        classes.add(declaration)
                    }
                    super.visitClass(declaration)
                }
            })
            return classes
        }
        
        /**
         * 根据名称查找类
         */
        @OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)
        fun findClass(name: String): IrClass? {
            requireNotNull(moduleFragment) { "Module fragment not available (compilation may have failed)" }
            var result: IrClass? = null
            moduleFragment.acceptChildrenVoid(object : IrVisitorVoid() {
                override fun visitClass(declaration: IrClass) {
                    if (declaration.name.asString() == name) {
                        result = declaration
                    }
                    super.visitClass(declaration)
                }
            })
            return result
        }
        
        /**
         * 根据名称查找函数
         */
        @OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)
        fun findFunction(name: String): IrFunction? {
            requireNotNull(moduleFragment) { "Module fragment not available (compilation may have failed)" }
            var result: IrFunction? = null
            moduleFragment.acceptChildrenVoid(object : IrVisitorVoid() {
                override fun visitFunction(declaration: IrFunction) {
                    if (declaration.name.asString() == name) {
                        result = declaration
                    }
                    super.visitFunction(declaration)
                }
            })
            return result
        }
        
        /**
         * 获取类的 defaultType (处理 UnsafeDuringIrConstructionAPI)
         */
        @OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)
        fun getDefaultType(irClass: IrClass): IrType {
            return irClass.symbol.defaultType
        }
        
        /**
         * 获取类的 properties (处理 UnsafeDuringIrConstructionAPI)
         */
        @OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)
        fun getProperties(irClass: IrClass): List<IrProperty> {
            return irClass.properties.toList()
        }
    }

    /**
     * 测试扩展 - 收集 IR 并提供上下文
     */
    private class TestExtension(
        private val collector: IrCollector,
        private val contextCapture: (IrPluginContext, IrModuleFragment) -> Unit,
        private val customAction: ((IrPluginContext, IrModuleFragment) -> Unit)? = null
    ) : IrGenerationExtension {
        @OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)
        override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
            // 捕获 context 和 module
            contextCapture(pluginContext, moduleFragment)
            
            // 收集 IR
            collector.clear()
            moduleFragment.acceptChildrenVoid(collector)
            
            // 执行自定义操作
            customAction?.invoke(pluginContext, moduleFragment)
        }
    }

    /**
     * 编译代码并返回 IR 测试上下文
     * 
     * 适用于测试 IR 收集、表达式分析等不需要访问符号的场景
     * 
     * @param sources 源代码文件
     * @param customAction 可选的自定义操作，在 IR 生成时执行
     * @return IR 测试上下文
     */
    fun compile(
        vararg sources: SourceFile,
        customAction: ((IrPluginContext, IrModuleFragment) -> Unit)? = null
    ): IrTestContext {
        val collector = IrCollector()
        var capturedContext: IrPluginContext? = null
        var capturedModule: IrModuleFragment? = null
        
        val compilation = KotlinCompilation().apply {
            this.sources = sources.toList()
            inheritClassPath = true
            compilerPluginRegistrars = listOf(
                object : org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar() {
                    override val supportsK2: Boolean = true
                    override val pluginId: String = "ir-test-framework"
                    
                    override fun ExtensionStorage.registerExtensions(
                        configuration: org.jetbrains.kotlin.config.CompilerConfiguration
                    ) {
                        org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension.registerExtension(
                            TestExtension(
                                collector = collector,
                                contextCapture = { ctx, mod -> 
                                    capturedContext = ctx
                                    capturedModule = mod
                                },
                                customAction = customAction
                            )
                        )
                    }
                }
            )
            messageOutputStream = System.out
        }

        val result = compilation.compile()
        
        return IrTestContext(
            pluginContext = capturedContext,
            collector = collector,
            moduleFragment = capturedModule,
            exitCode = result.exitCode
        )
    }

    /**
     * 在 IR 生成阶段执行测试
     * 
     * 适用于测试符号解析、注解读取等需要在 IR 生成阶段访问 IrPluginContext 的场景
     * 
     * @param sources 源代码文件
     * @param testAction 测试操作，接收 IrTestContext 和 IrPluginContext
     * @return IR 测试上下文
     */
    fun testInIrGeneration(
        vararg sources: SourceFile,
        testAction: (IrTestContext, IrPluginContext) -> Unit
    ): IrTestContext {
        return compile(*sources) { pluginContext, moduleFragment ->
            // 创建临时的 context 用于测试
            val tempContext = IrTestContext(
                pluginContext = pluginContext,
                collector = IrCollector(), // 空的 collector，因为在 customAction 中不需要
                moduleFragment = moduleFragment,
                exitCode = KotlinCompilation.ExitCode.OK
            )
            testAction(tempContext, pluginContext)
        }
    }

    /**
     * 创建源文件的便捷方法
     */
    fun source(name: String, content: String): SourceFile {
        return SourceFile.kotlin(name, content.trimIndent())
    }

    /**
     * 标准的 KPojo User 类，用于测试
     */
    val standardUserClass = source("User.kt", """
        package test
        import com.kotlinorm.interfaces.KPojo
        
        data class User(
            val id: Int,
            val name: String,
            val email: String,
            val age: Int,
            val password: String
        ) : KPojo
    """)
}
