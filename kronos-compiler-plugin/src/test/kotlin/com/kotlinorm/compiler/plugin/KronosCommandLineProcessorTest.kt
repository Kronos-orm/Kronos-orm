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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(CompilerConfiguration.Internals::class)
class KronosCommandLineProcessorTest {
    @Test
    fun exposesPluginMetadata() {
        val processor = KronosCommandLineProcessor()
        val optionNames = processor.pluginOptions.map { it.optionName }

        assertEquals("kronos-compiler-plugin", processor.pluginId)
        assertEquals(
            listOf("dump-ir", "dump-ir-path", "dump-ir-mode", "dump-ir-files", "debug"),
            optionNames,
        )
        assertTrue(processor.pluginOptions.all { !it.required })
    }

    @Test
    fun storesBooleanAndStringOptions() {
        val processor = KronosCommandLineProcessor()
        val configuration = CompilerConfiguration()

        processor.process("dump-ir", "true", configuration)
        processor.process("dump-ir-path", "build/ir", configuration)
        processor.process("dump-ir-mode", "kotlinLike", configuration)
        processor.process("dump-ir-files", "*User.kt,Order.kt", configuration)
        processor.process("debug", "TRUE", configuration)

        assertTrue(configuration.get(KronosCommandLineProcessor.ARG_OPTION_DUMP_IR) == true)
        assertEquals("build/ir", configuration.get(KronosCommandLineProcessor.ARG_OPTION_DUMP_IR_PATH))
        assertEquals("kotlinLike", configuration.get(KronosCommandLineProcessor.ARG_OPTION_DUMP_IR_MODE))
        assertEquals("*User.kt,Order.kt", configuration.get(KronosCommandLineProcessor.ARG_OPTION_DUMP_IR_FILES))
        assertTrue(configuration.get(KronosCommandLineProcessor.ARG_OPTION_DEBUG) == true)
    }

    @OptIn(CompilerConfiguration.Internals::class)
    @Test
    fun falseBooleanOptionsDoNotTreatOtherTextAsTrue() {
        val processor = KronosCommandLineProcessor()
        val configuration = CompilerConfiguration()

        processor.process("dump-ir", "yes", configuration)
        processor.process("debug", "false", configuration)

        assertFalse(configuration.get(KronosCommandLineProcessor.ARG_OPTION_DUMP_IR) == true)
        assertFalse(configuration.get(KronosCommandLineProcessor.ARG_OPTION_DEBUG) == true)
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

    private fun KronosCommandLineProcessor.process(
        optionName: String,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        val option = pluginOptions.single { it.optionName == optionName }
        processOption(option, value, configuration)
    }
}
