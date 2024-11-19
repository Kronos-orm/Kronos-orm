package com.kotlinorm.compiler.fir.transformer.kTable

import com.kotlinorm.compiler.fir.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.fir.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

class KTableParserForConditionTransformerTest {
    @Test
    fun `test 'eq' condition`() {
        "Eq" testCompile (
                """
                fun test(){
                    val expected = user["username"].eq("Alice")
                    assertEquals(expected, where { it.username == "Alice" })
                    assertEquals(expected, where { "Alice" == it.username })

                    user.username = "Alice"
                    assertEquals(expected, where { it.username.eq })
                }
                """
                )
    }

    @Test
    fun `test 'notEq' condition`() {
        "NotEq" testCompile (
                """
                fun test(){
                    user.username = "Alice"
                    val expected = user["username"].eq("Alice", not = true)
                    assertEquals(expected, where { it.username.neq })
                }
                """
                )
    }

    @Test
    fun `test 'isIn' condition`() {
        "IsIn" testCompile (
                """
                fun test(){
                    val ids = listOf<Int?>(1, 2, 3)
                    val expected1 = user["id"].isIn(ids)
                    assertEquals(expected1, where { it.id in ids })
                    assertEquals(expected1, where { ids.contains(it.id) })
                    
                    val listOfNames = listOf("Alice", "Bob", "Cindy")
                    val expected2 = user["username"].isIn(listOfNames)
                    assertEquals(expected2, where { it.username in listOfNames })
                    assertEquals(expected2, where { listOfNames.contains(it.username) })
                }
                """
                )
    }

    @Test
    fun `test 'like' condition`() {
        "Like" testCompile (
                """
                fun test(){
                    val expected = user["username"].like("%A")
                    assertEquals(expected, where { it.username like "%A" })

                    user.username = "%A"
                    assertEquals(expected, where { it.username.like })
                }
                """
                )
    }

    @Test
    fun `test 'notLike' condition`() {
        "NotLike" testCompile (
                """
                fun test(){
                    val expected = user["username"].like("%A", not = true)
                    assertEquals(expected, where { it.username notLike "%A" })

                    user.username = "%A"
                    assertEquals(expected, where { it.username.notLike })
                }
                """
                )
    }

    @Test
    fun `test 'startsWith' condition`() {
        "StartsWith" testCompile (
                """
                fun test(){
                    val expected = user["username"].like("A%")
                    assertEquals(expected, where { it.username.startsWith("A") })

                    user.username = "A"
                    assertEquals(expected, where { it.username.startsWith })
                }
                """
                )
    }

    @Test
    fun `test 'endsWith' condition`() {
        "EndsWith" testCompile (
                """
                fun test(){
                    val expected = user["username"].like("%A")
                    assertEquals(expected, where { it.username.endsWith("A") })

                    user.username = "A"
                    assertEquals(expected, where { it.username.endsWith })
                }
                """
                )
    }

    @Test
    fun `test 'contains' condition`() {
        "Contains" testCompile (
                """
                fun test(){
                    val expected = user["username"].like("%A%")
                    assertEquals(expected, where { it.username.contains("A") })
                    assertEquals(expected, where { "A" in it.username })

                    user.username = "A"
                    assertEquals(expected, where { it.username.contains })
                }
                """
                )
    }

    @Test
    fun `test 'lt' condition`() {
        "Lt" testCompile (
                """
                fun test(){
                    val expected = user["age"].lt(18)
                    assertEquals(expected, where { it.age < 18 })
                    assertEquals(expected, where { 18 > it.age })
                    
                    user.age = 18
                    assertEquals(expected, where { it.age.lt })
                }
                """
                )
    }

    @Test
    fun `test 'gt' condition`() {
        "Gt" testCompile (
                """
                fun test(){
                    val expected = user["age"].gt(18)
                    assertEquals(expected, where { it.age > 18 })
                    assertEquals(expected, where { 18 < it.age })
                    
                    user.age = 18
                    assertEquals(expected, where { it.age.gt })
                }
                """
                )
    }

