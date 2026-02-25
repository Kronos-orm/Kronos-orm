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

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/**
 * Utility functions for compile testing
 */
@OptIn(ExperimentalCompilerApi::class)
object CompileTestUtils {
    
    /**
     * Compiles the given source files with the Kronos compiler plugin
     *
     * @param sourceFiles the source files to compile
     * @param verbose whether to print compilation output
     * @return the compilation result
     */
    fun compile(
        vararg sourceFiles: SourceFile,
        verbose: Boolean = false
    ): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = sourceFiles.asList()
            inheritClassPath = true
            if (verbose) {
                messageOutputStream = System.out
            }
        }.compile()
    }
    
    /**
     * Creates a Kotlin source file with the given name and content
     *
     * @param name the file name (e.g., "User.kt")
     * @param content the file content
     * @return a SourceFile instance
     */
    fun kotlinSource(name: String, content: String): SourceFile {
        return SourceFile.kotlin(name, content.trimIndent())
    }
    
    /**
     * Asserts that the compilation succeeded
     *
     * @param result the compilation result
     */
    fun assertCompilationSucceeded(result: JvmCompilationResult) {
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            error("Compilation failed:\n${result.messages}")
        }
    }
    
    /**
     * Asserts that the compilation failed
     *
     * @param result the compilation result
     */
    fun assertCompilationFailed(result: JvmCompilationResult) {
        if (result.exitCode == KotlinCompilation.ExitCode.OK) {
            error("Expected compilation to fail, but it succeeded")
        }
    }
}
