package com.kotoframework.plugins

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class KotoK2GradlePlugin : KotlinCompilerPluginSupportPlugin {
    lateinit var project: Project
    lateinit var pluginId: String
    lateinit var group: String
    lateinit var artifactId: String
    lateinit var version: String

    override fun apply(target: Project) {
        super.apply(target)
        project = target
        pluginId = "com.kotoframework.koto-k2-compiler-plugin"
        group = "com.kotoframework"
        artifactId = "koto-k2-compiler-plugin"
        version = "2.0.0-SNAPSHOT"
        project.extensions.create("kotoK2Parser", KotoK2ParserExtension::class.java)
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(KotoK2ParserExtension::class.java) as KotoK2ParserExtension
        return project.provider {
            listOf()
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


    override fun getPluginArtifactForNative(): SubpluginArtifact = SubpluginArtifact(
        group,
        "$artifactId-native",
        version
    )


    /**
     * [isApplicable] is checked against compilations of the project, and if it returns true,
     * then [applyToCompilation] may be called later.
     */
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = project.plugins.hasPlugin(
        KotoK2GradlePlugin::class.java
    )
}