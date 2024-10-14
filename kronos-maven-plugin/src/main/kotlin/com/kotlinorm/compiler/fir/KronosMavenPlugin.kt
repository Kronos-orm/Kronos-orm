package com.kotlinorm.compiler.fir

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