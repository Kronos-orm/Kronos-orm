package com.kotlinorm.compiler.fir.outdated

import com.kotlinorm.compiler.fir.KotlinSourceDynamicCompiler
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/9 09:37
 **/
class UpsertParserTest {
    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `test upsert parser`() {
        val result = KotlinSourceDynamicCompiler.compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.CreateTime
            import com.kotlinorm.annotations.LogicDelete
            import com.kotlinorm.annotations.Table
            import com.kotlinorm.annotations.UpdateTime
            import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.orm.upsert.upsert
            
            @Table(name = "tb_user")
            data class User(
                var id: Int? = null,
                var username: String? = null,
                var gender: Int? = null,
                @UpdateTime var updateTime: String? = null,
                @CreateTime var createTime: String? = null,
                @LogicDelete var deleted: Int = 0
            ) : KPojo
            
            fun main() {
                Kronos.apply {
                    fieldNamingStrategy = LineHumpNamingStrategy
                    tableNamingStrategy = LineHumpNamingStrategy
                }
            
                val user = User(1)
                val testUser = User(1, "test")

                testUser.upsert { it.username }
                    .on{ it.id }.execute()
            }
      """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("MainKt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}