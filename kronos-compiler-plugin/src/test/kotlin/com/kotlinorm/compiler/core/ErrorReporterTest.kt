/**
 * Copyright 2022-2026 kronos-orm
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

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor

class ErrorReporterTest {
    private lateinit var messages: MutableList<Pair<CompilerMessageSeverity, String>>
    private lateinit var errorReporter: ErrorReporter

    @BeforeTest
    fun setup() {
        messages = mutableListOf()
        errorReporter = ErrorReporter(
            object : MessageCollector {
                override fun clear() {
                    messages.clear()
                }

                override fun hasErrors(): Boolean {
                    return messages.any { it.first == CompilerMessageSeverity.ERROR }
                }

                override fun report(
                    severity: CompilerMessageSeverity,
                    message: String,
                    location: CompilerMessageSourceLocation?,
                ) {
                    messages += severity to message
                }
            }
        )
    }

    @Test
    fun reportsErrorWithCompactIrFallback() {
        errorReporter.reportError(mockIrElement(), "Unsupported condition")

        assertEquals(1, messages.size)
        assertEquals(CompilerMessageSeverity.ERROR, messages.single().first)
        assertTrue(messages.single().second.contains("[Kronos] Unsupported condition"))
        assertTrue(messages.single().second.contains("IR: <IR dump failed: dump unavailable>"))
    }

    @Test
    fun reportsErrorWithSuggestion() {
        errorReporter.reportError(mockIrElement(), "Unsupported field", "Use a property reference")

        val message = messages.single().second
        assertTrue(message.contains("[Kronos] Unsupported field"))
        assertTrue(message.contains("Fix: Use a property reference"))
    }

    @Test
    fun reportsErrorWithExceptionDetails() {
        errorReporter.reportError(
            mockIrElement(),
            "Failed to transform",
            IllegalStateException("bad state"),
            "Reduce the expression",
        )

        val message = messages.single().second
        assertTrue(message.contains("Exception: IllegalStateException: bad state"))
        assertTrue(message.contains("Fix: Reduce the expression"))
    }

    @Test
    fun reportsWarningAndInfo() {
        errorReporter.reportWarning(mockIrElement(), "Condition fallback")
        errorReporter.reportInfo("Plugin initialized")

        assertEquals(CompilerMessageSeverity.WARNING, messages[0].first)
        assertEquals("[Kronos] Condition fallback", messages[0].second)
        assertEquals(CompilerMessageSeverity.INFO, messages[1].first)
        assertEquals("[Kronos] Plugin initialized", messages[1].second)
    }

    private fun mockIrElement(): IrElement {
        return object : IrElement {
            override var startOffset: Int = -1
            override var endOffset: Int = -1
            override var attributeOwnerId: IrElement = this

            override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R {
                throw RuntimeException("dump unavailable")
            }

            override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) = Unit

            override fun <D> transform(transformer: IrTransformer<D>, data: D): IrElement {
                return this
            }

            override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) = Unit
        }
    }
}
