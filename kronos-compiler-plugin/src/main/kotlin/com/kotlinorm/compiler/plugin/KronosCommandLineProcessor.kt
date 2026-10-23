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

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Command line processor for the Kronos compiler plugin
 */
@OptIn(ExperimentalCompilerApi::class)
class KronosCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "kronos-compiler-plugin"

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(MODULE_COORDINATE_OPTION_NAME, "<group:module>",
            "Derive provider identities from this module coordinate and the compiler platform", required = false),
        CliOption(JVM_SOURCE_SET_PREFIX_OPTION_NAME, "<jvm|empty>",
            "JVM source-set prefix; jvm for multiplatform modules, empty for JVM-only modules", required = false),
        CliOption(
            optionName = GENERATED_PROVIDER_ID_OPTION_NAME,
            valueDescription = "<stable-module-id>",
            description = "Stable id for this compilation's generated type provider (last value wins)",
            required = false,
            allowMultipleOccurrences = true
        ),
        CliOption(
            optionName = GENERATED_PROVIDER_FQ_NAME_OPTION_NAME,
            valueDescription = "<provider-fq-name>",
            description = "Module-unique generated type provider class name (last value wins)",
            required = false,
            allowMultipleOccurrences = true
        ),
        CliOption(
            optionName = GENERATED_PROVIDER_FRAGMENTS_OPTION_NAME,
            valueDescription = "true",
            description = "Emit source-owned generated type provider fragments for incremental compilation",
            required = false
        ),
        CliOption(
            optionName = SKIP_GENERATED_PROVIDER_OPTION_NAME,
            valueDescription = "true",
            description = "Explicitly skip JVM generated provider emission for a Native compilation",
            required = false
        ),
        CliOption(
            optionName = NATIVE_PROVIDER_ENTRY_FQ_NAME_OPTION_NAME,
            valueDescription = "<entry-fq-name>",
            description = "Common-source Native generated metadata entry function (last value wins)",
            required = false,
            allowMultipleOccurrences = true
        ),
        CliOption(
            optionName = NATIVE_PROVIDER_ID_OPTION_NAME,
            valueDescription = "<stable-module-id>",
            description = "Stable identity for a Native generated metadata entry (last value wins)",
            required = false,
            allowMultipleOccurrences = true
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        when (option.optionName) {
            MODULE_COORDINATE_OPTION_NAME -> configuration.put(ModuleCoordinateKey, value)
            JVM_SOURCE_SET_PREFIX_OPTION_NAME -> configuration.put(JvmSourceSetPrefixKey, value)
            GENERATED_PROVIDER_ID_OPTION_NAME -> configuration.put(GeneratedProviderIdKey, value)
            GENERATED_PROVIDER_FQ_NAME_OPTION_NAME -> configuration.put(GeneratedProviderFqNameKey, value)
            GENERATED_PROVIDER_FRAGMENTS_OPTION_NAME -> {
                require(value == "true") {
                    "Compiler option '$GENERATED_PROVIDER_FRAGMENTS_OPTION_NAME' must be 'true'"
                }
                configuration.put(GeneratedProviderFragmentsKey, true)
            }
            SKIP_GENERATED_PROVIDER_OPTION_NAME -> {
                require(value == "true") {
                    "Compiler option '$SKIP_GENERATED_PROVIDER_OPTION_NAME' must be 'true'"
                }
                configuration.put(SkipGeneratedProviderKey, true)
            }
            NATIVE_PROVIDER_ENTRY_FQ_NAME_OPTION_NAME ->
                configuration.put(NativeProviderEntryFqNameKey, value)
            NATIVE_PROVIDER_ID_OPTION_NAME ->
                configuration.put(NativeProviderIdKey, value)
            else -> throw IllegalArgumentException("Unexpected config option ${option.optionName}")
        }
    }
}
