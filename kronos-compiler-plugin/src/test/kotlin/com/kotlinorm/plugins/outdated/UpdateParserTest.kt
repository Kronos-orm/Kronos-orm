package com.kotlinorm.plugins.outdated

import com.kotlinorm.plugins.KotlinSourceDynamicCompiler
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateParserTest {
    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `test update parser`() {
        val result = KotlinSourceDynamicCompiler.compile(
            """
      import com.kotlinorm.Kronos
      import com.kotlinorm.annotations.Table
      import com.kotlinorm.annotations.UpdateTime
      import com.kotlinorm.annotations.LogicDelete
      import com.kotlinorm.beans.strategies.LineHumpNamingStrategy
      import com.kotlinorm.interfaces.KPojo
      import com.kotlinorm.orm.update.UpdateClause.Companion.by
      import com.kotlinorm.orm.update.UpdateClause.Companion.execute
      import com.kotlinorm.orm.update.update
      import com.kotlinorm.beans.config.KronosCommonStrategy
      import com.kotlinorm.beans.dsl.Field

      @Table(name = "tb_user")
      data class User(
          var id: Int? = null,
          var username: String? = null,
          var gender: Int? = null,
          @UpdateTime var updateTime: String? = null,
          @LogicDelete var deleted: String? = null
      ) : KPojo

      fun main() {
        Kronos.apply {
          fieldNamingStrategy = LineHumpNamingStrategy
          tableNamingStrategy = LineHumpNamingStrategy
        }

        val user = User(1)
        val testUser = User(1, "test")

        
        testUser.update()
            .set{
                  it["id"] += 10
                  it.id += 1
              }
            .where{ it.id == 1 }.execute()
      }
      """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("MainKt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}