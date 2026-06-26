package com.kotlinorm.compiler.plugin.utils

import com.kotlinorm.compiler.plugin.KotlinSourceDynamicCompiler
import com.kotlinorm.compiler.plugin.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.plugin.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

class KClassCreatorMultiFilesDetect {

    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun testAutoKClassMapperCreator() {
        val result = compile(
            KotlinSourceDynamicCompiler.KotlinSourceFile(
                """
                import com.kotlinorm.annotations.Table
                import com.kotlinorm.interfaces.KPojo
                @Table(name = "tb_user")
                data class User(
                    val id: Int? = null,
                    val name: String? = null
                ): KPojo
                """,
                "User"
            ),
            KotlinSourceDynamicCompiler.KotlinSourceFile(
                """
                import com.kotlinorm.annotations.Table
                import com.kotlinorm.interfaces.KPojo
                @Table(name = "tb_role")
                data class Role(
                    val id: Int? = null,
                    val name: String? = null
                ): KPojo
            """,
                "Role"
            ),
            KotlinSourceDynamicCompiler.KotlinSourceFile(
                """
                import com.kotlinorm.Kronos
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.utils.createInstance

                fun main() {
                    println("hello")
                    Kronos.init{}
                    println(createInstance<User>().kronosColumns())
                }

                inline fun <reified T: KPojo> createInstance(): T {
                    return T::class.createInstance()
                }
            """, testBaseName
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz =
            result.classLoader.loadClass("${this::class.simpleName!!.replaceFirstChar { it.uppercase() }}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}