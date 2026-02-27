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

package com.kotlinorm.compiler.utils

import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AnnotationUtils.kt - Annotation Reading Functions
 *
 * Tests the annotation reading helper functions that extract values from
 * Kronos annotations on properties and classes using the new IrTestFramework
 */
@OptIn(ExperimentalCompilerApi::class)
class AnnotationUtilsTest {

    @Test
    fun `test Column annotation value extraction`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.Column
                
                data class User(
                    @Column("user_id")
                    val id: Int,
                    @Column("user_name")
                    val name: String
                ) : KPojo
            """)
        ) { ctx, pluginContext ->
            ctx.assertSuccess()
            
            val userClass = ctx.findClass("User")
            assertNotNull(userClass, "Should find User class")
            
            val properties = ctx.getProperties(userClass)
            val idProperty = properties.find { it.name.asString() == "id" }
            val nameProperty = properties.find { it.name.asString() == "name" }
            
            assertNotNull(idProperty, "Should find id property")
            assertNotNull(nameProperty, "Should find name property")
            
            // Test getColumnName
            val idColumnName = idProperty.getColumnName()
            assertEquals("user_id", idColumnName, "Should extract column name 'user_id' from @Column annotation")
            
            val nameColumnName = nameProperty.getColumnName()
            assertEquals("user_name", nameColumnName, "Should extract column name 'user_name' from @Column annotation")
        }
    }

    @Test
    fun `test PrimaryKey annotation with identity parameter`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.PrimaryKey
                
                data class User(
                    @PrimaryKey(identity = true)
                    val id: Int,
                    val name: String
                ) : KPojo
            """)
        ) { ctx, pluginContext ->
            ctx.assertSuccess()
            
            val userClass = ctx.findClass("User")
            assertNotNull(userClass, "Should find User class")
            
            val properties = ctx.getProperties(userClass)
            val idProperty = properties.find { it.name.asString() == "id" }
            
            assertNotNull(idProperty, "Should find id property")
            
            // Test isIdentityPrimaryKey
            assertTrue(idProperty.isIdentityPrimaryKey(), "Should find @PrimaryKey(identity=true) on id")
        }
    }

    @Test
    fun `test Ignore annotation detection`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.Ignore
                
                data class User(
                    val id: Int,
                    @Ignore
                    val password: String
                ) : KPojo
            """)
        ) { ctx, pluginContext ->
            ctx.assertSuccess()
            
            val userClass = ctx.findClass("User")
            assertNotNull(userClass, "Should find User class")
            
            val properties = ctx.getProperties(userClass)
            val idProperty = properties.find { it.name.asString() == "id" }
            val passwordProperty = properties.find { it.name.asString() == "password" }
            
            assertNotNull(idProperty, "Should find id property")
            assertNotNull(passwordProperty, "Should find password property")
            
            // Test shouldIgnore
            assertTrue(!idProperty.shouldIgnore(), "@Ignore should NOT be found on id")
            assertTrue(passwordProperty.shouldIgnore(), "@Ignore should be found on password")
        }
    }

    @Test
    fun `test DateTimeFormat annotation value extraction`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.DateTimeFormat
                import java.time.LocalDateTime
                
                data class Event(
                    val id: Int,
                    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
                    val createdAt: LocalDateTime
                ) : KPojo
            """)
        ) { ctx, pluginContext ->
            ctx.assertSuccess()
            
            val eventClass = ctx.findClass("Event")
            assertNotNull(eventClass, "Should find Event class")
            
            val properties = ctx.getProperties(eventClass)
            val createdAtProperty = properties.find { it.name.asString() == "createdAt" }
            
            assertNotNull(createdAtProperty, "Should find createdAt property")
            
            // Test getDateTimeFormat
            val format = createdAtProperty.getDateTimeFormat()
            assertEquals("yyyy-MM-dd HH:mm:ss", format, "Should extract format string from @DateTimeFormat annotation")
        }
    }

    @Test
    fun `test Default annotation value extraction`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.Default
                
                data class User(
                    val id: Int,
                    @Default("active")
                    val status: String
                ) : KPojo
            """)
        ) { ctx, pluginContext ->
            ctx.assertSuccess()
            
            val userClass = ctx.findClass("User")
            assertNotNull(userClass, "Should find User class")
            
            val properties = ctx.getProperties(userClass)
            val statusProperty = properties.find { it.name.asString() == "status" }
            
            assertNotNull(statusProperty, "Should find status property")
            
            // Test getDefaultValue
            val defaultValue = statusProperty.getDefaultValue()
            assertEquals("active", defaultValue, "Should extract default value from @Default annotation")
        }
    }

    @Test
    fun `test Necessary annotation detection`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.Necessary
                
                data class User(
                    val id: Int,
                    @Necessary
                    val email: String
                ) : KPojo
            """)
        ) { ctx, pluginContext ->
            ctx.assertSuccess()
            
            val userClass = ctx.findClass("User")
            assertNotNull(userClass, "Should find User class")
            
            val properties = ctx.getProperties(userClass)
            val idProperty = properties.find { it.name.asString() == "id" }
            val emailProperty = properties.find { it.name.asString() == "email" }
            
            assertNotNull(idProperty, "Should find id property")
            assertNotNull(emailProperty, "Should find email property")
            
            // Test isNecessary
            assertTrue(!idProperty.isNecessary(), "@Necessary should NOT be found on id")
            assertTrue(emailProperty.isNecessary(), "@Necessary should be found on email")
        }
    }

    @Test
    fun `test Table annotation on class`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.Table
                
                @Table("users")
                data class User(
                    val id: Int,
                    val name: String
                ) : KPojo
            """)
        ) { ctx, pluginContext ->
            ctx.assertSuccess()
            
            val userClass = ctx.findClass("User")
            assertNotNull(userClass, "Should find User class")
            
            // Test getTableName
            val tableName = userClass.getTableName()
            assertEquals("users", tableName, "Should extract table name from @Table annotation")
        }
    }

    @Test
    fun `test multiple annotations on same property`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.*
                
                data class User(
                    @Column("user_name")
                    @Necessary
                    val name: String
                ) : KPojo
            """)
        ) { ctx, pluginContext ->
            ctx.assertSuccess()
            
            val userClass = ctx.findClass("User")
            assertNotNull(userClass, "Should find User class")
            
            val properties = ctx.getProperties(userClass)
            val nameProperty = properties.find { it.name.asString() == "name" }
            
            assertNotNull(nameProperty, "Should find name property")
            
            // Test both annotations
            val columnName = nameProperty.getColumnName()
            assertEquals("user_name", columnName, "Should extract column name from @Column annotation")
            
            assertTrue(nameProperty.isNecessary(), "@Necessary should be found on name")
        }
    }

    @Test
    fun `test properties without annotations`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                
                data class User(
                    val id: Int,
                    val name: String
                ) : KPojo
            """)
        ) { ctx, pluginContext ->
            ctx.assertSuccess()
            
            val userClass = ctx.findClass("User")
            assertNotNull(userClass, "Should find User class")
            
            val properties = ctx.getProperties(userClass)
            val idProperty = properties.find { it.name.asString() == "id" }
            val nameProperty = properties.find { it.name.asString() == "name" }
            
            assertNotNull(idProperty, "Should find id property")
            assertNotNull(nameProperty, "Should find name property")
            
            // Test that annotations are not found
            assertNull(idProperty.getColumnName(), "@Column should NOT be found on id")
            assertNull(nameProperty.getColumnName(), "@Column should NOT be found on name")
            assertTrue(!idProperty.shouldIgnore(), "@Ignore should NOT be found on id")
            assertTrue(!nameProperty.isNecessary(), "@Necessary should NOT be found on name")
        }
    }
}
