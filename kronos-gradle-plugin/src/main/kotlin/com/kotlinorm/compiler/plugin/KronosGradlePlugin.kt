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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.SetProperty
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
        version = "0.3.1-SNAPSHOT"
        target.logger.lifecycle("Loaded Gradle plugin ${javaClass.name} version $version")
        target.logger.lifecycle("Loaded Compiler plugin $group.$artifactId version $version")
        configureKotlinIncrementalCompilation(target)
        configureGeneratedTypeProviderService(target)
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val target = kotlinCompilation.target.project
        val compilationName = kotlinCompilation.defaultSourceSet.name
        return target.provider {
            val identity = gradleGeneratedTypeProviderIdentity(target.moduleCoordinate(compilationName))
            identity.compilerOptions.map { (key, value) -> SubpluginOption(key, value) }
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

    private fun configureGeneratedTypeProviderService(target: Project) {
        target.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            target.extensions.findByType(SourceSetContainer::class.java)
                ?.configureEach { sourceSet ->
                    val sourceSetName = sourceSet.name
                    val task = target.registerGeneratedTypeProviderService(
                        name = sourceSetName,
                        compilationTaskName = sourceSet.getCompileTaskName("kotlin"),
                        providerClassDirectories = sourceSet.output.classesDirs
                    )
                    // Passing the producer itself preserves task dependencies for every resource consumer.
                    sourceSet.resources.srcDir(task)
                }
        }
        target.pluginManager.withPlugin("com.android.application") {
            target.configureAndroidGeneratedTypeProviderService()
        }
        target.pluginManager.withPlugin("com.android.library") {
            target.configureAndroidGeneratedTypeProviderService()
        }
    }

    private fun Project.configureAndroidGeneratedTypeProviderService() {
        val androidComponents = extensions.findByName("androidComponents")
            ?: error("Android Components extension was not registered")
        val selector = androidComponents.invokeNoArg("selector")
        val allVariants = selector.invokeNoArg("all")
        androidComponents.invokeAction("onVariants", allVariants, Action<Any> { variant ->
            val variantName = variant.invokeNoArg("getName") as String
            val compilationTaskName = variant.invokeMethod("computeTaskName", "compile", "Kotlin") as String
            val task = registerGeneratedTypeProviderService(
                name = variantName,
                compilationTaskName = compilationTaskName,
                providerClassDirectories = provider {
                    tasks.findByName(compilationTaskName)?.outputs?.files ?: emptyList<java.io.File>()
                }
            )
            val resources = variant.invokeNoArg("getSources").invokeNoArg("getResources")
            val outputDirectory: (GenerateKronosGeneratedTypeService) -> DirectoryProperty = {
                it.outputDirectory
            }
            resources.invokeGeneratedSourceDirectory(task, outputDirectory)

            // Android's resource merger needs an explicit merge rule when app and library modules
            // both contribute generated providers.
            @Suppress("UNCHECKED_CAST")
            val mergePaths = variant.invokeNoArg("getPackaging")
                .invokeNoArg("getResources")
                .invokeNoArg("getMerges") as SetProperty<String>
            mergePaths.add(GENERATED_TYPE_PROVIDER_SERVICE_PATH)
        })
    }

    private fun Any.invokeNoArg(name: String): Any = invokeMethod(name)

    private fun Any.invokeMethod(name: String, vararg arguments: Any): Any {
        val method = javaClass.methods.firstOrNull {
            it.name == name && it.parameterCount == arguments.size
        } ?: error("Android Gradle Plugin API method $name/${arguments.size} was not found on ${javaClass.name}")
        return method.invoke(this, *arguments)
            ?: error("Android Gradle Plugin API method $name returned null")
    }

    private fun Any.invokeAction(name: String, selector: Any, action: Action<Any>) {
        val method = javaClass.methods.singleOrNull {
            it.name == name &&
                it.parameterCount == 2 &&
                it.parameterTypes[1] == Action::class.java
        } ?: error("Android Gradle Plugin API method $name(Action) was not found on ${javaClass.name}")
        method.invoke(this, selector, action)
    }

    private fun Any.invokeGeneratedSourceDirectory(
        task: Any,
        outputDirectory: Any
    ) {
        val method = javaClass.methods.singleOrNull {
            it.name == "addGeneratedSourceDirectory" &&
                it.parameterCount == 2 &&
                it.parameterTypes[1].name == "kotlin.jvm.functions.Function1"
        } ?: error("Android Gradle Plugin resources API does not support generated source directories")
        method.invoke(this, task, outputDirectory)
    }

    private fun Project.registerGeneratedTypeProviderService(
        name: String,
        compilationTaskName: String,
        providerClassDirectories: Any
    ) = tasks.register(
        "generateKronos${name.replaceFirstChar { it.uppercaseChar() }}GeneratedTypeService",
        GenerateKronosGeneratedTypeService::class.java
    ) { task ->
        val identity = provider {
            gradleGeneratedTypeProviderIdentity(moduleCoordinate(name))
        }
        task.providerClassName.set(identity.map { it.fqName })
        task.serviceContent.set(identity.map { it.serviceContent })
        task.providerClassDirectories.from(providerClassDirectories)
        task.outputDirectory.set(layout.buildDirectory.dir("generated/kronos/generatedTypeService/$name"))
        task.dependsOn(compilationTaskName)
    }

    private fun Project.moduleCoordinate(compilationName: String): String =
        "gradle:${this.group}:${name}:${path}:$compilationName"
}
