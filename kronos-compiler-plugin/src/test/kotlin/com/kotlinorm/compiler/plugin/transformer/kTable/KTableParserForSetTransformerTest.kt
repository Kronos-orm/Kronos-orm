package com.kotlinorm.compiler.plugin.transformer.kTable

import com.kotlinorm.compiler.plugin.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.plugin.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

class KTableParserForSetTransformerTest {
    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `KTable Parser For Select Transformer Test`() {
        val result = compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.*
            import com.kotlinorm.beans.dsl.Field
            import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.enums.KColumnType
            import com.kotlinorm.enums.KColumnType.TINYINT
            import com.kotlinorm.types.ToSet
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
                @Necessary
                var username: String? = null,
                @ColumnType(TINYINT)
                @Default("0")
                var gender: Int? = null,
                @Column("phone_number") val telephone: String? = null,
                @Column("email_address") val email: String? = null,
                val birthday: String? = null,
                @Serialize
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
            ) : KPojo {
                operator fun get(name: String): Field {
                    return kronosColumns().find { it.name == name }!!
                }
            }

            data class SetResult(
                val fields: MutableList<Field> = mutableListOf(),
                val fieldParamMap: MutableMap<Field, Any?> = mutableMapOf(),
                val plusAssignFields: MutableList<Pair<Field, Number>> = mutableListOf(),
                val minusAssignFields: MutableList<Pair<Field, Number>> = mutableListOf()
            )
            
            fun main() {
                Kronos.init {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
                }
            
                val user = User()
            
                fun set(block: ToSet<User, Unit>): SetResult? {
                    var rst: SetResult? = null

                    user.afterSet {
                        block!!(it)
                        rst = SetResult(
                            fields = fields,
                            fieldParamMap = fieldParamMap,
                            plusAssignFields = plusAssignFields,
                            minusAssignFields = minusAssignFields
                        )
                    }
                    return rst
                }
                    
                val expected = SetResult(
                    fields = mutableListOf(
                        user["id"],
                        user["username"],
                        user["age"],
                        user["version"]
                    ),
                    fieldParamMap = mutableMapOf(
                        user["id"] to 1,
                        user["username"] to "test",
                    ),
                    plusAssignFields = mutableListOf(
                        user["version"] to 1
                    ),
                    minusAssignFields = mutableListOf(
                        user["age"] to 10
                    )
                )

                assertEquals(expected, set {
                    it.id = 1
                    it.username = "test"
                    it.age -= 10
                    it.version += 1
                })

            }
        """.trimIndent(),
            testBaseName
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("${testBaseName}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}