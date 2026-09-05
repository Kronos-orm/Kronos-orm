/**
 * Copyright 2022-2025 kronos-orm
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

import org.apache.maven.plugin.MojoExecution
import org.apache.maven.project.MavenProject
import org.jetbrains.kotlin.maven.KotlinMavenPluginExtension
import org.jetbrains.kotlin.maven.PluginOption
import java.io.File

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/8/5 16:20
 **/
class KronosMavenPlugin : KotlinMavenPluginExtension {
    override fun isApplicable(
        project: MavenProject,
        execution: MojoExecution
    ) = true

    override fun getCompilerPluginId() = "com.kotlinorm.kronos-maven-plugin"

    override fun getPluginOptions(
        project: MavenProject,
        execution: MojoExecution
    ): MutableList<PluginOption> {
        val compilationName = execution.executionId.ifBlank { execution.goal }
        val moduleCoordinate = "maven:${project.groupId}:${project.artifactId}:$compilationName"
        val identity = mavenGeneratedTypeProviderIdentity(moduleCoordinate)
        syncGeneratedTypeProviderService(project, execution, identity.serviceContent)
        return identity.compilerOptions.mapTo(mutableListOf()) { (key, value) ->
            PluginOption("kronos-maven-plugin", "kronos-compiler-plugin", key, value)
        }
    }

    private fun syncGeneratedTypeProviderService(
        project: MavenProject,
        execution: MojoExecution,
        serviceContent: String
    ) {
        val testCompilation = execution.isTestCompilation()
        val outputDirectory = if (testCompilation) {
            project.build.testOutputDirectory
        } else {
            project.build.outputDirectory
        }
        syncGeneratedTypeProviderService(
            outputDirectory = File(outputDirectory),
            serviceContent = serviceContent,
            hasKotlinSources = project.hasKotlinSources(execution, testCompilation)
        )
    }

    private fun MojoExecution.isTestCompilation(): Boolean =
        goal.contains("test", ignoreCase = true) || executionId.contains("test", ignoreCase = true)

    private fun MavenProject.hasKotlinSources(
        execution: MojoExecution,
        testCompilation: Boolean
    ): Boolean {
        val roots = linkedSetOf<File>()
        val compilationRoots = if (testCompilation) testCompileSourceRoots else compileSourceRoots
        roots.addAll(compilationRoots.map(::File))
        basedir?.resolve(if (testCompilation) "src/test/kotlin" else "src/main/kotlin")?.let(roots::add)
        execution.configuration
            ?.getChild("sourceDirs")
            ?.children
            ?.mapNotNull { it.value?.takeIf { sourceDir -> sourceDir.isNotBlank() } }
            ?.mapTo(roots) { sourceDir -> basedir?.resolve(sourceDir) ?: File(sourceDir) }
        return roots.any(File::containsKotlinSource)
    }
}

internal fun syncGeneratedTypeProviderService(
    outputDirectory: File,
    serviceContent: String,
    hasKotlinSources: Boolean
) {
    val serviceFile = File(
        outputDirectory,
        "META-INF/services/com.kotlinorm.utils.GeneratedTypeProvider"
    )
    if (!hasKotlinSources) {
        serviceFile.delete()
        return
    }
    serviceFile.parentFile.mkdirs()
    serviceFile.writeText(serviceContent)
}

internal fun File.containsKotlinSource(): Boolean = when {
    isFile -> extension in KotlinSourceExtensions
    isDirectory -> walkTopDown().any { source -> source.isFile && source.extension in KotlinSourceExtensions }
    else -> false
}

private val KotlinSourceExtensions = setOf("kt", "kts")
