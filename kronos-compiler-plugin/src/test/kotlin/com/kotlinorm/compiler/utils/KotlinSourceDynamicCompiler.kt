/**
 * Copyright 2022-2026 kronos-orm
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

import com.kotlinorm.compiler.plugin.KronosCommandLineProcessor
import com.kotlinorm.compiler.plugin.KronosCompilerPluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
object KotlinSourceDynamicCompiler {

    fun compile(source: String, fileName: String? = null): JvmCompilationResult {
        return compile(listOf(SourceFile.kotlin(fileName?.let { "$it.kt" } ?: "main.kt", source)))
    }

    internal fun compile(sourceFiles: List<SourceFile>): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = sourceFiles
            compilerPluginRegistrars = listOf(KronosCompilerPluginRegistrar())
            commandLineProcessors = listOf(KronosCommandLineProcessor())
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()
    }
}

val Any.testBaseName: String
    get() = this::class.simpleName!!
