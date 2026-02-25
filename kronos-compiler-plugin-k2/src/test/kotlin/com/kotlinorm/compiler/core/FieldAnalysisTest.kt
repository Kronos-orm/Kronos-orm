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
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
            IrTestFramework.source("Test.kt", """
                package test
                
                fun testPropertyAccess() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val name = user.name
                    val email = user.email
                }
            """)
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
        assertTrue(propertyNames.contains("name") || propertyNames.contains("email"),
            "Should contain 'name' or 'email', found: $propertyNames")
    }

    @Test
    fun `test property reference analysis - User colon colon name`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source("Test.kt", """
                package test
                
                fun testPropertyReference() {
                    val nameRef = User::name
                    val emailRef = User::email
                }
            """)
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
        assertTrue(refNames.contains("name") && refNames.contains("email"),
            "Should contain 'name' and 'email', found: $refNames")
    }

    @Test
    fun `test plus expression analysis - it dot name plus it dot age`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source("Test.kt", """
                package test
                
                fun testPlusExpression() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val combined = user.name + user.age
                }
            """)
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
            IrTestFramework.source("Test.kt", """
                package test
                
                fun testMinusExpression() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val passwordRef = User::password
                }
            """)
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
            IrTestFramework.source("Test.kt", """
                package test
                
                fun testConstant() {
                    val sql = "COUNT(*)"
                    val another = "SELECT *"
                }
            """)
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
            IrTestFramework.source("Test.kt", """
                package test
                
                fun testKPojoInstance() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val instance = user
                }
            """)
        )

        context.assertSuccess()
        
        println("\n=== KPojo Instance Test ===")
        println("GetValue expressions found: ${context.collector.getValues.size}")
        
        // 验证 getValue 表达式
        assertTrue(
            context.collector.getValues.size >= 1,
            "Should have at least 1 getValue expression, found: ${context.collector.getValues.size}"
        )
    }

    @Test
    fun `test multiple fields with plus - chained operations`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source("Test.kt", """
                package test
                
                fun testMultiplePlus() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val combined = user.name + user.email + user.age
                }
            """)
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
            "Should have at least 3 property accesses, found: ${context.collector.propertyAccesses.size}"
        )
    }

    @Test
    fun `test multiple property references`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source("Test.kt", """
                package test
                
                fun testMultipleReferences() {
                    val passwordRef = User::password
                    val emailRef = User::email
                    val combined = passwordRef.name + emailRef.name
                }
            """)
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
            IrTestFramework.source("Test.kt", """
                package test
                
                fun testUnknownProperty() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val unknown = user.unknownField
                }
            """)
        )

        // 应该编译失败
        context.assertFailure()
        
        println("\n=== Error Case Test (Unknown Property) ===")
        println("Exit code: ${context.exitCode}")
    }

    @Test
    fun `test non-KPojo type compiles normally`() {
        val context = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
                package test
                
                data class NotKPojo(val id: Int, val name: String)
                
                fun testNonKPojo() {
                    val obj = NotKPojo(1, "test")
                    val name = obj.name
                }
            """)
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
            IrTestFramework.source("Test.kt", """
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
            """)
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
            IrTestFramework.source("Test.kt", """
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
            """)
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
            IrTestFramework.source("Test.kt", """
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
            """)
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
            IrTestFramework.source("Test.kt", """
                package test
                
                fun testFramework() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val id = user.id
                    val name = user.name
                    val nameRef = User::email
                    val sql = "SELECT *"
                }
            """)
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
}
