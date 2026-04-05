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
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for AnnotationUtils.kt - Annotation Reading Functions
 *
 * Tests the annotation reading helper functions that extract values from
 * Kronos annotations on properties and classes
 */
@OptIn(ExperimentalCompilerApi::class)
class AnnotationUtilsTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `test Column annotation value extraction`() {
        val debugPath = File(tempDir, "test_column/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.Column
                
                data class User(
                    @Column("user_id")
                    val id: Int,
                    @Column("user_name")
                    val name: String
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_column").absolutePath
        )

        result.assertSuccess()
        
        // Verify Column annotation was found
        val idColumnCheck = result.findAnnotationCheck("User.id", "Column")
        assertNotNull(idColumnCheck, "Should find @Column annotation on User.id")
        assertTrue(idColumnCheck.found, "@Column annotation should be found on User.id")
        
        val nameColumnCheck = result.findAnnotationCheck("User.name", "Column")
        assertNotNull(nameColumnCheck, "Should find @Column annotation on User.name")
        assertTrue(nameColumnCheck.found, "@Column annotation should be found on User.name")
        
        // Verify column names were extracted
        val idColumnName = result.findInfoMessage("Column name for User.id: user_id")
        assertNotNull(idColumnName, "Should extract column name 'user_id' from @Column annotation")
        
        val nameColumnName = result.findInfoMessage("Column name for User.name: user_name")
        assertNotNull(nameColumnName, "Should extract column name 'user_name' from @Column annotation")
    }

