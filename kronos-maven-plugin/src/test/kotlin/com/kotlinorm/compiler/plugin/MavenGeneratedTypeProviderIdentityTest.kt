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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MavenGeneratedTypeProviderIdentityTest {
    @Test
    fun namingRuleUsesStableSha256Prefix() {
        val identity = mavenGeneratedTypeProviderIdentity("kronos:shared:coordinate")

        assertEquals("kronos:shared:coordinate#e3673cf3b65e57f4", identity.id)
        assertEquals(
            "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_e3673cf3b65e57f4",
            identity.fqName
        )
    }

    @Test
    fun compilerOptionsAndServiceUseTheSameIdentity() {
        val identity = mavenGeneratedTypeProviderIdentity("maven:com.example:sample:default-compile")

        assertEquals(identity.id, identity.compilerOptions[GENERATED_PROVIDER_ID_OPTION_NAME])
        assertEquals(identity.fqName, identity.compilerOptions[GENERATED_PROVIDER_FQ_NAME_OPTION_NAME])
        assertEquals("${identity.fqName}\n", identity.serviceContent)
    }

    @Test
    fun twoModuleServiceEntriesRemainDistinctWhenMerged() {
        val first = mavenGeneratedTypeProviderIdentity("maven:com.example:first:default-compile")
        val second = mavenGeneratedTypeProviderIdentity("maven:com.example:second:default-compile")

        assertNotEquals(first.id, second.id)
        assertNotEquals(first.fqName, second.fqName)
        assertEquals(
            listOf(first.fqName, second.fqName),
            (first.serviceContent + second.serviceContent).lineSequence().filter(String::isNotBlank).toList()
        )
    }

    @Test
    fun emptyCompilationRemovesStaleServiceDescriptorLikeGradle() {
        val outputDirectory = Files.createTempDirectory("kronos-maven-empty").toFile()
        try {
            val emptySourceRoot = outputDirectory.resolve("src/main/kotlin").apply { mkdirs() }
            val serviceFile = outputDirectory.resolve(
                "META-INF/services/com.kotlinorm.utils.GeneratedTypeProvider"
            )
            serviceFile.parentFile.mkdirs()
            serviceFile.writeText("stale.Provider\n")

            assertFalse(emptySourceRoot.containsKotlinSource())
            syncGeneratedTypeProviderService(
                outputDirectory,
                "unused.Provider\n",
                hasKotlinSources = emptySourceRoot.containsKotlinSource()
            )

            assertFalse(serviceFile.exists())
        } finally {
            outputDirectory.deleteRecursively()
        }
    }

    @Test
    fun kotlinSourceCompilationWritesMatchingServiceDescriptor() {
        val root = Files.createTempDirectory("kronos-maven-source").toFile()
        try {
            val sourceRoot = root.resolve("src/main/kotlin").apply { mkdirs() }
            sourceRoot.resolve("Entity.kt").writeText("class Entity")
            val outputDirectory = root.resolve("target/classes")
            val identity = mavenGeneratedTypeProviderIdentity("maven:com.example:sample:default-compile")

            assertTrue(sourceRoot.containsKotlinSource())
            syncGeneratedTypeProviderService(
                outputDirectory,
                identity.serviceContent,
                hasKotlinSources = sourceRoot.containsKotlinSource()
            )

            assertEquals(
                identity.serviceContent,
                outputDirectory.resolve(
                    "META-INF/services/com.kotlinorm.utils.GeneratedTypeProvider"
                ).readText()
            )
        } finally {
            root.deleteRecursively()
        }
    }
}
