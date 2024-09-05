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
class SelectParserTest {

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
            import com.kotlinorm.annotations.TableIndex
            import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
            import com.kotlinorm.beans.dsl.KPojo
            import com.kotlinorm.orm.delete.delete
            import com.kotlinorm.orm.delete.DeleteClause.Companion.build
            import com.kotlinorm.orm.delete.DeleteClause.Companion.by
            import com.kotlinorm.annotations.CreateTime
            import com.kotlinorm.annotations.Default
            import com.kotlinorm.orm.select.select
            import java.util.Date
            import com.kotlinorm.annotations.Serializable
            import com.kotlinorm.utils.Extensions.safeMapperTo
            import com.kotlinorm.annotations.*
            import com.kotlinorm.enums.KColumnType.CHAR
            import com.kotlinorm.enums.SQLite
            import java.time.LocalDateTime

            @Table(name = "tb_user")
            @TableIndex("aaa", ["username"], SQLite.KIndexType.BINARY, SQLite.KIndexMethod.UNIQUE)
            @TableIndex(  "bbb",columns = ["username","gender"], type = SQLite.KIndexType.NOCASE)
            @TableIndex(  "ccc",columns = ["gender"])
            data class SqlliteUser(
                @PrimaryKey(identity = true)
                var id: Int? = null,
                var username: String? = null,
                @Column("gender")
                @ColumnType(CHAR)
                @Default("0")
                var gender: Int? = null,
            //    @ColumnType(INT)
            //    var age: Int? = null,
                @CreateTime
                @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
                @NotNull
                var createTime: String? = null,
                @UpdateTime
                @NotNull
                var updateTime: LocalDateTime? = null,
                @LogicDelete
                @NotNull
                var deleted: Boolean? = null
            ) : KPojo    
            @Table(name = "tb_user")
            @TableIndex(name = "idx_user_id", columns = ["id"], type = "UNIQUE", method = "BTREE")
            @TableIndex(name = "idx_user_name", columns = ["username"], type = "UNIQUE", method = "BTREE")
            @TableIndex(name = "idx_multi", columns = ["id", "username"], type = "UNIQUE", method = "BTREE")
            data class User(
                var id: Int? = null,
                @Serializable
                var username: String? = null,
                var gender: Int? = null,
                @CreateTime
                @Default("now()")
                var createTime: Date? = null
            ) : KPojo


            fun main() {
                Kronos.apply {
                     fieldNamingStrategy = LineHumpNamingStrategy
                     tableNamingStrategy = LineHumpNamingStrategy
                }
                    
                val user = User(1)
                val testUser = User(1, "test")
                val t = user.kronosColumns()
                val m = mapOf("id" to 2)
                val u = m.safeMapperTo<User>()
                                
                val (sql, paramMap) = user.select { it.username.`as`("name") + it.gender }.build()
                println(user.toDataMap())
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