    @Test
    fun `test 'le' condition`() {
        "Le" testCompile (
                """
                fun test(){
                    val expected = user["age"].le(18)
                    assertEquals(expected, where { it.age <= 18 })
                    assertEquals(expected, where { 18 >= it.age })
                    
                    user.age = 18
                    assertEquals(expected, where { it.age.le })
                }
                """
                )
    }

    @Test
    fun `test 'ge' condition`() {
        "Ge" testCompile (
                """
                fun test(){
                    val expected = user["age"].ge(18)
                    assertEquals(expected, where { it.age >= 18 })
                    assertEquals(expected, where { 18 <= it.age })
                    
                    user.age = 18
                    assertEquals(expected, where { it.age.ge })
                }
                """
                )
    }

    @Test
    fun `test 'between' condition`() {
        "Between" testCompile (
                """
                fun test(){
                    val expected = user["age"].between(18..30)
                    assertEquals(expected, where { it.age between 18..30 })
                }
                """
                )
    }

    @Test
    fun `test 'notBetween' condition`() {
        "NotBetween" testCompile (
                """
                fun test(){
                    val expected = user["age"].between(18..30, not = true)
                    assertEquals(expected, where { it.age notBetween 18..30 })
                }
                """
                )
    }

    @Test
    fun `test 'isNull' condition`() {
        "IsNull" testCompile (
                """
                fun test(){
                    val expected = user["age"].isNull()
                    assertEquals(expected, where { it.age.isNull })
                }
                """
                )
    }

    @Test
    fun `test 'notNull' condition`() {
        "NotNull" testCompile (
                """
                fun test(){
                    val expected = user["age"].isNull(not = true)
                    assertEquals(expected, where { it.age.notNull })
                }
                """
                )
    }

    @Test
    fun `test 'regexp' condition`() {
        "Regexp" testCompile (
                """
                fun test(){
                    val expected = user["username"].regexp("A.*")
                    assertEquals(expected, where { it.username regexp "A.*" })

                    user.username = "A.*"
                    assertEquals(expected, where { it.username.regexp })    
                }
                """
                )
    }

    @Test
    fun `test 'notRegexp' condition`() {
        "NotRegexp" testCompile (
                """
                fun test(){
                    val expected = user["username"].regexp("A.*", not = true)
                    assertEquals(expected, where { it.username notRegexp "A.*" })

                    user.username = "A.*"
                    assertEquals(expected, where { it.username.notRegexp })    
                }
                """
                )
    }

    @Test
    fun `test 'asSql' condition`() {
        "AsSql" testCompile (
                """
                fun test(){
                    val expected = sql("username = 'Alice'")
                    assertEquals(expected, where { "username = 'Alice'".asSql() })

                    val expected2 = sql(true)
                    assertEquals(expected2, where { (1 == 1).asSql() })    
                }
                """
                )
    }

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
            
            /**
             * User
             * Kotlin Data Class for User
             */
            @Table(name = "tb_user")
            @TableIndex("idx_username", ["name"], "UNIQUE")
            @TableIndex(name = "idx_multi", columns = ["id", "name"], "UNIQUE")
            data class User(
                @PrimaryKey(identity = true)
                var id: Int? = null,
                @NotNull
                var username: String? = null,
                @ColumnType(TINYINT)
                @Default("0")
                var gender: Int? = null,
                @Column("phone_number") val telephone: String? = null,
                @Column("email_address") val email: String? = null,
                val birthday: String? = null,
                @Serializable
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
                operator fun get(name: String): Field {
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
                Kronos.apply {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
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

    @OptIn(ExperimentalCompilerApi::class)
    infix fun String.testCompile(@Language("kotlin") code: String) {
        if (this.any { !it.isLetter() } && this.first().isLowerCase()) {
            throw IllegalArgumentException("测试名称必须全部都是字母，且首字母必须大写")
        }
        val result = compile(mainKt.replace("//inject", code), this@KTableParserForConditionTransformerTest.testBaseName + this)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz = result.classLoader.loadClass("${this@KTableParserForConditionTransformerTest.testBaseName + this}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}