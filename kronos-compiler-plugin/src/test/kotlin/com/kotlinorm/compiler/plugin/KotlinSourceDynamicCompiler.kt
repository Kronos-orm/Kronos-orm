package com.kotlinorm.compiler.plugin

import com.kotlinorm.compiler.plugin.KronosCommandLineProcessor.Companion.OPTION_DEBUG_MODE
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

object KotlinSourceDynamicCompiler {
    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        @org.intellij.lang.annotations.Language("kotlin") source: String, fileName: String? = null
    ): JvmCompilationResult {
        val result = compile(sourceFiles = listOf(SourceFile.kotlin(fileName?.let { "$it.kt" } ?: "main.kt", source)))
        return result
    }

    data class KotlinSourceFile(@org.intellij.lang.annotations.Language("kotlin") val source: String, val fileName: String? = null)

    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        vararg sourceFiles: KotlinSourceFile,
    ): JvmCompilationResult {
        val result = compile(sourceFiles = sourceFiles.map { SourceFile.kotlin(it.fileName?.let { "$it.kt" } ?: "main.kt", it.source) })
        return result
    }

    @OptIn(ExperimentalCompilerApi::class)
    internal fun compile(
        sourceFiles: List<SourceFile>,
        plugin: CompilerPluginRegistrar = KronosParserCompilerPluginRegistrar(),
    ): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = sourceFiles
            compilerPluginRegistrars = listOf(plugin)
            val processor = KronosCommandLineProcessor()
            commandLineProcessors = listOf(processor)
            pluginOptions = listOf(
                processor.option(OPTION_DEBUG_MODE, "true"),
            )
            inheritClassPath = true
        }.compile()
    }

    @OptIn(ExperimentalCompilerApi::class)
    private fun CommandLineProcessor.option(key: String, value: Any?): PluginOption {
        return PluginOption(pluginId, key, value.toString())
    }
}

val Any.testBaseName
    get() = this::class.simpleName!!