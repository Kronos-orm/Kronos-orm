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

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

class GradleSourceOwnedProviderFragmentsTest {
    @Test
    fun preservesLiveFactoriesAndRemovesStaleProvidersAcrossSourceLifecycle() {
        val kronosVersion = System.getProperty("kronos.version")
            ?: error("Missing kronos.version test property")
        val kotlinVersion = System.getProperty("kronos.kotlin.version")
            ?: error("Missing kronos.kotlin.version test property")
        val projectDir = Files.createTempDirectory("kronos-source-provider-fragments")

        try {
            writeFixture(projectDir, kronosVersion, kotlinVersion)

            val initial = runFixture(
                projectDir = projectDir,
                types = listOf("fixture.Stable", "fixture.Mutable", "fixture.Moving"),
                tasks = listOf("clean", "run")
            )
            assertFactories(initial, "stable", "initial", "moving")
            val initialServiceEntries = serviceEntries(projectDir)
            val initialProviderIds = providerIds(initial)
            assertEquals(3, initialServiceEntries.size)
            assertEquals(3, initialProviderIds.size)

            writeFixtureFile(
                projectDir,
                "src/main/kotlin/fixture/Mutable.kt",
                mutableKPojo(defaultMarker = "changed")
            )
            val changed = runFixture(
                projectDir = projectDir,
                types = listOf("fixture.Stable", "fixture.Mutable", "fixture.Moving")
            )
            assertIncremental(changed)
            assertFactories(changed, "stable", "changed", "moving")
            assertEquals(initialServiceEntries, serviceEntries(projectDir))
            assertEquals(initialProviderIds, providerIds(changed))

            assertTrue(Files.deleteIfExists(projectDir.resolve("src/main/kotlin/fixture/Mutable.kt")))
            val deleted = runFixture(
                projectDir = projectDir,
                types = listOf("fixture.Stable", "fixture.Moving")
            )
            assertIncremental(deleted)
            assertFactories(deleted, "stable", "moving")
            val deletedServiceEntries = serviceEntries(projectDir)
            val deletedProviderIds = providerIds(deleted)
            assertEquals(2, deletedServiceEntries.size)
            assertEquals(2, deletedProviderIds.size)
            assertEquals(1, initialServiceEntries.minus(deletedServiceEntries).size)
            assertEquals(1, initialProviderIds.minus(deletedProviderIds).size)
            assertEquals(2, initialProviderIds.intersect(deletedProviderIds).size)
            assertStaleProviderClassesAreGone(projectDir, initialServiceEntries.minus(deletedServiceEntries))

            Files.move(
                projectDir.resolve("src/main/kotlin/fixture/Moving.kt"),
                projectDir.resolve("src/main/kotlin/fixture/Moved.kt")
            )
            val renamed = runFixture(
                projectDir = projectDir,
                types = listOf("fixture.Stable", "fixture.Moving")
            )
            assertIncremental(renamed)
            assertFactories(renamed, "stable", "moving")
            val renamedServiceEntries = serviceEntries(projectDir)
            val renamedProviderIds = providerIds(renamed)
            assertEquals(2, renamedServiceEntries.size)
            assertEquals(2, renamedProviderIds.size)
            assertEquals(1, deletedServiceEntries.intersect(renamedServiceEntries).size)
            assertEquals(1, deletedServiceEntries.minus(renamedServiceEntries).size)
            assertEquals(1, renamedServiceEntries.minus(deletedServiceEntries).size)
            assertEquals(1, deletedProviderIds.intersect(renamedProviderIds).size)
            assertEquals(1, deletedProviderIds.minus(renamedProviderIds).size)
            assertEquals(1, renamedProviderIds.minus(deletedProviderIds).size)
            assertStaleProviderClassesAreGone(projectDir, deletedServiceEntries.minus(renamedServiceEntries))
        } finally {
            deleteRecursively(projectDir)
        }
    }

    private fun runFixture(
        projectDir: Path,
        types: List<String>,
        tasks: List<String> = listOf("run")
    ): BuildResult = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withPluginClasspath()
        .withArguments(
            listOf("-Pfixture.types=${types.joinToString(",")}") +
                tasks +
                listOf("--refresh-dependencies", "--info", "--stacktrace")
        )
        .build()

    private fun assertFactories(result: BuildResult, vararg markers: String) {
        val expected = markers.joinToString(separator = ";")
        assertTrue(
            result.output.contains("FACTORIES: $expected"),
            "Expected factory output '$expected', but Gradle output was:\n${result.output}"
        )
    }

    private fun assertIncremental(result: BuildResult) {
        assertTrue(
            result.output.contains("INCREMENTAL_COMPILER"),
            "Expected Kotlin incremental compilation, but Gradle output was:\n${result.output}"
        )
    }

    private fun providerIds(result: BuildResult): Set<String> {
        val line = result.output.lineSequence().singleOrNull { it.startsWith("PROVIDERS: ") }
            ?: error("Missing runtime provider output:\n${result.output}")
        return line.removePrefix("PROVIDERS: ")
            .split(',')
            .filter(String::isNotBlank)
            .toSet()
    }

