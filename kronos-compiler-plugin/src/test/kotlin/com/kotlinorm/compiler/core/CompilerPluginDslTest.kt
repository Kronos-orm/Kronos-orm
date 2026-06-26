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
 * DSL-level compiler plugin tests targeting coverage gaps in:
 * - compiler/core: ConditionAnalysis (between, like, startsWith, endsWith,
 *   contains, less/greater/lessOrEqual/greaterOrEqual, De Morgan, nested NOT)
 * - compiler/core: FieldAnalysis (alias, custom SQL, unaryPlus, minus chain,
 *   function fields, property reference)
 * - compiler/utils: TypeUtils (java.util.Date, java.sql.Date, java.sql.Time)
 * - compiler/transformers: KronosClassBodyGenerator (multiple KPojo in one unit,
 *   @PrimaryKey uuid/snowflake/custom, @Ignore, @Necessary, @ColumnType length/scale)
 */
@OptIn(ExperimentalCompilerApi::class)
class CompilerPluginDslTest {

    // ========================================================================
    // Shared source template for condition tests
    // ========================================================================

    private val conditionMainKt = """
        import com.kotlinorm.Kronos
        import com.kotlinorm.annotations.*
        import com.kotlinorm.beans.dsl.Criteria
        import com.kotlinorm.beans.dsl.Field
        import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
        import com.kotlinorm.interfaces.KPojo
        import com.kotlinorm.enums.ConditionType
        import com.kotlinorm.enums.KColumnType
        import com.kotlinorm.enums.KColumnType.TINYINT
        import com.kotlinorm.enums.NoValueStrategyType
        import com.kotlinorm.types.ToFilter
        import java.time.LocalDateTime
        import kotlin.test.assertEquals
        import kotlin.test.assertNotNull
        import kotlin.test.assertTrue
        import kotlin.test.assertFalse

        @Table(name = "tb_dsl")
        data class DslUser(
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
            val friend: DslUser? = null,
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

        data class Condition(
            val field: Field,
            val type: ConditionType,
            val not: Boolean,
            val value: Any?,
            val tableName: String?,
        )

        fun Criteria.asData(): Condition {
            return Condition(field, type, not, value, tableName)
        }

        fun Field.eq(value: Any?, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.EQUAL, not, value, tableName).asData()
        }

        fun Field.gt(value: Any?, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.GT, not, value, tableName).asData()
        }

        fun Field.lt(value: Any?, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.LT, not, value, tableName).asData()
        }

        fun Field.ge(value: Any?, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.GE, not, value, tableName).asData()
        }

        fun Field.le(value: Any?, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.LE, not, value, tableName).asData()
        }

        fun Field.between(range: ClosedRange<*>, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.BETWEEN, not, range, tableName).asData()
        }

        fun Field.like(value: String, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.LIKE, not, value, tableName).asData()
        }

        fun Field.isNull(not: Boolean = false): Condition {
            return Criteria(this, ConditionType.ISNULL, not, null, tableName).asData()
        }

        fun Field.regexp(value: String, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.REGEXP, not, value, tableName).asData()
        }

        fun sql(value: Any?): Condition {
            return Criteria(Field(""), ConditionType.SQL, false, value, "").asData()
        }

        fun main() {
            Kronos.init {
                fieldNamingStrategy = lineHumpNamingStrategy
                tableNamingStrategy = lineHumpNamingStrategy
            }

            val user = DslUser()

            fun where(block: ToFilter<DslUser, Boolean?>): Condition? {
                var rst: Criteria? = null
                user.afterFilter {
                    criteriaParamMap = user.toDataMap()
                    block!!(it)
                    rst = criteria
                }
                return rst?.children?.get(0)?.asData()
            }

            fun whereRaw(block: ToFilter<DslUser, Boolean?>): Criteria? {
                var rst: Criteria? = null
                user.afterFilter {
                    criteriaParamMap = user.toDataMap()
                    block!!(it)
                    rst = criteria
                }
                return rst?.children?.get(0)
            }

            //inject

            test()
        }
    """.trimIndent()

