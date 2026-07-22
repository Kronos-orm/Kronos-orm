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
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Command line processor for the Kronos compiler plugin
 */
@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
class KronosCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "kronos-compiler-plugin"

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            optionName = GeneratedProviderIdOptionName,
            valueDescription = "<stable-module-id>",
            description = "Stable id for this compilation's generated type provider",
            required = false
        ),
        CliOption(
            optionName = GeneratedProviderFqNameOptionName,
            valueDescription = "<provider-fq-name>",
            description = "Module-unique generated type provider class name",
            required = false
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        when (option.optionName) {
            GeneratedProviderIdOptionName -> configuration.put(GeneratedProviderIdKey, value)
            GeneratedProviderFqNameOptionName -> configuration.put(GeneratedProviderFqNameKey, value)
            else -> throw IllegalArgumentException("Unexpected config option ${option.optionName}")
        }
    }
}
