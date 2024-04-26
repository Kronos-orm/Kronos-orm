package com.kotoframework

import com.kotoframework.plugins.KotoK2ParserCompilerPluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class UpdateParserTest {
  @OptIn(ExperimentalCompilerApi::class)
  @Test
  fun `IR plugin success`() {
//    val result = compile(
//      sourceFile = SourceFile.fromPath(File("/Users/sundaiyue/IdeaProjects/kotoframework/koto-plugins/src/test/kotlin/com/kotoframework/plugins/test/UpdateParserTest.kt"))
//    )
    val result = compile(
      sourceFile = SourceFile.kotlin("main.kt", """
      import com.kotoframework.KotoApp
      import com.kotoframework.interfaces.KPojo
      import com.kotoframework.orm.update.update
      import com.kotoframework.annotations.Table
      import com.kotoframework.beans.namingStrategy.LineHumpNamingStrategy

      @Table(name = "tb_user")
      data class User(
          var id: Int? = null,
          var username: String? = null,
          var gender: Int? = null
      ) : KPojo

      fun main() {
        KotoApp.apply {
            fieldNamingStrategy = LineHumpNamingStrategy()
            tableNamingStrategy = LineHumpNamingStrategy()
        }

        val user = User(1)
        val testUser = User(1, "test")

        testUser.update { it.id + it.username }
            .set { it.gender = 1 }
            .where { it.id < 1 }
            .execute()
      }
      """.trimIndent())
    )
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

    val ktClazz = result.classLoader.loadClass("MainKt")
    val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
    main.invoke(null)
  }

  @OptIn(ExperimentalCompilerApi::class)
  fun compile(
    sourceFiles: List<SourceFile>,
    plugin: CompilerPluginRegistrar = KotoK2ParserCompilerPluginRegistrar(),
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
    plugin: CompilerPluginRegistrar = KotoK2ParserCompilerPluginRegistrar(),
  ): JvmCompilationResult {
    return compile(listOf(sourceFile), plugin)
  }
}