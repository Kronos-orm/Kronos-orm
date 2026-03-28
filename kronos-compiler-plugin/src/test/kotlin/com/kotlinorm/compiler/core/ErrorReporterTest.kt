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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import kotlin.test.assertTrue

class ErrorReporterTest {
    private lateinit var messages: MutableList<Pair<CompilerMessageSeverity, String>>
    private lateinit var errorReporter: ErrorReporter

    @BeforeEach
    fun setup() {
        messages = mutableListOf()
        val messageCollector = object : MessageCollector {
            override fun clear() {
                messages.clear()
            }

            override fun hasErrors(): Boolean {
                return messages.any { it.first == CompilerMessageSeverity.ERROR }
            }

            override fun report(severity: CompilerMessageSeverity, message: String, location: org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation?) {
                messages.add(severity to message)
            }
        }
        errorReporter = ErrorReporter(messageCollector)
    }

    @Test
    fun `should report error without suggestion`() {
        // Given
        val element = createMockIrElement()
        val message = "Test error message"

        // When
        errorReporter.reportError(element, message)

        // Then
        assertTrue(messages.size == 1)
        assertTrue(messages[0].first == CompilerMessageSeverity.ERROR)
        assertTrue(messages[0].second.contains(message))
    }

    @Test
    fun `should report error with suggestion`() {
        // Given
        val element = createMockIrElement()
        val message = "Test error message"
        val suggestion = "Try this fix"

        // When
        errorReporter.reportError(element, message, suggestion)

        // Then
        assertTrue(messages.size == 1)
        assertTrue(messages[0].first == CompilerMessageSeverity.ERROR)
        assertTrue(messages[0].second.contains(message))
        assertTrue(messages[0].second.contains("Suggestion: $suggestion"))
    }

    @Test
    fun `should report warning`() {
        // Given
        val element = createMockIrElement()
        val message = "Test warning message"

        // When
        errorReporter.reportWarning(element, message)

        // Then
        assertTrue(messages.size == 1)
        assertTrue(messages[0].first == CompilerMessageSeverity.WARNING)
        assertTrue(messages[0].second.contains(message))
    }

    @Test
    fun `should report info`() {
        // Given
        val message = "Test info message"

        // When
        errorReporter.reportInfo(message)

        // Then
        assertTrue(messages.size == 1)
        assertTrue(messages[0].first == CompilerMessageSeverity.INFO)
        assertTrue(messages[0].second == message)
    }

    @Test
    fun `should report multiple errors`() {
        // Given
        val element = createMockIrElement()

        // When
        errorReporter.reportError(element, "Error 1")
        errorReporter.reportError(element, "Error 2")
        errorReporter.reportWarning(element, "Warning 1")

        // Then
        assertTrue(messages.size == 3)
        assertTrue(messages.count { it.first == CompilerMessageSeverity.ERROR } == 2)
        assertTrue(messages.count { it.first == CompilerMessageSeverity.WARNING } == 1)
    }

    private fun createMockIrElement(): IrElement {
        return object : IrElement {
            override var startOffset: Int = 0
            override var endOffset: Int = 10
            override var attributeOwnerId: IrElement = this

            override fun <R, D> accept(visitor: org.jetbrains.kotlin.ir.visitors.IrVisitor<R, D>, data: D): R {
                throw UnsupportedOperationException()
            }

            override fun <D> acceptChildren(visitor: org.jetbrains.kotlin.ir.visitors.IrVisitor<Unit, D>, data: D) {
                throw UnsupportedOperationException()
            }

            override fun <D> transform(transformer: org.jetbrains.kotlin.ir.visitors.IrTransformer<D>, data: D): IrElement {
                throw UnsupportedOperationException()
            }

            override fun <D> transformChildren(transformer: org.jetbrains.kotlin.ir.visitors.IrTransformer<D>, data: D) {
                throw UnsupportedOperationException()
            }
        }
    }
}
