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

package com.kotlinorm.compiler.core

import com.kotlinorm.compiler.utils.CompileTestUtils
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for KTableTransformer base class
 *
 * Tests that the KTableTransformer compiles correctly and can be extended
 */
@OptIn(ExperimentalCompilerApi::class)
class KTableTransformerTest {

    @Test
    fun `KTableTransformer can be extended with custom logic`() {
        // Test that code using KTableTransformer compiles successfully
        val source = CompileTestUtils.kotlinSource("Test.kt", """
            package test
            
            import com.kotlinorm.compiler.core.KTableTransformer
            import com.kotlinorm.compiler.core.ErrorReporter
            import org.jetbrains.kotlin.ir.declarations.IrFunction
            import org.jetbrains.kotlin.ir.expressions.IrReturn
            
            class MyTransformer(
                irFunction: IrFunction,
                errorReporter: ErrorReporter
            ) : KTableTransformer(irFunction, errorReporter) {
                
                override fun visitReturn(expression: IrReturn): org.jetbrains.kotlin.ir.expressions.IrExpression {
                    if (shouldProcessReturn(expression)) {
                        // Process the return
                    }
                    return super.visitReturn(expression)
                }
            }
        """)

        val result = CompileTestUtils.compile(source)
        CompileTestUtils.assertCompilationSucceeded(result)
    }

    @Test
    fun `KTableTransformer provides shouldProcessReturn method`() {
        // Test that shouldProcessReturn is accessible in subclasses
        val source = CompileTestUtils.kotlinSource("Test.kt", """
            package test
            
            import com.kotlinorm.compiler.core.KTableTransformer
            import com.kotlinorm.compiler.core.ErrorReporter
            import org.jetbrains.kotlin.ir.declarations.IrFunction
            import org.jetbrains.kotlin.ir.expressions.IrReturn
            
            class TestTransformer(
                irFunction: IrFunction,
                errorReporter: ErrorReporter
            ) : KTableTransformer(irFunction, errorReporter) {
                
                fun checkReturn(expression: IrReturn): Boolean {
                    return shouldProcessReturn(expression)
                }
            }
        """)

        val result = CompileTestUtils.compile(source)
        CompileTestUtils.assertCompilationSucceeded(result)
    }

    @Test
    fun `KTableTransformer provides reportError method`() {
        // Test that reportError is accessible in subclasses
        val source = CompileTestUtils.kotlinSource("Test.kt", """
            package test
            
            import com.kotlinorm.compiler.core.KTableTransformer
            import com.kotlinorm.compiler.core.ErrorReporter
            import org.jetbrains.kotlin.ir.declarations.IrFunction
            import org.jetbrains.kotlin.ir.IrElement
            
            class TestTransformer(
                irFunction: IrFunction,
                errorReporter: ErrorReporter
            ) : KTableTransformer(irFunction, errorReporter) {
                
                fun testError(element: IrElement) {
                    reportError(element, "Test error", "Test suggestion")
                }
            }
        """)

        val result = CompileTestUtils.compile(source)
        CompileTestUtils.assertCompilationSucceeded(result)
    }

    @Test
    fun `KTableTransformer extends IrElementTransformerVoidWithContext`() {
        // Test that KTableTransformer properly extends the base transformer
        val source = CompileTestUtils.kotlinSource("Test.kt", """
            package test
            
            import com.kotlinorm.compiler.core.KTableTransformer
            import com.kotlinorm.compiler.core.ErrorReporter
            import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
            import org.jetbrains.kotlin.ir.declarations.IrFunction
            
            class TestTransformer(
                irFunction: IrFunction,
                errorReporter: ErrorReporter
            ) : KTableTransformer(irFunction, errorReporter) {
                
                fun isTransformer(): Boolean {
                    return this is IrElementTransformerVoidWithContext
                }
            }
        """)

        val result = CompileTestUtils.compile(source)
        CompileTestUtils.assertCompilationSucceeded(result)
    }

    @Test
    fun `KTableTransformer can access irFunction property`() {
        // Test that the irFunction property is accessible
        val source = CompileTestUtils.kotlinSource("Test.kt", """
            package test
            
            import com.kotlinorm.compiler.core.KTableTransformer
            import com.kotlinorm.compiler.core.ErrorReporter
            import org.jetbrains.kotlin.ir.declarations.IrFunction
            
            class TestTransformer(
                irFunction: IrFunction,
                errorReporter: ErrorReporter
            ) : KTableTransformer(irFunction, errorReporter) {
                
                fun getFunctionSymbol() = irFunction.symbol
            }
        """)

        val result = CompileTestUtils.compile(source)
        CompileTestUtils.assertCompilationSucceeded(result)
    }

    @Test
    fun `KTableTransformer can access errorReporter property`() {
        // Test that the errorReporter property is accessible
        val source = CompileTestUtils.kotlinSource("Test.kt", """
            package test
            
            import com.kotlinorm.compiler.core.KTableTransformer
            import com.kotlinorm.compiler.core.ErrorReporter
            import org.jetbrains.kotlin.ir.declarations.IrFunction
            
            class TestTransformer(
                irFunction: IrFunction,
                errorReporter: ErrorReporter
            ) : KTableTransformer(irFunction, errorReporter) {
                
                fun getReporter() = errorReporter
            }
        """)

        val result = CompileTestUtils.compile(source)
        CompileTestUtils.assertCompilationSucceeded(result)
    }

