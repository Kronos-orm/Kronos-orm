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

import com.intellij.facet.FacetManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.util.Computable
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.util.concurrent.Callable

abstract class KronosIdeaFixtureTestCase : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        addFixtureLibrary("kotlin-stdlib", KotlinArtifacts.instance.kotlinStdlib)
        addFixtureLibrary("kronos-syntax", requiredJar(SyntaxJarProperty))
        addFixtureLibrary("kronos-core", requiredJar(CoreJarProperty))
        configureKronosCompilerPlugin(requiredJar(CompilerPluginJarProperty))
    }

    @OptIn(KaPlatformInterface::class)
    protected fun assertKronosFirPluginLoaded(file: KtFile) {
        val registrarClassNames = ApplicationManager.getApplication()
            .executeOnPooledThread(Callable {
                ApplicationManager.getApplication().runReadAction(Computable {
                    analyze(file) {
                        checkNotNull(KotlinCompilerPluginsProvider.getInstance(project)) {
                            "Kotlin compiler plugins provider is unavailable for the IDEA fixture project"
                        }
                            .getRegisteredExtensions(useSiteModule, FirExtensionRegistrarAdapter)
                            .map { registrar -> registrar.javaClass.name }
                    }
                })
            })
            .get()

        assertTrue(
            "Kronos FIR registrar was not loaded; registered FIR registrars: $registrarClassNames",
            registrarClassNames.any { it == KronosFirRegistrarClassName },
        )
    }

    private fun addFixtureLibrary(name: String, jar: File) {
        PsiTestUtil.addLibrary(testRootDisposable, module, name, "", jar.absolutePath)
    }

    private fun configureKronosCompilerPlugin(compilerPluginJar: File) {
        WriteAction.runAndWait<RuntimeException> {
            val facetType = KotlinFacetType.INSTANCE
            val configuration = facetType.createDefaultConfiguration()
            configuration.settings.apply {
                useProjectSettings = false
                compilerArguments = K2JVMCompilerArguments().apply {
                    pluginClasspaths = arrayOf(compilerPluginJar.absolutePath)
                }
                updateMergedArguments()
            }

            val facetManager = FacetManager.getInstance(module)
            val facet = facetManager.createFacet(facetType, "Kotlin", configuration, null)
            facetManager.createModifiableModel().apply {
                addFacet(facet)
                commit()
            }
        }
    }

    private fun requiredJar(propertyName: String): File {
        val path = System.getProperty(propertyName)
            ?: error("Missing IDEA fixture system property: $propertyName")
        return File(path).also { file ->
            check(file.isFile) { "IDEA fixture jar does not exist: ${file.absolutePath}" }
        }
    }

    private companion object {
        const val CompilerPluginJarProperty = "kronos.idea.test.compilerPluginJar"
        const val CoreJarProperty = "kronos.idea.test.coreJar"
        const val SyntaxJarProperty = "kronos.idea.test.syntaxJar"
        const val KronosFirRegistrarClassName = "com.kotlinorm.compiler.fir.KronosFirExtensionRegistrar"
    }
}
