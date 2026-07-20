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

package com.kotlinorm.idea

import com.kotlinorm.compiler.plugin.KronosCompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.fir.extensions.KotlinFirCompilerPluginConfigurationForIdeProvider

@OptIn(ExperimentalCompilerApi::class)
class KronosFirCompilerPluginConfigurationForIdeProvider : KotlinFirCompilerPluginConfigurationForIdeProvider {
    override fun isConfigurationProviderForCompilerPlugin(registrar: CompilerPluginRegistrar): Boolean =
        KronosIdeaSafe.guard("compiler plugin configuration matching", false) {
            registrar is KronosCompilerPluginRegistrar || registrar.pluginId == KronosCompilerPluginId
        }

    override fun provideCompilerConfigurationWithCustomOptions(original: CompilerConfiguration): CompilerConfiguration =
        KronosIdeaSafe.guard("compiler plugin IDE configuration", original) {
            original.copy().apply {
                val languageVersionSettings = original[CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS]
                    ?: LanguageVersionSettingsImpl.DEFAULT
                put(
                    CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS,
                    KronosIdeaLanguageVersionSettings(languageVersionSettings),
                )
            }
        }
}

private class KronosIdeaLanguageVersionSettings(
    private val delegate: LanguageVersionSettings,
) : LanguageVersionSettings by delegate {
    override fun supportsFeature(feature: LanguageFeature): Boolean =
        feature == LanguageFeature.CollectionLiterals || delegate.supportsFeature(feature)

    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State =
        if (feature == LanguageFeature.CollectionLiterals) {
            LanguageFeature.State.ENABLED
        } else {
            delegate.getFeatureSupport(feature)
        }

    override fun getCustomizedLanguageFeatures(): Map<LanguageFeature, LanguageFeature.State> =
        delegate.getCustomizedLanguageFeatures() +
            (LanguageFeature.CollectionLiterals to LanguageFeature.State.ENABLED)
}

internal const val KronosCompilerPluginId = "kronos-compiler-plugin"