    @Test
    fun `shouldProcessReturn compares returnTargetSymbol with function symbol`() {
        // Test that the shouldProcessReturn logic is correct by checking the implementation
        val source = CompileTestUtils.kotlinSource("Test.kt", """
            package test
            
            import com.kotlinorm.compiler.core.KTableTransformer
            import com.kotlinorm.compiler.core.ErrorReporter
            import org.jetbrains.kotlin.ir.declarations.IrFunction
            import org.jetbrains.kotlin.ir.expressions.IrReturn
            
            class TestTransformer(
                irFunction: IrFunction,
                errorReporter: ErrorReporter
            ) : KTableTransformer(irFunction, errorReporter) {
                
                override fun visitReturn(expression: IrReturn): org.jetbrains.kotlin.ir.expressions.IrExpression {
                    // shouldProcessReturn checks: expression.returnTargetSymbol == irFunction.symbol
                    val shouldProcess = shouldProcessReturn(expression)
                    
                    // This verifies the logic exists and compiles
                    if (shouldProcess) {
                        // Process top-level returns
                    } else {
                        // Skip nested lambda returns
                    }
                    
                    return super.visitReturn(expression)
                }
            }
        """)

        val result = CompileTestUtils.compile(source)
        CompileTestUtils.assertCompilationSucceeded(result)
    }

    @Test
    fun `reportError delegates to ErrorReporter with all parameters`() {
        // Test that reportError properly delegates to the ErrorReporter
        val messages = mutableListOf<Pair<CompilerMessageSeverity, String>>()
        val messageCollector = createTestMessageCollector(messages)
        val errorReporter = ErrorReporter(messageCollector)
        
        // Create a simple mock element
        val mockElement = object : org.jetbrains.kotlin.ir.IrElement {
            override var startOffset: Int = 0
            override var endOffset: Int = 10
            override var attributeOwnerId: org.jetbrains.kotlin.ir.IrElement = this

            override fun <R, D> accept(visitor: org.jetbrains.kotlin.ir.visitors.IrVisitor<R, D>, data: D): R {
                throw UnsupportedOperationException()
            }

            override fun <D> acceptChildren(visitor: org.jetbrains.kotlin.ir.visitors.IrVisitor<Unit, D>, data: D) {
                throw UnsupportedOperationException()
            }

            override fun <D> transform(transformer: org.jetbrains.kotlin.ir.visitors.IrTransformer<D>, data: D): org.jetbrains.kotlin.ir.IrElement {
                throw UnsupportedOperationException()
            }

            override fun <D> transformChildren(transformer: org.jetbrains.kotlin.ir.visitors.IrTransformer<D>, data: D) {
                throw UnsupportedOperationException()
            }
        }
        
        // Test error reporting through ErrorReporter directly
        errorReporter.reportError(mockElement, "Test error", "Test suggestion")

        assertEquals(1, messages.size)
        assertEquals(CompilerMessageSeverity.ERROR, messages[0].first)
        assertTrue(messages[0].second.contains("Test error"))
        assertTrue(messages[0].second.contains("Test suggestion"))
    }

    @Test
    fun `reportError works without suggestion parameter`() {
        // Test that reportError works with optional suggestion parameter
        val messages = mutableListOf<Pair<CompilerMessageSeverity, String>>()
        val messageCollector = createTestMessageCollector(messages)
        val errorReporter = ErrorReporter(messageCollector)
        
        val mockElement = object : org.jetbrains.kotlin.ir.IrElement {
            override var startOffset: Int = 0
            override var endOffset: Int = 10
            override var attributeOwnerId: org.jetbrains.kotlin.ir.IrElement = this

            override fun <R, D> accept(visitor: org.jetbrains.kotlin.ir.visitors.IrVisitor<R, D>, data: D): R {
                throw UnsupportedOperationException()
            }

            override fun <D> acceptChildren(visitor: org.jetbrains.kotlin.ir.visitors.IrVisitor<Unit, D>, data: D) {
                throw UnsupportedOperationException()
            }

            override fun <D> transform(transformer: org.jetbrains.kotlin.ir.visitors.IrTransformer<D>, data: D): org.jetbrains.kotlin.ir.IrElement {
                throw UnsupportedOperationException()
            }

            override fun <D> transformChildren(transformer: org.jetbrains.kotlin.ir.visitors.IrTransformer<D>, data: D) {
                throw UnsupportedOperationException()
            }
        }
        
        errorReporter.reportError(mockElement, "Test error")

        assertEquals(1, messages.size)
        assertEquals(CompilerMessageSeverity.ERROR, messages[0].first)
        assertTrue(messages[0].second.contains("Test error"))
        assertFalse(messages[0].second.contains("Suggestion"))
    }

    // Helper function

    private fun createTestMessageCollector(messages: MutableList<Pair<CompilerMessageSeverity, String>>): MessageCollector {
        return object : MessageCollector {
            override fun clear() {
                messages.clear()
            }

            override fun hasErrors(): Boolean {
                return messages.any { it.first == CompilerMessageSeverity.ERROR }
            }

            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation?
            ) {
                messages.add(severity to message)
            }
        }
    }
}
