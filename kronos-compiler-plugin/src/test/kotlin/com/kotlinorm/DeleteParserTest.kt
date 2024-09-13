package com.kotlinorm

import com.kotlinorm.plugins.KronosParserCompilerPluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
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
//      sourceFile = SourceFile.fromPath(File("/Users/sundaiyue/IdeaProjects/kotlinorm/koto-plugins/src/test/kotlin/com/kotlinorm/plugins/test/UpdateParserTest.kt"))
//    )
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt", """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.Table
            import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
            import com.kotlinorm.beans.dsl.KPojo
            import com.kotlinorm.orm.delete.delete
            import com.kotlinorm.orm.delete.DeleteClause.Companion.build
            import com.kotlinorm.orm.delete.DeleteClause.Companion.by
            import com.kotlinorm.annotations.CreateTime
            import com.kotlinorm.utils.Extensions.mapperTo
                    
            @Table(name = "tb_user")
            data class User(
                var id: Int? = null, // id，主键
                // 用户名
                var username: String? = null,
                var gender: Int? = null, /*性别*/
                /*创建时间*/
                @CreateTime
                var createTime: String? = null
            ) : KPojo


            fun main() {
                Kronos.apply {
                     fieldNamingStrategy = LineHumpNamingStrategy
                     tableNamingStrategy = LineHumpNamingStrategy
                }
                    
                val user = User(1)
                val testUser = User(1, "test")
                val m = mutableMapOf("id" to 2)
                val u = m.mapperTo<User>()
                    
                val (sql, paramMap) = user.delete().by { it.id }.build()
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