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
 * Expanded tests for the Sort Transformer — covers multiple sort fields,
 * default sort direction, mixed asc/desc, and @Column annotated fields.
 */
@OptIn(ExperimentalCompilerApi::class)
class SortTransformerTest {

    @Suppress("LanguageDetectionInspection")
    private val mainKt = """
        import com.kotlinorm.Kronos
        import com.kotlinorm.annotations.*
        import com.kotlinorm.beans.dsl.Field
        import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
        import com.kotlinorm.interfaces.KPojo
        import com.kotlinorm.enums.KColumnType.TINYINT
        import com.kotlinorm.types.ToSort
        import com.kotlinorm.enums.SortType
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

            fun sort(block: ToSort<User, Any?>): List<Pair<Field, SortType>>? {
                var rst: List<Pair<Field, SortType>>? = null
                user.afterSort {
                    block!!(it)
                    rst = sortedFields
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
            this@SortTransformerTest.testBaseName + this
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("${this@SortTransformerTest.testBaseName + this}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }

    // ========================================================================
    // 1. Multiple sort fields with default asc
    // ========================================================================

    @Test
    fun `test multiple sort fields default asc`() {
        "MultiDefaultAsc" testCompile """
            fun test() {
                assertEquals(
                    listOf(
                        user.getColumn("id") to SortType.ASC,
                        user.getColumn("username") to SortType.ASC,
                        user.getColumn("age") to SortType.ASC,
                    ),
                    sort { it.id + it.username + it.age }
                )
            }
        """
    }

    // ========================================================================
    // 2. Mixed asc and desc
    // ========================================================================

    @Test
    fun `test mixed asc and desc sort`() {
        "MixedAscDesc" testCompile """
            fun test() {
                assertEquals(
                    listOf(
                        user.getColumn("id") to SortType.ASC,
                        user.getColumn("username") to SortType.DESC,
                        user.getColumn("age") to SortType.ASC,
                    ),
                    sort { it.id.asc() + it.username.desc() + it.age.asc() }
                )
            }
        """
    }

    // ========================================================================
    // 3. Single field desc
    // ========================================================================

    @Test
    fun `test single field desc`() {
        "SingleDesc" testCompile """
            fun test() {
                assertEquals(
                    listOf(user.getColumn("age") to SortType.DESC),
                    sort { it.age.desc() }
                )
            }
        """
    }

    // ========================================================================
    // 4. Single field asc
    // ========================================================================

    @Test
    fun `test single field asc`() {
        "SingleAsc" testCompile """
            fun test() {
                assertEquals(
                    listOf(user.getColumn("id") to SortType.ASC),
                    sort { it.id.asc() }
                )
            }
        """
    }

    // ========================================================================
    // 5. Sort by @Column annotated field
    // ========================================================================

    @Test
    fun `test sort by Column annotated field`() {
        "ColumnAnnotatedSort" testCompile """
            fun test() {
                val result = sort { it.telephone.desc() }!!
                assertEquals(1, result.size)
                assertEquals("telephone", result[0].first.name)
                assertEquals("phone_number", result[0].first.columnName)
                assertEquals(SortType.DESC, result[0].second)
            }
        """
    }

    // ========================================================================
    // 6. Sort with four fields
    // ========================================================================

    @Test
    fun `test sort with four fields`() {
        "FourFields" testCompile """
            fun test() {
                assertEquals(
                    listOf(
                        user.getColumn("id") to SortType.DESC,
                        user.getColumn("username") to SortType.ASC,
                        user.getColumn("age") to SortType.DESC,
                        user.getColumn("gender") to SortType.ASC,
                    ),
                    sort { it.id.desc() + it.username + it.age.desc() + it.gender }
                )
            }
        """
    }

    // ========================================================================
    // 7. Sort by createTime desc
    // ========================================================================

    @Test
    fun `test sort by createTime desc`() {
        "CreateTimeDesc" testCompile """
            fun test() {
                val result = sort { it.createTime.desc() }!!
                assertEquals(1, result.size)
                assertEquals("createTime", result[0].first.name)
                assertEquals(SortType.DESC, result[0].second)
            }
        """
    }

    // ========================================================================
    // 8. Sort by updateTime asc
    // ========================================================================

    @Test
    fun `test sort by updateTime asc`() {
        "UpdateTimeAsc" testCompile """
            fun test() {
                val result = sort { it.updateTime.asc() }!!
                assertEquals(1, result.size)
                assertEquals("updateTime", result[0].first.name)
                assertEquals(SortType.ASC, result[0].second)
            }
        """
    }

    // ========================================================================
    // 9. Sort by version field
    // ========================================================================

    @Test
    fun `test sort by version field`() {
        "VersionSort" testCompile """
            fun test() {
                val result = sort { it.version.desc() }!!
                assertEquals(1, result.size)
                assertEquals("version", result[0].first.name)
                assertEquals(SortType.DESC, result[0].second)
            }
        """
    }

    // ========================================================================
    // 10. Sort by email (Column annotated) asc
    // ========================================================================

    @Test
    fun `test sort by email Column annotated asc`() {
        "EmailColumnSort" testCompile """
            fun test() {
                val result = sort { it.email.asc() }!!
                assertEquals(1, result.size)
                assertEquals("email", result[0].first.name)
                assertEquals("email_address", result[0].first.columnName)
                assertEquals(SortType.ASC, result[0].second)
            }
        """
    }
}
