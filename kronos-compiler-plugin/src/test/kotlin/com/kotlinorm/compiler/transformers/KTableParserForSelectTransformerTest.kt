/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.compiler.transformers

import com.kotlinorm.compiler.utils.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.utils.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class KTableParserForSelectTransformerTest {

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
                fun getColumn(name: String): Field {
                    return kronosColumns().find { it.name == name } ?: error("${'$'}name not found")
                }
            }

            fun main() {
                Kronos.init {
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

                assertEquals(
                    listOf(
                        user.getColumn("id"),
                        user.getColumn("username"),
                        user.getColumn("gender"),
                        user.getColumn("telephone"),
                        user.getColumn("email"),
                        user.getColumn("birthday"),
                        user.getColumn("habits"),
                        user.getColumn("age"),
                        user.getColumn("avatar"),
                        user.getColumn("friendId"),
                        user.getColumn("createTime"),
                        user.getColumn("updateTime"),
                        user.getColumn("version"),
                        user.getColumn("deleted"),
                        Field("1", type = KColumnType.CUSTOM_CRITERIA_SQL)
                    ),
                    user.select { it + "1" }
                )

                assertEquals(
                    listOf(user.getColumn("id")),
                    user.select { it.id }
                )

                assertEquals(
                    listOf(user.getColumn("id"), user.getColumn("username")),
                    user.select { it.id + it.username }
                )

                assertEquals(
                    listOf(user.getColumn("id"), user.getColumn("username")),
                    user.select { it::id + it::username }
                )

                val countField = user.select { f.count(it.id).as_("cnt") }
                assertEquals(1, countField.size)
                assertNotNull(countField[0])
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
