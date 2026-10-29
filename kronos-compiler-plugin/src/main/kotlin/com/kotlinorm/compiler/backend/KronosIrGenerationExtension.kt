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
import com.kotlinorm.compiler.backend.transformers.GeneratedProviderRoots
import com.kotlinorm.compiler.backend.transformers.NativeGeneratedTypeMetadataEntryGenerator
import com.kotlinorm.compiler.plugin.GeneratedTypeProviderMode
import com.kotlinorm.compiler.utils.KPojoClassId
import com.kotlinorm.compiler.utils.KTableForConditionClassId
import com.kotlinorm.compiler.utils.KTableForInsertSelectClassId
import com.kotlinorm.compiler.utils.KTableForReferenceClassId
import com.kotlinorm.compiler.utils.KTableForSelectClassId
import com.kotlinorm.compiler.utils.KTableForSetClassId
import com.kotlinorm.compiler.utils.KTableForSortClassId
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * IR generation extension for the Kronos compiler plugin
 *
 * This is the main entry point for IR transformations
 */
internal class KronosIrGenerationExtension(
    private val configuration: CompilerConfiguration,
    private val generatedTypeProvider: GeneratedTypeProviderMode
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        println("[Kronos] Kronos compiler plugin K2 initialized")

        // A project can apply the Gradle plugin to a Native compilation before the
        // staged common runtime exposes the KPojo and DSL contracts on that classpath.
        // There is no Kronos source to transform in that case; a complete Native
        // consumer runtime still resolves this symbol and follows the normal path.
        if (!pluginContext.hasKronosRuntime()) return

        val provider = when (generatedTypeProvider) {
            is GeneratedTypeProviderMode.Enabled -> generatedTypeProvider
            is GeneratedTypeProviderMode.Native -> null
            GeneratedTypeProviderMode.Disabled -> null
            GeneratedTypeProviderMode.Missing -> error(
                "Missing compiler option 'generated-provider-id'. " +
                    "JVM and Android compilations must configure a generated type provider."
            )
        }
        val errorReporter = ErrorReporter(configuration)

        // Apply transformations
        val transformer = KronosParserTransformer(pluginContext, configuration)
        moduleFragment.transform(transformer, null)
        val projectionTransformer = KronosProjectionIrTransformer(
            pluginContext,
            errorReporter,
            (generatedTypeProvider as? GeneratedTypeProviderMode.Native)
                ?.entryFqName?.asString()?.substringAfterLast('.')?.plus("ProjectionMetadataAnchor")
        )
        moduleFragment.transform(projectionTransformer, null)
        provider?.let {
            GeneratedTypeProviderGenerator.generate(
                pluginContext,
                moduleFragment,
                GeneratedProviderRoots(
                    transformer.kPojoClasses + projectionTransformer.projectionClasses,
                    transformer.enumClasses,
                    transformer.directQueryTaskTypeRoots
                ),
                it.configuration,
                it.useSourceFragments,
                transformer.directQueryTaskTypeRootsByFile,
                errorReporter
            )
        }
        if (generatedTypeProvider is GeneratedTypeProviderMode.Native) {
            NativeGeneratedTypeMetadataEntryGenerator.generate(
                pluginContext = pluginContext,
                moduleFragment = moduleFragment,
                entryFqName = generatedTypeProvider.entryFqName,
                providerId = generatedTypeProvider.id,
                dependencyEntries = generatedTypeProvider.dependencyEntries,
                kPojoClasses = transformer.kPojoClasses + projectionTransformer.projectionClasses,
                enumClasses = transformer.enumClasses,
                directQueryTaskTypeRoots = transformer.directQueryTaskTypeRoots,
                errorReporter = errorReporter
            )
        }
    }
}

private val kronosRuntimeClassIds = listOf(
    KPojoClassId,
    KTableForSelectClassId,
    KTableForInsertSelectClassId,
    KTableForSetClassId,
    KTableForConditionClassId,
    KTableForSortClassId,
    KTableForReferenceClassId
)

@Suppress("DEPRECATION")
private fun IrPluginContext.hasKronosRuntime(): Boolean =
    kronosRuntimeClassIds.all { referenceClass(it) != null }
