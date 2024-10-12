package com.kotlinorm.plugins

import com.kotlinorm.plugins.KronosParserCompilerPluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.assertEquals

object KotlinSourceDynamicCompiler {
    @OptIn(ExperimentalCompilerApi::class)
    fun compile(@org.intellij.lang.annotations.Language("kotlin") source: String): JvmCompilationResult {
        val result = compile(
            sourceFiles = listOf(
                SourceFile.kotlin(
                    "main.kt", source
                )
            )
        )
        return result
    }

    @OptIn(ExperimentalCompilerApi::class)
    private fun compile(
        sourceFiles: List<SourceFile>,
        plugin: CompilerPluginRegistrar = KronosParserCompilerPluginRegistrar(),
    ): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = sourceFiles
            compilerPluginRegistrars = listOf(plugin)
            inheritClassPath = true
        }.compile()
    }
}