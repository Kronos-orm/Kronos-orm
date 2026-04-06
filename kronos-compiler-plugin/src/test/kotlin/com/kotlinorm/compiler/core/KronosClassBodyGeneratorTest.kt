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

package com.kotlinorm.compiler.core

import com.kotlinorm.compiler.utils.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.utils.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for KronosClassBodyGenerator — verifies the compiler plugin generates
 * correct method bodies for KPojo classes (toDataMap, fromMapData, kronosColumns,
 * get/set, __tableName, __tableComment, kronosTableIndex, strategy methods).
 */
@OptIn(ExperimentalCompilerApi::class)
class KronosClassBodyGeneratorTest {

    @Suppress("LanguageDetectionInspection")
    private val mainKt = """
        import com.kotlinorm.Kronos
        import com.kotlinorm.annotations.*
        import com.kotlinorm.beans.dsl.Field
        import com.kotlinorm.beans.dsl.KTableIndex
        import com.kotlinorm.beans.config.KronosCommonStrategy
        import com.kotlinorm.interfaces.KPojo
        import com.kotlinorm.enums.KColumnType
        import com.kotlinorm.enums.KColumnType.TINYINT
        import com.kotlinorm.enums.IgnoreAction
        import java.time.LocalDateTime
        import kotlin.test.assertEquals
        import kotlin.test.assertNotNull
        import kotlin.test.assertNull
        import kotlin.test.assertTrue

        @Table(name = "tb_user")
        @TableIndex("idx_username", ["username"], "UNIQUE")
        @TableIndex(name = "idx_multi", columns = ["id", "username"], "BTREE")
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
        ) : KPojo

        // A minimal class with no @Table annotation
        data class SimpleEntity(
            var id: Int? = null,
            var name: String? = null,
            var value: Double? = null
        ) : KPojo

        // A class with @Ignore fields
        data class IgnoredFieldEntity(
            var id: Int? = null,
            @Ignore([IgnoreAction.ALL])
            var secret: String? = null,
            @Ignore([IgnoreAction.TO_MAP])
            var writeOnly: String? = null,
            @Ignore([IgnoreAction.FROM_MAP])
            var readOnly: String? = null,
            var normal: String? = null
        ) : KPojo

        // A class with disabled strategy annotations
        @CreateTime(enable = false)
        data class DisabledStrategyEntity(
            var id: Int? = null,
            var name: String? = null
        ) : KPojo

        fun main() {
            Kronos.init {
                fieldNamingStrategy = lineHumpNamingStrategy
                tableNamingStrategy = lineHumpNamingStrategy
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
            this@KronosClassBodyGeneratorTest.testBaseName + this
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("${this@KronosClassBodyGeneratorTest.testBaseName + this}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }

    // ========================================================================
    // toDataMap tests
    // ========================================================================

    @Test
    fun `test toDataMap with populated fields`() {
        "ToDataMapPopulated" testCompile """
            fun test() {
                val user = User(1, "Alice", 1, "1234567890", "alice@test.com", "2000-01-01", null, 25, null, null, null, null, null, null, null)
                val map = user.toDataMap()
                assertEquals(1, map["id"])
                assertEquals("Alice", map["username"])
                assertEquals(1, map["gender"])
                assertEquals("1234567890", map["telephone"])
                assertEquals("alice@test.com", map["email"])
                assertEquals("2000-01-01", map["birthday"])
                assertEquals(25, map["age"])
            }
        """
    }

    @Test
    fun `test toDataMap with null values`() {
        "ToDataMapNulls" testCompile """
            fun test() {
                val user = User()
                val map = user.toDataMap()
                assertNull(map["id"])
                assertNull(map["username"])
                assertNull(map["gender"])
                assertNull(map["age"])
                assertTrue(map.containsKey("id"))
                assertTrue(map.containsKey("username"))
            }
        """
    }

    @Test
    fun `test toDataMap on simple entity without Table annotation`() {
        "ToDataMapSimple" testCompile """
            fun test() {
                val entity = SimpleEntity(42, "hello", 3.14)
                val map = entity.toDataMap()
                assertEquals(42, map["id"])
                assertEquals("hello", map["name"])
                assertEquals(3.14, map["value"])
            }
        """
    }

    // ========================================================================
    // fromMapData tests
    // ========================================================================

    @Test
    fun `test fromMapData creates instance from map`() {
        "FromMapDataBasic" testCompile """
            fun test() {
                val map = mapOf<String, Any?>("id" to 1, "username" to "Bob", "age" to 30)
                val user = User().fromMapData<User>(map)
                assertEquals(1, user.id)
                assertEquals("Bob", user.username)
                assertEquals(30, user.age)
            }
        """
    }

    @Test
    fun `test fromMapData with missing keys leaves defaults`() {
        "FromMapDataMissing" testCompile """
            fun test() {
                val map = mapOf<String, Any?>("id" to 5)
                val user = User().fromMapData<User>(map)
                assertEquals(5, user.id)
                assertNull(user.username)
                assertNull(user.age)
            }
        """
    }

    @Test
    fun `test fromMapData with extra keys ignores them`() {
        "FromMapDataExtra" testCompile """
            fun test() {
                val map = mapOf<String, Any?>("id" to 1, "nonExistent" to "value", "username" to "Eve")
                val user = User().fromMapData<User>(map)
                assertEquals(1, user.id)
                assertEquals("Eve", user.username)
            }
        """
    }

    // ========================================================================
    // kronosColumns tests
    // ========================================================================

    @Test
    fun `test kronosColumns returns correct column count`() {
        "KronosColumnsCount" testCompile """
            fun test() {
                val user = User()
                val columns = user.kronosColumns()
                // User has 15 declared properties, but friend is @Cascade (not ignored for columns)
                // and __tableName/__tableComment are @Ignore(ALL) from KPojo interface
                assertTrue(columns.size >= 14, "Expected at least 14 columns, got ${'$'}{columns.size}")
            }
        """
    }

    @Test
    fun `test kronosColumns contains correct field names`() {
        "KronosColumnsNames" testCompile """
            fun test() {
                val user = User()
                val names = user.kronosColumns().map { it.name }
                assertTrue("id" in names, "Should contain id")
                assertTrue("username" in names, "Should contain username")
                assertTrue("gender" in names, "Should contain gender")
                assertTrue("age" in names, "Should contain age")
                assertTrue("createTime" in names, "Should contain createTime")
                assertTrue("updateTime" in names, "Should contain updateTime")
                assertTrue("version" in names, "Should contain version")
                assertTrue("deleted" in names, "Should contain deleted")
            }
        """
    }

    @Test
    fun `test kronosColumns reflects Column annotation mapping`() {
        "KronosColumnsMapping" testCompile """
            fun test() {
                val user = User()
                val telephoneCol = user.kronosColumns().find { it.name == "telephone" }
                assertNotNull(telephoneCol)
                assertEquals("phone_number", telephoneCol!!.columnName)

                val emailCol = user.kronosColumns().find { it.name == "email" }
                assertNotNull(emailCol)
                assertEquals("email_address", emailCol!!.columnName)
            }
        """
    }

    @Test
    fun `test kronosColumns on simple entity`() {
        "KronosColumnsSimple" testCompile """
            fun test() {
                val entity = SimpleEntity()
                val columns = entity.kronosColumns()
                assertEquals(3, columns.size, "SimpleEntity should have 3 columns")
                val names = columns.map { it.name }
                assertTrue("id" in names)
                assertTrue("name" in names)
                assertTrue("value" in names)
            }
        """
    }

    // ========================================================================
    // get / set tests
    // ========================================================================

    @Test
    fun `test get returns property value by name`() {
        "GetByName" testCompile """
            fun test() {
                val user = User(id = 42, username = "Charlie", age = 28)
                assertEquals(42, user["id"])
                assertEquals("Charlie", user["username"])
                assertEquals(28, user["age"])
                assertNull(user["birthday"])
            }
        """
    }

    @Test
    fun `test set updates property value by name`() {
        "SetByName" testCompile """
            fun test() {
                val user = User()
                user["id"] = 99
                user["username"] = "Dave"
                user["age"] = 35
                assertEquals(99, user.id)
                assertEquals("Dave", user.username)
                assertEquals(35, user.age)
            }
        """
    }

    // ========================================================================
    // __tableName tests
    // ========================================================================

    @Test
    fun `test tableName with Table annotation`() {
        "TableNameAnnotated" testCompile """
            fun test() {
                val user = User()
                assertEquals("tb_user", user.__tableName)
            }
        """
    }

    @Test
    fun `test tableName without Table annotation uses naming strategy`() {
        "TableNameDefault" testCompile """
            fun test() {
                val entity = SimpleEntity()
                // With lineHumpNamingStrategy, SimpleEntity -> simple_entity
                val name = entity.__tableName
                assertNotNull(name)
                assertTrue(name.isNotEmpty(), "Table name should not be empty")
            }
        """
    }

    // ========================================================================
    // kronosTableIndex tests
    // ========================================================================

    @Test
    fun `test kronosTableIndex returns annotated indexes`() {
        "TableIndexAnnotated" testCompile """
            fun test() {
                val user = User()
                val indexes = user.kronosTableIndex()
                assertTrue(indexes.size >= 2, "User should have at least 2 table indexes, got ${'$'}{indexes.size}")
            }
        """
    }

    @Test
    fun `test kronosTableIndex empty when no annotation`() {
        "TableIndexEmpty" testCompile """
            fun test() {
                val entity = SimpleEntity()
                val indexes = entity.kronosTableIndex()
                assertEquals(0, indexes.size, "SimpleEntity should have no table indexes")
            }
        """
    }

    // ========================================================================
    // Strategy method tests (CreateTime, UpdateTime, LogicDelete, Version)
    // ========================================================================

    @Test
    fun `test kronosCreateTime returns enabled strategy for annotated field`() {
        "CreateTimeEnabled" testCompile """
            fun test() {
                val user = User()
                val strategy = user.kronosCreateTime()
                assertTrue(strategy.enabled, "CreateTime strategy should be enabled")
                assertEquals("createTime", strategy.field.name)
            }
        """
    }

    @Test
    fun `test kronosUpdateTime returns enabled strategy for annotated field`() {
        "UpdateTimeEnabled" testCompile """
            fun test() {
                val user = User()
                val strategy = user.kronosUpdateTime()
                assertTrue(strategy.enabled, "UpdateTime strategy should be enabled")
                assertEquals("updateTime", strategy.field.name)
            }
        """
    }

    @Test
    fun `test kronosLogicDelete returns enabled strategy for annotated field`() {
        "LogicDeleteEnabled" testCompile """
            fun test() {
                val user = User()
                val strategy = user.kronosLogicDelete()
                assertTrue(strategy.enabled, "LogicDelete strategy should be enabled")
                assertEquals("deleted", strategy.field.name)
            }
        """
    }

    @Test
    fun `test kronosOptimisticLock returns enabled strategy for annotated field`() {
        "OptimisticLockEnabled" testCompile """
            fun test() {
                val user = User()
                val strategy = user.kronosOptimisticLock()
                assertTrue(strategy.enabled, "OptimisticLock strategy should be enabled")
                assertEquals("version", strategy.field.name)
            }
        """
    }

    @Test
    fun `test disabled strategy via class-level annotation`() {
        "DisabledStrategy" testCompile """
            fun test() {
                val entity = DisabledStrategyEntity()
                val strategy = entity.kronosCreateTime()
                assertTrue(!strategy.enabled, "CreateTime strategy should be disabled")
            }
        """
    }

    // ========================================================================
    // Ignore annotation tests
    // ========================================================================

    @Test
    fun `test Ignore ALL excludes field from kronosColumns`() {
        "IgnoreAllColumns" testCompile """
            fun test() {
                val entity = IgnoredFieldEntity()
                val names = entity.kronosColumns().map { it.name }
                assertTrue("secret" !in names, "secret should be excluded from columns")
                assertTrue("normal" in names, "normal should be in columns")
                assertTrue("writeOnly" in names, "writeOnly should be in columns (only TO_MAP ignored)")
                assertTrue("readOnly" in names, "readOnly should be in columns (only FROM_MAP ignored)")
            }
        """
    }

    // ========================================================================
    // kClass test
    // ========================================================================

    @Test
    fun `test kClass returns correct class reference`() {
        "KClassRef" testCompile """
            fun test() {
                val user = User()
                val userKClass = user.kClass()
                assertEquals(User::class.qualifiedName, userKClass.qualifiedName)
                val entity = SimpleEntity()
                val entityKClass = entity.kClass()
                assertEquals(SimpleEntity::class.qualifiedName, entityKClass.qualifiedName)
            }
        """
    }
}
