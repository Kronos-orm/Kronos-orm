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
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.util.Properties

private const val COMPILER_PLUGIN_VERSION_RESOURCE = "META-INF/kronos/compiler-plugin.properties"

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
        version = compilerPluginVersion()
        target.logger.lifecycle("Loaded Gradle plugin ${javaClass.name} version $version")
        target.logger.lifecycle("Loaded Compiler plugin $group.$artifactId version $version")
        configureGeneratedTypeProviderService(target)
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val target = kotlinCompilation.target.project
        if (kotlinCompilation.target is KotlinNativeTarget) {
            val owner = nativeMetadataOwnerSourceSetName(kotlinCompilation.name, kotlinCompilation.defaultSourceSet.name)
            if (owner == kotlinCompilation.defaultSourceSet.name) {
                val entry = target.registerGeneratedNativeMetadataEntry(owner,
                    target.gradleGeneratedTypeProviderIdentityFor(owner))
                kotlinCompilation.defaultSourceSet.kotlin.srcDir(entry)
            }
            val dependencies = target.nativeCompilationMetadataDependencies(kotlinCompilation)
            val identity = target.gradleGeneratedTypeProviderIdentityFor(owner)
            val entriesLocation = target.layout.buildDirectory.file(
                "generated/kronos/nativeOptions/${kotlinCompilation.target.name}/${kotlinCompilation.name}/dependency-entries.txt")
            val entriesTask = target.tasks.register(
                "generateKronos${kotlinCompilation.target.name.replaceFirstChar(Char::uppercaseChar)}" +
                    "${kotlinCompilation.name.replaceFirstChar(Char::uppercaseChar)}NativeMetadataOptions",
                GenerateKronosNativeMetadataOptions::class.java
            ) { task ->
                task.providerId.set(identity.map { it.id })
                // Keep resolution and producer-task provenance inside Gradle's file collection model.
                task.dependencyMetadata.from(dependencies.incoming.artifactView { }.files)
                task.additionalEntries.set(target.provider {
                    kotlinCompilation.allAssociatedCompilations.map { associated ->
                        val scope = nativeMetadataOwnerSourceSetName(associated.name, associated.defaultSourceSet.name)
                        gradleGeneratedTypeProviderIdentity(target.moduleCoordinate(scope)).nativeEntryFqName + "Dependencies"
                    }.distinct().sorted()
                })
                task.outputFile.set(entriesLocation)
            }
            val entriesFile = entriesTask.flatMap { it.outputFile }
            kotlinCompilation.compileTaskProvider.configure { task ->
                task.inputs.file(entriesFile).withPropertyName("kronosNativeMetadataDependencyEntries")
                    .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.NONE)
            }
            // A producer-backed mapped provider cannot be queried before execution, even for
            // a path. KGP receives the independent location; task inputs retain the producer edge.
            return nativeMetadataEntriesFileOption(entriesLocation).map { fileOption ->
                // KGP may query options while realizing its compile task. Reading an output
                // location is safe here; only the generator task reads dependency artifacts.
                identity.get().nativeCompilerOptions.map { (key, value) -> SubpluginOption(key, value) } +
                    fileOption
            }
        }

        val compilationName = kotlinCompilation.defaultSourceSet.name
        return target.provider {
            val identity = gradleGeneratedTypeProviderIdentity(target.moduleCoordinate(compilationName))
            val options = if (kotlinCompilation.target is KotlinMetadataTarget) {
                // Only commonMain owns the source skeleton. Hierarchical metadata
                // fragments are transformed here and included in the complete target KLIB.
                if (compilationName in setOf("commonMain", "commonTest")) identity.nativeCompilerOptions
                else mapOf(SKIP_GENERATED_PROVIDER_OPTION_NAME to "true")
            } else {
                identity.compilerOptions
            }
            options.map { (key, value) -> SubpluginOption(key, value) }
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
        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            target.extensions.configure(KotlinMultiplatformExtension::class.java) { kotlin ->
                target.configureNativeMetadataSources(kotlin)
                kotlin.targets.withType(KotlinJvmTarget::class.java).configureEach { jvmTarget ->
                    jvmTarget.compilations.configureEach { compilation ->
                        val task = target.registerGeneratedTypeProviderService(
                            name = compilation.defaultSourceSet.name,
                            compilationTaskName = compilation.compileKotlinTaskName,
                            // MPP's aggregate classesDirs includes processed resources and would create a cycle:
                            // compile -> resources (including this task) -> generated provider. The compile task's
                            // own outputs contain the generated classes and keep the producer edge acyclic.
                            providerClassDirectories = compilation.compileTaskProvider
                        )
                        // The generated service belongs to the JVM compilation only; Native uses explicit metadata
                        // registration in later KMP tasks and must not consume JVM ServiceLoader resources.
                        compilation.defaultSourceSet.resources.srcDir(task)
                    }
                }
            }
        }
        target.pluginManager.withPlugin("com.android.application") {
            target.configureAndroidGeneratedTypeProviderService()
        }
        target.pluginManager.withPlugin("com.android.library") {
            target.configureAndroidGeneratedTypeProviderService()
        }
    }

    private fun Project.configureNativeMetadataSources(kotlin: KotlinMultiplatformExtension) {
        val main = kotlin.sourceSets.findByName("commonMain") ?: return
        val mainIdentity = gradleGeneratedTypeProviderIdentityFor(main.name)
        val mainTask = registerGeneratedNativeMetadataEntry(main.name, mainIdentity)
        mainTask.configure { it.dependencyMetadata.from(nativeMetadataDependencies(main.name, listOf(main))) }
        main.kotlin.srcDir(mainTask)
        publishNativeMetadataManifest(mainIdentity)

        kotlin.sourceSets.findByName("commonTest")?.let { test ->
            val testTask = registerGeneratedNativeMetadataEntry(test.name, gradleGeneratedTypeProviderIdentityFor(test.name))
            testTask.configure {
                it.dependencyMetadata.from(nativeMetadataDependencies(test.name, listOf(main, test)))
                it.additionalEntries.set(mainIdentity.map { identity -> listOf(identity.nativeEntryFqName + "Dependencies") })
            }
            test.kotlin.srcDir(testTask)
        }
    }

    private fun Project.nativeMetadataDependencies(name: String, sourceSets: List<KotlinSourceSet>) =
        configurations.create("kronos${name.replaceFirstChar(Char::uppercaseChar)}NativeMetadataDependencies") {
            it.isCanBeConsumed = false
            it.isCanBeResolved = true
            // Dependency entries retain their graphs, preserving implementation visibility on JVM.
            it.isTransitive = false
            it.extendsFrom(*sourceSets.flatMap { sourceSet ->
                listOf(configurations.getByName(sourceSet.apiConfigurationName),
                    configurations.getByName(sourceSet.implementationConfigurationName))
            }.toTypedArray())
            it.attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "kotlin-metadata"))
            it.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
        }

    /** Resolve every participating source set, including platform-only and associated-main dependencies. */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    private fun Project.nativeCompilationMetadataDependencies(compilation: KotlinCompilation<*>) =
        configurations.create("kronos${compilation.target.name.replaceFirstChar(Char::uppercaseChar)}" +
            "${compilation.name.replaceFirstChar(Char::uppercaseChar)}NativeMetadataDependencies") {
            it.isCanBeConsumed = false
            it.isCanBeResolved = true
            it.isTransitive = false
            val metadata = it
            // Source-set hierarchies and associations may be configured after targets are declared.
            // Populate the graph immediately before resolution, once the complete model is available.
            metadata.withDependencies {
                val compilations = compilation.allAssociatedCompilations + compilation
                val sourceSets = compilations.flatMap { associated -> associated.allKotlinSourceSets }.distinct()
                metadata.extendsFrom(*sourceSets.flatMap { sourceSet ->
                    listOf(configurations.getByName(sourceSet.apiConfigurationName),
                        configurations.getByName(sourceSet.implementationConfigurationName))
                }.toTypedArray())
            }
            it.attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "kotlin-metadata"))
            it.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
        }

    private fun Project.publishNativeMetadataManifest(identity: Provider<GradleGeneratedTypeProviderIdentity>) {
        val manifest = tasks.register("generateKronosNativeMetadataManifest", GenerateKronosNativeMetadataManifest::class.java) {
            it.providerId.set(identity.map { provider -> provider.id })
            it.entryFqName.set(identity.map { provider -> provider.nativeEntryFqName + "Dependencies" })
            it.outputFile.set(layout.buildDirectory.file("generated/kronos/nativeManifest/$NATIVE_METADATA_MANIFEST_PATH"))
        }
        tasks.withType(Jar::class.java).matching { it.name == "allMetadataJar" }.configureEach { jar ->
            jar.from(manifest.map { it.outputFile }) { it.into("META-INF/kronos") }
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
        task.providerClassNamePrefix.set(identity.map { it.fqName })
        task.providerClassDirectories.from(providerClassDirectories)
        task.outputDirectory.set(layout.buildDirectory.dir("generated/kronos/generatedTypeService/$name"))
        task.dependsOn(compilationTaskName)
    }

    private fun Project.gradleGeneratedTypeProviderIdentityFor(compilationName: String) =
        provider { gradleGeneratedTypeProviderIdentity(moduleCoordinate(compilationName)) }

    private fun Project.registerGeneratedNativeMetadataEntry(
        sourceSet: String,
        identity: Provider<GradleGeneratedTypeProviderIdentity>
    ) = tasks.register(
        "generateKronos${sourceSet.replaceFirstChar(Char::uppercaseChar)}NativeMetadataEntry",
        GenerateKronosNativeMetadataEntry::class.java
    ) { task ->
        task.entryFqName.set(identity.map { it.nativeEntryFqName })
        task.providerId.set(identity.map { it.id })
        task.outputDirectory.set(layout.buildDirectory.dir("generated/kronos/nativeMetadata/$sourceSet"))
    }

    private fun Project.moduleCoordinate(compilationName: String): String =
        "gradle:${this.group}:${name}:${path}:$compilationName"

    private fun compilerPluginVersion(): String {
        val properties = Properties()
        val stream = javaClass.classLoader.getResourceAsStream(COMPILER_PLUGIN_VERSION_RESOURCE)
            ?: error("Missing $COMPILER_PLUGIN_VERSION_RESOURCE in the Kronos Gradle plugin artifact")
        stream.use(properties::load)
        return properties.getProperty("version")
            ?.takeIf(String::isNotBlank)
            ?: error("Missing compiler plugin version in $COMPILER_PLUGIN_VERSION_RESOURCE")
    }
}
