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

package com.kotlinorm.compiler.backend

import com.kotlinorm.compiler.core.ErrorReporter
import com.kotlinorm.compiler.backend.transformers.KronosProjectionIrTransformer
import com.kotlinorm.compiler.backend.transformers.KronosParserTransformer
import com.kotlinorm.compiler.backend.transformers.GeneratedTypeProviderGenerator
import com.kotlinorm.compiler.plugin.GeneratedTypeProviderConfiguration
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * IR generation extension for the Kronos compiler plugin
 *
 * This is the main entry point for IR transformations
 */
internal class KronosIrGenerationExtension(
    private val messageCollector: MessageCollector,
    private val generatedTypeProvider: GeneratedTypeProviderConfiguration?
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        println("[Kronos] Kronos compiler plugin K2 initialized")

        val errorReporter = ErrorReporter(messageCollector)
        val provider = requireNotNull(generatedTypeProvider) {
            "Kronos generated type provider options were not supplied by the build plugin"
        }

        // Apply transformations
        val transformer = KronosParserTransformer(pluginContext, messageCollector)
        moduleFragment.transform(transformer, null)
        val projectionTransformer = KronosProjectionIrTransformer(pluginContext, errorReporter)
        moduleFragment.transform(projectionTransformer, null)
        GeneratedTypeProviderGenerator.generate(
            pluginContext,
            moduleFragment,
            transformer.kPojoClasses + projectionTransformer.projectionClasses,
            transformer.enumClasses,
            provider,
            errorReporter
        )
    }
}
