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
 * Expanded tests for the Reference Transformer — covers multiple reference
 * fields, field subtraction, @Cascade annotated fields, and @Column fields.
 */
@OptIn(ExperimentalCompilerApi::class)
class ReferenceTransformerTest {

    @Suppress("LanguageDetectionInspection")
    private val mainKt = """
        import com.kotlinorm.Kronos
        import com.kotlinorm.annotations.*
        import com.kotlinorm.beans.dsl.Field
        import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
        import com.kotlinorm.interfaces.KPojo
        import com.kotlinorm.enums.KColumnType.TINYINT
        import com.kotlinorm.types.ToReference
        import java.time.LocalDateTime
        import kotlin.test.assertEquals
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
                return kronosColumns().find { it.name == name }!!
            }
        }

        fun main() {
            Kronos.init {
                fieldNamingStrategy = lineHumpNamingStrategy
                tableNamingStrategy = lineHumpNamingStrategy
            }

            val user = User()

            fun reference(block: ToReference<User, Any?>): List<Field> {
                val rst: MutableList<Field> = mutableListOf()
                user.afterReference {
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
            this@ReferenceTransformerTest.testBaseName + this
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("${this@ReferenceTransformerTest.testBaseName + this}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }

    // ========================================================================
    // 1. Multiple reference fields
    // ========================================================================

    @Test
    fun `test multiple reference fields`() {
        "MultiRef" testCompile """
            fun test() {
                assertEquals(
                    listOf(
                        user.getColumn("id"),
                        user.getColumn("username"),
                        user.getColumn("age"),
                    ),
                    reference { it::id + it::username + it::age }
                )
            }
        """
    }

    // ========================================================================
    // 2. Single reference field with unaryPlus
    // ========================================================================

    @Test
    fun `test single reference field with unaryPlus`() {
        "SingleUnaryPlus" testCompile """
            fun test() {
                assertEquals(
                    listOf(user.getColumn("username")),
                    reference { +it::username }
                )
            }
        """
    }

    // ========================================================================
    // 3. Reference with @Column annotated field
    // ========================================================================

    @Test
    fun `test reference with Column annotated field`() {
        "ColumnAnnotatedRef" testCompile """
            fun test() {
                val result = reference { it::id + it::telephone }
                assertEquals(2, result.size)
                assertEquals("telephone", result[1].name)
                assertEquals("phone_number", result[1].columnName)
            }
        """
    }

    // ========================================================================
    // 4. Reference with email (Column annotated)
    // ========================================================================

    @Test
    fun `test reference with email Column annotated`() {
        "EmailRef" testCompile """
            fun test() {
                val result = reference { it::id + it::email }
                assertEquals(2, result.size)
                assertEquals("email", result[1].name)
                assertEquals("email_address", result[1].columnName)
            }
        """
    }

    // ========================================================================
    // 5. Reference with four fields
    // ========================================================================

    @Test
    fun `test reference with four fields`() {
        "FourFieldRef" testCompile """
            fun test() {
                val result = reference { it::id + it::username + it::age + it::gender }
                assertEquals(4, result.size)
                assertEquals("id", result[0].name)
                assertEquals("username", result[1].name)
                assertEquals("age", result[2].name)
                assertEquals("gender", result[3].name)
            }
        """
    }

    // ========================================================================
    // 6. Reference with @Cascade annotated field
    // ========================================================================

    @Test
    fun `test reference with Cascade annotated field`() {
        "CascadeRef" testCompile """
            fun test() {
                val result = reference { it::id + it::friend }
                assertEquals(2, result.size)
                assertEquals("friend", result[1].name)
            }
        """
    }

    // ========================================================================
    // 7. Reference with createTime field
    // ========================================================================

    @Test
    fun `test reference with createTime field`() {
        "CreateTimeRef" testCompile """
            fun test() {
                val result = reference { it::id + it::createTime }
                assertEquals(2, result.size)
                assertEquals("createTime", result[1].name)
            }
        """
    }

    // ========================================================================
    // 8. Reference with version field
    // ========================================================================

    @Test
    fun `test reference with version field`() {
        "VersionRef" testCompile """
            fun test() {
                val result = reference { it::id + it::version }
                assertEquals(2, result.size)
                assertEquals("version", result[1].name)
            }
        """
    }

    // ========================================================================
    // 9. Reference with deleted field
    // ========================================================================

    @Test
    fun `test reference with deleted field`() {
        "DeletedRef" testCompile """
            fun test() {
                val result = reference { it::id + it::deleted }
                assertEquals(2, result.size)
                assertEquals("deleted", result[1].name)
            }
        """
    }

    // ========================================================================
    // 10. Reference with five fields including annotated ones
    // ========================================================================

    @Test
    fun `test reference with five mixed fields`() {
        "FiveMixedRef" testCompile """
            fun test() {
                val result = reference { it::id + it::username + it::telephone + it::email + it::age }
                assertEquals(5, result.size)
                assertEquals("id", result[0].name)
                assertEquals("username", result[1].name)
                assertEquals("telephone", result[2].name)
                assertEquals("phone_number", result[2].columnName)
                assertEquals("email", result[3].name)
                assertEquals("email_address", result[3].columnName)
                assertEquals("age", result[4].name)
            }
        """
    }
}
