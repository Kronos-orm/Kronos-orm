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

package com.kotlinorm.compiler

import com.kotlinorm.compiler.plugin.KronosCompilerPluginRegistrar
import com.kotlinorm.compiler.plugin.GeneratedProviderFqNameKey
import com.kotlinorm.compiler.plugin.GeneratedProviderFragmentsKey
import com.kotlinorm.compiler.plugin.GeneratedProviderIdKey
import com.kotlinorm.compiler.plugin.NativeProviderEntryFqNameKey
import com.kotlinorm.compiler.plugin.NativeProviderIdKey
import com.kotlinorm.compiler.plugin.NativeProviderDependencyEntriesKey
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.JvmClassFileArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractFirLightTreeDiagnosticsTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractJvmBlackBoxCodegenTestBase
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.getOrCreateTempDirectory
import org.opentest4j.AssertionFailedError
import java.io.File
import java.security.MessageDigest

/**
 * Base class for Kronos official JVM box tests.
 *
 * These tests run through Kotlin's official FIR black-box codegen pipeline instead of
 * the lightweight compile-testing harness. That gives Kronos coverage for real FIR
 * resolution, FIR-to-IR conversion, backend code generation, IR verification, and
 * execution of `box(): String` from files under `kronos-compiler-plugin/testData/box`.
 */
abstract class AbstractKronosJvmBoxTest(
    parser: FirParser = FirParser.LightTree,
) : AbstractJvmBlackBoxCodegenTestBase(parser) {
    init {
        KronosCompilerTestEnvironment.prepare()
    }

    /** Exercises the Native explicit-entry backend policy through the official IR verifier. */
    protected open val nativeMetadataEntry: String? = null

    protected open val sourceProviderFragments: Boolean = false

    protected open val nativeMetadataDependencyEntries: List<String> = emptyList()

    /**
     * Uses the Kotlin test framework's environment-provided stdlib locations.
     */
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        KronosCompilerTestEnvironment.prepare()
        return EnvironmentBasedStandardLibrariesPathProvider
    }

    /**
     * Registers Kronos compiler plugin configuration, runtime classpath, and language flags for testData sources.
     */
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(this)

        defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.FULL_JDK
            +JvmEnvironmentConfigurationDirectives.WITH_REFLECT
            +CodegenTestDirectives.IGNORE_DEXING
            LanguageSettingsDirectives.LANGUAGE with "+CollectionLiterals"
        }

        useConfigurators({ services -> KronosCompilerPluginConfigurator(services, nativeMetadataEntry, sourceProviderFragments, nativeMetadataDependencyEntries) })
        useCustomRuntimeClasspathProviders({ services ->
            KronosRuntimeClasspathProvider(services, nativeMetadataEntry == null, sourceProviderFragments)
        })
    }

    protected val testDataDir: String
        get() = File(KronosCompilerTestEnvironment.projectDir, "testData").absolutePath
}

/**
 * Binds a JUnit test class to one `testData/box/<directory>` suite.
 *
 * Concrete suite classes call `box("caseName")`; this helper expands it to the
 * corresponding `.kt` test data file and lets the official compiler test runner
 * compile and execute its `box()` function.
 */
abstract class AbstractKronosJvmBoxSuite(
    private val directory: String,
    parser: FirParser = FirParser.LightTree,
) : AbstractKronosJvmBoxTest(parser) {
    /**
     * Runs one `.kt` file from this suite's `testData/box` directory.
     */
    protected fun box(name: String) {
        runTest("$testDataDir/box/$directory/$name.kt")
    }
}

/**
 * Base class for Kronos official FIR diagnostics tests.
 */
abstract class AbstractKronosJvmDiagnosticsTest : AbstractFirLightTreeDiagnosticsTest() {
    init {
        KronosCompilerTestEnvironment.prepare()
    }

    /**
     * Uses the Kotlin test framework's environment-provided stdlib locations.
     */
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        KronosCompilerTestEnvironment.prepare()
        return EnvironmentBasedStandardLibrariesPathProvider
    }

    /**
     * Registers Kronos compiler plugin configuration and language flags for diagnostics testData sources.
     */
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(this)

        defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.FULL_JDK
            LanguageSettingsDirectives.LANGUAGE with "+CollectionLiterals"
        }

        useConfigurators(::KronosCompilerPluginConfigurator)
    }

    protected val testDataDir: String
        get() = File(KronosCompilerTestEnvironment.projectDir, "testData").absolutePath
}

/**
 * Binds a JUnit test class to one `testData/diagnostics/<directory>` suite.
 */
