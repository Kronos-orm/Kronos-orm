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
class KTableParserForConditionTransformerTest {
    @Test
    fun `test 'eq' condition`() {
        "Eq" testCompile """
            fun test(){
                val expected = user.getColumn("username").eq("Alice")
                assertEquals(expected, where { it.username == "Alice" })
                assertEquals(expected, where { "Alice" == it.username })

                user.username = "Alice"
                assertEquals(expected, where { it.username.eq })
            }
        """
    }

    @Test
    fun `test 'notEq' condition`() {
        "NotEq" testCompile """
            fun test(){
                user.username = "Alice"
                val expected = user.getColumn("username").eq("Alice", not = true)
                assertEquals(expected, where { it.username.neq })
            }
        """
    }

    @Test
    fun `test 'isIn' condition`() {
        "IsIn" testCompile """
            fun test(){
                val ids = listOf<Int?>(1, 2, 3)
                val expected1 = user.getColumn("id").isIn(ids)
                assertEquals(expected1, where { it.id in ids })
                assertEquals(expected1, where { ids.contains(it.id) })

                val listOfNames = listOf("Alice", "Bob", "Cindy")
                val expected2 = user.getColumn("username").isIn(listOfNames)
                assertEquals(expected2, where { it.username in listOfNames })
                assertEquals(expected2, where { listOfNames.contains(it.username) })
            }
        """
    }

    @Test
    fun `test 'like' condition`() {
        "Like" testCompile """
            fun test(){
                val expected = user.getColumn("username").like("%A")
                assertEquals(expected, where { it.username like "%A" })

                user.username = "%A"
                assertEquals(expected, where { it.username.like })
            }
        """
    }

    @Test
    fun `test 'notLike' condition`() {
        "NotLike" testCompile """
            fun test(){
                val expected = user.getColumn("username").like("%A", not = true)
                assertEquals(expected, where { it.username notLike "%A" })

                user.username = "%A"
                assertEquals(expected, where { it.username.notLike })
            }
        """
    }

    @Test
    fun `test 'startsWith' condition`() {
        "StartsWith" testCompile """
            fun test(){
                val expected = user.getColumn("username").like("A%")
                assertEquals(expected, where { it.username.startsWith("A") })

                user.username = "A"
                assertEquals(expected, where { it.username.startsWith })
            }
        """
    }

    @Test
    fun `test 'endsWith' condition`() {
        "EndsWith" testCompile """
            fun test(){
                val expected = user.getColumn("username").like("%A")
                assertEquals(expected, where { it.username.endsWith("A") })

                user.username = "A"
                assertEquals(expected, where { it.username.endsWith })
            }
        """
    }

    @Test
    fun `test 'contains' condition`() {
        "Contains" testCompile """
            fun test(){
                val expected = user.getColumn("username").like("%A%")
                assertEquals(expected, where { it.username.contains("A") })
                assertEquals(expected, where { "A" in it.username })

                user.username = "A"
                assertEquals(expected, where { it.username.contains })
            }
        """
    }

    @Test
    fun `test 'lt' condition`() {
        "Lt" testCompile """
            fun test(){
                val expected = user.getColumn("age").lt(18)
                assertEquals(expected, where { it.age < 18 })
                assertEquals(expected, where { 18 > it.age })

                user.age = 18
                assertEquals(expected, where { it.age.lt })
            }
        """
    }

    @Test
    fun `test 'gt' condition`() {
        "Gt" testCompile """
            fun test(){
                val expected = user.getColumn("age").gt(18)
                assertEquals(expected, where { it.age > 18 })
                assertEquals(expected, where { 18 < it.age })

                user.age = 18
                assertEquals(expected, where { it.age.gt })
            }
        """
    }

    @Test
    fun `test 'le' condition`() {
        "Le" testCompile """
            fun test(){
                val expected = user.getColumn("age").le(18)
                assertEquals(expected, where { it.age <= 18 })
                assertEquals(expected, where { 18 >= it.age })

                user.age = 18
                assertEquals(expected, where { it.age.le })
            }
        """
    }

    @Test
    fun `test 'ge' condition`() {
        "Ge" testCompile """
            fun test(){
                val expected = user.getColumn("age").ge(18)
                assertEquals(expected, where { it.age >= 18 })
                assertEquals(expected, where { 18 <= it.age })

                user.age = 18
                assertEquals(expected, where { it.age.ge })
            }
        """
    }

    @Test
    fun `test 'between' condition`() {
        "Between" testCompile """
            fun test(){
                val expected = user.getColumn("age").between(18..30)
                assertEquals(expected, where { it.age between 18..30 })
            }
        """
    }

    @Test
    fun `test 'notBetween' condition`() {
        "NotBetween" testCompile """
            fun test(){
                val expected = user.getColumn("age").between(18..30, not = true)
                assertEquals(expected, where { it.age notBetween 18..30 })
            }
        """
    }

    @Test
    fun `test 'isNull' condition`() {
        "IsNull" testCompile """
            fun test(){
                val expected = user.getColumn("age").isNull()
                assertEquals(expected, where { it.age.isNull })
            }
        """
    }

    @Test
    fun `test 'notNull' condition`() {
        "Necessary" testCompile """
            fun test(){
                val expected = user.getColumn("age").isNull(not = true)
                assertEquals(expected, where { it.age.notNull })
            }
        """
    }

    @Test
    fun `test 'asSql' condition`() {
        "AsSql" testCompile """
            fun test(){
                val expected = sql("username = 'Alice'")
                assertEquals(expected, where { "username = 'Alice'".asSql() })

                val expected2 = sql(true)
                assertEquals(expected2, where { (1 == 1).asSql() })
            }
        """
    }

    @Test
    fun `test 'takeIf' condition`() {
        "TakeIf" testCompile """
            fun test(){
                user.id = 1
                val expected: Condition? = null
                assertEquals(expected, where { (it.id == 1).takeIf(false) })
            }
        """
    }

    @Test
    fun `test function field as left operand in GT`() {
        "FuncGt" testCompile """
            fun test(){
                val result = where { f.length(it.username) > 5 }
                assertNotNull(result)
                assertEquals(ConditionType.GT, result!!.type)
            }
        """
    }

    @Test
    fun `test function field on both sides of GT`() {
        "FuncBothSides" testCompile """
            fun test(){
                val result = where { f.add(it.age, 1) > f.sub(it.age, 1) }
                assertNotNull(result)
                assertEquals(ConditionType.GT, result!!.type)
            }
        """
    }

    @Test
    fun `test function field in equality`() {
        "FuncEq" testCompile """
            fun test(){
                val result = where { f.length(it.username) == 5 }
                assertNotNull(result)
                assertEquals(ConditionType.EQUAL, result!!.type)
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
        import com.kotlinorm.types.ToFilter
        import java.time.LocalDateTime
        import kotlin.test.assertEquals
        import kotlin.test.assertNotNull
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
            this@KTableParserForConditionTransformerTest.testBaseName + this
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("${this@KTableParserForConditionTransformerTest.testBaseName + this}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}
