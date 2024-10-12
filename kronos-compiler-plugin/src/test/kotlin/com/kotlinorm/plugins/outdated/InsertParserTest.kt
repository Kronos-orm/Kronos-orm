package com.kotlinorm.plugins.outdated

import com.kotlinorm.plugins.KotlinSourceDynamicCompiler.compile
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/6 14:02
 **/
class InsertParserTest {
    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `test insert parser`() {
        val result = compile(
            """
              import com.kotlinorm.Kronos
              import com.kotlinorm.annotations.Table
              import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
              import com.kotlinorm.interfaces.KPojo
              import com.kotlinorm.orm.insert.InsertClause.Companion.execute
              import com.kotlinorm.orm.insert.insert
               
              @Table(name = "tb_user")
              data class User(
                  var id: Int? = null,
                  var username: String? = null,
                  var gender: Int? = null
              ) : KPojo
               
              fun main() {
                Kronos.apply {
                  fieldNamingStrategy = LineHumpNamingStrategy
                  tableNamingStrategy = LineHumpNamingStrategy
                }
                
                class A{
                    val user = User(1)
                    val testUser = User(1, "test")
                    fun a(){
                      arrayOf(user, testUser).insert().execute()
                    }
                }
                A().a()
              }
              """.trimIndent()
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("MainKt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}