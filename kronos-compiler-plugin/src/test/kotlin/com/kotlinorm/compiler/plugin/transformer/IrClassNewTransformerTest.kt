package com.kotlinorm.compiler.plugin.transformer

import com.kotlinorm.compiler.plugin.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.plugin.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

class IrClassNewTransformerTest {
    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `KPojo Class New Transformer Test`() {
        val result = compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.Kronos.init
            import com.kotlinorm.annotations.*
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.enums.KColumnType.TINYINT
            import com.kotlinorm.utils.createInstance
            import java.time.LocalDateTime
            import kotlin.test.assertEquals
            import kotlin.test.assertNotNull
            
            
            /**
             * User
             * Kotlin Data Class for User
             */
            @Table(name = "tb_user")
            @TableIndex("idx_username", ["name"], "UNIQUE")
            @TableIndex(name = "idx_multi", columns = ["id", "name"], "UNIQUE")
            data class User(
                @PrimaryKey(identity = true)
                var id: Int? = null,
                @NotNull
                var username: String? = null,
                @ColumnType(TINYINT)
                @Default("0")
                var gender: Int? = null,
                @Column("phone_number") val telephone: String? = null,
                @Column("email_address") val email: String? = null,
                val birthday: String? = null,
                @Serializable
                val habits: List<String>? = null,
                val age: Int? = null,
                val avatar: String? = null,
                val friendId: Int? = null,
                @Cascade(["friendId"], ["id"])
                val friend: User? = null,
                @CreateTime
                @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
                var createTime: String? = null,
                @UpdateTime
                var updateTime: LocalDateTime? = null,
                @Version
                var version: Int? = null,
                @LogicDelete
                var deleted: Boolean? = null
            ) : KPojo
            
            data class Customer(val id: Int? = null): KPojo

            
            
            fun main() {
                Kronos.init {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
                }
            
                val user = User::class.createInstance()
                assertEquals(user.kronosTableName(), "tb_user")
                assertNotNull(user.kronosColumns().find { it.name == "id" })
                assertNotNull(user.kronosColumns().find { it.name == "username" })
                assertNotNull(user.kronosColumns().find { it.name == "gender" })
                assertNotNull(user.kronosColumns().find { it.name == "telephone" })
                assertNotNull(user.kronosColumns().find { it.name == "email" })
                assertNotNull(user.kronosColumns().find { it.name == "birthday" })
                assertNotNull(user.kronosColumns().find { it.name == "habits" })
                assertNotNull(user.kronosColumns().find { it.name == "age" })
                assertNotNull(user.kronosColumns().find { it.name == "avatar" })
                assertNotNull(user.kronosColumns().find { it.name == "friendId" })
                assertNotNull(user.kronosColumns().find { it.name == "friend" })
                assertNotNull(user.kronosColumns().find { it.name == "createTime" })
                assertNotNull(user.kronosColumns().find { it.name == "updateTime" })
                assertNotNull(user.kronosColumns().find { it.name == "version" })
                assertNotNull(user.kronosColumns().find { it.name == "deleted" })

            }
        """.trimIndent(),
            testBaseName
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz =
            result.classLoader.loadClass("${this::class.simpleName!!.replaceFirstChar { it.uppercase() }}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}