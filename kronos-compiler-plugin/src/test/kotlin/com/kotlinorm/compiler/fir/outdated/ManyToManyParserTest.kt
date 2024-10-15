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
 *@create: 2024/5/9 09:37
 **/
class ManyToManyParserTest {

    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `test many to many parser`() {
        val result = compile(
            """
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

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("MainKt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}