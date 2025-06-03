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

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
class KronosCommandLineProcessor : CommandLineProcessor {
    companion object {
        const val OPTION_DEBUG_MODE = "debug"
        const val OPTION_DEBUG_INFO_PATH = "debug-info-path"

        val ARG_OPTION_DEBUG_MODE = CompilerConfigurationKey<Boolean>(com.kotlinorm.compiler.plugin.KronosCommandLineProcessor.Companion.OPTION_DEBUG_MODE)
        val ARG_OPTION_DEBUG_INFO_PATH = CompilerConfigurationKey<String>(com.kotlinorm.compiler.plugin.KronosCommandLineProcessor.Companion.OPTION_DEBUG_INFO_PATH)
    }

    override val pluginId: String = "kronos-compiler-plugin"

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            optionName = com.kotlinorm.compiler.plugin.KronosCommandLineProcessor.Companion.OPTION_DEBUG_MODE,
            valueDescription = "true or false",
            description = "Enable debug mode, print debug information",
            required = false,
        ),
        CliOption(
            optionName = com.kotlinorm.compiler.plugin.KronosCommandLineProcessor.Companion.OPTION_DEBUG_INFO_PATH,
            valueDescription = "path",
            description = "Debug information output path",
            required = false,
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        println("processOption:: option=$option value=$value")
        return when (option.optionName) {
            com.kotlinorm.compiler.plugin.KronosCommandLineProcessor.Companion.OPTION_DEBUG_MODE -> configuration.put(
                com.kotlinorm.compiler.plugin.KronosCommandLineProcessor.Companion.ARG_OPTION_DEBUG_MODE, value.lowercase() == "true")
            com.kotlinorm.compiler.plugin.KronosCommandLineProcessor.Companion.OPTION_DEBUG_INFO_PATH -> configuration.put(
                com.kotlinorm.compiler.plugin.KronosCommandLineProcessor.Companion.ARG_OPTION_DEBUG_INFO_PATH, value)
            else -> throw IllegalArgumentException("Unexpected config option ${option.optionName}")
        }
    }

}