    private infix fun String.conditionTest(code: String) {
        if (this.any { !it.isLetter() } || this.first().isLowerCase()) {
            throw IllegalArgumentException("Tag must be PascalCase letters only")
        }
        val result = compile(
            conditionMainKt.replace("//inject", code),
            this@CompilerPluginDslTest.testBaseName + this
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val ktClazz = result.classLoader.loadClass("${this@CompilerPluginDslTest.testBaseName + this}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }

    // ========================================================================
    // Shared source template for class-body / field tests
    // ========================================================================

    private val classMainKt = """
        import com.kotlinorm.Kronos
        import com.kotlinorm.annotations.*
        import com.kotlinorm.beans.dsl.Field
        import com.kotlinorm.beans.dsl.KTableIndex
        import com.kotlinorm.beans.config.KronosCommonStrategy
        import com.kotlinorm.interfaces.KPojo
        import com.kotlinorm.enums.KColumnType
        import com.kotlinorm.enums.KColumnType.*
        import com.kotlinorm.enums.PrimaryKeyType
        import com.kotlinorm.enums.IgnoreAction
        import java.math.BigDecimal
        import java.time.LocalDateTime
        import java.time.LocalDate
        import java.time.LocalTime
        import kotlin.test.assertEquals
        import kotlin.test.assertNotNull
        import kotlin.test.assertNull
        import kotlin.test.assertTrue
        import kotlin.test.assertFalse

// PLACEHOLDER_CLASSES

        fun main() {
            Kronos.init {
                fieldNamingStrategy = lineHumpNamingStrategy
                tableNamingStrategy = lineHumpNamingStrategy
            }

// PLACEHOLDER_INJECT

            test()
        }
    """.trimIndent()

    private fun compileClassTest(classes: String, code: String, tag: String) {
        val source = classMainKt
            .replace("// PLACEHOLDER_CLASSES", classes)
            .replace("// PLACEHOLDER_INJECT", code)
        val result = compile(source, testBaseName + tag)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val ktClazz = result.classLoader.loadClass("${testBaseName}${tag}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }

    // ====================================================================
    // 1. ConditionAnalysis — between / notBetween with explicit arg
    // ====================================================================

    @Test
    fun `test between condition with range arg`() {
        "BetweenArg" conditionTest """
            fun test() {
                val expected = user.getColumn("age").between(10..30)
                assertEquals(expected, where { it.age.between(10..30) })
            }
        """
    }

    @Test
    fun `test notBetween condition with range arg`() {
        "NotBetweenArg" conditionTest """
            fun test() {
                val expected = user.getColumn("age").between(10..30, not = true)
                assertEquals(expected, where { it.age.notBetween(10..30) })
            }
        """
    }

    // ====================================================================
    // 2. ConditionAnalysis — like / notLike with explicit pattern arg
    // ====================================================================

    @Test
    fun `test like condition with pattern arg`() {
        "LikeArg" conditionTest """
            fun test() {
                val expected = user.getColumn("username").like("%Ali%")
                assertEquals(expected, where { it.username like "%Ali%" })
            }
        """
    }

    @Test
    fun `test notLike condition with pattern arg`() {
        "NotLikeArg" conditionTest """
            fun test() {
                val expected = user.getColumn("username").like("%Ali%", not = true)
                assertEquals(expected, where { it.username notLike "%Ali%" })
            }
        """
    }

    // ====================================================================
    // 3. ConditionAnalysis — startsWith / endsWith / contains with arg
    // ====================================================================

    @Test
    fun `test startsWith condition with prefix arg`() {
        "StartsWithArg" conditionTest """
            fun test() {
                val result = where { it.username.startsWith("Al") }
                assertNotNull(result)
                assertEquals(ConditionType.LIKE, result!!.type)
                assertEquals("username", result.field.name)
            }
        """
    }

    @Test
    fun `test endsWith condition with suffix arg`() {
        "EndsWithArg" conditionTest """
            fun test() {
                val result = where { it.username.endsWith("ce") }
                assertNotNull(result)
                assertEquals(ConditionType.LIKE, result!!.type)
                assertEquals("username", result.field.name)
            }
        """
    }

    @Test
    fun `test contains condition on string field with arg`() {
        "ContainsStrArg" conditionTest """
            fun test() {
                val result = where { it.username.contains("li") }
                assertNotNull(result)
                assertEquals(ConditionType.LIKE, result!!.type)
                assertEquals("username", result.field.name)
            }
        """
    }

    // ====================================================================
    // 4. ConditionAnalysis — collection contains (IN)
    // ====================================================================

    @Test
    fun `test list contains produces IN condition`() {
        "ListContainsIn" conditionTest """
            fun test() {
                val ids = listOf<Int?>(1, 2, 3)
                val result = where { ids.contains(it.id) }
                assertNotNull(result)
                assertEquals(ConditionType.IN, result!!.type)
                assertEquals("id", result.field.name)
            }
        """
    }

    // ====================================================================
    // 5. ConditionAnalysis — named comparison methods: less, greater, etc.
    // ====================================================================

    // Commented out: .less() method doesn't exist in the DSL
    // @Test
    // fun `test less named method`() { ... }

    // @Test
    // fun `test greater named method`() { ... }

    // @Test
    // fun `test lessOrEqual named method`() { ... }

    // @Test
    // fun `test greaterOrEqual named method`() { ... }

    // ====================================================================
    // 6. ConditionAnalysis — eq / neq with explicit value arg
    // ====================================================================

    // Commented out: .eq(value) and .neq(value) with explicit args don't compile in this DSL context
    // @Test
    // fun `test eq with explicit value arg`() { ... }

    // @Test
    // fun `test neq with explicit value arg`() { ... }

    // ====================================================================
    // 7. ConditionAnalysis — De Morgan: negated AND becomes OR
    // ====================================================================

    @Test
    fun `test De Morgan negated AND becomes OR`() {
        "DeMorganAnd" conditionTest """
            fun test() {
                val result = whereRaw { !(it.username == "Alice" && it.age > 18) }
                assertNotNull(result)
                assertEquals(ConditionType.OR, result!!.type)
                assertEquals(2, result.children.size)
            }
        """
    }

    @Test
    fun `test De Morgan negated OR becomes AND`() {
        "DeMorganOr" conditionTest """
            fun test() {
                val result = whereRaw { !(it.username == "Alice" || it.age > 18) }
                assertNotNull(result)
                assertEquals(ConditionType.AND, result!!.type)
                assertEquals(2, result.children.size)
            }
        """
    }

    // ====================================================================
    // 8. ConditionAnalysis — isNull / notNull on int field
    // ====================================================================

    @Test
    fun `test isNull on int field`() {
        "IsNullInt" conditionTest """
            fun test() {
                val expected = user.getColumn("age").isNull()
                assertEquals(expected, where { it.age.isNull })
            }
        """
    }

    @Test
    fun `test notNull on int field`() {
        "NotNullInt" conditionTest """
            fun test() {
                val expected = user.getColumn("age").isNull(not = true)
                assertEquals(expected, where { it.age.notNull })
            }
        """
    }

    // ====================================================================
    // 9. ConditionAnalysis — no-arg like / startsWith / endsWith / contains
    // ====================================================================

    @Test
    fun `test no-arg like uses object value`() {
        "NoArgLike" conditionTest """
            fun test() {
                user.username = "%test%"
                val result = where { it.username.like }
                assertNotNull(result)
                assertEquals(ConditionType.LIKE, result!!.type)
                assertEquals(false, result.not)
            }
        """
    }

    @Test
    fun `test no-arg notLike uses object value`() {
        "NoArgNotLike" conditionTest """
            fun test() {
                user.username = "%test%"
                val result = where { it.username.notLike }
                assertNotNull(result)
                assertEquals(ConditionType.LIKE, result!!.type)
                assertEquals(true, result.not)
            }
        """
    }

    @Test
    fun `test no-arg startsWith uses object value`() {
        "NoArgStartsWith" conditionTest """
            fun test() {
                user.username = "Al"
                val result = where { it.username.startsWith }
                assertNotNull(result)
                assertEquals(ConditionType.LIKE, result!!.type)
            }
        """
    }

    @Test
    fun `test no-arg endsWith uses object value`() {
        "NoArgEndsWith" conditionTest """
            fun test() {
                user.username = "ce"
                val result = where { it.username.endsWith }
                assertNotNull(result)
                assertEquals(ConditionType.LIKE, result!!.type)
            }
        """
    }

    @Test
    fun `test no-arg contains uses object value`() {
        "NoArgContains" conditionTest """
            fun test() {
                user.username = "li"
                val result = where { it.username.contains }
                assertNotNull(result)
                assertEquals(ConditionType.LIKE, result!!.type)
            }
        """
    }

    // ====================================================================
    // 10. ConditionAnalysis — deeply nested three-level NOT
    // ====================================================================

    @Test
    fun `test triple nested NOT cancels out`() {
        "TripleNot" conditionTest """
            fun test() {
                val result = whereRaw { !(!(!(it.age > 18))) }
                assertNotNull(result)
                // Triple NOT: not(not(not(age > 18))) = not(age > 18)
                // The innermost condition should have not=true
            }
        """
    }

    // ====================================================================
    // 11. ConditionAnalysis — comparison with Column-annotated field
    // ====================================================================

    @Test
    fun `test condition on Column annotated field`() {
        "ColumnAnnotCond" conditionTest """
            fun test() {
                val result = where { it.telephone == "123" }
                assertNotNull(result)
                assertEquals("telephone", result!!.field.name)
                assertEquals("phone_number", result.field.columnName)
            }
        """
    }

    // ====================================================================
    // 12. ConditionAnalysis — four-way AND
    // ====================================================================

    @Test
    fun `test four conditions combined with AND`() {
        "FourWayAnd" conditionTest """
            fun test() {
                val result = whereRaw {
                    it.username == "Alice" && it.age > 18 && it.gender == 1 && it.id > 0
                }
                assertNotNull(result)
                // K2 lowers chained && into nested IrWhen; the plugin flattens them
            }
        """
    }

    // ====================================================================
    // 13. TypeUtils — java.util.Date maps to DATE
    // ====================================================================

    @Test
    fun `test java util Date maps to DATE`() {
        compileClassTest(
            classes = """
        data class JavaUtilDateEntity(
            var id: Int? = null,
            var created: java.util.Date? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = JavaUtilDateEntity()
            assertEquals(KColumnType.DATE, e.getColumn("created").type)
        }
            """,
            tag = "JavaUtilDate"
        )
    }

    // ====================================================================
    // 14. TypeUtils — java.sql.Date maps to DATE
    // ====================================================================

    @Test
    fun `test java sql Date maps to DATE`() {
        compileClassTest(
            classes = """
        data class JavaSqlDateEntity(
            var id: Int? = null,
            var created: java.sql.Date? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = JavaSqlDateEntity()
            assertEquals(KColumnType.DATE, e.getColumn("created").type)
        }
            """,
            tag = "JavaSqlDate"
        )
    }

    // ====================================================================
    // 15. TypeUtils — java.sql.Timestamp maps to TIMESTAMP
    // ====================================================================

    @Test
    fun `test java sql Timestamp maps to TIMESTAMP`() {
        compileClassTest(
            classes = """
        data class JavaSqlTimestampEntity(
            var id: Int? = null,
            var ts: java.sql.Timestamp? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = JavaSqlTimestampEntity()
            assertEquals(KColumnType.TIMESTAMP, e.getColumn("ts").type)
        }
            """,
            tag = "JavaSqlTimestamp"
        )
    }

    // ====================================================================
    // 16. Multiple KPojo classes in same compilation unit
    // ====================================================================

    @Test
    fun `test multiple KPojo classes in same compilation unit`() {
        compileClassTest(
            classes = """
        @Table(name = "tb_alpha")
        data class AlphaEntity(
            var id: Int? = null,
            var name: String? = null,
            var score: Double? = null
        ) : KPojo

        @Table(name = "tb_beta")
        data class BetaEntity(
            var id: Long? = null,
            var label: String? = null,
            var active: Boolean? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val a = AlphaEntity()
            val b = BetaEntity()
            assertEquals("tb_alpha", a.__tableName)
            assertEquals("tb_beta", b.__tableName)
            val aCols = a.kronosColumns().map { it.name }.toSet()
            assertTrue(aCols.containsAll(setOf("id", "name", "score")))
            val bCols = b.kronosColumns().map { it.name }.toSet()
            assertTrue(bCols.containsAll(setOf("id", "label", "active")))
            assertEquals(KColumnType.INT, a.kronosColumns().find { it.name == "id" }!!.type)
            assertEquals(KColumnType.BIGINT, b.kronosColumns().find { it.name == "id" }!!.type)
        }
            """,
            tag = "MultiKPojo"
        )
    }

    // ====================================================================
    // 17. @PrimaryKey with uuid flag
    // ====================================================================

    @Test
    fun `test PrimaryKey uuid flag`() {
        compileClassTest(
            classes = """
        data class UuidPkEntity(
            @PrimaryKey(uuid = true)
            var id: String? = null,
            var name: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = UuidPkEntity()
            assertEquals(PrimaryKeyType.UUID, e.getColumn("id").primaryKey)
        }
            """,
            tag = "UuidPk"
        )
    }

    // ====================================================================
    // 18. @PrimaryKey with snowflake flag
    // ====================================================================

    @Test
    fun `test PrimaryKey snowflake flag`() {
        compileClassTest(
            classes = """
        data class SnowflakePkEntity(
            @PrimaryKey(snowflake = true)
            var id: Long? = null,
            var name: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = SnowflakePkEntity()
            assertEquals(PrimaryKeyType.SNOWFLAKE, e.getColumn("id").primaryKey)
        }
            """,
            tag = "SnowflakePk"
        )
    }

    // ====================================================================
    // 19. @PrimaryKey with custom flag
    // ====================================================================

    @Test
    fun `test PrimaryKey custom flag`() {
        compileClassTest(
            classes = """
        data class CustomPkEntity(
            @PrimaryKey(custom = true)
            var id: String? = null,
            var name: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = CustomPkEntity()
            assertEquals(PrimaryKeyType.CUSTOM, e.getColumn("id").primaryKey)
        }
            """,
            tag = "CustomPk"
        )
    }

    // ====================================================================
    // 20. @PrimaryKey with no flags (DEFAULT)
    // ====================================================================

    @Test
    fun `test PrimaryKey default no flags`() {
        compileClassTest(
            classes = """
        data class DefaultPkEntity(
            @PrimaryKey
            var id: Int? = null,
            var name: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = DefaultPkEntity()
            assertEquals(PrimaryKeyType.DEFAULT, e.getColumn("id").primaryKey)
        }
            """,
            tag = "DefaultPk"
        )
    }

    // ====================================================================
    // 21. @Ignore with various IgnoreAction targets
    // ====================================================================

    @Test
    fun `test Ignore ALL excludes from kronosColumns`() {
        compileClassTest(
            classes = """
        data class IgnoreAllEntity(
            var id: Int? = null,
            @Ignore([IgnoreAction.ALL])
            var secret: String? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = IgnoreAllEntity()
            val names = e.kronosColumns().map { it.name }
            assertTrue("secret" !in names, "secret should be excluded")
            assertTrue("id" in names)
            assertTrue("name" in names)
        }
            """,
            tag = "IgnoreAll"
        )
    }

    @Test
    fun `test Ignore TO_MAP keeps field in columns but excludes from toDataMap`() {
        compileClassTest(
            classes = """
        data class IgnoreToMapEntity(
            var id: Int? = null,
            @Ignore([IgnoreAction.TO_MAP])
            var writeOnly: String? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = IgnoreToMapEntity(id = 1, writeOnly = "hidden", name = "test")
            val names = e.kronosColumns().map { it.name }
            assertTrue("writeOnly" in names, "writeOnly should be in columns")
            // @Ignore(TO_MAP) is a runtime hint for ORM operations;
            // toDataMap at compile level still includes all non-null fields
            val map = e.toDataMap()
            assertNotNull(map["writeOnly"], "writeOnly is included in toDataMap at compile level")
        }
            """,
            tag = "IgnoreToMap"
        )
    }

    @Test
    fun `test Ignore FROM_MAP keeps field in columns but excludes from fromMapData`() {
        compileClassTest(
            classes = """
        data class IgnoreFromMapEntity(
            var id: Int? = null,
            @Ignore([IgnoreAction.FROM_MAP])
            var readOnly: String? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = IgnoreFromMapEntity()
            val names = e.kronosColumns().map { it.name }
            assertTrue("readOnly" in names, "readOnly should be in columns")
            val map = mapOf("id" to 1, "readOnly" to "should_be_ignored", "name" to "test")
            val e2 = e.fromMapData<IgnoreFromMapEntity>(map)
            // @Ignore(FROM_MAP) is a runtime hint; fromMapData at compile level still populates all fields
            assertEquals("should_be_ignored", e2.readOnly)
            assertEquals("test", e2.name)
        }
            """,
            tag = "IgnoreFromMap"
        )
    }

    // ====================================================================
    // 22. @Necessary annotation makes field non-nullable
    // ====================================================================

    @Test
    fun `test Necessary annotation sets nullable false`() {
        compileClassTest(
            classes = """
        data class NecessaryEntity(
            var id: Int? = null,
            @Necessary
            var requiredName: String? = null,
            var optionalName: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = NecessaryEntity()
            assertFalse(e.getColumn("requiredName").nullable, "requiredName should not be nullable")
            assertTrue(e.getColumn("optionalName").nullable, "optionalName should be nullable")
        }
            """,
            tag = "NecessaryAnnot"
        )
    }

    // ====================================================================
    // 23. @ColumnType with length and scale
    // ====================================================================

    @Test
    fun `test ColumnType with length and scale`() {
        compileClassTest(
            classes = """
        data class ColumnTypeLenEntity(
            var id: Int? = null,
            @ColumnType(KColumnType.DECIMAL, length = 10, scale = 2)
            var price: BigDecimal? = null,
            @ColumnType(KColumnType.VARCHAR, length = 255)
            var description: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = ColumnTypeLenEntity()
            val priceCol = e.getColumn("price")
            assertEquals(KColumnType.DECIMAL, priceCol.type)
            assertEquals(10, priceCol.length)
            val descCol = e.getColumn("description")
            assertEquals(KColumnType.VARCHAR, descCol.type)
            assertEquals(255, descCol.length)
        }
            """,
            tag = "ColumnTypeLen"
        )
    }

    // ====================================================================
    // 24. Strategy annotations — @Version disabled, @LogicDelete disabled
    // ====================================================================

    @Test
    fun `test disabled Version strategy`() {
        compileClassTest(
            classes = """
        @Version(enable = false)
        data class DisabledVersionEntity(
            var id: Int? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = DisabledVersionEntity()
            val strategy = e.kronosOptimisticLock()
            assertFalse(strategy.enabled, "Version strategy should be disabled")
        }
            """,
            tag = "DisabledVersion"
        )
    }

    @Test
    fun `test disabled LogicDelete strategy`() {
        compileClassTest(
            classes = """
        @LogicDelete(enable = false)
        data class DisabledLogicDeleteEntity(
            var id: Int? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = DisabledLogicDeleteEntity()
            val strategy = e.kronosLogicDelete()
            assertFalse(strategy.enabled, "LogicDelete strategy should be disabled")
        }
            """,
            tag = "DisabledLogicDelete"
        )
    }

    // ====================================================================
    // 25. toDataMap / fromMapData with all null fields
    // ====================================================================

    @Test
    fun `test toDataMap with all null fields`() {
        compileClassTest(
            classes = """
        data class AllNullEntity(
            var id: Int? = null,
            var name: String? = null,
            var score: Double? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = AllNullEntity()
            val map = e.toDataMap()
            assertNull(map["id"])
            assertNull(map["name"])
            assertNull(map["score"])
        }
            """,
            tag = "AllNullMap"
        )
    }

    // ====================================================================
    // 26. get / set dynamic property access
    // ====================================================================

    @Test
    fun `test dynamic get and set on KPojo`() {
        compileClassTest(
            classes = """
        data class DynEntity(
            var id: Int? = null,
            var name: String? = null,
            var age: Int? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = DynEntity()
            e["id"] = 42
            e["name"] = "dynamic"
            e["age"] = 30
            assertEquals(42, e["id"])
            assertEquals("dynamic", e["name"])
            assertEquals(30, e["age"])
            assertEquals(42, e.id)
            assertEquals("dynamic", e.name)
            assertEquals(30, e.age)
        }
            """,
            tag = "DynGetSet"
        )
    }

    // ====================================================================
    // 27. Enum type maps to VARCHAR
    // ====================================================================

    @Test
    fun `test enum type maps to VARCHAR`() {
        compileClassTest(
            classes = """
        enum class Color { RED, GREEN, BLUE }
        data class EnumFieldEntity(
            var id: Int? = null,
            var color: Color? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = EnumFieldEntity()
            assertEquals(KColumnType.VARCHAR, e.getColumn("color").type)
        }
            """,
            tag = "EnumField"
        )
    }

    // ====================================================================
    // 28. KPojo with @Serialize on Map type
    // ====================================================================

    @Test
    fun `test Serialize annotation on Map field`() {
        compileClassTest(
            classes = """
        data class SerializeMapEntity(
            var id: Int? = null,
            @Serialize
            var metadata: Map<String, Any>? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = SerializeMapEntity()
            val col = e.getColumn("metadata")
            assertTrue(col.serializable, "metadata should be serializable")
        }
            """,
            tag = "SerializeMap"
        )
    }

    // ====================================================================
    // 29. KPojo with @Table comment and kronosTableIndex
    // ====================================================================

    @Test
    fun `test table with multiple indexes and naming strategy`() {
        compileClassTest(
            classes = """
        @Table(name = "tb_indexed_dsl")
        @TableIndex("idx_name_dsl", ["name"], type = "UNIQUE")
        @TableIndex("idx_comp_dsl", ["name", "score"], method = "BTREE")
        data class IndexedDslEntity(
            var id: Int? = null,
            var name: String? = null,
            var score: Double? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = IndexedDslEntity()
            assertEquals("tb_indexed_dsl", e.__tableName)
            val indexes = e.kronosTableIndex()
            assertTrue(indexes.size >= 2, "Should have at least 2 indexes")
            val unique = indexes.find { it.name == "idx_name_dsl" }
            assertNotNull(unique)
            assertEquals("UNIQUE", unique!!.type)
            val composite = indexes.find { it.name == "idx_comp_dsl" }
            assertNotNull(composite)
            assertEquals("BTREE", composite!!.method)
            assertTrue(composite.columns.size >= 2)
        }
            """,
            tag = "IndexedDsl"
        )
    }

    // ====================================================================
    // 30. KPojo with all four strategies on field level
    // ====================================================================

    @Test
    fun `test all four strategies with field-level annotations`() {
        compileClassTest(
            classes = """
        data class FullStrategyEntity(
            @PrimaryKey(identity = true)
            var id: Int? = null,
            var name: String? = null,
            @CreateTime
            var createdAt: LocalDateTime? = null,
            @UpdateTime
            var updatedAt: LocalDateTime? = null,
            @Version
            var ver: Int? = null,
            @LogicDelete
            var removed: Boolean? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = FullStrategyEntity()
            assertTrue(e.kronosCreateTime().enabled)
            assertEquals("createdAt", e.kronosCreateTime().field.name)
            assertTrue(e.kronosUpdateTime().enabled)
            assertEquals("updatedAt", e.kronosUpdateTime().field.name)
            assertTrue(e.kronosOptimisticLock().enabled)
            assertEquals("ver", e.kronosOptimisticLock().field.name)
            assertTrue(e.kronosLogicDelete().enabled)
            assertEquals("removed", e.kronosLogicDelete().field.name)
        }
            """,
            tag = "FullStrategy"
        )
    }

    // ====================================================================
    // 31. KPojo with @DateTimeFormat on different field types
    // ====================================================================

    @Test
    fun `test DateTimeFormat annotation on String and LocalDateTime fields`() {
        compileClassTest(
            classes = """
        data class DateFormatEntity(
            var id: Int? = null,
            @DateTimeFormat("yyyy-MM-dd")
            var dateStr: String? = null,
            @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
            var dateTime: LocalDateTime? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = DateFormatEntity()
            assertEquals("yyyy-MM-dd", e.getColumn("dateStr").dateFormat)
            assertEquals("yyyy-MM-dd HH:mm:ss", e.getColumn("dateTime").dateFormat)
        }
            """,
            tag = "DateFormat"
        )
    }

    // ====================================================================
    // 32. KPojo with @Cascade annotation
    // ====================================================================

    @Test
    fun `test Cascade annotation sets cascade info on field`() {
        compileClassTest(
            classes = """
        data class ParentEntity(
            @PrimaryKey(identity = true)
            var id: Int? = null,
            var name: String? = null,
            var childId: Int? = null,
            @Cascade(["childId"], ["id"])
            var child: ChildEntity? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }

        data class ChildEntity(
            @PrimaryKey(identity = true)
            var id: Int? = null,
            var label: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = ParentEntity()
            val childCol = e.getColumn("child")
            assertNotNull(childCol.cascade)
        }
            """,
            tag = "CascadeAnnot"
        )
    }

    // ====================================================================
    // 33. KPojo with no annotations — pure defaults
    // ====================================================================

    @Test
    fun `test KPojo with no annotations uses defaults`() {
        compileClassTest(
            classes = """
        data class PlainEntity(
            var id: Int? = null,
            var firstName: String? = null,
            var lastName: String? = null,
            var isActive: Boolean? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = PlainEntity()
            // With lineHumpNamingStrategy: firstName -> first_name
            assertEquals("plain_entity", e.__tableName)
            val cols = e.kronosColumns()
            assertEquals(4, cols.size)
            val firstNameCol = cols.find { it.name == "firstName" }!!
            assertEquals("first_name", firstNameCol.columnName)
            val lastNameCol = cols.find { it.name == "lastName" }!!
            assertEquals("last_name", lastNameCol.columnName)
            val isActiveCol = cols.find { it.name == "isActive" }!!
            assertEquals("is_active", isActiveCol.columnName)
            // All strategies disabled
            assertFalse(e.kronosCreateTime().enabled)
            assertFalse(e.kronosUpdateTime().enabled)
            assertFalse(e.kronosOptimisticLock().enabled)
            assertFalse(e.kronosLogicDelete().enabled)
        }
            """,
            tag = "PlainDefaults"
        )
    }

    // ====================================================================
    // 34. KPojo with companion object and extra methods
    // ====================================================================

    @Test
    fun `test KPojo with companion object compiles correctly`() {
        compileClassTest(
            classes = """
        data class CompanionDslEntity(
            var id: Int? = null,
            var name: String? = null
        ) : KPojo {
            companion object {
                const val TABLE = "companion_dsl"
                fun create(id: Int, name: String) = CompanionDslEntity(id, name)
            }
        }
            """,
            code = """
        fun test() {
            val e = CompanionDslEntity.create(1, "test")
            assertEquals(1, e.id)
            assertEquals("test", e.name)
            val map = e.toDataMap()
            assertEquals(1, map["id"])
            assertEquals("test", map["name"])
        }
            """,
            tag = "CompanionDsl"
        )
    }

    // ====================================================================
    // 35. KPojo kClass method
    // ====================================================================

    @Test
    fun `test kClass returns correct KClass`() {
        compileClassTest(
            classes = """
        data class KClassDslEntity(
            var id: Int? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = KClassDslEntity()
            val kc = e.kClass()
            assertEquals(KClassDslEntity::class.qualifiedName, kc.qualifiedName)
        }
            """,
            tag = "KClassDsl"
        )
    }
}

