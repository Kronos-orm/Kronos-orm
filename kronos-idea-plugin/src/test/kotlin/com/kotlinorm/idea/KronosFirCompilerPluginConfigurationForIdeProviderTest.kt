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

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(CompilerConfiguration.Internals::class)
class KronosFirCompilerPluginConfigurationForIdeProviderTest {
    @Test
    fun `IDE configuration enables collection literals without mutating imported settings`() {
        val importedSettings = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.KOTLIN_2_4,
            apiVersion = ApiVersion.KOTLIN_2_4,
            specificFeatures = mapOf(
                LanguageFeature.CollectionLiterals to LanguageFeature.State.DISABLED,
                LanguageFeature.ContextSensitiveResolutionUsingExpectedType to LanguageFeature.State.DISABLED,
            ),
        )
        val original = CompilerConfiguration().apply {
            put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, importedSettings)
        }

        val configured = KronosFirCompilerPluginConfigurationForIdeProvider()
            .provideCompilerConfigurationWithCustomOptions(original)
        val configuredSettings = configured.getNotNull(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)

        assertNotSame(original, configured)
        assertSame(importedSettings, original.getNotNull(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS))
        assertFalse(importedSettings.supportsFeature(LanguageFeature.CollectionLiterals))
        assertTrue(configuredSettings.supportsFeature(LanguageFeature.CollectionLiterals))
        assertFalse(configuredSettings.supportsFeature(LanguageFeature.ContextSensitiveResolutionUsingExpectedType))
        assertEquals(importedSettings.languageVersion, configuredSettings.languageVersion)
        assertEquals(importedSettings.apiVersion, configuredSettings.apiVersion)
        assertEquals(
            LanguageFeature.State.ENABLED,
            configuredSettings.getCustomizedLanguageFeatures()[LanguageFeature.CollectionLiterals],
        )
    }

    @Test
    fun `IDE configuration supplies collection literals when imported settings are absent`() {
        val configured = KronosFirCompilerPluginConfigurationForIdeProvider()
            .provideCompilerConfigurationWithCustomOptions(CompilerConfiguration())
        val configuredSettings = configured.getNotNull(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)

        assertTrue(configuredSettings.supportsFeature(LanguageFeature.CollectionLiterals))
    }
}
