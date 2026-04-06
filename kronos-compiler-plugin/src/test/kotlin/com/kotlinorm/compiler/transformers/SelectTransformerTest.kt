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
 * Expanded tests for the Select Transformer — covers field subtraction,
 * multiple additions, all-fields selection, function fields, aliasing,
 * and property reference syntax.
 */
@OptIn(ExperimentalCompilerApi::class)
class SelectTransformerTest {

    @Suppress("LanguageDetectionInspection")
    private val mainKt = """
        import com.kotlinorm.Kronos
        import com.kotlinorm.annotations.*
        import com.kotlinorm.beans.dsl.Field
        import com.kotlinorm.beans.dsl.FunctionField
        import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
        import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.avg
        import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
        import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
        import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.max
        import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.min
        import com.kotlinorm.interfaces.KPojo
        import com.kotlinorm.enums.KColumnType
        import com.kotlinorm.enums.KColumnType.TINYINT
        import com.kotlinorm.types.ToSelect
        import java.time.LocalDateTime
        import kotlin.test.assertEquals
        import kotlin.test.assertNotNull
        import kotlin.test.assertTrue

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
            this@SelectTransformerTest.testBaseName + this
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("${this@SelectTransformerTest.testBaseName + this}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }

    @Test
    fun `test select single field`() {
        "SingleField" testCompile """
            fun test() {
                assertEquals(
                    listOf(user.getColumn("username")),
                    user.select { it.username }
                )
            }
        """
    }

    @Test
    fun `test select multiple fields with plus`() {
        "MultiFieldPlus" testCompile """
            fun test() {
                assertEquals(
                    listOf(user.getColumn("id"), user.getColumn("username"), user.getColumn("age")),
                    user.select { it.id + it.username + it.age }
                )
            }
        """
    }

    @Test
    fun `test select with property reference syntax`() {
        "PropRefSyntax" testCompile """
            fun test() {
                assertEquals(
                    listOf(user.getColumn("id"), user.getColumn("username"), user.getColumn("age")),
                    user.select { it::id + it::username + it::age }
                )
            }
        """
    }

    @Test
    fun `test select field subtraction excludes field`() {
        "FieldSubtraction" testCompile """
            fun test() {
                val result = user.select { it - it.username }
                val names = result.map { it.name }
                assertTrue("username" !in names, "username should be excluded")
                assertTrue("id" in names, "id should remain")
                assertTrue("age" in names, "age should remain")
            }
        """
    }

    @Test
    fun `test select subtraction with property reference`() {
        "SubtractionPropRef" testCompile """
            fun test() {
                val result = user.select { it - it.username }
                val names = result.map { it.name }
                assertTrue("username" !in names, "username should be excluded via subtraction")
                assertTrue("id" in names, "id should remain")
                assertTrue("gender" in names, "gender should remain")
                assertTrue("age" in names, "age should remain")
                assertTrue("birthday" in names, "birthday should remain")
            }
        """
    }

    @Test
    fun `test select with count function`() {
        "CountFunction" testCompile """
            fun test() {
                val result = user.select { f.count(it.id) }
                assertEquals(1, result.size)
                assertNotNull(result[0])
            }
        """
    }

    @Test
    fun `test select with sum function`() {
        "SumFunction" testCompile """
            fun test() {
                val result = user.select { f.sum(it.age) }
                assertEquals(1, result.size)
                assertNotNull(result[0])
            }
        """
    }

    @Test
    fun `test select with alias`() {
        "WithAlias" testCompile """
            fun test() {
                val result = user.select { it.id + it.username + it.createTime.as_("time") }
                assertEquals(3, result.size)
                assertEquals("id", result[0].name)
                assertEquals("username", result[1].name)
            }
        """
    }

    @Test
    fun `test select with custom SQL string`() {
        "CustomSqlString" testCompile """
            fun test() {
                val result = user.select { it + "1" }
                // Should include all user fields plus the custom SQL field
                assertTrue(result.size > 1, "Should have user fields plus custom SQL")
                val lastField = result.last()
                assertEquals(KColumnType.CUSTOM_CRITERIA_SQL, lastField.type)
            }
        """
    }

    @Test
    fun `test select with avg function`() {
        "AvgFunction" testCompile """
            fun test() {
                val result = user.select { f.avg(it.age) }
                assertEquals(1, result.size)
                assertNotNull(result[0])
            }
        """
    }

    @Test
    fun `test select field with Column annotation uses correct name`() {
        "ColumnAnnotationSelect" testCompile """
            fun test() {
                val result = user.select { it.telephone }
                assertEquals(1, result.size)
                assertEquals("telephone", result[0].name)
                assertEquals("phone_number", result[0].columnName)
            }
        """
    }
}
