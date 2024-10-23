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

package com.kotlinorm.compiler.fir

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class KronosParserCompilerPluginRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val debug = configuration.get(KronosCommandLineProcessor.ARG_OPTION_DEBUG_MODE, false)
        val debugInfoPath = configuration.get(KronosCommandLineProcessor.ARG_OPTION_DEBUG_INFO_PATH, "build/tmp/kronosIrDebug")
        val functions = configuration.get(KronosCommandLineProcessor.ARG_OPTION_FUNCTIONS, "")
        IrGenerationExtension.registerExtension(KronosParserExtension(debug, debugInfoPath,
            functions
            .split(",")
            .filter { it.isNotBlank() }
            .toTypedArray()
        ))
    }
}