    private fun serviceEntries(projectDir: Path): Set<String> {
        val serviceFile = projectDir.resolve(
            "build/generated/kronos/generatedTypeService/main/$GENERATED_TYPE_PROVIDER_SERVICE_PATH"
        )
        assertTrue(Files.isRegularFile(serviceFile), "Missing generated provider service file at $serviceFile")
        return Files.readAllLines(serviceFile, StandardCharsets.UTF_8)
            .filter(String::isNotBlank)
            .toSet()
    }

    private fun assertStaleProviderClassesAreGone(projectDir: Path, staleProviders: Set<String>) {
        staleProviders.forEach { provider ->
            val classFile = projectDir.resolve("build/classes/kotlin/main/${provider.replace('.', '/')}.class")
            assertFalse(Files.exists(classFile), "Stale generated provider class remains at $classFile")
        }
    }

    private fun writeFixture(projectDir: Path, kronosVersion: String, kotlinVersion: String) {
        writeFixtureFile(
            projectDir,
            "settings.gradle.kts",
            """
            pluginManagement {
                repositories {
                    mavenLocal()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            rootProject.name = "source-owned-provider-fragments-fixture"
            """
        )
        writeFixtureFile(
            projectDir,
            "build.gradle.kts",
            """
            plugins {
                kotlin("jvm") version "$kotlinVersion"
                application
                id("com.kotlinorm.kronos-gradle-plugin")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                implementation("com.kotlinorm:kronos-core-jvm:$kronosVersion")
                implementation(kotlin("reflect"))
            }

            application {
                mainClass.set("fixture.MainKt")
            }

            tasks.named<org.gradle.api.tasks.JavaExec>("run") {
                args(
                    providers.gradleProperty("fixture.types").orNull
                        ?.split(',')
                        ?.filter { it.isNotBlank() }
                        .orEmpty()
                )
            }
            """
        )
        writeFixtureFile(projectDir, "src/main/kotlin/fixture/Stable.kt", stableKPojo())
        writeFixtureFile(projectDir, "src/main/kotlin/fixture/Mutable.kt", mutableKPojo(defaultMarker = "initial"))
        writeFixtureFile(projectDir, "src/main/kotlin/fixture/Moving.kt", movingKPojo())
        writeFixtureFile(projectDir, "src/main/kotlin/fixture/Main.kt", mainSource())
    }

    private fun writeFixtureFile(projectDir: Path, relativePath: String, content: String) {
        val file = projectDir.resolve(relativePath)
        Files.createDirectories(file.parent)
        Files.write(file, (content.trimIndent().trim() + "\n").toByteArray(StandardCharsets.UTF_8))
    }

    private fun stableKPojo(): String =
        """
        package fixture

        import com.kotlinorm.interfaces.KPojo

        data class Stable(val marker: String = "stable") : KPojo
        """

    private fun mutableKPojo(defaultMarker: String): String =
        """
        package fixture

        import com.kotlinorm.interfaces.KPojo

        data class Mutable(val marker: String = "$defaultMarker") : KPojo
        """

    private fun movingKPojo(): String =
        """
        package fixture

        import com.kotlinorm.interfaces.KPojo

        data class Moving(val marker: String = "moving") : KPojo
        """

    private fun mainSource(): String =
        """
        @file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

        package fixture

        import com.kotlinorm.Kronos
        import com.kotlinorm.utils.GeneratedTypeProvider
        import java.io.File
        import java.net.URL
        import java.net.URLClassLoader
        import java.util.Enumeration
        import java.util.ServiceLoader
        import kotlin.reflect.full.createType

        fun main(args: Array<String>) {
            val markers = args.map { className ->
                val type = Class.forName(className).kotlin.createType()
                val value = Kronos.createKPojo(type)["marker"]
                value?.toString() ?: "<null>"
            }
            println("FACTORIES: " + markers.joinToString(separator = ";"))
            val providers = FixtureProviderClassLoader().use { classLoader ->
                ServiceLoader.load(GeneratedTypeProvider::class.java, classLoader)
                    .map { it.id }
                    .sorted()
            }
            println("PROVIDERS: " + providers.joinToString(separator = ","))
        }

        private const val GENERATED_TYPE_PROVIDER_SERVICE_PATH =
            "META-INF/services/com.kotlinorm.utils.GeneratedTypeProvider"

        private class FixtureProviderClassLoader : URLClassLoader(
            arrayOf(
                File("build/classes/kotlin/main").toURI().toURL(),
                File("build/generated/kronos/generatedTypeService/main").toURI().toURL()
            ),
            GeneratedTypeProvider::class.java.classLoader
        ) {
            override fun getResources(name: String): Enumeration<URL> =
                if (name == GENERATED_TYPE_PROVIDER_SERVICE_PATH) findResources(name) else super.getResources(name)
        }
        """

    private fun deleteRecursively(root: Path) {
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
