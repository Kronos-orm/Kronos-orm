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

package com.kotlinorm.toolchain

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateNativeMetadataTest {
    @Test
    fun moduleCoordinatesPreserveJvmServicesAndNativeEntryNames() {
        val directory = Files.createTempDirectory("kronos-toolchain-metadata")
        try {
            generate(directory)
            assertEquals("com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_dd77fa85ae6af937\n",
                directory.resolve("jvm-main/$SERVICE").toFile().readText())
            assertEquals("com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_53ab2bdcf06653bd\n",
                directory.resolve("jvm-test/$SERVICE").toFile().readText())
            assertEquals("gradle:com.kotlinorm:kronos-core::kronos-core:commonMain#1bfffc5c3b720294\t" +
                "com.kotlinorm.generated.native.KronosGeneratedTypeMetadata_1bfffc5c3b720294Dependencies\n",
                directory.resolve("resources/$NATIVE_METADATA_MANIFEST_PATH").toFile().readText())
            assertFalse(Files.exists(directory.resolve("resources/$SERVICE")))
            val mainEntry = "KronosGeneratedTypeMetadata_1bfffc5c3b720294"
            assertTrue(Files.isRegularFile(directory.resolve("main/com/kotlinorm/generated/native/$mainEntry.kt")))
            assertTrue(Files.isRegularFile(directory.resolve(
                "main/com/kotlinorm/generated/projection/${mainEntry}ProjectionMetadataAnchor.kt")))
            val testEntry = "KronosGeneratedTypeMetadata_c83bf2c80ae4adbc"
            val test = directory.resolve("test/com/kotlinorm/generated/native/$testEntry.kt").toFile().readText()
            assertEquals(listOf("com.kotlinorm.generated.native.${mainEntry}Dependencies(registry)",
                "$testEntry(registry)"), test.lineSequence().map(String::trim).filter { it.endsWith("(registry)") }.toList())
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test
    fun jvmOnlyModulesUseMainAndTestSourceSetNames() {
        val directory = Files.createTempDirectory("kronos-toolchain-jvm-metadata")
        try {
            generate(directory, module = "kronos-testing", prefix = "")
            assertEquals("com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_f345b44112dcc866\n",
                directory.resolve("jvm-main/$SERVICE").toFile().readText())
            assertEquals("com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_62877160764389e1\n",
                directory.resolve("jvm-test/$SERVICE").toFile().readText())
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test
    fun disablingGenerationRemovesStaleServiceDescriptorsAndSourceAnchors() {
        val directory = Files.createTempDirectory("kronos-toolchain-plain-module")
        try {
            generate(directory)
            generate(directory, enabled = false)
            for (name in listOf("main", "test", "resources", "jvm-main", "jvm-test")) {
                assertEquals(emptyList(), directory.resolve(name).toFile().walkTopDown().filter { it.isFile }.toList())
            }
        } finally { directory.toFile().deleteRecursively() }
    }

    private fun generate(directory: Path, enabled: Boolean = true,
                         module: String = "kronos-core", prefix: String = "jvm") {
        generateNativeMetadata(module, "com.kotlinorm", enabled, prefix, emptyList(),
            directory.resolve("main"), directory.resolve("test"), directory.resolve("resources"),
            directory.resolve("jvm-main"), directory.resolve("jvm-test"))
    }

    private companion object {
        const val SERVICE = "META-INF/services/com.kotlinorm.utils.GeneratedTypeProvider"
    }
}
