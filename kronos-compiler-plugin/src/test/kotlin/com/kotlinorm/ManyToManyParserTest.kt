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
 *@create: 2024/5/9 09:37
 **/
class ManyToManyParserTest {
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
            import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
            import com.kotlinorm.annotations.PrimaryKey
            import com.kotlinorm.beans.dsl.KCascade.Companion.manyToMany
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.annotations.Cascade
            
            data class Role(
                @PrimaryKey(identity = true)
                var id: Int? = null,
                var name: String? = null,
                var rolePermissions: List<RolePermissionRelation>? = emptyList(),
            ): KPojo {
                var permissions: List<Permission> by manyToMany(::rolePermissions)
            }
            
            data class RolePermissionRelation(
                @PrimaryKey(identity = true)
                var id: Int? = null,
                var roleId: Int? = null,
                var permissionId: Int? = null,
                @Cascade(["roleId"], ["id"])
                var role: Role? = null,
                @Cascade(["permissionId"], ["id"])
                var permission: Permission? = null
            ): KPojo
            
            data class Permission(
                @PrimaryKey(identity = true)
                var id: Int? = null,
                var name: String? = null,
                var rolePermissions: List<RolePermissionRelation>? = null
            ) : KPojo {
                val roles: List<Role> by manyToMany(::rolePermissions)
            }

            fun main() {
                Kronos.apply {
                    fieldNamingStrategy = LineHumpNamingStrategy
                    tableNamingStrategy = LineHumpNamingStrategy
                }
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