package com.kotoframework

import com.kotoframework.plugins.KronosParserCompilerPluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/6 17:07
 **/
class DeleteParserTest {

    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `IR plugin success`() {
//    val result = compile(
//      sourceFile = SourceFile.fromPath(File("/Users/sundaiyue/IdeaProjects/kotoframework/koto-plugins/src/test/kotlin/com/kotoframework/plugins/test/UpdateParserTest.kt"))
//    )
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt", """
            import com.kotoframework.annotations.Table
//            import com.kotoframework.beans.namingStrategy.LineHumpNamingStrategy
            import com.kotoframework.interfaces.KPojo
            import com.kotoframework.orm.delete.delete
                    
            @Table(name = "tb_user")
            data class User(
                var id: Int? = null,
                var username: String? = null,
                var gender: Int? = null
            ) : KPojo
                    
            fun main() {
//                Kronos.apply {
//                     fieldNamingStrategy = LineHumpNamingStrategy
//                     tableNamingStrategy = LineHumpNamingStrategy
//                }
                    
                val user = User(1)
                val testUser = User(1, "test")
                    
                val (sql, paramMap) = user.delete().by { it.id }
            }        
      """.trimIndent()
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("MainKt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }

    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        sourceFiles: List<SourceFile>,
        plugin: CompilerPluginRegistrar = KronosParserCompilerPluginRegistrar(),
    ): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = sourceFiles
            compilerPluginRegistrars = listOf(plugin)
            inheritClassPath = true
        }.compile()
    }

    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        sourceFile: SourceFile,
        plugin: CompilerPluginRegistrar = KronosParserCompilerPluginRegistrar(),
    ): JvmCompilationResult {
        return compile(listOf(sourceFile), plugin)
    }
}