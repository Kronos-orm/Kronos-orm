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
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

/**
 * Unified error reporting utility for the Kronos compiler plugin
 *
 * Responsible for reporting compilation errors to the Kotlin compiler with friendly error messages
 *
 * @property messageCollector The Kotlin compiler's message collector
 */
class ErrorReporter(
    private val messageCollector: MessageCollector
) {
    /**
     * Reports an error
     *
     * @param element IR element (used for context)
     * @param message Error message
     * @param suggestion Optional fix suggestion
     */
    fun reportError(
        element: IrElement,
        message: String,
        suggestion: String? = null
    ) {
        val irDump = try { element.dumpKotlinLike() } catch (e: Exception) { "<IR dump failed: ${e.message}>" }
        val fullMessage = buildString {
            append(message)
            append("\nIR element: ${element::class.simpleName}")
            append("\nIR dump:\n$irDump")
            if (suggestion != null) {
                append("\n")
                append("Suggestion: ")
                append(suggestion)
            }
        }
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            fullMessage,
            null
        )
    }

    /**
     * Reports an error with exception details
     *
     * @param element IR element (used for context)
     * @param message Error message
     * @param e The exception that occurred
     * @param suggestion Optional fix suggestion
     */
    fun reportError(
        element: IrElement,
        message: String,
        e: Exception,
        suggestion: String? = null
    ) {
        val irDump = try { element.dumpKotlinLike() } catch (dumpEx: Exception) { "<IR dump failed: ${dumpEx.message}>" }
        val fullMessage = buildString {
            append(message)
            append("\nException: ${e::class.simpleName}: ${e.message}")
            append("\nStack trace:\n${e.stackTraceToString().take(500)}")
            append("\nIR element: ${element::class.simpleName}")
            append("\nIR dump:\n$irDump")
            if (suggestion != null) {
                append("\n")
                append("Suggestion: ")
                append(suggestion)
            }
        }
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            fullMessage,
            null
        )
    }

    /**
     * Reports a warning
     *
     * @param element IR element (used for context)
     * @param message Warning message
     */
    fun reportWarning(
        element: IrElement,
        message: String
    ) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            message,
            null
        )
    }

    /**
     * Reports an informational message
     *
     * @param message Information message
     */
    fun reportInfo(message: String) {
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            message,
            null
        )
    }
}
