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
class JoinParserTest {

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
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.orm.delete.delete
            import com.kotlinorm.orm.delete.DeleteClause.Companion.build
            import com.kotlinorm.orm.delete.DeleteClause.Companion.by
            import com.kotlinorm.annotations.CreateTime
            import com.kotlinorm.orm.select.select
            import java.util.Date
            import com.kotlinorm.annotations.Serializable
            import com.kotlinorm.utils.Extensions.safeMapperTo
            import com.kotlinorm.orm.join.join
                    
            @Table(name = "tb_user")
            data class User(
                var id: Int? = null,
                @Serializable
                var username: String? = null,
                var gender: Int? = null,
                @CreateTime
                var createTime: Date? = null
            ) : KPojo

            data class UserRelation(
                var id: Int? = null,
                var username: String? = null,
                var gender: Int? = null,
                var id2: Int? = null
            ) : KPojo



            fun main() {
                Kronos.apply {
                     fieldNamingStrategy = LineHumpNamingStrategy
                     tableNamingStrategy = LineHumpNamingStrategy
                }
                    
            data class C(val id: Int? = null)
            val c = C(1)
            val (sql, paramMap) =
                User(1).join(
                    UserRelation(1, "123", 1, 1),
                ) { user, relation ->
                    leftJoin(relation) { 
                        user.id > 1 &&
                        user.id <= 1 &&
                        1 > user.id &&
                        1 <= user.id &&
                        user.id == 1 &&
                        user.id != 1 &&
                        c.id >= user.id &&
                        user.id > relation.id2.value &&
                        relation.id2.value >= user.id &&
                        relation.id2.toString()!!.cast() >= user.id
                    }
                    select {
                        user.id + relation.gender
                    }
                    where { user.id == 1 }
                    orderBy { user.id.desc() }
                }.build()
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