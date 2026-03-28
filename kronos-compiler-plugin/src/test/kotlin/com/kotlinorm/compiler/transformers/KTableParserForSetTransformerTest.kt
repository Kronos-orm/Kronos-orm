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
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class KTableParserForSetTransformerTest {

    @Test
    fun `KTable Parser For Set Transformer Test`() {
        val result = compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.*
            import com.kotlinorm.beans.dsl.Field
            import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.enums.KColumnType.TINYINT
            import com.kotlinorm.types.ToSet
            import java.time.LocalDateTime
            import kotlin.test.assertEquals

            @Table(name = "tb_user")
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
                var age: Int? = null,
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
                        user.getColumn("id"),
                        user.getColumn("username"),
                        user.getColumn("age"),
                        user.getColumn("version")
                    ),
                    fieldParamMap = mutableMapOf(
                        user.getColumn("id") to 1,
                        user.getColumn("username") to "test",
                    ),
                    plusAssignFields = mutableListOf(
                        user.getColumn("version") to 1
                    ),
                    minusAssignFields = mutableListOf(
                        user.getColumn("age") to 10
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
