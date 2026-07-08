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
import com.kotlinorm.compiler.backend.KronosIrGenerationExtension
import com.kotlinorm.compiler.fir.KronosFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.registerExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * Compiler plugin registrar for the Kronos compiler plugin
 *
 * Registers the IR generation extension with the Kotlin compiler
 */
@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class KronosCompilerPluginRegistrar : CompilerPluginRegistrar() {

    override val pluginId: String = "kronos-compiler-plugin"

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrar.registerExtension(KronosFirExtensionRegistrar())

        val messageCollector = configuration.get(
            org.jetbrains.kotlin.config.CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            org.jetbrains.kotlin.cli.common.messages.MessageCollector.NONE
        )
        IrGenerationExtension.registerExtension(
            KronosIrGenerationExtension(messageCollector)
        )
    }
}
