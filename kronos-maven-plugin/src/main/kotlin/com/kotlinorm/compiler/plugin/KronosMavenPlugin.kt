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
        println("Loaded Maven plugin " + javaClass.name)
        return mutableListOf()
    }
}