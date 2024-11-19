package com.kotlinorm.compiler.fir.transformer.kTable

import com.kotlinorm.compiler.fir.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.fir.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

class KTableParserForSelectTransformerTest {
    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `KTable Parser For Select Transformer Test`() {

        val result = compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.*
            import com.kotlinorm.beans.dsl.Field
            import com.kotlinorm.beans.dsl.FunctionField
            import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
            import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.avg
            import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
            import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.enums.KColumnType
            import com.kotlinorm.enums.KColumnType.TINYINT
            import com.kotlinorm.types.ToSelect
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
            ) : KPojo {
                operator fun get(name: String): Field {
                    return kronosColumns().find { it.name == name } ?: throw IllegalArgumentException("Field ${'$'}name not found")
                }
            }
            
            
            fun main() {
                Kronos.apply {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
                }
            
                val user = User()
            
                fun User.select(block: ToSelect<User, Any?>): List<Field> {
                    val rst: MutableList<Field> = mutableListOf()

                    afterSelect {
                        block!!(it)
                        rst += fields
                    }
                    return rst
                }
                
                fun List<User>.select(block: ToSelect<User, Any?>): List<List<Field>>{
                    return map { it.select(block) }
                }


                assertEquals(
                    listOf(
                        user["id"],
                    ),
                    user.select {
                        it.id
                    }
                )


                assertEquals(
                    listOf(
                        user["id"],
                    ),
                    user.select {
                        +it.id
                    }
                )
                
                assertEquals(
                    listOf(
                        user["id"],
                        user["username"],
                    ),
                    user.select {
                        it.id + it.username
                    }
                )
                
                assertEquals(
                    listOf(
                        user["id"],
                        user["username"].apply{ name = "name" },
                    ),
                    user.select {
                        it::id + it.username.as_("name")
                    }
                )
                
                assertEquals(
                    listOf(
                        user["id"],
                        Field("1", type = KColumnType.CUSTOM_CRITERIA_SQL),
                    ),
                    user.select {
                        it.id + "1"
                    }
                )
                
                assertEquals(
                    listOf(
                        FunctionField(
                            "count", 
                            listOf(
                                Pair(user["id"], user.id)
                            )
                        ).apply{
                            name = "cnt"
                        }
                    ),
                    user.select {
                        f.count(it.id).as_("cnt")
                    }
                )
                
                assertEquals(
                    listOf(listOf(user["id"])),
                    listOf(user).select {
                        it.id
                    }
                )


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