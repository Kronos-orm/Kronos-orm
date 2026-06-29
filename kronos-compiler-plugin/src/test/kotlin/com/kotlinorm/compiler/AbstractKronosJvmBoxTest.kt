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
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirBlackBoxCodegenTestBase
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

/**
 * Base class for Kronos official JVM box tests.
 *
 * These tests run through Kotlin's official FIR black-box codegen pipeline instead of
 * the lightweight compile-testing harness. That gives Kronos coverage for real FIR
 * resolution, FIR-to-IR conversion, backend code generation, IR verification, and
 * execution of `box(): String` from files under `kronos-compiler-plugin/testData/box`.
 */
abstract class AbstractKronosJvmBoxTest : AbstractFirBlackBoxCodegenTestBase(FirParser.LightTree) {
    /**
     * Uses the Kotlin test framework's environment-provided stdlib locations.
     */
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }

    /**
     * Registers Kronos compiler plugin configuration, runtime classpath, and language flags for testData sources.
     */
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(this)

        defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.FULL_JDK
            +CodegenTestDirectives.IGNORE_DEXING
            LanguageSettingsDirectives.LANGUAGE with "+CollectionLiterals"
        }

        useConfigurators(::KronosCompilerPluginConfigurator)
        useCustomRuntimeClasspathProviders(::KronosRuntimeClasspathProvider)
    }

    protected val testDataDir: String
        get() = requireNotNull(System.getProperty("kronos.compiler.plugin.projectDir")) {
            "Missing kronos.compiler.plugin.projectDir"
        } + "/testData"
}

/**
 * Binds a JUnit test class to one `testData/box/<directory>` suite.
 *
 * Concrete suite classes call `box("caseName")`; this helper expands it to the
 * corresponding `.kt` test data file and lets the official compiler test runner
 * compile and execute its `box()` function.
 */
abstract class AbstractKronosJvmBoxSuite(
    private val directory: String
) : AbstractKronosJvmBoxTest() {
    /**
     * Runs one `.kt` file from this suite's `testData/box` directory.
     */
    protected fun box(name: String) {
        runTest("$testDataDir/box/$directory/$name.kt")
    }
}

@OptIn(ExperimentalCompilerApi::class)
private class KronosCompilerPluginConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    private val registrar = KronosCompilerPluginRegistrar()

    /**
     * Adds Kronos test runtime artifacts to the compiler classpath for testData compilation.
     */
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.addJvmClasspathRoots(kronosCompilerTestClasspath)
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

private class KronosRuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    /**
     * Provides runtime artifacts needed when the generated `box()` method is executed.
     */
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return kronosCompilerTestClasspath
    }
}

private val kronosCompilerTestClasspath: List<File> by lazy {
    val classpath = System.getProperty("kronos.compiler.test.classpath")
        ?: error("Missing kronos.compiler.test.classpath system property")

    classpath
        .split(File.pathSeparator)
        .filter(String::isNotBlank)
        .map(::File)
}