abstract class AbstractKronosJvmDiagnosticsSuite(
    private val directory: String
) : AbstractKronosJvmDiagnosticsTest() {
    /**
     * Runs one `.kt` file from this suite's `testData/diagnostics` directory.
     */
    protected fun diagnostics(name: String) {
        val testDataFile = "$testDataDir/diagnostics/$directory/$name.kt"
        try {
            runTest(testDataFile)
        } catch (error: AssertionFailedError) {
            writeDiagnosticsActual(name, error)
            throw error
        }
    }

    private fun writeDiagnosticsActual(name: String, error: AssertionFailedError) {
        if (!error.isActualDefined) return
        val actual = error.actual.stringRepresentation ?: return
        val projectDir = KronosCompilerTestEnvironment.projectDir
        val actualFile = File(projectDir, "build/tmp/kronos-diagnostics-actual/$directory/$name.kt")
        actualFile.parentFile.mkdirs()
        actualFile.writeText(actual)
    }
}

@OptIn(ExperimentalCompilerApi::class)
private class KronosCompilerPluginConfigurator(
    testServices: TestServices,
    private val nativeEntry: String? = null,
    private val sourceFragments: Boolean = false,
    private val nativeDependencyEntries: List<String> = emptyList()
) : EnvironmentConfigurator(testServices) {
    private val registrar = KronosCompilerPluginRegistrar()

    /**
     * Adds Kronos test runtime artifacts to the compiler classpath for testData compilation.
     */
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.addJvmClasspathRoots(kronosCompilerTestClasspath)
        val identity = testGeneratedTypeProviderIdentity("kronos-test:${module.name}")
        if (nativeEntry != null) {
            configuration.put(NativeProviderIdKey, identity.id)
            configuration.put(NativeProviderEntryFqNameKey, nativeEntry)
            configuration.put(NativeProviderDependencyEntriesKey, nativeDependencyEntries)
        } else {
            configuration.put(GeneratedProviderIdKey, identity.id)
            configuration.put(GeneratedProviderFqNameKey, identity.fqName)
            configuration.put(GeneratedProviderFragmentsKey, sourceFragments)
        }
    }

    /**
     * Registers the actual Kronos compiler plugin under test.
     */
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        with(registrar) {
            registerExtensions(configuration)
        }
    }
}

private data class TestGeneratedTypeProviderIdentity(val id: String, val fqName: String)

private fun testGeneratedTypeProviderIdentity(moduleCoordinate: String): TestGeneratedTypeProviderIdentity {
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(moduleCoordinate.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        .take(16)
    return TestGeneratedTypeProviderIdentity(
        id = "$moduleCoordinate#$hash",
        fqName = "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_$hash"
    )
}

private class KronosRuntimeClasspathProvider(
    testServices: TestServices,
    private val includeGeneratedServices: Boolean = true,
    private val sourceFragments: Boolean = false
) : RuntimeClasspathProvider(testServices) {
    /**
     * Provides runtime artifacts needed when the generated `box()` method is executed.
     */
    override fun runtimeClassPaths(module: TestModule): List<File> {
        if (!includeGeneratedServices) return kronosCompilerTestClasspath
        val providerResources = buildList {
            val visited = mutableSetOf<TestModule>()

            fun addProviderResource(current: TestModule) {
                if (!visited.add(current)) return
                add(generatedProviderResourceRoot(current))
                current.allDependencies
                    .filter { it.kind == DependencyKind.Binary }
                    .forEach { addProviderResource(it.dependencyModule) }
            }

            addProviderResource(module)
        }
        return kronosCompilerTestClasspath + providerResources
    }

    private fun generatedProviderResourceRoot(module: TestModule): File {
        val identity = testGeneratedTypeProviderIdentity("kronos-test:${module.name}")
        val identityHash = identity.fqName.substringAfterLast('_')
        val resourceRoot = testServices.getOrCreateTempDirectory("kronos-provider-$identityHash")
        val serviceFile = File(resourceRoot, GeneratedTypeProviderServicePath)
        serviceFile.parentFile.mkdirs()
        val providers = if (sourceFragments) {
            // The JVM backend's file identities can differ from frontend in-memory paths, and
            // files without contributions emit no fragment. Advertise the actual module output.
            val artifact = testServices.artifactsProvider.getArtifact(module, ArtifactKinds.Jvm) as JvmClassFileArtifact
            val providerName = Regex("${Regex.escape(identity.fqName)}_[0-9a-f]{16}")
            artifact.classFileFactory.asList().mapNotNull { output ->
                output.relativePath.takeIf { it.endsWith(".class") }
                    ?.removeSuffix(".class")?.replace('/', '.')
                    ?.takeIf(providerName::matches)
            }.sorted()
        } else listOf(identity.fqName)
        serviceFile.writeText(providers.joinToString(separator = "\n", postfix = "\n"))
        return resourceRoot
    }
}

private const val GeneratedTypeProviderServicePath =
    "META-INF/services/com.kotlinorm.utils.GeneratedTypeProvider"

private val kronosCompilerTestClasspath: List<File>
    get() = KronosCompilerTestEnvironment.classpath
