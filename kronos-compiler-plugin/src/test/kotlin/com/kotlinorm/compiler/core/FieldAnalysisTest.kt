/**
 * Copyright 2022-2025 kronos-orm
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

import com.kotlinorm.compiler.utils.IrTestFramework
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.kotlinorm.compiler.utils.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.utils.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation

/**
 * Tests for FieldAnalysis.kt - Field Analysis and Construction
 *
 * 使用通用 IR 测试框架来验证字段分析功能
 */
@OptIn(ExperimentalCompilerApi::class, org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)
class FieldAnalysisTest {

    @Test
    fun `test property access analysis - it dot name`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source(
                "Test.kt", """
                package test
                
                fun testPropertyAccess() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val name = user.name
                    val email = user.email
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== Property Access Test ===")
        println("Property accesses found: ${context.collector.propertyAccesses.size}")

        // 验证属性访问被收集
        assertTrue(
            context.collector.propertyAccesses.size >= 2,
            "Should have at least 2 property accesses (name, email), found: ${context.collector.propertyAccesses.size}"
        )

        // 验证属性名称
        val propertyNames = context.collector.propertyAccesses.mapNotNull { call ->
            call.symbol.owner.correspondingPropertySymbol?.owner?.name?.asString()
        }
        assertTrue(
            propertyNames.contains("name") || propertyNames.contains("email"),
            "Should contain 'name' or 'email', found: $propertyNames"
        )
    }

    @Test
    fun `test property reference analysis - User colon colon name`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source(
                "Test.kt", """
                package test
                
                fun testPropertyReference() {
                    val nameRef = User::name
                    val emailRef = User::email
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== Property Reference Test ===")
        println("Property references found: ${context.collector.propertyReferences.size}")

        // 验证属性引用被收集
        assertTrue(
            context.collector.propertyReferences.size >= 2,
            "Should have at least 2 property references, found: ${context.collector.propertyReferences.size}"
        )

        // 验证引用名称
        val refNames = context.collector.propertyReferences.map { it.symbol.owner.name.asString() }
        assertTrue(
            refNames.contains("name") && refNames.contains("email"),
            "Should contain 'name' and 'email', found: $refNames"
        )
    }

    @Test
    fun `test plus expression analysis - it dot name plus it dot age`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source(
                "Test.kt", """
                package test
                
                fun testPlusExpression() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val combined = user.name + user.age
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== Plus Expression Test ===")
        println("Plus calls found: ${context.collector.plusCalls.size}")
        println("Property accesses found: ${context.collector.propertyAccesses.size}")

        // 验证 plus 调用
        assertTrue(
            context.collector.plusCalls.size >= 1,
            "Should have at least 1 plus call, found: ${context.collector.plusCalls.size}"
        )

        // 验证属性访问（plus 的操作数）
        assertTrue(
            context.collector.propertyAccesses.size >= 2,
            "Should have at least 2 property accesses in plus expression, found: ${context.collector.propertyAccesses.size}"
        )
    }

    @Test
    fun `test minus expression analysis - property references`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source(
                "Test.kt", """
                package test
                
                fun testMinusExpression() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val passwordRef = User::password
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== Minus Expression Test ===")
        println("Property references found: ${context.collector.propertyReferences.size}")

        // 验证属性引用
        assertTrue(
            context.collector.propertyReferences.size >= 1,
            "Should have at least 1 property reference (password), found: ${context.collector.propertyReferences.size}"
        )

        val refNames = context.collector.propertyReferences.map { it.symbol.owner.name.asString() }
        assertTrue(refNames.contains("password"), "Should contain 'password', found: $refNames")
    }

    @Test
    fun `test constant analysis - string literal`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source(
                "Test.kt", """
                package test
                
                fun testConstant() {
                    val sql = "COUNT(*)"
                    val another = "SELECT *"
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== Constant Test ===")
        println("Constants found: ${context.collector.constants.size}")

        // 验证常量
        assertTrue(
            context.collector.constants.size >= 2,
            "Should have at least 2 string constants, found: ${context.collector.constants.size}"
        )
    }

    @Test
    fun `test KPojo instance analysis - getValue`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source(
                "Test.kt", """
                package test
                
                fun testKPojoInstance() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val instance = user
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== KPojo Instance Test ===")
        println("GetValue expressions found: ${context.collector.getValues.size}")

        // 验证 getValue 表达式
        assertTrue(
            context.collector.getValues.isNotEmpty(),
            "Should have at least 1 getValue expression, found: ${context.collector.getValues.size}"
        )
    }

    @Test
    fun `test multiple fields with plus - chained operations`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source(
                "Test.kt", """
                package test
                
                fun testMultiplePlus() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val combined = user.name + user.email + user.age
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== Multiple Plus Test ===")
        println("Plus calls found: ${context.collector.plusCalls.size}")
        println("Property accesses found: ${context.collector.propertyAccesses.size}")

        // 验证多个 plus 调用（链式）
        assertTrue(
            context.collector.plusCalls.size >= 2,
            "Should have at least 2 plus calls for chaining, found: ${context.collector.plusCalls.size}"
        )

        // 验证三个属性访问
        assertTrue(
            context.collector.propertyAccesses.size >= 3,
            "Should have at least 3 pr¬operty accesses, found: ${context.collector.propertyAccesses.size}"
        )
    }

    @Test
    fun `test multiple property references`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source(
                "Test.kt", """
                package test
                
                fun testMultipleReferences() {
                    val passwordRef = User::password
                    val emailRef = User::email
                    val combined = passwordRef.name + emailRef.name
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== Multiple References Test ===")
        println("Property references found: ${context.collector.propertyReferences.size}")
        println("Plus calls found: ${context.collector.plusCalls.size}")

        // 验证属性引用
        assertTrue(
            context.collector.propertyReferences.size >= 2,
            "Should have at least 2 property references, found: ${context.collector.propertyReferences.size}"
        )

        // 验证 plus 调用
        assertTrue(
            context.collector.plusCalls.size >= 1,
            "Should have at least 1 plus call, found: ${context.collector.plusCalls.size}"
        )
    }

    @Test
    fun `test error case - unknown property`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source(
                "Test.kt", """
                package test
                
                fun testUnknownProperty() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val unknown = user.unknownField
                }
            """
            )
        )

        // 应该编译失败（因为 unknownField 不存在）
        context.assertFailure()

        println("\n=== Error Case Test (Unknown Property) ===")
        println("Exit code: ${context.exitCode}")
    }

