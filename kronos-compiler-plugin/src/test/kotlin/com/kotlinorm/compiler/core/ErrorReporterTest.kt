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
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.AbstractIrFileEntry
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor

class ErrorReporterTest {
    private lateinit var messages: MutableList<Triple<CompilerMessageSeverity, String, CompilerMessageSourceLocation?>>
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
                    messages += Triple(severity, message, location)
                }
            }
        )
    }

    @Test
    fun reportsErrorWithCompactIrFallback() {
        errorReporter.reportError(mockIrElement(), "Unsupported condition")

        assertEquals(1, messages.size)
        assertEquals(CompilerMessageSeverity.ERROR, messages.single().first)
        assertEquals(
            "[Kronos] Unsupported condition\n  IR: <IR dump failed: dump unavailable>",
            messages.single().second
        )
    }

    @Test
    fun reportsErrorWithSuggestion() {
        errorReporter.reportError(mockIrElement(), "Unsupported field", "Use a property reference")

        assertEquals(
            "[Kronos] Unsupported field\n  IR: <IR dump failed: dump unavailable>\n  Fix: Use a property reference",
            messages.single().second
        )
    }

    @Test
    fun reportsErrorWithExceptionDetails() {
        errorReporter.reportError(
            mockIrElement(),
            "Failed to transform",
            IllegalStateException("bad state"),
            "Reduce the expression",
        )

        assertEquals(
            "[Kronos] Failed to transform\n  Exception: IllegalStateException: bad state\n  IR: <IR dump failed: dump unavailable>\n  Fix: Reduce the expression",
            messages.single().second
        )
    }

    @Test
    fun reportsErrorWithExceptionDetailsWithoutSuggestion() {
        errorReporter.reportError(
            mockIrElement(),
            "Failed to transform",
            IllegalArgumentException("bad argument"),
        )

        assertEquals(
            "[Kronos] Failed to transform\n  Exception: IllegalArgumentException: bad argument\n  IR: <IR dump failed: dump unavailable>",
            messages.single().second
        )
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

    @Test
    fun reportsSourceLocationWhenFileEntryAndOffsetAreAvailable() {
        errorReporter.currentFileEntry = object : AbstractIrFileEntry() {
            override val name: String = "User.kt"
            override val maxOffset: Int = 32
            override val lineStartOffsets: IntArray = intArrayOf(0, 12)
            override val firstRelevantLineIndex: Int = 0
        }

        errorReporter.reportWarning(mockIrElement(startOffset = 14), "Located warning")

        val location = messages.single().third
        assertEquals("User.kt", location?.path)
        assertEquals(2, location?.line)
        assertEquals(3, location?.column)
    }

    @Test
    fun reportsNullSourceLocationWhenOffsetIsUnavailable() {
        errorReporter.currentFileEntry = object : AbstractIrFileEntry() {
            override val name: String = "User.kt"
            override val maxOffset: Int = 32
            override val lineStartOffsets: IntArray = intArrayOf(0, 12)
            override val firstRelevantLineIndex: Int = 0
        }

        errorReporter.reportWarning(mockIrElement(startOffset = -1), "Unlocated warning")

        assertEquals(null, messages.single().third)
    }

    private fun mockIrElement(startOffset: Int = -1): IrElement {
        return object : IrElement {
            override var startOffset: Int = startOffset
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
