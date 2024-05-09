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

package com.kotlinorm.plugins

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
        private const val OPTION_IGNORE_WARNING = "ignoreWarning"

        val ARG_OPTION_IGNORE_WARNING = CompilerConfigurationKey<Boolean>(OPTION_IGNORE_WARNING)
    }

    override val pluginId: String = "kronos-compiler-plugin"

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            optionName = OPTION_IGNORE_WARNING,
            valueDescription = "true or false",
            description = "ignore warning",
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
            OPTION_IGNORE_WARNING -> configuration.put(ARG_OPTION_IGNORE_WARNING, value.lowercase() == "true")
            else -> throw IllegalArgumentException("Unexpected config option ${option.optionName}")
        }
    }

}