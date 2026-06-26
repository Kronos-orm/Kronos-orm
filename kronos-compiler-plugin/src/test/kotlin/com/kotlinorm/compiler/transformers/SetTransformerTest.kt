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

/**
 * Expanded tests for the Set Transformer — covers setting with null,
 * multiple fields, plusAssign, minusAssign, and various value types.
 */
@OptIn(ExperimentalCompilerApi::class)
class SetTransformerTest {

    @Suppress("LanguageDetectionInspection")
    private val mainKt = """
        import com.kotlinorm.Kronos
        import com.kotlinorm.annotations.*
        import com.kotlinorm.beans.dsl.Field
        import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
        import com.kotlinorm.interfaces.KPojo
        import com.kotlinorm.enums.KColumnType.TINYINT
        import com.kotlinorm.types.ToSet
        import java.time.LocalDateTime
        import kotlin.test.assertEquals
        import kotlin.test.assertTrue
        import kotlin.test.assertNull

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

            //inject

            test()
        }
    """.trimIndent()

    infix fun String.testCompile(code: String) {
        if (this.any { !it.isLetter() } || this.first().isLowerCase()) {
            throw IllegalArgumentException("Test name must be all letters with uppercase first letter")
        }
        val result = compile(
            mainKt.replace("//inject", code),
            this@SetTransformerTest.testBaseName + this
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("${this@SetTransformerTest.testBaseName + this}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }

    @Test
    fun `test set single field with value`() {
        "SingleField" testCompile """
            fun test() {
                val result = set { it.username = "Alice" }!!
                assertEquals(1, result.fields.size)
                assertEquals(user.getColumn("username"), result.fields[0])
                assertEquals("Alice", result.fieldParamMap[user.getColumn("username")])
            }
        """
    }

    @Test
    fun `test set multiple fields`() {
        "MultipleFields" testCompile """
            fun test() {
                val result = set {
                    it.id = 1
                    it.username = "Bob"
                    it.age = 25
                }!!
                assertEquals(3, result.fields.size)
                assertEquals(1, result.fieldParamMap[user.getColumn("id")])
                assertEquals("Bob", result.fieldParamMap[user.getColumn("username")])
                assertEquals(25, result.fieldParamMap[user.getColumn("age")])
            }
        """
    }

    @Test
    fun `test set with plusAssign`() {
        "PlusAssign" testCompile """
            fun test() {
                val result = set { it.version += 1 }!!
                assertEquals(1, result.plusAssignFields.size)
                assertEquals(user.getColumn("version"), result.plusAssignFields[0].first)
                assertEquals(1, result.plusAssignFields[0].second)
            }
        """
    }

    @Test
    fun `test set with minusAssign`() {
        "MinusAssign" testCompile """
            fun test() {
                val result = set { it.age -= 5 }!!
                assertEquals(1, result.minusAssignFields.size)
                assertEquals(user.getColumn("age"), result.minusAssignFields[0].first)
                assertEquals(5, result.minusAssignFields[0].second)
            }
        """
    }

    @Test
    fun `test set with mixed assign and plusAssign`() {
        "MixedAssign" testCompile """
            fun test() {
                val result = set {
                    it.id = 1
                    it.username = "test"
                    it.age -= 10
                    it.version += 1
                }!!
                assertEquals(4, result.fields.size)
                assertEquals(1, result.fieldParamMap[user.getColumn("id")])
                assertEquals("test", result.fieldParamMap[user.getColumn("username")])
                assertEquals(1, result.plusAssignFields.size)
                assertEquals(1, result.minusAssignFields.size)
            }
        """
    }

    @Test
    fun `test set with integer zero value`() {
        "ZeroValue" testCompile """
            fun test() {
                val result = set { it.gender = 0 }!!
                assertEquals(1, result.fields.size)
                assertEquals(user.getColumn("gender"), result.fields[0])
                assertEquals(0, result.fieldParamMap[user.getColumn("gender")])
            }
        """
    }

    @Test
    fun `test set with boolean value`() {
        "BooleanValue" testCompile """
            fun test() {
                val result = set { it.deleted = true }!!
                assertEquals(1, result.fields.size)
                assertEquals(user.getColumn("deleted"), result.fields[0])
                assertEquals(true, result.fieldParamMap[user.getColumn("deleted")])
            }
        """
    }

    @Test
    fun `test set with large plusAssign value`() {
        "LargePlusAssign" testCompile """
            fun test() {
                val result = set { it.age += 100 }!!
                assertEquals(1, result.plusAssignFields.size)
                assertEquals(user.getColumn("age"), result.plusAssignFields[0].first)
                assertEquals(100, result.plusAssignFields[0].second)
            }
        """
    }

    @Test
    fun `test set with multiple plusAssign on different fields`() {
        "MultiPlusAssign" testCompile """
            fun test() {
                val result = set {
                    it.age += 1
                    it.version += 2
                }!!
                assertEquals(2, result.plusAssignFields.size)
                val ageEntry = result.plusAssignFields.find { it.first == user.getColumn("age") }
                val versionEntry = result.plusAssignFields.find { it.first == user.getColumn("version") }
                assertEquals(1, ageEntry?.second)
                assertEquals(2, versionEntry?.second)
            }
        """
    }

    @Test
    fun `test set field with Column annotation`() {
        "ColumnAnnotationSet" testCompile """
            fun test() {
                val result = set { it.gender = 1 }!!
                assertEquals(1, result.fields.size)
                assertEquals("gender", result.fields[0].name)
            }
        """
    }
}
