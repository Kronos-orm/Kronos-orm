/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.beans.logging

import com.kotlinorm.beans.logging.BundledSimpleLoggerAdapter.Companion.format
import com.kotlinorm.enums.ColorPrintCode
import com.kotlinorm.enums.KLogLevel
import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Log line
 *
 * Create empty Log line
 *
 * @property text the log message
 * @property codes the style codes
 * @property endLine whether to end the line
 *
 * @author OUSC
 * @create 2022/11/12 14:21
 */
class KLogMessage(
    private val text: String,
    internal var codes: Array<ColorPrintCode> = arrayOf(),
    private var endLine: Boolean = false,
) {
    /**
     * End log line
     *
     * @return Log line
     */
    fun endl(): KLogMessage {
        endLine = true
        return this
    }

    /**
     * Print
     *
     * @return Log line
     */
    fun print(level: KLogLevel) {
        val out = if (level > KLogLevel.WARN) System.out else System.err
        if (endLine) {
            out.println(format(text, codes))
        } else {
            out.print(format(text, codes))
        }
    }

    /**
     * Write
     *
     * @param path
     */
    fun write(path: Path) {
        if (!SystemFileSystem.exists(path)) {
            throw RuntimeException("File $path not found.")
        }
        with(SystemFileSystem.sink(path, true)) {
            (text + "\n".takeIf { endLine }).toByteArray().let { byteArray ->
                write(
                    Buffer().apply {
                        write(byteArray)
                    },
                    byteArray.size.toLong()
                )
            }
        }
    }

    /**
     * Convert the current KLogMessage object to an array of KLogMessage objects.
     */
    fun toArray(): Array<KLogMessage> {
        return arrayOf(this)
    }

    companion object {
        /**
         * Creates a new instance of KLogMessage with the given text and optional color print codes.
         *
         * @param text the log message text
         * @param codes an optional array of color print codes to apply to the log message
         * @return a new instance of KLogMessage
         */
        fun logMessageOf(text: String, codes: Array<ColorPrintCode> = arrayOf()): KLogMessage {
            return KLogMessage(text, codes)
        }

        /**
         * Formats an array of KLogMessage objects into a single string.
         *
         * @return The formatted string containing the text of each KLogMessage object, with an optional newline character after each message.
         */
        fun Array<KLogMessage>.formatted(): String {
            val sb = StringBuilder()
            this.forEach {
                sb.append(it.text)
                if (it.endLine) {
                    sb.append("\r\n")
                }
            }
            return sb.toString()
        }
    }
}
