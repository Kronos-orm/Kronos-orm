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
package com.kotlinorm.codegen

import com.kotlinorm.Kronos
import com.kotlinorm.beans.logging.KLogMessage.Companion.kMsgOf
import com.kotlinorm.enums.ColorPrintCode
import java.io.File

/**
 * A value class representing a Kotlin file with its content as a string.
 *
 * @property str The content of the Kotlin file as a string.
 */
@JvmInline
value class KotlinFile(
    val str: String
) {
    /**
     * A value class representing a Kotlin file with its content as a string.
     *
     * @property str The content of the Kotlin file as a string.
     */
    fun write(filePath: String) {
        File(filePath).apply {
            if (!parentFile.exists()) {
                parentFile.mkdirs()
            }
            if (!exists()) {
                createNewFile()
            }
            writeText(str)
        }
        Kronos.defaultLogger(this).info(
            kMsgOf(
                "File generated successfully: $filePath",
                ColorPrintCode.GREEN
            ).toArray()
        )
    }
}