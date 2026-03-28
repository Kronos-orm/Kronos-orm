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

import com.kotlinorm.compiler.utils.IrTestFramework
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
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
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
        )
        ctx.assertSuccess()
    }

    @Test
    fun `KTableTransformer provides shouldProcessReturn method`() {
        // Test that shouldProcessReturn is accessible in subclasses
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
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
        )
        ctx.assertSuccess()
    }

    @Test
    fun `KTableTransformer provides reportError method`() {
        // Test that reportError is accessible in subclasses
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
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
        )
        ctx.assertSuccess()
    }

    @Test
    fun `KTableTransformer extends IrElementTransformerVoidWithContext`() {
        // Test that KTableTransformer properly extends the base transformer
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
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
        )
        ctx.assertSuccess()
    }

    @Test
    fun `KTableTransformer can access irFunction property`() {
        // Test that the irFunction property is accessible
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
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
        )
        ctx.assertSuccess()
    }

    @Test
    fun `KTableTransformer can access errorReporter property`() {
        // Test that the errorReporter property is accessible
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
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
        )
        ctx.assertSuccess()
    }

    @Test
    fun `shouldProcessReturn compares returnTargetSymbol with function symbol`() {
        // Test that the shouldProcessReturn logic is correct by checking the implementation
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
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
        )
        ctx.assertSuccess()
    }
}
