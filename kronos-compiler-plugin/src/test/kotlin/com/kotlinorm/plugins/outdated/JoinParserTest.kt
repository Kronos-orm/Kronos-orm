package com.kotlinorm.plugins.outdated

import com.kotlinorm.plugins.KotlinSourceDynamicCompiler
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
class JoinParserTest {

    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `test join parser`() {
        val result = KotlinSourceDynamicCompiler.compile(
            """
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

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("MainKt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}