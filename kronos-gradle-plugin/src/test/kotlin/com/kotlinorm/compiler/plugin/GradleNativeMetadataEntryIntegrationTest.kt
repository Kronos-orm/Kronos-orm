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
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

/**
 * Exercises the Native source entry through Kotlin/Native, rather than merely
 * checking task output. The fixture intentionally supplies narrow common
 * stubs: Task 06 owns the production common KPojo runtime migration.
 */
class GradleNativeMetadataEntryIntegrationTest {
    private val hostTarget: String
        get() {
            check(System.getProperty("os.name").startsWith("Mac")) {
                "Native metadata integration requires a macOS host."
            }
            return when (System.getProperty("os.arch")) {
                "aarch64", "arm64" -> "macosArm64"
                "x86_64", "amd64" -> "macosX64"
                else -> error("Unsupported macOS architecture: ${System.getProperty("os.arch")}")
            }
        }

    @Test
    fun fillsAndLinksTheCommonNativeMetadataEntry() {
        val kronosVersion = System.getProperty("kronos.version")
            ?: error("Missing kronos.version test property")
        val kotlinVersion = System.getProperty("kronos.kotlin.version")
            ?: error("Missing kronos.kotlin.version test property")
        val projectDir = Files.createTempDirectory("kronos-native-metadata-entry")

        try {
            writeFixture(projectDir, kronosVersion, kotlinVersion)
            val result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(buildList {
                    add("${hostTarget}Test")
                    if (hostTarget == "macosArm64") add("iosSimulatorArm64Test")
                    addAll(
                        listOf(
                            "linkDebugFrameworkIosArm64",
                            "linkDebugFrameworkIosSimulatorArm64",
                            "--stacktrace",
                            "--info",
                            "--console=plain"
                        )
                    )
                })
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":${hostTarget}Test")?.outcome)
            if (hostTarget == "macosArm64") {
                assertEquals(TaskOutcome.SUCCESS, result.task(":iosSimulatorArm64Test")?.outcome)
            }
            assertEquals(TaskOutcome.SUCCESS, result.task(":linkDebugFrameworkIosArm64")?.outcome)
            assertEquals(TaskOutcome.SUCCESS, result.task(":linkDebugFrameworkIosSimulatorArm64")?.outcome)
        } finally {
            deleteRecursively(projectDir)
        }
    }

    private fun writeFixture(projectDir: Path, kronosVersion: String, kotlinVersion: String) {
        val identity = gradleGeneratedTypeProviderIdentity(
            "gradle:com.example:native-metadata-entry-fixture:::commonMain"
        )
        write(projectDir, "settings.gradle.kts", """
            pluginManagement {
                repositories {
                    mavenLocal()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            rootProject.name = "native-metadata-entry-fixture"
            gradle.beforeProject { group = "com.example" }
        """)
        val localNativeHome = Path.of(
            System.getProperty("user.home"),
            ".konan",
            "kotlin-native-prebuilt-macos-${if (hostTarget == "macosArm64") "aarch64" else "x86_64"}-$kotlinVersion"
        )
        if (Files.isDirectory(localNativeHome)) {
            write(projectDir, "gradle.properties", "kotlin.native.home=$localNativeHome")
        }
        write(projectDir, "build.gradle.kts", """
            plugins {
                kotlin("multiplatform") version "$kotlinVersion"
                id("com.kotlinorm.kronos-gradle-plugin")
            }
            repositories {
                mavenLocal()
                mavenCentral()
            }

            kotlin {
                $hostTarget()
                iosArm64 {
                    binaries.framework {
                        baseName = "NativeMetadataEntryFixture"
                    }
                }
                iosSimulatorArm64 {
                    binaries.framework {
                        baseName = "NativeMetadataEntryFixture"
                    }
                }
                sourceSets {
                    commonTest.dependencies {
                        implementation(kotlin("test"))
                    }
                }
            }
        """)
        write(projectDir, "src/commonMain/kotlin/com/kotlinorm/interfaces/KPojo.kt", """
            package com.kotlinorm.interfaces

            interface KPojo
        """)
        write(projectDir, "src/commonMain/kotlin/com/kotlinorm/beans/dsl/KronosDslStubs.kt", """
            package com.kotlinorm.beans.dsl

            class KTableForSelect
            class KTableForInsertSelect
            class KTableForSet
            class KTableForCondition
            class KTableForSort
            class KTableForReference
        """)
        write(projectDir, "src/commonMain/kotlin/com/kotlinorm/utils/StaticMetadataStubs.kt", """
            package com.kotlinorm.utils

            enum class StaticTypeVariance { INVARIANT, IN, OUT, STAR }

            enum class StaticContainerKind { NONE, ITERABLE, ARRAY, PRIMITIVE_ARRAY, MAP }

            data class StaticTypeArgument(
                val variance: StaticTypeVariance,
                val type: StaticTypeRef? = null,
            )

            data class StaticTypeRef(
                val key: String,
                val nullable: Boolean,
                val arguments: List<StaticTypeArgument> = emptyList(),
            )

            data class StaticTypeInfo(
                val type: StaticTypeRef,
                val directSupertypes: List<StaticTypeRef> = emptyList(),
                val containerKind: StaticContainerKind = StaticContainerKind.NONE,
                val elementType: StaticTypeRef? = null,
                val isKPojo: Boolean = false,
                val enumKey: String? = null,
                val declarationVariances: List<StaticTypeVariance> = emptyList(),
            )

            class StaticTypeMetadataRegistry {
                val entries = mutableListOf<Pair<String, List<StaticTypeInfo>>>()

                fun registerGeneratedTypeMetadata(
                    providerId: String,
                    typeInfos: List<StaticTypeInfo>,
                ) {
                    entries += providerId to typeInfos
                }
            }
        """)
        write(projectDir, "src/commonMain/kotlin/com/kotlinorm/beans/task/KronosAtomicQueryTask.kt", """
            package com.kotlinorm.beans.task

            import com.kotlinorm.utils.StaticTypeInfo
            import kotlin.reflect.KType

            class KronosAtomicQueryTask(
                val targetType: KType,
                val targetTypeInfo: StaticTypeInfo? = null,
            )
        """)
        write(projectDir, "src/commonMain/kotlin/fixture/NativeMetadataSource.kt", """
            @file:OptIn(ExperimentalStdlibApi::class)

            package fixture

            import com.kotlinorm.beans.task.KronosAtomicQueryTask
            import kotlin.reflect.typeOf

            enum class NativeStatus { READY, CLOSED }

            val nativeTask = KronosAtomicQueryTask(typeOf<NativeStatus>())
        """)
        write(projectDir, "src/commonTest/kotlin/fixture/NativeMetadataEntryTest.kt", """
            package fixture

            import com.kotlinorm.generated.native.${identity.nativeEntryFqName.substringAfterLast('.')}
            import com.kotlinorm.utils.StaticTypeMetadataRegistry
            import kotlin.test.Test
            import kotlin.test.assertEquals
            import kotlin.test.assertNotNull

            class NativeMetadataEntryTest {
                @Test
                fun generatedEntryRegistersCompilerDescriptors() {
                    val directType = assertNotNull(nativeTask.targetTypeInfo)
                    assertEquals("fixture.NativeStatus", directType.enumKey)

                    val registry = StaticTypeMetadataRegistry()
                    ${identity.nativeEntryFqName.substringAfterLast('.')}(registry)

                    assertEquals(listOf("${identity.id}"), registry.entries.map { it.first })
                    assertEquals(
                        directType,
                        registry.entries.single().second.single { it.type == directType.type }
                    )
                }
            }
        """)
    }

    private fun write(projectDir: Path, relativePath: String, content: String) {
        val target = projectDir.resolve(relativePath)
        Files.createDirectories(target.parent)
        Files.write(target, (content.trimIndent().trim() + "\n").toByteArray(StandardCharsets.UTF_8))
    }

    private fun deleteRecursively(root: Path) {
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
