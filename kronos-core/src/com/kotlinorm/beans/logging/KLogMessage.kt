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

package com.kotlinorm.beans.logging

import com.kotlinorm.interfaces.PrintCode

/**
 * Log line
 *
 * Create empty Log line
 *
 * @property text the log message
 * @property codes the style codes
 * @property newLine whether to end the line
 *
 * @author OUSC
 * @create 2022/11/12 14:21
 */
expect class KLogMessage(
    text: String,
    codes: Array<PrintCode> = emptyArray(),
    newLine: Boolean = false
) {
    var text: String

    internal var codes: Array<PrintCode>

    var newLine: Boolean

    companion object {
        /**
         * Formats an array of KLogMessage objects into a single string.
         *
         * @return The formatted string containing the text of each KLogMessage object, with an optional newline character after each message.
         */
        fun Array<KLogMessage>.formatted(): String
    }
}
