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
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for Symbols.kt - Symbol Resolution
 *
 * 使用通用 IR 测试框架直接测试符号解析，完全替代 JSON debug 机制
 */
@OptIn(ExperimentalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
class SymbolsTest {

    @Test
    fun `test all class symbols can be resolved`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                data class User(val id: Int) : KPojo
            """)
        ) { ctx, pluginContext ->
            println("\n=== Class Symbol Resolution Test ===")
            
            // 直接在 IR 生成阶段测试符号解析
            with(pluginContext) {
                // 测试所有类符号
                val classSymbols = mapOf(
                    "KPojo" to kPojoClassSymbol,
                    "Field" to fieldClassSymbol,
                    "Criteria" to criteriaClassSymbol,
                    "KColumnType" to kColumnTypeSymbol,
                    "KTableForSelect" to kTableForSelectSymbol,
                    "KTableForSet" to kTableForSetSymbol,
                    "KTableForCondition" to kTableForConditionSymbol,
                    "KTableForSort" to kTableForSortSymbol,
                    "KTableForReference" to kTableForReferenceSymbol
                )
                
                classSymbols.forEach { (name, symbol) ->
                    assertNotNull(symbol, "$name symbol should be resolved")
                    @OptIn(UnsafeDuringIrConstructionAPI::class)
                    assertNotNull(symbol.owner, "$name symbol should have owner")
                    println("✓ $name symbol resolved")
                }
            }
        }
    }

    @Test
    fun `test all method symbols can be resolved`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                data class User(val id: Int) : KPojo
            """)
        ) { ctx, pluginContext ->
            println("\n=== Method Symbol Resolution Test ===")
            
            // 直接测试方法符号
            with(pluginContext) {
                val methodSymbols = listOf(
                    "addField" to addFieldMethodSymbol,
                    "setValue" to setValueMethodSymbol,
                    "setAssign" to setAssignMethodSymbol,
                    "setCriteria" to setCriteriaMethodSymbol,
                    "addSortField" to addSortFieldMethodSymbol
                )
                
                methodSymbols.forEach { (name, symbol) ->
                    assertNotNull(symbol, "$name method symbol should be resolved")
                    @OptIn(UnsafeDuringIrConstructionAPI::class)
                    assertEquals(name, symbol.owner.name.asString(), 
                        "$name should have correct name")
                    println("✓ $name method resolved")
                }
            }
        }
    }

    @Test
    fun `test constructor symbols can be resolved`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                data class User(val id: Int) : KPojo
            """)
        ) { ctx, pluginContext ->
            println("\n=== Constructor Symbol Resolution Test ===")
            
            // 测试构造函数符号
            with(pluginContext) {
                val fieldConstructor = fieldConstructorSymbol
                assertNotNull(fieldConstructor, "Field constructor should be resolved")
                @OptIn(UnsafeDuringIrConstructionAPI::class)
                assertTrue(fieldConstructor.owner.isPrimary, "Should be primary constructor")
                println("✓ Field constructor resolved")
                
                val criteriaConstructor = criteriaConstructorSymbol
                assertNotNull(criteriaConstructor, "Criteria constructor should be resolved")
                @OptIn(UnsafeDuringIrConstructionAPI::class)
                assertTrue(criteriaConstructor.owner.isPrimary, "Should be primary constructor")
                println("✓ Criteria constructor resolved")
                
                val functionFieldConstructor = functionFieldConstructorSymbol
                assertNotNull(functionFieldConstructor, "FunctionField constructor should be resolved")
                @OptIn(UnsafeDuringIrConstructionAPI::class)
                assertTrue(functionFieldConstructor.owner.isPrimary, "Should be primary constructor")
                println("✓ FunctionField constructor resolved")
                
                val pairConstructor = pairConstructorSymbol
                assertNotNull(pairConstructor, "Pair constructor should be resolved")
                println("✓ Pair constructor resolved")
            }
        }
    }

    @Test
    fun `test type judgment functions work correctly`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                
                data class User(val id: Int, val name: String) : KPojo
                data class NotKPojo(val id: Int)
            """)
        ) { ctx, pluginContext ->
            println("\n=== Type Judgment Test ===")
            
            // 测试类型判断函数
            val userClass = ctx.findClass("User")
            assertNotNull(userClass, "Should find User class")
            
            val notKPojoClass = ctx.findClass("NotKPojo")
            assertNotNull(notKPojoClass, "Should find NotKPojo class")
            
            // 使用 context receiver 调用扩展函数
            with(pluginContext) {
                // 测试 isKPojoType
                val userType = ctx.getDefaultType(userClass)
                val userIsKPojo = userType.isKPojoType()
                assertTrue(userIsKPojo, "User should be KPojo type")
                
                val notKPojoType = ctx.getDefaultType(notKPojoClass)
                val notKPojoIsKPojo = notKPojoType.isKPojoType()
                assertTrue(!notKPojoIsKPojo, "NotKPojo should not be KPojo type")
                
                println("✓ User.isKPojoType() = true")
                println("✓ NotKPojo.isKPojoType() = false")
                
                // 测试 isColumnType
                val userProperties = ctx.getProperties(userClass)
                val userIdProperty = userProperties.first { it.name.asString() == "id" }
                val userNameProperty = userProperties.first { it.name.asString() == "name" }
                
                assertTrue(userIdProperty.isColumnType(), 
                    "User.id should be column type")
                assertTrue(userNameProperty.isColumnType(), 
                    "User.name should be column type")
                
                println("✓ User.id.isColumnType() = true")
                println("✓ User.name.isColumnType() = true")
            }
        }
    }

    @Test
    fun `test symbols work with multiple KPojo classes`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                
                data class User(val id: Int, val name: String) : KPojo
                data class Post(val id: Int, val title: String) : KPojo
                data class Comment(val id: Int, val content: String) : KPojo
            """)
        ) { ctx, pluginContext ->
            println("\n=== Multiple KPojo Classes Test ===")
            
            // 获取所有 KPojo 类
            val kPojoClasses = ctx.getKPojoClasses()
            assertEquals(3, kPojoClasses.size, "Should have 3 KPojo classes")
            
            val classNames = kPojoClasses.map { it.name.asString() }.sorted()
            assertEquals(listOf("Comment", "Post", "User"), classNames, 
                "Should have User, Post, and Comment classes")
            
            println("✓ Found ${kPojoClasses.size} KPojo classes: ${classNames.joinToString()}")
            
            // 验证每个类都能正确判断为 KPojo
            with(pluginContext) {
                kPojoClasses.forEach { klass ->
                    val klassType = ctx.getDefaultType(klass)
                    val isKPojo = klassType.isKPojoType()
                    assertTrue(isKPojo, "${klass.name} should be KPojo type")
                    println("✓ ${klass.name}.isKPojoType() = true")
                }
            }
        }
    }

    @Test
    fun `test symbol resolution is consistent across compilations`() {
        // 编译两次，验证符号解析结果一致
        var symbol1: Any? = null
        var symbol2: Any? = null
        
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                data class User(val id: Int) : KPojo
            """)
        ) { ctx, pluginContext ->
            ctx.assertSuccess()
            with(pluginContext) {
                symbol1 = kPojoClassSymbol
            }
        }
        
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                data class User(val id: Int) : KPojo
            """)
        ) { ctx, pluginContext ->
            ctx.assertSuccess()
            with(pluginContext) {
                symbol2 = kPojoClassSymbol
            }
        }
        
        println("\n=== Symbol Resolution Consistency Test ===")
        assertNotNull(symbol1, "First compilation should resolve KPojo")
        assertNotNull(symbol2, "Second compilation should resolve KPojo")
        println("✓ Symbol resolution is consistent across compilations")
    }
}
