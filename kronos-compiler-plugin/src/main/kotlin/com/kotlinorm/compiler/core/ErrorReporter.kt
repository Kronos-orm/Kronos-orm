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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.util.BodyPrintingStrategy
import org.jetbrains.kotlin.ir.util.CustomKotlinLikeDumpStrategy
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

/**
 * Unified error reporting utility for the Kronos compiler plugin
 *
 * Reports compilation errors/warnings in standard compiler error format:
 *   file:line:col: error: message
 *
 * @property messageCollector The Kotlin compiler's message collector
 */
class ErrorReporter(
    private val messageCollector: MessageCollector
) {
    var currentFileEntry: IrFileEntry? = null

    companion object {
        /** Concise dump options for error messages — no file info, compact bodies */
        private val errorDumpOptions = KotlinLikeDumpOptions(
            CustomKotlinLikeDumpStrategy.Default,
            printRegionsPerFile = false,
            printFileName = false,
            printFilePath = false,
            useNamedArguments = false,
            bodyPrintingStrategy = BodyPrintingStrategy.NO_BODIES,
            printMemberDeclarations = false,
            printUnitReturnType = false,
            stableOrder = false
        )
    }

    /**
     * Extracts source location from an IR element using the current file entry.
     */
    private fun locationOf(element: IrElement): CompilerMessageLocation? {
        val entry = currentFileEntry ?: return null
        val offset = element.startOffset
        if (offset < 0) return null
        return try {
            val line = entry.getLineNumber(offset) + 1
            val col = entry.getColumnNumber(offset) + 1
            CompilerMessageLocation.create(entry.name, line, col, null)
        } catch (_: Exception) {
            null
        }
    }

    /** Compact IR dump for error context */
    private fun dumpElement(element: IrElement): String {
        return try {
            element.dumpKotlinLike(errorDumpOptions).lines().take(5).joinToString("\n")
        } catch (e: Exception) {
            "<IR dump failed: ${e.message}>"
        }
    }

    /**
     * Reports an error
     *
     * @param element IR element (used for source location)
     * @param message Error message
     * @param suggestion Optional fix suggestion
     */
    fun reportError(
        element: IrElement,
        message: String,
        suggestion: String? = null
    ) {
        val fullMessage = buildString {
            append("[Kronos] $message")
            append("\n  IR: ${dumpElement(element)}")
            if (suggestion != null) {
                append("\n  Fix: $suggestion")
            }
        }
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            fullMessage,
            locationOf(element)
        )
    }

    /**
     * Reports an error with exception details
     *
     * @param element IR element (used for source location)
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
        val fullMessage = buildString {
            append("[Kronos] $message")
            append("\n  Exception: ${e::class.simpleName}: ${e.message}")
            append("\n  IR: ${dumpElement(element)}")
            if (suggestion != null) {
                append("\n  Fix: $suggestion")
            }
        }
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            fullMessage,
            locationOf(element)
        )
    }

    /**
     * Reports a warning
     *
     * @param element IR element (used for source location)
     * @param message Warning message
     */
    fun reportWarning(
        element: IrElement,
        message: String
    ) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "[Kronos] $message",
            locationOf(element)
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
            "[Kronos] $message",
            null
        )
    }
}