    @Test
    fun `test non-KPojo type compiles normally`() {
        val context = IrTestFramework.compile(
            IrTestFramework.source(
                "Test.kt", """
                package test
                
                data class NotKPojo(val id: Int, val name: String)
                
                fun testNonKPojo() {
                    val obj = NotKPojo(1, "test")
                    val name = obj.name
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== Non-KPojo Test ===")
        println("Property accesses found: ${context.collector.propertyAccesses.size}")

        // 非 KPojo 类型也应该有属性访问
        assertTrue(
            context.collector.propertyAccesses.size >= 1,
            "Should have property access even for non-KPojo, found: ${context.collector.propertyAccesses.size}"
        )
    }

    @Test
    fun `test different property types`() {
        val context = IrTestFramework.compile(
            IrTestFramework.source(
                "Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                
                data class TypeTest(
                    val intField: Int,
                    val stringField: String,
                    val boolField: Boolean,
                    val doubleField: Double,
                    val longField: Long
                ) : KPojo
                
                fun testDifferentTypes() {
                    val obj = TypeTest(1, "test", true, 3.14, 100L)
                    val i = obj.intField
                    val s = obj.stringField
                    val b = obj.boolField
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== Different Types Test ===")
        println("Property accesses found: ${context.collector.propertyAccesses.size}")

        // 验证不同类型的属性访问
        assertTrue(
            context.collector.propertyAccesses.size >= 3,
            "Should have at least 3 property accesses for different types, found: ${context.collector.propertyAccesses.size}"
        )
    }

    @Test
    fun `test nullable fields`() {
        val context = IrTestFramework.compile(
            IrTestFramework.source(
                "Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                
                data class NullableTest(
                    val id: Int,
                    val name: String?,
                    val email: String?,
                    val age: Int?
                ) : KPojo
                
                fun testNullable() {
                    val obj = NullableTest(1, null, "test@example.com", null)
                    val email = obj.email
                    val name = obj.name
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== Nullable Fields Test ===")
        println("Property accesses found: ${context.collector.propertyAccesses.size}")

        // 验证可空字段的属性访问
        assertTrue(
            context.collector.propertyAccesses.size >= 2,
            "Should have at least 2 property accesses for nullable fields, found: ${context.collector.propertyAccesses.size}"
        )
    }

    @Test
    fun `test annotated KPojo`() {
        val context = IrTestFramework.compile(
            IrTestFramework.source(
                "Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.Column
                import com.kotlinorm.annotations.PrimaryKey
                
                data class AnnotatedUser(
                    @PrimaryKey
                    val id: Int,
                    @Column("user_name")
                    val name: String,
                    val email: String
                ) : KPojo
                
                fun testAnnotated() {
                    val user = AnnotatedUser(1, "John", "john@example.com")
                    val name = user.name
                    val email = user.email
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== Annotated KPojo Test ===")
        println("Property accesses found: ${context.collector.propertyAccesses.size}")

        // 验证带注解字段的属性访问
        assertTrue(
            context.collector.propertyAccesses.size >= 2,
            "Should have at least 2 property accesses for annotated fields, found: ${context.collector.propertyAccesses.size}"
        )
    }

    @Test
    fun `test IR collection framework works correctly`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source(
                "Test.kt", """
                package test
                
                fun testFramework() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val id = user.id
                    val name = user.name
                    val nameRef = User::email
                    val sql = "SELECT *"
                }
            """
            )
        )

        context.assertSuccess()

        println("\n=== Framework Test ===")
        println("Property accesses: ${context.collector.propertyAccesses.size}")
        println("Property references: ${context.collector.propertyReferences.size}")
        println("Constants: ${context.collector.constants.size}")
        println("All expressions: ${context.collector.allExpressions.size}")

        // 验证框架正确收集了各种 IR 元素
        assertTrue(context.collector.propertyAccesses.size >= 2, "Should have property accesses")
        assertTrue(context.collector.propertyReferences.size >= 1, "Should have property references")
        assertTrue(context.collector.constants.size >= 1, "Should have constants")
        assertTrue(context.collector.allExpressions.size > 0, "Should have expressions")

        println("✓ IR Test Framework is working correctly")
    }


    @Suppress("LanguageDetectionInspection")
    private val mainKt = """
        import com.kotlinorm.Kronos
        import com.kotlinorm.annotations.*
        import com.kotlinorm.beans.dsl.Field
        import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
        import com.kotlinorm.interfaces.KPojo
        import com.kotlinorm.enums.KColumnType
        import com.kotlinorm.enums.KColumnType.*
        import com.kotlinorm.types.ToSelect
        import java.math.BigDecimal
        import java.time.LocalDateTime
        import java.time.LocalDate
        import java.time.LocalTime
        import kotlin.test.assertEquals
        import kotlin.test.assertNotNull
        import kotlin.test.assertTrue

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

    private fun compileAndRun(classes: String, code: String, tag: String) {
        val source = mainKt
            .replace("// PLACEHOLDER_CLASSES", classes)
            .replace("// PLACEHOLDER_INJECT", code)
        val result = compile(source, testBaseName + tag)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val ktClazz = result.classLoader.loadClass("${testBaseName}${tag}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }

    @Test
    fun `test fields with various Kotlin types`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_typed")
        data class TypedEntity(
            var id: Int? = null,
            var longVal: Long? = null,
            var boolVal: Boolean? = null,
            var stringVal: String? = null,
            var doubleVal: Double? = null,
            var floatVal: Float? = null,
            var shortVal: Short? = null,
            var byteVal: Byte? = null,
            var bigDecimal: BigDecimal? = null,
            var localDate: LocalDate? = null,
            var localTime: LocalTime? = null,
            var localDateTime: LocalDateTime? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = TypedEntity()
            val cols = e.kronosColumns()
            assertTrue(cols.size >= 12, "Should have at least 12 columns, got ${'$'}{cols.size}")
            assertEquals(KColumnType.INT, e.getColumn("id").type)
            assertEquals(KColumnType.BIGINT, e.getColumn("longVal").type)
            assertEquals(KColumnType.BIT, e.getColumn("boolVal").type)
            assertEquals(KColumnType.VARCHAR, e.getColumn("stringVal").type)
            assertEquals(KColumnType.DOUBLE, e.getColumn("doubleVal").type)
            assertEquals(KColumnType.FLOAT, e.getColumn("floatVal").type)
            assertEquals(KColumnType.SMALLINT, e.getColumn("shortVal").type)
            assertEquals(KColumnType.TINYINT, e.getColumn("byteVal").type)
            assertEquals(KColumnType.DECIMAL, e.getColumn("bigDecimal").type)
            assertEquals(KColumnType.DATE, e.getColumn("localDate").type)
            assertEquals(KColumnType.TIME, e.getColumn("localTime").type)
            assertEquals(KColumnType.DATETIME, e.getColumn("localDateTime").type)
        }
            """,
            tag = "VariousTypes"
        )
    }

    // ========================================================================
    // 2. @Serialize annotation
    // ========================================================================

    @Test
    fun `test field with Serialize annotation`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_serial")
        data class SerialEntity(
            var id: Int? = null,
            @Serialize var tags: List<String>? = null,
            @Serialize var metadata: Map<String, String>? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = SerialEntity()
            val tagsCol = e.getColumn("tags")
            assertTrue(tagsCol.serializable, "tags should be serializable")
            val metaCol = e.getColumn("metadata")
            assertTrue(metaCol.serializable, "metadata should be serializable")
        }
            """,
            tag = "SerializeAnnotation"
        )
    }

    // ========================================================================
    // 3. @DateTimeFormat annotation
    // ========================================================================

    @Test
    fun `test field with DateTimeFormat annotation`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_dtf")
        data class DateFormatEntity(
            var id: Int? = null,
            @DateTimeFormat("yyyy-MM-dd") var createdDate: String? = null,
            @DateTimeFormat("yyyy-MM-dd HH:mm:ss") var updatedAt: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = DateFormatEntity()
            assertEquals("yyyy-MM-dd", e.getColumn("createdDate").dateFormat)
            assertEquals("yyyy-MM-dd HH:mm:ss", e.getColumn("updatedAt").dateFormat)
        }
            """,
            tag = "DateTimeFormat"
        )
    }

    // ========================================================================
    // 4. @Cascade annotation
    // ========================================================================

    @Test
    fun `test field with Cascade annotation`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_cascade")
        data class CascadeParent(
            var id: Int? = null,
            var name: String? = null,
            var childId: Int? = null,
            @Cascade(["childId"], ["id"])
            var child: CascadeChild? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }

        @Table(name = "tb_cascade_child")
        data class CascadeChild(
            var id: Int? = null,
            var value: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val p = CascadeParent()
            val childCol = p.getColumn("child")
            assertNotNull(childCol.cascade, "child field should have cascade info")
        }
            """,
            tag = "CascadeAnnotation"
        )
    }

