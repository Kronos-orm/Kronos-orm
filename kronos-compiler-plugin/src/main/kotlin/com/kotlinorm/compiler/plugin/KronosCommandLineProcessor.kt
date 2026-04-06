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

package com.kotlinorm.compiler.plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

/**
 * Command line processor for the Kronos compiler plugin
 *
 * Handles plugin options like dump-ir mode and dump-ir path
 */
@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
class KronosCommandLineProcessor : CommandLineProcessor {
    companion object {
        const val OPTION_DUMP_IR = "dump-ir"
        const val OPTION_DUMP_IR_PATH = "dump-ir-path"
        const val OPTION_DUMP_IR_MODE = "dump-ir-mode"
        const val OPTION_DUMP_IR_FILES = "dump-ir-files"
        const val OPTION_DEBUG = "debug"

        val ARG_OPTION_DUMP_IR = CompilerConfigurationKey<Boolean>(OPTION_DUMP_IR)
        val ARG_OPTION_DUMP_IR_PATH = CompilerConfigurationKey<String>(OPTION_DUMP_IR_PATH)
        val ARG_OPTION_DUMP_IR_MODE = CompilerConfigurationKey<String>(OPTION_DUMP_IR_MODE)
        val ARG_OPTION_DUMP_IR_FILES = CompilerConfigurationKey<String>(OPTION_DUMP_IR_FILES)
        val ARG_OPTION_DEBUG = CompilerConfigurationKey<Boolean>(OPTION_DEBUG)
    }

    override val pluginId: String = "kronos-compiler-plugin"

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            optionName = OPTION_DUMP_IR,
            valueDescription = "true or false",
            description = "Enable IR dump mode",
            required = false,
        ),
        CliOption(
            optionName = OPTION_DUMP_IR_PATH,
            valueDescription = "path",
            description = "IR dump and debug log output path (default: build/tmp/kronosDebug)",
            required = false,
        ),
        CliOption(
            optionName = OPTION_DUMP_IR_MODE,
            valueDescription = "kotlinLike or common",
            description = "IR dump mode: kotlinLike (Kotlin-like format) or common (plain IR text format)",
            required = false,
        ),
        CliOption(
            optionName = OPTION_DUMP_IR_FILES,
            valueDescription = "comma-separated file patterns",
            description = "Filter files to dump (e.g., 'User.kt,Order.kt' or '*Service.kt'). If not specified, all files will be dumped.",
            required = false,
        ),
        CliOption(
            optionName = OPTION_DEBUG,
            valueDescription = "true or false",
            description = "Enable debug logging mode to capture symbol resolution and type judgments (JSON format)",
            required = false,
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        return when (option.optionName) {
            OPTION_DUMP_IR -> {
                val boolValue = value.lowercase() == "true"
                configuration.put(ARG_OPTION_DUMP_IR, boolValue)
            }
            OPTION_DUMP_IR_PATH -> {
                configuration.put(ARG_OPTION_DUMP_IR_PATH, value)
            }
            OPTION_DUMP_IR_MODE -> {
                configuration.put(ARG_OPTION_DUMP_IR_MODE, value)
            }
            OPTION_DUMP_IR_FILES -> {
                configuration.put(ARG_OPTION_DUMP_IR_FILES, value)
            }
            OPTION_DEBUG -> {
                val boolValue = value.lowercase() == "true"
                configuration.put(ARG_OPTION_DEBUG, boolValue)
            }
            else -> throw IllegalArgumentException("Unexpected config option ${option.optionName}")
        }
    }
}
