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

package com.kotlinorm.compiler.utils

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

/**
 * IR Verification utilities for testing compiler plugin behavior
 *
 * Provides tools to compile with the real plugin and verify behavior through debug logs
 */
@OptIn(ExperimentalCompilerApi::class)
object IRVerificationUtils {

    /**
     * Compile with real plugin configuration (including debug logging)
     */
    fun compileWithPlugin(
        vararg sources: SourceFile,
        dumpIr: Boolean = false,
        dumpIrPath: String = "build/test-debug",
        dumpIrMode: String = "kotlinLike",
        debug: Boolean = false
    ): CompilationResult {
        val compilation = KotlinCompilation().apply {
            this.sources = sources.toList()
            inheritClassPath = true
            compilerPluginRegistrars = listOf(
                com.kotlinorm.compiler.plugin.KronosCompilerPluginRegistrar()
            )
            commandLineProcessors = listOf(
                com.kotlinorm.compiler.plugin.KronosCommandLineProcessor()
            )
            
            // Set plugin options
            pluginOptions = buildList {
                if (dumpIr) {
                    add(PluginOption("kronos-compiler-plugin-k2", "dump-ir", "true"))
                    add(PluginOption("kronos-compiler-plugin-k2", "dump-ir-path", dumpIrPath))
                    add(PluginOption("kronos-compiler-plugin-k2", "dump-ir-mode", dumpIrMode))
                }
                if (debug) {
                    add(PluginOption("kronos-compiler-plugin-k2", "debug", "true"))
                    add(PluginOption("kronos-compiler-plugin-k2", "dump-ir-path", dumpIrPath))
                }
            }
            
            messageOutputStream = System.out
        }

        val result = compilation.compile()
        
        // Read debug log if enabled (always JSON format, in dumpIrPath/debug.json)
        val debugLog = if (debug) {
            val debugFile = File(dumpIrPath, "debug.json")
            readDebugLog(debugFile.absolutePath)
        } else {
            emptyList()
        }
        
        return CompilationResult(result.exitCode, debugLog, if (debug) File(dumpIrPath, "debug.json").absolutePath else null)
    }

    /**
     * Compile with a custom IR generation extension for testing
     */
    fun compileWithCustomExtension(
        vararg sources: SourceFile,
        extension: org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension,
        dumpIr: Boolean = false,
        dumpIrPath: String = "build/test-debug",
        dumpIrMode: String = "kotlinLike"
    ): CompilationResult {
        val compilation = KotlinCompilation().apply {
            this.sources = sources.toList()
            inheritClassPath = true
            compilerPluginRegistrars = listOf(
                object : org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar() {
                    override val supportsK2: Boolean = true
                    override val pluginId: String = "test-utils-verification-plugin"
                    
                    override fun ExtensionStorage.registerExtensions(configuration: org.jetbrains.kotlin.config.CompilerConfiguration) {
                        org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension.registerExtension(extension)
                    }
                }
            )
            
            messageOutputStream = System.out
        }

        val result = compilation.compile()
        
        // Read debug log (always JSON format, in dumpIrPath/debug.json)
        val debugFile = File(dumpIrPath, "debug.json")
        val debugLog = readDebugLog(debugFile.absolutePath)
        
        return CompilationResult(result.exitCode, debugLog, debugFile.absolutePath)
    }

    /**
     * Compilation result with debug information
     */
    data class CompilationResult(
        val exitCode: KotlinCompilation.ExitCode,
        val debugLog: List<DebugLogger.LogEntry>,
        val debugPath: String?
    ) {
        fun assertSuccess() {
            if (exitCode != KotlinCompilation.ExitCode.OK) {
                throw AssertionError("Compilation failed with exit code: $exitCode")
            }
        }
        
        fun getSymbolResolutions(): List<DebugLogger.LogEntry.SymbolResolution> {
            return debugLog.filterIsInstance<DebugLogger.LogEntry.SymbolResolution>()
        }
        
        fun getTypeJudgments(): List<DebugLogger.LogEntry.TypeJudgment> {
            return debugLog.filterIsInstance<DebugLogger.LogEntry.TypeJudgment>()
        }
        
        fun getPropertyJudgments(): List<DebugLogger.LogEntry.PropertyJudgment> {
            return debugLog.filterIsInstance<DebugLogger.LogEntry.PropertyJudgment>()
        }
        
        fun getAnnotationChecks(): List<DebugLogger.LogEntry.AnnotationCheck> {
            return debugLog.filterIsInstance<DebugLogger.LogEntry.AnnotationCheck>()
        }
        
        fun getInfoMessages(): List<DebugLogger.LogEntry.Info> {
            return debugLog.filterIsInstance<DebugLogger.LogEntry.Info>()
        }
        
        /**
         * Find annotation check for a specific target and annotation
         */
        fun findAnnotationCheck(target: String, annotationName: String): DebugLogger.LogEntry.AnnotationCheck? {
            return getAnnotationChecks().find { 
                it.target == target && it.annotationName == annotationName 
            }
        }
        
        /**
         * Find type judgment for a specific type and judgment
         */
        fun findTypeJudgment(typeName: String, judgment: String): DebugLogger.LogEntry.TypeJudgment? {
            return getTypeJudgments().find { 
                it.typeName.contains(typeName) && it.judgment == judgment 
            }
        }
        
        /**
         * Find info message containing specific text
         */
        fun findInfoMessage(text: String): DebugLogger.LogEntry.Info? {
            return getInfoMessages().find { it.message.contains(text) }
        }
    }

    /**
     * Read debug log from file (JSON format only)
     */
    private fun readDebugLog(path: String): List<DebugLogger.LogEntry> {
        val file = File(path)
        if (!file.exists()) {
            return emptyList()
        }
        
        return try {
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<List<DebugLogger.LogEntry>>(file.readText())
        } catch (e: Exception) {
            println("Warning: Failed to read debug log from $path: ${e.message}")
            emptyList()
        }
    }

    /**
     * Helper to create a source file from string
     */
    fun source(name: String, content: String): SourceFile {
        return SourceFile.kotlin(name, content.trimIndent())
    }
}
