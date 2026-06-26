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
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class ConditionAnalysisTest {

    @Test
    fun `test 'notEq' with argument`() {
        "NotEqArg" testCompile """
            fun test(){
                val expected = user.getColumn("username").eq("Alice", not = true)
                assertEquals(expected, where { it.username != "Alice" })
                assertEquals(expected, where { "Alice" != it.username })
            }
        """
    }

    @Test
    fun `test 'regexp' condition with argument`() {
        "RegexpArg" testCompile """
            fun test(){
                val expected = user.getColumn("username").regexp("^A.*")
                assertEquals(expected, where { it.username regexp "^A.*" })
            }
        """
    }

    @Test
    fun `test 'notRegexp' condition with argument`() {
        "NotRegexpArg" testCompile """
            fun test(){
                val expected = user.getColumn("username").regexp("^A.*", not = true)
                assertEquals(expected, where { it.username notRegexp "^A.*" })
            }
        """
    }

    @Test
    fun `test 'regexp' no-arg form`() {
        "RegexpNoArg" testCompile """
            fun test(){
                user.username = "^A.*"
                val result = where { it.username.regexp }
                assertNotNull(result)
                assertEquals(ConditionType.REGEXP, result!!.type)
                assertEquals(false, result.not)
            }
        """
    }

    @Test
    fun `test 'notRegexp' no-arg form`() {
        "NotRegexpNoArg" testCompile """
            fun test(){
                user.username = "^B.*"
                val result = where { it.username.notRegexp }
                assertNotNull(result)
                assertEquals(ConditionType.REGEXP, result!!.type)
                assertEquals(true, result.not)
            }
        """
    }

    @Test
    fun `test 'isIn' with array contains`() {
        "IsInArray" testCompile """
            fun test(){
                val arr = arrayOf<Int?>(1, 2, 3)
                val result = where { arr.contains(it.id) }
                assertNotNull(result)
                assertEquals(ConditionType.IN, result!!.type)
                assertEquals("id", result.field.name)
            }
        """
    }

    @Test
    fun `test AND combination of two conditions`() {
        "AndCombo" testCompile """
            fun test(){
                val result = whereRaw { it.username == "Alice" && it.age > 18 }
                assertNotNull(result)
                assertEquals(ConditionType.AND, result!!.type)
                assertEquals(2, result.children.size)
            }
        """
    }

    @Test
    fun `test OR combination of two conditions`() {
        "OrCombo" testCompile """
            fun test(){
                val result = whereRaw { it.username == "Alice" || it.age > 18 }
                assertNotNull(result)
                assertEquals(ConditionType.OR, result!!.type)
                assertEquals(2, result.children.size)
            }
        """
    }

    @Test
    fun `test nested AND inside OR`() {
        "NestedAndOr" testCompile """
            fun test(){
                val result = whereRaw { (it.username == "Alice" && it.age > 18) || it.id == 1 }
                assertNotNull(result)
                assertEquals(ConditionType.OR, result!!.type)
                assertEquals(2, result.children.size)
            }
        """
    }

    @Test
    fun `test negation with NOT operator`() {
        "NegationNot" testCompile """
            fun test(){
                val expected = user.getColumn("username").eq("Alice", not = true)
                assertEquals(expected, where { !(it.username == "Alice") })
            }
        """
    }

    @Test
    fun `test 'takeIf' with true condition includes criteria`() {
        "TakeIfTrue" testCompile """
            fun test(){
                val expected = user.getColumn("id").eq(1)
                assertEquals(expected, where { (it.id == 1).takeIf(true) })
            }
        """
    }

    @Test
    fun `test 'takeIf' with false condition excludes criteria`() {
        "TakeIfFalse" testCompile """
            fun test(){
                val expected: Condition? = null
                assertEquals(expected, where { (it.id == 1).takeIf(false) })
            }
        """
    }

    @Test
    fun `test 'ifNoValue' with Ignore strategy`() {
        "IfNoValueIgnore" testCompile """
            fun test(){
                val result = whereRaw { it.username.eq.ifNoValue(NoValueStrategyType.Ignore) }
                assertNotNull(result)
                assertEquals(NoValueStrategyType.Ignore, result!!.noValueStrategyType)
            }
        """
    }

    @Test
    fun `test 'ifNoValue' with True strategy`() {
        "IfNoValueTrue" testCompile """
            fun test(){
                val result = whereRaw { it.username.eq.ifNoValue(NoValueStrategyType.True) }
                assertNotNull(result)
                assertEquals(NoValueStrategyType.True, result!!.noValueStrategyType)
            }
        """
    }

    @Test
    fun `test 'ifNoValue' with False strategy`() {
        "IfNoValueFalse" testCompile """
            fun test(){
                val result = whereRaw { it.username.eq.ifNoValue(NoValueStrategyType.False) }
                assertNotNull(result)
                assertEquals(NoValueStrategyType.False, result!!.noValueStrategyType)
            }
        """
    }

    @Test
    fun `test 'asSql' with boolean expression`() {
        "AsSqlBool" testCompile """
            fun test(){
                val expected = sql(true)
                assertEquals(expected, where { (1 == 1).asSql() })

                val expected2 = sql(false)
                assertEquals(expected2, where { (1 == 2).asSql() })
            }
        """
    }

    @Test
    fun `test 'asSql' with string expression`() {
        "AsSqlStr" testCompile """
            fun test(){
                val expected = sql("age > 18")
                assertEquals(expected, where { "age > 18".asSql() })
            }
        """
    }

    @Test
    fun `test no-arg 'eq' uses object value`() {
        "NoArgEq" testCompile """
            fun test(){
                user.username = "Bob"
                val result = where { it.username.eq }
                assertNotNull(result)
                assertEquals(ConditionType.EQUAL, result!!.type)
                assertEquals(false, result.not)
            }
        """
    }

    @Test
    fun `test no-arg 'neq' uses object value`() {
        "NoArgNeq" testCompile """
            fun test(){
                user.username = "Bob"
                val result = where { it.username.neq }
                assertNotNull(result)
                assertEquals(ConditionType.EQUAL, result!!.type)
                assertEquals(true, result.not)
            }
        """
    }

    @Test
    fun `test no-arg 'lt' uses object value`() {
        "NoArgLt" testCompile """
            fun test(){
                user.age = 25
                val result = where { it.age.lt }
                assertNotNull(result)
                assertEquals(ConditionType.LT, result!!.type)
            }
        """
    }

    @Test
    fun `test no-arg 'gt' uses object value`() {
        "NoArgGt" testCompile """
            fun test(){
                user.age = 25
                val result = where { it.age.gt }
                assertNotNull(result)
                assertEquals(ConditionType.GT, result!!.type)
            }
        """
    }

    @Test
    fun `test no-arg 'le' uses object value`() {
        "NoArgLe" testCompile """
            fun test(){
                user.age = 25
                val result = where { it.age.le }
                assertNotNull(result)
                assertEquals(ConditionType.LE, result!!.type)
            }
        """
    }

    @Test
    fun `test no-arg 'ge' uses object value`() {
        "NoArgGe" testCompile """
            fun test(){
                user.age = 25
                val result = where { it.age.ge }
                assertNotNull(result)
                assertEquals(ConditionType.GE, result!!.type)
            }
        """
    }

    @Test
    fun `test field minus exclusion with eq`() {
        "MinusEq" testCompile """
            fun test(){
                user.id = 1
                user.username = "Alice"
                user.age = 25
                val result = whereRaw { (it - it.gender).eq }
                assertNotNull(result)
                // Should produce AND criteria for all columns except gender
                assertEquals(ConditionType.AND, result!!.type)
                val fieldNames = result.children.mapNotNull { it?.field?.name }
                assertTrue(fieldNames.contains("id"))
                assertTrue(fieldNames.contains("username"))
                assertTrue(!fieldNames.contains("gender"))
            }
        """
    }

    @Test
    fun `test chained field minus exclusion`() {
        "ChainedMinus" testCompile """
            fun test(){
                user.id = 1
                user.username = "Alice"
                val result = whereRaw { (it - it.gender - it.age).eq }
                assertNotNull(result)
                assertEquals(ConditionType.AND, result!!.type)
                val fieldNames = result.children.mapNotNull { it?.field?.name }
                assertTrue(!fieldNames.contains("gender"))
                assertTrue(!fieldNames.contains("age"))
            }
        """
    }

    @Test
    fun `test 'eq' with null comparison produces EQUAL with null value`() {
        "EqNull" testCompile """
            fun test(){
                val expected = user.getColumn("username").eq(null)
                assertEquals(expected, where { it.username == null })
            }
        """
    }

    @Test
    fun `test 'notEq' with null comparison produces EQUAL not with null value`() {
        "NeqNull" testCompile """
            fun test(){
                val expected = user.getColumn("username").eq(null, not = true)
                assertEquals(expected, where { it.username != null })
            }
        """
    }

    @Test
    fun `test reversed comparison operators`() {
        "ReversedCmp" testCompile """
            fun test(){
                val expectedGt = user.getColumn("age").gt(18)
                assertEquals(expectedGt, where { 18 < it.age })

                val expectedLt = user.getColumn("age").lt(18)
                assertEquals(expectedLt, where { 18 > it.age })

                val expectedGe = user.getColumn("age").ge(18)
                assertEquals(expectedGe, where { 18 <= it.age })

                val expectedLe = user.getColumn("age").le(18)
                assertEquals(expectedLe, where { 18 >= it.age })
            }
        """
    }

    @Test
    fun `test 'isNull' on string field`() {
        "IsNullStr" testCompile """
            fun test(){
                val expected = user.getColumn("username").isNull()
                assertEquals(expected, where { it.username.isNull })
            }
        """
    }

    @Test
    fun `test 'notNull' on string field`() {
        "NotNullStr" testCompile """
            fun test(){
                val expected = user.getColumn("username").isNull(not = true)
                assertEquals(expected, where { it.username.notNull })
            }
        """
    }

    @Test
    fun `test complex nested condition with three levels`() {
        "ThreeLevelNest" testCompile """
            fun test(){
                val result = whereRaw {
                    (it.username == "Alice" || it.username == "Bob") && it.age > 18
                }
                assertNotNull(result)
                assertEquals(ConditionType.AND, result!!.type)
                assertEquals(2, result.children.size)
            }
        """
    }

    @Language("kotlin")
    private val mainKt = """
        import com.kotlinorm.Kronos
        import com.kotlinorm.annotations.*
        import com.kotlinorm.beans.dsl.Criteria
        import com.kotlinorm.beans.dsl.Field
        import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
        import com.kotlinorm.interfaces.KPojo
        import com.kotlinorm.enums.ConditionType
        import com.kotlinorm.enums.KColumnType.TINYINT
        import com.kotlinorm.enums.NoValueStrategyType
        import com.kotlinorm.types.ToFilter
        import java.time.LocalDateTime
        import kotlin.test.assertEquals
        import kotlin.test.assertNotNull
        import kotlin.test.assertTrue
        import com.kotlinorm.functions.FunctionManager.registerFunctionBuilder
        import com.kotlinorm.functions.bundled.builders.PostgresFunctionBuilder
        import com.kotlinorm.functions.bundled.exts.MathFunctions.add
        import com.kotlinorm.functions.bundled.exts.MathFunctions.sub
        import com.kotlinorm.functions.bundled.exts.StringFunctions.length

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

        fun Field.isIn(collection: List<*>, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.IN, not, collection, tableName).asData()
        }

        fun Field.like(value: String, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.LIKE, not, value, tableName).asData()
        }

        fun Field.lt(value: Any?, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.LT, not, value, tableName).asData()
        }

        fun Field.gt(value: Any?, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.GT, not, value, tableName).asData()
        }

        fun Field.le(value: Any?, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.LE, not, value, tableName).asData()
        }

        fun Field.ge(value: Any?, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.GE, not, value, tableName).asData()
        }

        fun Field.between(range: ClosedRange<*>, not: Boolean = false): Condition {
            return Criteria(this, ConditionType.BETWEEN, not, range, tableName).asData()
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
                registerFunctionBuilder(PostgresFunctionBuilder)
            }

            val user = User()

            fun where(block: ToFilter<User, Boolean?>): Condition? {
                var rst: Criteria? = null
                user.afterFilter {
                    criteriaParamMap = user.toDataMap()
                    block!!(it)
                    rst = criteria
                }
                return rst?.children?.get(0)?.asData()
            }

            fun whereRaw(block: ToFilter<User, Boolean?>): Criteria? {
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

    infix fun String.testCompile(code: String) {
        if (this.any { !it.isLetter() } || this.first().isLowerCase()) {
            throw IllegalArgumentException("Test name must be all letters with uppercase first letter")
        }
        val result = compile(
            mainKt.replace("//inject", code),
            this@ConditionAnalysisTest.testBaseName + this
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("${this@ConditionAnalysisTest.testBaseName + this}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}
