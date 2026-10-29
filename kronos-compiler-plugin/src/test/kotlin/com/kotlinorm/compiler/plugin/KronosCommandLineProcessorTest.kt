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

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
            listOf(
                "native-provider-dependency-entries",
                "native-provider-dependency-entries-file",
                "module-coordinate",
                "jvm-source-set-prefix",
                "generated-provider-id",
                "generated-provider-fq-name",
                "generated-provider-fragments",
                "skip-generated-provider",
                "native-provider-entry-fq-name",
                "native-provider-id"
            ),
            optionNames
        )
        assertEquals(10, processor.pluginOptions.size)
        processor.pluginOptions.forEach { option ->
            assertFalse(option.required, "${option.optionName} must be optional for IDE/FIR-only loading")
        }
    }

    @Test
    fun nativeDependencyEntriesAreValidatedSortedAndDeduplicated() {
        val processor = KronosCommandLineProcessor()
        val configuration = CompilerConfiguration().apply {
            put(NativeProviderEntryFqNameKey, "fixture.installMetadata")
            put(NativeProviderIdKey, "native:sample")
        }
        processor.processOption(processor.pluginOptions.single {
            it.optionName == NATIVE_PROVIDER_DEPENDENCY_ENTRIES_OPTION_NAME
        }, "fixture.z,fixture.a,fixture.z", configuration)
        val mode = configuration.generatedTypeProviderMode() as GeneratedTypeProviderMode.Native
        assertEquals(listOf("fixture.a", "fixture.z"), mode.dependencyEntries.map { it.asString() })
        configuration.put(NativeProviderDependencyEntriesKey, listOf("fixture.bad-entry"))
        assertFailsWith<IllegalArgumentException> { configuration.generatedTypeProviderMode() }
        assertFailsWith<IllegalArgumentException> {
            CompilerConfiguration().apply {
                put(NativeProviderDependencyEntriesKey, listOf("fixture.a"))
            }.generatedTypeProviderMode()
        }
    }

    @Test
    fun nativeDependencyFileIsReadAtCompilerExecutionAndMergedWithInlineEntries() {
        val directory = Files.createTempDirectory("kronos-native-entries")
        try {
            val file = directory.resolve("generated entries.txt")
            val processor = KronosCommandLineProcessor()
            val configuration = CompilerConfiguration().apply {
                put(NativeProviderEntryFqNameKey, "fixture.installMetadata")
                put(NativeProviderIdKey, "native:sample")
            }
            processor.processOption(processor.option(NATIVE_PROVIDER_DEPENDENCY_ENTRIES_FILE_OPTION_NAME),
                file.toString(), configuration)
            assertFalse(Files.exists(file), "Configuring the compiler must not read or create the task output")
            processor.processOption(processor.option(NATIVE_PROVIDER_DEPENDENCY_ENTRIES_OPTION_NAME),
                "fixture.z,fixture.shared", configuration)
            Files.writeString(file, "fixture.shared\nfixture.a\nfixture.a\n\n")
            fun entries() = (configuration.generatedTypeProviderMode() as GeneratedTypeProviderMode.Native)
                .dependencyEntries.map { it.asString() }
            assertEquals(listOf("fixture.a", "fixture.shared", "fixture.z"), entries())
            Files.writeString(file, "fixture.rebuilt\n")
            assertEquals(listOf("fixture.rebuilt", "fixture.shared", "fixture.z"), entries(),
                "A subsequent compilation must see the producer's new contents")
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test
    fun nativeDependencyFileWorksWithDerivedIdentityAndEmptyDependencies() {
        val directory = Files.createTempDirectory("kronos-native-empty-entries")
        try {
            val file = Files.createFile(directory.resolve("entries.txt"))
            val configuration = CompilerConfiguration().apply {
                put(ModuleCoordinateKey, "com.kotlinorm:kronos-core")
                put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, "kronos-core")
                put(org.jetbrains.kotlin.config.CommonConfigurationKeys.TARGET_PLATFORM,
                    org.jetbrains.kotlin.platform.konan.NativePlatforms.unspecifiedNativePlatform)
                put(NativeProviderDependencyEntriesFileKey, file.toString())
            }
            assertEquals(emptyList(), (configuration.generatedTypeProviderMode() as GeneratedTypeProviderMode.Native).dependencyEntries)
            Files.writeString(file, "fixture.fromTarget\n")
            assertEquals(listOf("fixture.fromTarget"),
                (configuration.generatedTypeProviderMode() as GeneratedTypeProviderMode.Native).dependencyEntries.map { it.asString() })
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test
    fun nativeDependencyFileRejectsMissingDirectoryMalformedUtf8AndInvalidSymbols() {
        val directory = Files.createTempDirectory("kronos-native-invalid-entries")
        try {
            val file = directory.resolve("entries.txt")
            val configuration = CompilerConfiguration().apply {
                put(NativeProviderEntryFqNameKey, "fixture.installMetadata")
                put(NativeProviderIdKey, "native:sample")
                put(NativeProviderDependencyEntriesFileKey, file.toString())
            }
            assertTrue(assertFailsWith<IllegalArgumentException> { configuration.generatedTypeProviderMode() }
                .message.orEmpty().contains(file.toString()))
            configuration.put(NativeProviderDependencyEntriesFileKey, directory.toString())
            assertFailsWith<IllegalArgumentException> { configuration.generatedTypeProviderMode() }
            configuration.put(NativeProviderDependencyEntriesFileKey, file.toString())
            Files.write(file, byteArrayOf(0xc3.toByte(), 0x28))
            val malformed = assertFailsWith<IllegalArgumentException> { configuration.generatedTypeProviderMode() }
            assertTrue(malformed.message.orEmpty().contains("UTF-8"))
            assertTrue(malformed.cause is java.io.IOException)
            Files.writeString(file, "fixture.valid\nfixture.invalid-entry\n")
            assertTrue(assertFailsWith<IllegalArgumentException> { configuration.generatedTypeProviderMode() }
                .message.orEmpty().contains("fixture.invalid-entry"))
            configuration.put(NativeProviderDependencyEntriesFileKey, " ")
            assertFailsWith<IllegalArgumentException> { configuration.generatedTypeProviderMode() }
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test
    fun nativeDependencyFileRequiresNativeProviderEvenWhenTheFileIsEmpty() {
        val directory = Files.createTempDirectory("kronos-native-file-platform")
        try {
            val file = Files.createFile(directory.resolve("entries.txt"))
            assertFailsWith<IllegalArgumentException> {
                CompilerConfiguration().apply {
                    put(NativeProviderDependencyEntriesFileKey, file.toString())
                }.generatedTypeProviderMode()
            }
            assertFailsWith<IllegalArgumentException> {
                CompilerConfiguration().apply {
                    put(ModuleCoordinateKey, "com.kotlinorm:kronos-core")
                    put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, "kronos-core")
                    put(org.jetbrains.kotlin.config.CommonConfigurationKeys.TARGET_PLATFORM,
                        org.jetbrains.kotlin.platform.jvm.JvmPlatforms.defaultJvmPlatform)
                    put(NativeProviderDependencyEntriesFileKey, file.toString())
                }.generatedTypeProviderMode()
            }
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test
    fun moduleCoordinatesPreservePublishedJvmAndNativeProviderIdentities() {
        val cases = listOf(
            Triple("kronos-core", "jvmMain", "dd77fa85ae6af937"),
            Triple("kronos-core_test", "jvmTest", "53ab2bdcf06653bd"),
            Triple("kronos-core", "commonMain", "1bfffc5c3b720294"),
            Triple("kronos-core_test", "commonTest", "c83bf2c80ae4adbc")
        )
        for ((name, scope, hash) in cases) {
            val processor = KronosCommandLineProcessor()
            val configuration = CompilerConfiguration().apply {
                processor.processOption(processor.option(MODULE_COORDINATE_OPTION_NAME), "com.kotlinorm:kronos-core", this)
                processor.processOption(processor.option(JVM_SOURCE_SET_PREFIX_OPTION_NAME), "jvm", this)
                put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, name)
                put(org.jetbrains.kotlin.config.CommonConfigurationKeys.TARGET_PLATFORM,
                    if (scope.startsWith("jvm")) org.jetbrains.kotlin.platform.jvm.JvmPlatforms.defaultJvmPlatform
                    else org.jetbrains.kotlin.platform.konan.NativePlatforms.unspecifiedNativePlatform)
            }
            val id = "gradle:com.kotlinorm:kronos-core::kronos-core:$scope#$hash"
            val expected = if (scope.startsWith("jvm")) GeneratedTypeProviderMode.Enabled(
                GeneratedTypeProviderConfiguration(id, org.jetbrains.kotlin.name.FqName(
                    "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_$hash")), false
            ) else GeneratedTypeProviderMode.Native(id, org.jetbrains.kotlin.name.FqName(
                "com.kotlinorm.generated.native.KronosGeneratedTypeMetadata_$hash"))
            assertEquals(expected, configuration.generatedTypeProviderMode())
        }
    }

    @Test
    fun jvmOnlyModuleCoordinatesRetainMainAndTestProviderIdentities() {
        for ((name, scope, hash) in listOf(
            Triple("kronos-testing", "main", "f345b44112dcc866"),
            Triple("kronos-testing_test", "test", "62877160764389e1")
        )) {
            val configuration = CompilerConfiguration().apply {
                put(ModuleCoordinateKey, "com.kotlinorm:kronos-testing")
                put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, name)
                put(org.jetbrains.kotlin.config.JVMConfigurationKeys.JVM_TARGET, org.jetbrains.kotlin.config.JvmTarget.JVM_1_8)
            }
            assertEquals(GeneratedTypeProviderMode.Enabled(GeneratedTypeProviderConfiguration(
                "gradle:com.kotlinorm:kronos-testing::kronos-testing:$scope#$hash",
                org.jetbrains.kotlin.name.FqName("com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_$hash")
            ), false), configuration.generatedTypeProviderMode())
        }
    }

    @Test
    fun moduleCoordinatesValidateTheirFormatPlatformAndExclusiveIdentityPolicy() {
        fun valid() = CompilerConfiguration().apply {
            put(ModuleCoordinateKey, "com.kotlinorm:kronos-core")
            put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, "kronos-core")
            put(org.jetbrains.kotlin.config.CommonConfigurationKeys.TARGET_PLATFORM,
                org.jetbrains.kotlin.platform.konan.NativePlatforms.unspecifiedNativePlatform)
        }
        assertEquals(GeneratedTypeProviderMode.Missing, CompilerConfiguration().apply {
            put(ModuleCoordinateKey, "com.kotlinorm:kronos-core")
            put(JvmSourceSetPrefixKey, "jvm")
        }.generatedTypeProviderMode())
        for (value in listOf("", "group", "group:module:version", "group:bad/module")) {
            assertFailsWith<IllegalArgumentException> { valid().apply { put(ModuleCoordinateKey, value) }.generatedTypeProviderMode() }
        }
        for (key in listOf(GeneratedProviderIdKey, GeneratedProviderFqNameKey, NativeProviderIdKey, NativeProviderEntryFqNameKey)) {
            assertFailsWith<IllegalArgumentException> { valid().apply { put(key, "explicit") }.generatedTypeProviderMode() }
        }
        assertFailsWith<IllegalArgumentException> { valid().apply { put(SkipGeneratedProviderKey, true) }.generatedTypeProviderMode() }
        assertFailsWith<IllegalArgumentException> { valid().apply { put(GeneratedProviderFragmentsKey, true) }.generatedTypeProviderMode() }
        assertFailsWith<IllegalArgumentException> {
            CompilerConfiguration().apply { put(JvmSourceSetPrefixKey, "jvm") }.generatedTypeProviderMode()
        }
        assertFailsWith<IllegalArgumentException> {
            CompilerConfiguration().apply {
                put(ModuleCoordinateKey, "com.kotlinorm:kronos-core")
                put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, "kronos-core")
            }.generatedTypeProviderMode()
        }
        for (name in listOf("kronos-core", "kronos-core_commonMain", "kronos-core_nativeMain", "kronos-core_appleMain")) {
            assertEquals(GeneratedTypeProviderMode.Missing, valid().apply {
                put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, name)
                put(org.jetbrains.kotlin.config.CommonConfigurationKeys.TARGET_PLATFORM,
                    org.jetbrains.kotlin.platform.CommonPlatforms.defaultCommonPlatform)
            }.generatedTypeProviderMode())
        }
        assertFailsWith<IllegalArgumentException> {
            valid().apply {
                put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, "unknown_commonMain")
                put(org.jetbrains.kotlin.config.CommonConfigurationKeys.TARGET_PLATFORM,
                    org.jetbrains.kotlin.platform.CommonPlatforms.defaultCommonPlatform)
            }.generatedTypeProviderMode()
        }
    }

    @Test
    fun moduleCoordinatesRejectUnrecognizedCompilerScopesAndSourceSetPrefixes() {
        val configuration = CompilerConfiguration().apply {
            put(ModuleCoordinateKey, "com.kotlinorm:kronos-core")
            put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, "kronos-core_integrationTest")
            put(org.jetbrains.kotlin.config.CommonConfigurationKeys.TARGET_PLATFORM,
                org.jetbrains.kotlin.platform.jvm.JvmPlatforms.defaultJvmPlatform)
        }
        assertFailsWith<IllegalStateException> { configuration.generatedTypeProviderMode() }
        configuration.put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, "kronos-core")
        configuration.put(JvmSourceSetPrefixKey, "unknown")
        assertFailsWith<IllegalArgumentException> { configuration.generatedTypeProviderMode() }
    }

    @Test
    fun nativeMetadataKlibsUseFrontendOnlyModeForOfficialSharedFragments() {
        val configuration = CompilerConfiguration().apply {
            put(ModuleCoordinateKey, "com.kotlinorm:kronos-core")
            put(org.jetbrains.kotlin.config.CommonConfigurationKeys.TARGET_PLATFORM,
                org.jetbrains.kotlin.platform.konan.NativePlatforms.unspecifiedNativePlatform)
            put(org.jetbrains.kotlin.config.CommonConfigurationKeys.METADATA_KLIB, true)
        }
        for (fragment in listOf("common", "native", "apple", "ios", "linux", "macos", "mingw", "tvos", "watchos")) {
            for (name in listOf("kronos-core_$fragment", "kronos-core_${fragment}Main")) {
                configuration.put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, name)
                assertEquals(GeneratedTypeProviderMode.Missing, configuration.generatedTypeProviderMode(), name)
            }
        }
        configuration.put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, "kronos-core")
        assertEquals(GeneratedTypeProviderMode.Missing, configuration.generatedTypeProviderMode())
    }

    @Test
    fun nativeMetadataKlibsRejectUnknownModuleAndFragmentNames() {
        val configuration = CompilerConfiguration().apply {
            put(ModuleCoordinateKey, "com.kotlinorm:kronos-core")
            put(org.jetbrains.kotlin.config.CommonConfigurationKeys.TARGET_PLATFORM,
                org.jetbrains.kotlin.platform.konan.NativePlatforms.unspecifiedNativePlatform)
            put(org.jetbrains.kotlin.config.CommonConfigurationKeys.METADATA_KLIB, true)
        }
        for (name in listOf("other-core_native", "kronos-core_unknown", "kronos-core_nativeTest", "kronos-core_native_extra")) {
            configuration.put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, name)
            assertEquals("Unsupported metadata compiler module '$name' for Kronos module 'kronos-core'",
                assertFailsWith<IllegalArgumentException> { configuration.generatedTypeProviderMode() }.message)
        }
    }

    @Test
    fun sharedNativeFragmentNamesRequireTheNativeMetadataCompilerFlag() {
        for (metadata in listOf(false, true)) {
            for (platform in listOf(
                org.jetbrains.kotlin.platform.konan.NativePlatforms.unspecifiedNativePlatform,
                org.jetbrains.kotlin.platform.jvm.JvmPlatforms.defaultJvmPlatform,
                org.jetbrains.kotlin.platform.js.JsPlatforms.defaultJsPlatform
            )) {
                if (metadata && platform == org.jetbrains.kotlin.platform.konan.NativePlatforms.unspecifiedNativePlatform) continue
                val configuration = CompilerConfiguration().apply {
                    put(ModuleCoordinateKey, "com.kotlinorm:kronos-core")
                    put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, "kronos-core_native")
                    put(org.jetbrains.kotlin.config.CommonConfigurationKeys.TARGET_PLATFORM, platform)
                    put(org.jetbrains.kotlin.config.CommonConfigurationKeys.METADATA_KLIB, metadata)
                }
                assertFailsWith<IllegalStateException> { configuration.generatedTypeProviderMode() }
            }
        }
    }

    @Test
    fun allowsProviderIdentityToBeAbsentForIdeFirOnlyLoading() {
        assertNull(CompilerConfiguration().generatedTypeProviderConfigurationOrNull())
        assertEquals(GeneratedTypeProviderMode.Missing, CompilerConfiguration().generatedTypeProviderMode())
    }

    @Test
    fun testSettingsCanOverrideTheMainJvmProviderIdentity() {
        val processor = KronosCommandLineProcessor()
        val configuration = CompilerConfiguration()
        listOf("main", "test").forEach { scope ->
            processor.processOption(processor.option(GENERATED_PROVIDER_ID_OPTION_NAME), scope, configuration)
            processor.processOption(
                processor.option(GENERATED_PROVIDER_FQ_NAME_OPTION_NAME),
                "com.kotlinorm.generated.factory.Provider_$scope", configuration
            )
        }
        assertEquals(
            GeneratedTypeProviderMode.Enabled(
                GeneratedTypeProviderConfiguration("test", org.jetbrains.kotlin.name.FqName(
                    "com.kotlinorm.generated.factory.Provider_test"
                )), false
            ), configuration.generatedTypeProviderMode()
        )
        assertEquals(true, processor.option(GENERATED_PROVIDER_ID_OPTION_NAME).allowMultipleOccurrences)
        assertEquals(true, processor.option(GENERATED_PROVIDER_FQ_NAME_OPTION_NAME).allowMultipleOccurrences)
    }

    @Test
    fun testSettingsCanOverrideTheMainNativeProviderIdentity() {
        val processor = KronosCommandLineProcessor()
        val configuration = CompilerConfiguration()
        listOf("main", "test").forEach { scope ->
            processor.processOption(processor.option(NATIVE_PROVIDER_ID_OPTION_NAME), scope, configuration)
            processor.processOption(
                processor.option(NATIVE_PROVIDER_ENTRY_FQ_NAME_OPTION_NAME),
                "fixture.install_$scope", configuration
            )
        }
        assertEquals(
            GeneratedTypeProviderMode.Native("test", org.jetbrains.kotlin.name.FqName("fixture.install_test")),
            configuration.generatedTypeProviderMode()
        )
        assertEquals(true, processor.option(NATIVE_PROVIDER_ID_OPTION_NAME).allowMultipleOccurrences)
        assertEquals(true, processor.option(NATIVE_PROVIDER_ENTRY_FQ_NAME_OPTION_NAME).allowMultipleOccurrences)
    }

    @Test
    fun acceptsCommonNativeMetadataEntryWithoutJvmProviderIdentity() {
        val processor = KronosCommandLineProcessor()
        val configuration = CompilerConfiguration()
        processor.processOption(
            processor.option(NATIVE_PROVIDER_ENTRY_FQ_NAME_OPTION_NAME),
            "com.kotlinorm.generated.native.installMetadata",
            configuration
        )
        processor.processOption(
            processor.option(NATIVE_PROVIDER_ID_OPTION_NAME),
            "gradle:com.example:sample::commonMain#0123456789abcdef",
            configuration
        )

        assertEquals(
            GeneratedTypeProviderMode.Native(
                "gradle:com.example:sample::commonMain#0123456789abcdef",
                org.jetbrains.kotlin.name.FqName("com.kotlinorm.generated.native.installMetadata")
            ),
            configuration.generatedTypeProviderMode()
        )
    }

    @Test
    fun rejectsIncompleteOrBlankExplicitNativeMetadataIdentity() {
        fun configured(entry: String? = null, id: String? = null) = CompilerConfiguration().apply {
            entry?.let { put(NativeProviderEntryFqNameKey, it) }
            id?.let { put(NativeProviderIdKey, it) }
        }
        val invalid = listOf(
            configured(id = "native:sample") to "Missing compiler option 'native-provider-entry-fq-name'",
            configured(entry = "fixture.installMetadata") to "Missing compiler option 'native-provider-id'",
            configured(entry = "fixture.installMetadata", id = " ") to "Native generated provider id must not be blank"
        )
        for ((configuration, diagnostic) in invalid) {
            assertEquals(diagnostic, assertFailsWith<IllegalArgumentException> {
                configuration.generatedTypeProviderMode()
            }.message)
        }
    }

    @Test
    fun rejectsExplicitNativeMetadataWithJvmProviderOptionsOrSkip() {
        val native = CompilerConfiguration().apply {
            put(NativeProviderEntryFqNameKey, "fixture.installMetadata")
            put(NativeProviderIdKey, "native:sample")
        }
        for (key in listOf(GeneratedProviderIdKey, GeneratedProviderFqNameKey)) {
            assertEquals("Native metadata entry options cannot be combined with JVM generated provider options",
                assertFailsWith<IllegalArgumentException> {
                    native.copy().apply { put(key, "jvm:sample") }.generatedTypeProviderMode()
                }.message)
        }
        assertEquals("Native metadata entry options cannot be combined with 'skip-generated-provider'",
            assertFailsWith<IllegalArgumentException> {
                native.copy().apply { put(SkipGeneratedProviderKey, true) }.generatedTypeProviderMode()
            }.message)
    }

    @Test
    fun rejectsProviderFragmentsWhenProviderGenerationIsSkipped() {
        val processor = KronosCommandLineProcessor()
        val configuration = CompilerConfiguration()
        for (name in listOf(SKIP_GENERATED_PROVIDER_OPTION_NAME, GENERATED_PROVIDER_FRAGMENTS_OPTION_NAME)) {
            processor.processOption(processor.option(name), "true", configuration)
        }
        assertEquals("Compiler option 'generated-provider-fragments' cannot be combined with 'skip-generated-provider'",
            assertFailsWith<IllegalArgumentException> { configuration.generatedTypeProviderMode() }.message)
    }

    @Test
    fun rejectsNonExplicitSourceOwnedProviderFragmentValues() {
        val processor = KronosCommandLineProcessor()
        assertEquals("Compiler option 'generated-provider-fragments' must be 'true'",
            assertFailsWith<IllegalArgumentException> {
                processor.processOption(processor.option(GENERATED_PROVIDER_FRAGMENTS_OPTION_NAME),
                    "false", CompilerConfiguration())
            }.message)
    }

    @Test
    fun rejectsUnsupportedExplicitCompilerPlatformForModuleCoordinates() {
        val platform = org.jetbrains.kotlin.platform.js.JsPlatforms.defaultJsPlatform
        val configuration = CompilerConfiguration().apply {
            put(ModuleCoordinateKey, "com.kotlinorm:kronos-core")
            put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, "kronos-core")
            put(org.jetbrains.kotlin.config.CommonConfigurationKeys.TARGET_PLATFORM, platform)
        }
        assertEquals("Unsupported compiler platform for Kronos module 'kronos-core': $platform",
            assertFailsWith<IllegalArgumentException> { configuration.generatedTypeProviderMode() }.message)
    }

    @Test
    fun acceptsExplicitNativeProviderOptOutOnlyWithoutAnIdentity() {
        val processor = KronosCommandLineProcessor()
        val configuration = CompilerConfiguration()
        processor.processOption(
            processor.option("skip-generated-provider"),
            "true",
            configuration
        )

        assertEquals(GeneratedTypeProviderMode.Disabled, configuration.generatedTypeProviderMode())

        configuration.put(GeneratedProviderIdKey, "gradle:com.example:sample::native#0123456789abcdef")
        configuration.put(
            GeneratedProviderFqNameKey,
            "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_0123456789abcdef"
        )
        assertEquals(
            "Compiler option 'skip-generated-provider' cannot be combined with generated provider options",
            assertFailsWith<IllegalArgumentException> {
                configuration.generatedTypeProviderMode()
            }.message
        )
    }

    @Test
    fun rejectsNonExplicitNativeProviderOptOutValues() {
        val processor = KronosCommandLineProcessor()
        assertEquals(
            "Compiler option 'skip-generated-provider' must be 'true'",
            assertFailsWith<IllegalArgumentException> {
                processor.processOption(
                    processor.option("skip-generated-provider"),
                    "false",
                    CompilerConfiguration()
                )
            }.message
        )
    }

    @Test
    fun enablesSourceOwnedProviderFragments() {
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
        processor.processOption(
            processor.option("generated-provider-fragments"),
            "true",
            configuration
        )

        val mode = configuration.generatedTypeProviderMode()
        assertEquals(
            GeneratedTypeProviderConfiguration(
                "gradle:com.example:sample::main#0123456789abcdef",
                org.jetbrains.kotlin.name.FqName(
                    "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_0123456789abcdef"
                )
            ),
            (mode as GeneratedTypeProviderMode.Enabled).configuration
        )
        assertEquals(true, mode.useSourceFragments)
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
