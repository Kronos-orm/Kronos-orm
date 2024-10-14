package com.kotlinorm.compiler.fir.outdated

import com.kotlinorm.compiler.fir.KotlinSourceDynamicCompiler.compile
import com.tschuchort.compiletesting.KotlinCompilation
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
    fun `delete parser test`() {
        val result = compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.Table
            import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
            import com.kotlinorm.interfaces.KPojo
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
                /* 创建时间 */
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

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("MainKt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}