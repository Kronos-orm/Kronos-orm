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

package com.kotlinorm.compiler.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(CompilerConfiguration.Internals::class)
class KronosCommandLineProcessorTest {
    @Test
    fun exposesPluginMetadata() {
        val processor = KronosCommandLineProcessor()
        val optionNames = processor.pluginOptions.map { it.optionName }

        assertEquals("kronos-compiler-plugin", processor.pluginId)
        assertEquals(emptyList(), optionNames)
    }

    @OptIn(CompilerConfiguration.Internals::class)
    @Test
    fun rejectsUnknownOption() {
        val processor = KronosCommandLineProcessor()
        val option = CliOption("unknown", "<value>", "unknown", required = false)

        val error = assertFailsWith<IllegalArgumentException> {
            processor.processOption(option, "value", CompilerConfiguration())
        }

        assertEquals("Unexpected config option unknown", error.message)
    }
}
