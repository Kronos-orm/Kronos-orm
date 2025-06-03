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
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class KronosGradlePlugin : KotlinCompilerPluginSupportPlugin {
    lateinit var project: Project
    lateinit var pluginId: String
    lateinit var group: String
    lateinit var artifactId: String
    lateinit var version: String

    override fun apply(target: Project) {
        super.apply(target)
        project = target
        pluginId = "com.kotlinorm.kronos-compiler-gradle-plugin"
        group = "com.kotlinorm"
        artifactId = "kronos-compiler-plugin"
        version = "0.0.4-SNAPSHOT"
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        println("Loaded Gradle plugin ${javaClass.name} version $version")
        println("Loaded Compiler plugin $group.$artifactId version $version")
        return kotlinCompilation.target.project.provider { listOf() }
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
        KronosGradlePlugin::class.java
    )
}
