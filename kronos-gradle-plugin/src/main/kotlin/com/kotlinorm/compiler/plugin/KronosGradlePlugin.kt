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

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

class KronosGradlePlugin : KotlinCompilerPluginSupportPlugin {
    lateinit var project: Project
    lateinit var pluginId: String
    lateinit var group: String
    lateinit var artifactId: String
    lateinit var version: String

    override fun apply(target: Project) {
        super.apply(target)
        project = target
        pluginId = "kronos-compiler-plugin"
        group = "com.kotlinorm"
        artifactId = "kronos-compiler-plugin"
        version = "0.2.3-SNAPSHOT"
        configureKotlinIncrementalCompilation(target)
        configureKPojoFactoryProviderService(target)
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        println("Loaded Gradle plugin ${javaClass.name} version $version")
        println("Loaded Compiler plugin $group.$artifactId version $version")
        return kotlinCompilation.target.project.provider {
            emptyList()
        }
    }

    override fun getCompilerPluginId(): String {
        return pluginId
    }

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        group,
        artifactId,
        version
    )

    /**
     * [isApplicable] is checked against compilations of the project, and if it returns true,
     * then [applyToCompilation] may be called later.
     */
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = project.plugins.hasPlugin(
        KronosGradlePlugin::class.java
    )

    private fun configureKotlinIncrementalCompilation(target: Project) {
        target.tasks.withType(KotlinCompilationTask::class.java).configureEach {
            it.disableIncrementalCompilation()
        }
    }

    private fun KotlinCompilationTask<*>.disableIncrementalCompilation() {
        // KPojo factory generation depends on a complete module IR view.
        javaClass.methods
            .firstOrNull { method ->
                method.name == "setIncremental" &&
                    method.parameterTypes.contentEquals(arrayOf(Boolean::class.javaPrimitiveType))
            }
            ?.invoke(this, false)
    }

    private fun configureKPojoFactoryProviderService(target: Project) {
        target.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            val generatedDir = target.layout.buildDirectory.dir("generated/kronos/KPojoFactoryService")
            val task = target.tasks.register("generateKronosKPojoFactoryService") { task ->
                val outputFile = generatedDir.map {
                    it.file("META-INF/services/com.kotlinorm.utils.KPojoFactoryProvider")
                }
                task.outputs.file(outputFile)
                task.doLast {
                    val file = outputFile.get().asFile
                    file.parentFile.mkdirs()
                    file.writeText("com.kotlinorm.generated.factory.KronosGeneratedKPojoFactoryProvider\n")
                }
            }
            target.extensions.findByType(SourceSetContainer::class.java)
                ?.named("main")
                ?.configure { sourceSet ->
                    sourceSet.resources.srcDir(generatedDir)
                    target.tasks.named(sourceSet.processResourcesTaskName).configure { processResourcesTask ->
                        processResourcesTask.dependsOn(task)
                    }
                }
            target.tasks.matching { it.name == "sourcesJar" }.configureEach { sourcesJar ->
                sourcesJar.dependsOn(task)
            }
        }
    }
}
