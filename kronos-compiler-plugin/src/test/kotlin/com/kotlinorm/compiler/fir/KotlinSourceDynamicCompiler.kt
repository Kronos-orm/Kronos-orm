package com.kotlinorm.compiler.fir

import com.kotlinorm.compiler.fir.KronosCommandLineProcessor.Companion.OPTION_DEBUG_MODE
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.assertEquals

object KotlinSourceDynamicCompiler {
    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        @org.intellij.lang.annotations.Language("kotlin") source: String, fileName: String? = null
    ): JvmCompilationResult {
        val result = compile(sourceFiles = listOf(SourceFile.kotlin(fileName?.let { "$it.kt" } ?: "main.kt", source)))
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