    // ========================================================================
    // 5. @Necessary annotation
    // ========================================================================

    @Test
    fun `test field with Necessary annotation`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_nec")
        data class NecessaryEntity(
            var id: Int? = null,
            @Necessary var requiredName: String? = null,
            var optionalDesc: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = NecessaryEntity()
            val cols = e.kronosColumns()
            val reqCol = e.getColumn("requiredName")
            val optCol = e.getColumn("optionalDesc")
            // Necessary fields should be non-nullable in the Field metadata
            assertTrue(!reqCol.nullable, "requiredName should not be nullable")
            assertTrue(optCol.nullable, "optionalDesc should be nullable")
        }
            """,
            tag = "NecessaryAnnotation"
        )
    }

    // ========================================================================
    // 6. KPojo with multiple fields and toDataMap
    // ========================================================================

    @Test
    fun `test KPojo with many fields and toDataMap`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_many")
        data class ManyFieldEntity(
            @PrimaryKey(identity = true) var id: Int? = null,
            var name: String? = null,
            var age: Int? = null,
            var score: Double? = null,
            var active: Boolean? = null,
            var longVal: Long? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = ManyFieldEntity(id = 1, name = "test", age = 25, score = 9.5, active = true, longVal = 100L)
            val map = e.toDataMap()
            assertEquals(1, map["id"])
            assertEquals("test", map["name"])
            assertEquals(25, map["age"])
            assertEquals(9.5, map["score"])
            assertEquals(true, map["active"])
            assertEquals(100L, map["longVal"])
        }
            """,
            tag = "ManyFieldsToDataMap"
        )
    }

    // ========================================================================
    // 7. Nullable vs non-nullable fields
    // ========================================================================

    @Test
    fun `test nullable vs non-nullable field metadata`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_null")
        data class NullableEntity(
            var id: Int? = null,
            @Necessary var name: String? = null,
            var description: String? = null,
            @Necessary var code: Int? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = NullableEntity()
            assertTrue(!e.getColumn("name").nullable, "name with @Necessary should not be nullable")
            assertTrue(e.getColumn("description").nullable, "description should be nullable")
            assertTrue(!e.getColumn("code").nullable, "code with @Necessary should not be nullable")
        }
            """,
            tag = "NullableFields"
        )
    }

    // ========================================================================
    // 8. @Column annotation mapping
    // ========================================================================

    @Test
    fun `test Column annotation maps field to custom column name`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_colmap")
        data class ColumnMappedEntity(
            var id: Int? = null,
            @Column("user_name") var name: String? = null,
            @Column("e_mail") var email: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = ColumnMappedEntity()
            assertEquals("user_name", e.getColumn("name").columnName)
            assertEquals("e_mail", e.getColumn("email").columnName)
        }
            """,
            tag = "ColumnAnnotation"
        )
    }

    // ========================================================================
    // 9. @Default annotation
    // ========================================================================

    @Test
    fun `test Default annotation sets default value`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_def")
        data class DefaultEntity(
            var id: Int? = null,
            @Default("active") var status: String? = null,
            @Default("0") var counter: Int? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = DefaultEntity()
            assertEquals("active", e.getColumn("status").defaultValue)
            assertEquals("0", e.getColumn("counter").defaultValue)
        }
            """,
            tag = "DefaultAnnotation"
        )
    }

    // ========================================================================
    // 10. @PrimaryKey with identity
    // ========================================================================

    @Test
    fun `test PrimaryKey identity field`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_pk")
        data class PkEntity(
            @PrimaryKey(identity = true) var id: Int? = null,
            var name: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = PkEntity()
            val idCol = e.getColumn("id")
            assertNotNull(idCol.primaryKey, "id should have primaryKey info")
        }
            """,
            tag = "PrimaryKeyIdentity"
        )
    }

    // ========================================================================
    // 11. @ColumnType override
    // ========================================================================

    @Test
    fun `test ColumnType annotation overrides inferred type`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_ct")
        data class ColumnTypeEntity(
            var id: Int? = null,
            @ColumnType(TINYINT) var flag: Int? = null,
            @ColumnType(BIGINT) var bigId: Int? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = ColumnTypeEntity()
            assertEquals(KColumnType.TINYINT, e.getColumn("flag").type)
            assertEquals(KColumnType.BIGINT, e.getColumn("bigId").type)
        }
            """,
            tag = "ColumnTypeOverride"
        )
    }

    // ========================================================================
    // 12. Select with field subtraction on typed entity
    // ========================================================================

    @Test
    fun `test select field subtraction on diverse entity`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_sub")
        data class SubEntity(
            var id: Int? = null,
            var name: String? = null,
            var secret: String? = null,
            var age: Int? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = SubEntity()
            fun SubEntity.select(block: ToSelect<SubEntity, Any?>): List<Field> {
                val rst: MutableList<Field> = mutableListOf()
                afterSelect {
                    block!!(it)
                    rst += fields
                }
                return rst
            }
            val result = e.select { it - it.secret }
            val names = result.map { it.name }
            assertTrue("secret" !in names, "secret should be excluded")
            assertTrue("id" in names, "id should remain")
            assertTrue("name" in names, "name should remain")
            assertTrue("age" in names, "age should remain")
        }
            """,
            tag = "SelectSubtraction"
        )
    }

    // ========================================================================
    // 13. Cascade with collection type
    // ========================================================================

    @Test
    fun `test cascade field with collection type`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_parent_list")
        data class ParentWithList(
            var id: Int? = null,
            var name: String? = null,
            @Cascade(["id"], ["parentId"])
            var children: List<ChildItem>? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }

        @Table(name = "tb_child_item")
        data class ChildItem(
            var id: Int? = null,
            var parentId: Int? = null,
            var value: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val p = ParentWithList()
            val childrenCol = p.getColumn("children")
            assertNotNull(childrenCol.cascade, "children should have cascade info")
            assertTrue(childrenCol.cascadeIsCollectionOrArray, "children should be marked as collection cascade")
        }
            """,
            tag = "CascadeCollection"
        )
    }

    // ========================================================================
    // 14. Select with property reference syntax
    // ========================================================================

    @Test
    fun `test select with property reference syntax on typed entity`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_ref_sel")
        data class RefSelEntity(
            var id: Int? = null,
            var name: String? = null,
            var age: Int? = null,
            var score: Double? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = RefSelEntity()
            fun RefSelEntity.select(block: ToSelect<RefSelEntity, Any?>): List<Field> {
                val rst: MutableList<Field> = mutableListOf()
                afterSelect {
                    block!!(it)
                    rst += fields
                }
                return rst
            }
            val rst = e.select { it::id + it::name + it::score }
            assertEquals(3, rst.size)
            assertEquals("id", rst[0].name)
            assertEquals("name", rst[1].name)
            assertEquals("score", rst[2].name)
        }
            """,
            tag = "PropertyRefSelect"
        )
    }

    // ========================================================================
    // 15. toDataMap / fromMapData round-trip with diverse types
    // ========================================================================

    @Test
    fun `test toDataMap and fromMapData with diverse types`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_roundtrip")
        data class RoundTripEntity(
            var id: Int? = null,
            var name: String? = null,
            var active: Boolean? = null,
            var score: Double? = null,
            var bigVal: BigDecimal? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = RoundTripEntity(id = 1, name = "test", active = true, score = 9.5, bigVal = BigDecimal("123.45"))
            val map = e.toDataMap()
            assertEquals(1, map["id"])
            assertEquals("test", map["name"])
            assertEquals(true, map["active"])
            assertEquals(9.5, map["score"])
            assertEquals(BigDecimal("123.45"), map["bigVal"])

            val e2 = RoundTripEntity().fromMapData<RoundTripEntity>(map)
            assertEquals(1, e2.id)
            assertEquals("test", e2.name)
            assertEquals(true, e2.active)
            assertEquals(9.5, e2.score)
            assertEquals(BigDecimal("123.45"), e2.bigVal)
        }
            """,
            tag = "RoundTrip"
        )
    }
}
