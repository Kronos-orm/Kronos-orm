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

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Basic tests for the Kronos compiler plugin
 *
 * Tests plugin loading and parameter passing
 */
@OptIn(ExperimentalCompilerApi::class)
class KronosPluginTest {

    @Test
    fun `plugin loads successfully`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "Test.kt",
                """
                package test
                
                fun main() {
                    println("Hello, World!")
                }
                """.trimIndent()
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `plugin accepts dump-ir parameter`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "Test.kt",
                """
                package test
                
                fun main() {
                    println("Hello, World!")
                }
                """.trimIndent()
            ),
            pluginOptions = listOf(
                PluginOption("kronos-compiler-plugin", "dump-ir", "true")
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `plugin accepts dump-ir-path parameter`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "Test.kt",
                """
                package test
                
                fun main() {
                    println("Hello, World!")
                }
                """.trimIndent()
            ),
            pluginOptions = listOf(
                PluginOption("kronos-compiler-plugin", "dump-ir", "true"),
                PluginOption("kronos-compiler-plugin", "dump-ir-path", "build/test-ir-dump")
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `plugin accepts dump-ir-mode kotlinLike`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "Test.kt",
                """
                package test
                
                fun main() {
                    println("Hello, World!")
                }
                """.trimIndent()
            ),
            pluginOptions = listOf(
                PluginOption("kronos-compiler-plugin", "dump-ir", "true"),
                PluginOption("kronos-compiler-plugin", "dump-ir-mode", "kotlinLike")
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `plugin accepts dump-ir-mode common`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "Test.kt",
                """
                package test
                
                fun main() {
                    println("Hello, World!")
                }
                """.trimIndent()
            ),
            pluginOptions = listOf(
                PluginOption("kronos-compiler-plugin", "dump-ir", "true"),
                PluginOption("kronos-compiler-plugin", "dump-ir-mode", "common")
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `plugin accepts dump-ir-files parameter`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "Test.kt",
                """
                package test
                
                fun main() {
                    println("Hello, World!")
                }
                """.trimIndent()
            ),
            pluginOptions = listOf(
                PluginOption("kronos-compiler-plugin", "dump-ir", "true"),
                PluginOption("kronos-compiler-plugin", "dump-ir-files", "Test.kt,User.kt")
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `plugin accepts dump-ir-files with wildcard pattern`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "Test.kt",
                """
                package test
                
                fun main() {
                    println("Hello, World!")
                }
                """.trimIndent()
            ),
            pluginOptions = listOf(
                PluginOption("kronos-compiler-plugin", "dump-ir", "true"),
                PluginOption("kronos-compiler-plugin", "dump-ir-files", "*Service.kt,*Repository.kt")
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    private fun compile(
        sourceFile: SourceFile,
        pluginOptions: List<PluginOption> = emptyList()
    ): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = listOf(sourceFile)
            compilerPluginRegistrars = listOf(KronosCompilerPluginRegistrar())
            commandLineProcessors = listOf(KronosCommandLineProcessor())
            this.pluginOptions = pluginOptions
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()
    }
}
