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

package com.kotlinorm.compiler.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(CompilerConfiguration.Internals::class)
class KronosCommandLineProcessorTest {
    @Test
    fun exposesPluginMetadata() {
        val processor = KronosCommandLineProcessor()
        val optionNames = processor.pluginOptions.map { it.optionName }

        assertEquals("kronos-compiler-plugin", processor.pluginId)
        assertEquals(
            listOf("generated-provider-id", "generated-provider-fq-name"),
            optionNames
        )
        assertEquals(2, processor.pluginOptions.size)
        processor.pluginOptions.forEach { option ->
            assertFalse(option.required, "${option.optionName} must be optional for IDE/FIR-only loading")
        }
    }

    @Test
    fun allowsProviderIdentityToBeAbsentForIdeFirOnlyLoading() {
        assertNull(CompilerConfiguration().generatedTypeProviderConfigurationOrNull())
    }

    @Test
    fun storesGeneratedProviderOptions() {
        val processor = KronosCommandLineProcessor()
        val configuration = CompilerConfiguration()

        processor.processOption(
            processor.option("generated-provider-id"),
            "gradle:com.example:sample::main#0123456789abcdef",
            configuration
        )
        processor.processOption(
            processor.option("generated-provider-fq-name"),
            "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_0123456789abcdef",
            configuration
        )

        val generatedProvider = configuration.generatedTypeProviderConfiguration()
        assertEquals("gradle:com.example:sample::main#0123456789abcdef", generatedProvider.id)
        assertEquals(
            "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_0123456789abcdef",
            generatedProvider.fqName.asString()
        )
    }

    @Test
    fun requiresBothGeneratedProviderOptionsWhenProviderGenerationIsRequested() {
        assertEquals(
            "Missing compiler option 'generated-provider-id'",
            assertFailsWith<IllegalStateException> {
                CompilerConfiguration().generatedTypeProviderConfiguration()
            }.message
        )

        val idOnly = CompilerConfiguration().apply {
            put(GeneratedProviderIdKey, "gradle:com.example:sample::main#0123456789abcdef")
        }
        assertEquals(
            "Missing compiler option 'generated-provider-fq-name'",
            assertFailsWith<IllegalStateException> {
                idOnly.generatedTypeProviderConfiguration()
            }.message
        )
    }

    @Test
    fun rejectsPartialGeneratedProviderOptionPairs() {
        val idOnly = CompilerConfiguration().apply {
            put(GeneratedProviderIdKey, "gradle:com.example:sample::main#0123456789abcdef")
        }
        val fqNameOnly = CompilerConfiguration().apply {
            put(
                GeneratedProviderFqNameKey,
                "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_0123456789abcdef"
            )
        }

        assertEquals(
            "Missing compiler option 'generated-provider-fq-name'",
            assertFailsWith<IllegalArgumentException> {
                idOnly.generatedTypeProviderConfigurationOrNull()
            }.message
        )
        assertEquals(
            "Missing compiler option 'generated-provider-id'",
            assertFailsWith<IllegalArgumentException> {
                fqNameOnly.generatedTypeProviderConfigurationOrNull()
            }.message
        )
    }

    @Test
    fun rejectsBlankOrInvalidGeneratedProviderIdentity() {
        fun configuration(id: String, fqName: String) = CompilerConfiguration().apply {
            put(GeneratedProviderIdKey, id)
            put(GeneratedProviderFqNameKey, fqName)
        }

        assertEquals(
            "Generated type provider id must not be blank",
            assertFailsWith<IllegalArgumentException> {
                configuration(
                    " ",
                    "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_0123456789abcdef"
                ).generatedTypeProviderConfigurationOrNull()
            }.message
        )
        assertEquals(
            "Generated type provider fq-name must not be blank",
            assertFailsWith<IllegalArgumentException> {
                configuration("valid-id", "").generatedTypeProviderConfigurationOrNull()
            }.message
        )
        assertEquals(
            "Generated type provider must be in com.kotlinorm.generated.factory: com.example.Provider",
            assertFailsWith<IllegalArgumentException> {
                configuration("valid-id", "com.example.Provider").generatedTypeProviderConfigurationOrNull()
            }.message
        )
        assertEquals(
            "Generated type provider class name must be a valid Kotlin identifier: invalid-name",
            assertFailsWith<IllegalArgumentException> {
                configuration(
                    "valid-id",
                    "com.kotlinorm.generated.factory.invalid-name"
                ).generatedTypeProviderConfigurationOrNull()
            }.message
        )
    }

    @OptIn(CompilerConfiguration.Internals::class)
    @Test
    fun rejectsUnknownOption() {
        val processor = KronosCommandLineProcessor()
        val option = CliOption("unknown", "<value>", "unknown", required = false)

        val error = assertFailsWith<IllegalArgumentException> {
            processor.processOption(option, "value", CompilerConfiguration())
        }

        assertEquals("Unexpected config option unknown", error.message)
    }

    private fun KronosCommandLineProcessor.option(name: String): AbstractCliOption =
        pluginOptions.single { it.optionName == name }
}