    @Test
    fun `test PrimaryKey annotation with identity parameter`() {
        val debugPath = File(tempDir, "test_primary_key/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.PrimaryKey
                
                data class User(
                    @PrimaryKey(identity = true)
                    val id: Int,
                    val name: String
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_primary_key").absolutePath
        )

        result.assertSuccess()
        
        // Verify PrimaryKey annotation with identity=true was found
        val pkCheck = result.findAnnotationCheck("User.id", "PrimaryKey(identity=true)")
        assertNotNull(pkCheck, "Should find @PrimaryKey(identity=true) annotation on User.id")
        assertTrue(pkCheck.found, "@PrimaryKey(identity=true) should be found on User.id")
    }

    @Test
    fun `test Ignore annotation detection`() {
        val debugPath = File(tempDir, "test_ignore/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.Ignore
                
                data class User(
                    val id: Int,
                    @Ignore
                    val password: String
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_ignore").absolutePath
        )

        result.assertSuccess()
        
        // Verify Ignore annotation was found on password
        val passwordIgnoreCheck = result.findAnnotationCheck("User.password", "Ignore")
        assertNotNull(passwordIgnoreCheck, "Should find @Ignore annotation on User.password")
        assertTrue(passwordIgnoreCheck.found, "@Ignore annotation should be found on User.password")
        
        // Verify Ignore annotation was NOT found on id
        val idIgnoreCheck = result.findAnnotationCheck("User.id", "Ignore")
        assertNotNull(idIgnoreCheck, "Should check @Ignore annotation on User.id")
        assertTrue(!idIgnoreCheck.found, "@Ignore annotation should NOT be found on User.id")
    }

    @Test
    fun `test DateTimeFormat annotation value extraction`() {
        val debugPath = File(tempDir, "test_datetime_format/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.DateTimeFormat
                import java.time.LocalDateTime
                
                data class Event(
                    val id: Int,
                    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
                    val createdAt: LocalDateTime
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_datetime_format").absolutePath
        )

        result.assertSuccess()
        
        // Verify DateTimeFormat annotation was found
        val formatCheck = result.findAnnotationCheck("Event.createdAt", "DateTimeFormat")
        assertNotNull(formatCheck, "Should find @DateTimeFormat annotation on Event.createdAt")
        assertTrue(formatCheck.found, "@DateTimeFormat annotation should be found on Event.createdAt")
        
        // Verify format string was extracted
        val formatValue = result.findInfoMessage("DateTime format for Event.createdAt: yyyy-MM-dd HH:mm:ss")
        assertNotNull(formatValue, "Should extract format string 'yyyy-MM-dd HH:mm:ss' from @DateTimeFormat annotation")
    }

    @Test
    fun `test Default annotation value extraction`() {
        val debugPath = File(tempDir, "test_default/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.Default
                
                data class User(
                    val id: Int,
                    @Default("active")
                    val status: String
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_default").absolutePath
        )

        result.assertSuccess()
        
        // Verify Default annotation was found
        val defaultCheck = result.findAnnotationCheck("User.status", "Default")
        assertNotNull(defaultCheck, "Should find @Default annotation on User.status")
        assertTrue(defaultCheck.found, "@Default annotation should be found on User.status")
        
        // Verify default value was extracted
        val defaultValue = result.findInfoMessage("Default value for User.status: active")
        assertNotNull(defaultValue, "Should extract default value 'active' from @Default annotation")
    }

    @Test
    fun `test Necessary annotation detection`() {
        val debugPath = File(tempDir, "test_necessary/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.Necessary
                
                data class User(
                    val id: Int,
                    @Necessary
                    val email: String
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_necessary").absolutePath
        )

        result.assertSuccess()
        
        // Verify Necessary annotation was found on email
        val emailNecessaryCheck = result.findAnnotationCheck("User.email", "Necessary")
        assertNotNull(emailNecessaryCheck, "Should find @Necessary annotation on User.email")
        assertTrue(emailNecessaryCheck.found, "@Necessary annotation should be found on User.email")
        
        // Verify Necessary annotation was NOT found on id
        val idNecessaryCheck = result.findAnnotationCheck("User.id", "Necessary")
        assertNotNull(idNecessaryCheck, "Should check @Necessary annotation on User.id")
        assertTrue(!idNecessaryCheck.found, "@Necessary annotation should NOT be found on User.id")
    }

    @Test
    fun `test Table annotation on class`() {
        val debugPath = File(tempDir, "test_table/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.Table
                
                @Table("users")
                data class User(
                    val id: Int,
                    val name: String
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_table").absolutePath
        )

        result.assertSuccess()
        
        // Verify Table annotation was found
        val tableCheck = result.findAnnotationCheck("User", "Table")
        assertNotNull(tableCheck, "Should find @Table annotation on User class")
        assertTrue(tableCheck.found, "@Table annotation should be found on User class")
        
        // Verify table name was extracted
        val tableName = result.findInfoMessage("Table name for User: users")
        assertNotNull(tableName, "Should extract table name 'users' from @Table annotation")
    }

    @Test
    fun `test multiple annotations on same property`() {
        val debugPath = File(tempDir, "test_multiple/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import com.kotlinorm.annotations.*
                import com.kotlinorm.enums.KColumnType
                
                data class User(
                    @Column("user_name")
                    @Necessary
                    val name: String
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_multiple").absolutePath
        )

        result.assertSuccess()
        
        // Verify both annotations were found
        val columnCheck = result.findAnnotationCheck("User.name", "Column")
        assertNotNull(columnCheck, "Should find @Column annotation on User.name")
        assertTrue(columnCheck.found, "@Column annotation should be found on User.name")
        
        val necessaryCheck = result.findAnnotationCheck("User.name", "Necessary")
        assertNotNull(necessaryCheck, "Should find @Necessary annotation on User.name")
        assertTrue(necessaryCheck.found, "@Necessary annotation should be found on User.name")
        
        // Verify column name was extracted
        val columnName = result.findInfoMessage("Column name for User.name: user_name")
        assertNotNull(columnName, "Should extract column name 'user_name' from @Column annotation")
    }

    @Test
    fun `test properties without annotations`() {
        val debugPath = File(tempDir, "test_no_annotations/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                
                data class User(
                    val id: Int,
                    val name: String
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_no_annotations").absolutePath
        )

        result.assertSuccess()
        
        // Verify annotations were checked but not found
        val idColumnCheck = result.findAnnotationCheck("User.id", "Column")
        assertNotNull(idColumnCheck, "Should check @Column annotation on User.id")
        assertTrue(!idColumnCheck.found, "@Column annotation should NOT be found on User.id")
        
        val nameColumnCheck = result.findAnnotationCheck("User.name", "Column")
        assertNotNull(nameColumnCheck, "Should check @Column annotation on User.name")
        assertTrue(!nameColumnCheck.found, "@Column annotation should NOT be found on User.name")
    }
}
