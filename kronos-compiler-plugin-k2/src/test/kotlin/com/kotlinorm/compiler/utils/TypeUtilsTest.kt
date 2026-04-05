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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for TypeUtils.kt - Type Mapping and Type Checking Functions
 *
 * Tests the type mapping functions that convert Kotlin types to KColumnType enum values
 * and various type checking utility functions
 */
@OptIn(ExperimentalCompilerApi::class)
class TypeUtilsTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `test mapTypeNameToKColumnType for primitive types`() {
        // Boolean
        assertEquals("BIT", mapTypeNameToKColumnType("kotlin.Boolean"))
        
        // Integer types
        assertEquals("TINYINT", mapTypeNameToKColumnType("kotlin.Byte"))
        assertEquals("SMALLINT", mapTypeNameToKColumnType("kotlin.Short"))
        assertEquals("INT", mapTypeNameToKColumnType("kotlin.Int"))
        assertEquals("BIGINT", mapTypeNameToKColumnType("kotlin.Long"))
        
        // Floating point types
        assertEquals("FLOAT", mapTypeNameToKColumnType("kotlin.Float"))
        assertEquals("DOUBLE", mapTypeNameToKColumnType("kotlin.Double"))
        
        // Decimal
        assertEquals("DECIMAL", mapTypeNameToKColumnType("java.math.BigDecimal"))
        
        // Character types
        assertEquals("CHAR", mapTypeNameToKColumnType("kotlin.Char"))
        assertEquals("VARCHAR", mapTypeNameToKColumnType("kotlin.String"))
    }

    @Test
    fun `test mapTypeNameToKColumnType for date and time types`() {
        // Date types
        assertEquals("DATE", mapTypeNameToKColumnType("java.util.Date"))
        assertEquals("DATE", mapTypeNameToKColumnType("java.sql.Date"))
        assertEquals("DATE", mapTypeNameToKColumnType("java.time.LocalDate"))
        assertEquals("DATE", mapTypeNameToKColumnType("kotlinx.datetime.LocalDate"))
        
        // Time types
        assertEquals("TIME", mapTypeNameToKColumnType("java.time.LocalTime"))
        assertEquals("TIME", mapTypeNameToKColumnType("kotlinx.datetime.LocalTime"))
        
        // DateTime types
        assertEquals("DATETIME", mapTypeNameToKColumnType("java.time.LocalDateTime"))
        assertEquals("DATETIME", mapTypeNameToKColumnType("kotlinx.datetime.LocalDateTime"))
        
        // Timestamp
        assertEquals("TIMESTAMP", mapTypeNameToKColumnType("java.sql.Timestamp"))
    }

    @Test
    fun `test mapTypeNameToKColumnType for binary types`() {
        assertEquals("BLOB", mapTypeNameToKColumnType("kotlin.ByteArray"))
    }

    @Test
    fun `test mapTypeNameToKColumnType for special types`() {
        assertEquals("CUSTOM_CRITERIA_SQL", mapTypeNameToKColumnType("CUSTOM_CRITERIA_SQL"))
    }

    @Test
    fun `test mapTypeNameToKColumnType for unknown types defaults to VARCHAR`() {
        assertEquals("VARCHAR", mapTypeNameToKColumnType("com.example.CustomType"))
        assertEquals("VARCHAR", mapTypeNameToKColumnType("kotlin.collections.List"))
        assertEquals("VARCHAR", mapTypeNameToKColumnType(""))
        assertEquals("VARCHAR", mapTypeNameToKColumnType("unknown.Type"))
    }

    @Test
    fun `test mapTypeToKColumnType with IrType for Int`() {
        val debugPath = File(tempDir, "test_int/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                
                data class User(
                    val id: Int,
                    val age: Int
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_int").absolutePath
        )

        result.assertSuccess()
        
        // Verify Int type was mapped to INT
        val idTypeMapping = result.findTypeJudgment("Int", "mapTypeToKColumnType")
        assertNotNull(idTypeMapping, "Should find type mapping for Int")
        assertTrue(idTypeMapping.result, "Type mapping should succeed")
        assertTrue(idTypeMapping.reason.contains("INT"), "Int should map to INT, got: ${idTypeMapping.reason}")
    }

    @Test
    fun `test mapTypeToKColumnType with IrType for String`() {
        val debugPath = File(tempDir, "test_string/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                
                data class User(
                    val name: String,
                    val email: String
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_string").absolutePath
        )

        result.assertSuccess()
        
        // Verify String type was mapped to VARCHAR
        val nameTypeMapping = result.findTypeJudgment("String", "mapTypeToKColumnType")
        assertNotNull(nameTypeMapping, "Should find type mapping for String")
        assertTrue(nameTypeMapping.result, "Type mapping should succeed")
        assertTrue(nameTypeMapping.reason.contains("VARCHAR"), "String should map to VARCHAR, got: ${nameTypeMapping.reason}")
    }

    @Test
    fun `test type checking functions for numeric types`() {
        val debugPath = File(tempDir, "test_numeric/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import java.math.BigDecimal
                
                data class Product(
                    val quantity: Int,
                    val price: Double,
                    val discount: Float,
                    val total: BigDecimal
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_numeric").absolutePath
        )

        result.assertSuccess()
        
        // Verify numeric type checks
        val intNumericCheck = result.findTypeJudgment("Int", "isNumericType")
        assertNotNull(intNumericCheck, "Should check if Int is numeric")
        assertTrue(intNumericCheck.result, "Int should be numeric")
        
        val doubleNumericCheck = result.findTypeJudgment("Double", "isNumericType")
        assertNotNull(doubleNumericCheck, "Should check if Double is numeric")
        assertTrue(doubleNumericCheck.result, "Double should be numeric")
        
        val floatNumericCheck = result.findTypeJudgment("Float", "isNumericType")
        assertNotNull(floatNumericCheck, "Should check if Float is numeric")
        assertTrue(floatNumericCheck.result, "Float should be numeric")
        
        val bigDecimalNumericCheck = result.findTypeJudgment("BigDecimal", "isNumericType")
        assertNotNull(bigDecimalNumericCheck, "Should check if BigDecimal is numeric")
        assertTrue(bigDecimalNumericCheck.result, "BigDecimal should be numeric")
    }

    @Test
    fun `test type checking functions for temporal types`() {
        val debugPath = File(tempDir, "test_temporal/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import java.time.LocalDate
                import java.time.LocalDateTime
                
                data class Event(
                    val date: LocalDate,
                    val timestamp: LocalDateTime
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_temporal").absolutePath
        )

        result.assertSuccess()
        
        // Verify temporal type checks
        val dateTemporalCheck = result.findTypeJudgment("LocalDate", "isTemporalType")
        assertNotNull(dateTemporalCheck, "Should check if LocalDate is temporal")
        assertTrue(dateTemporalCheck.result, "LocalDate should be temporal")
        
        val dateTimeTemporalCheck = result.findTypeJudgment("LocalDateTime", "isTemporalType")
        assertNotNull(dateTimeTemporalCheck, "Should check if LocalDateTime is temporal")
        assertTrue(dateTimeTemporalCheck.result, "LocalDateTime should be temporal")
    }

    @Test
    fun `test type checking functions for string types`() {
        val debugPath = File(tempDir, "test_string_check/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                
                data class User(
                    val name: String,
                    val initial: Char
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_string_check").absolutePath
        )

        result.assertSuccess()
        
        // Verify string type checks
        val stringCheck = result.findTypeJudgment("String", "isStringType")
        assertNotNull(stringCheck, "Should check if String is string type")
        assertTrue(stringCheck.result, "String should be string type")
        
        val charCheck = result.findTypeJudgment("Char", "isStringType")
        assertNotNull(charCheck, "Should check if Char is string type")
        assertTrue(charCheck.result, "Char should be string type")
    }

    @Test
    fun `test type checking functions for binary types`() {
        val debugPath = File(tempDir, "test_binary/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                
                data class Document(
                    val content: ByteArray
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_binary").absolutePath
        )

        result.assertSuccess()
        
        // Verify binary type check
        val binaryCheck = result.findTypeJudgment("ByteArray", "isBinaryType")
        assertNotNull(binaryCheck, "Should check if ByteArray is binary type")
        assertTrue(binaryCheck.result, "ByteArray should be binary type")
    }

    @Test
    fun `test type checking functions return false for non-matching types`() {
        val debugPath = File(tempDir, "test_non_matching/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                
                data class User(
                    val name: String
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_non_matching").absolutePath
        )

        result.assertSuccess()
        
        // Verify String is not numeric
        val stringNumericCheck = result.findTypeJudgment("String", "isNumericType")
        assertNotNull(stringNumericCheck, "Should check if String is numeric")
        assertFalse(stringNumericCheck.result, "String should NOT be numeric")
        
        // Verify String is not temporal
        val stringTemporalCheck = result.findTypeJudgment("String", "isTemporalType")
        assertNotNull(stringTemporalCheck, "Should check if String is temporal")
        assertFalse(stringTemporalCheck.result, "String should NOT be temporal")
        
        // Verify String is not binary
        val stringBinaryCheck = result.findTypeJudgment("String", "isBinaryType")
        assertNotNull(stringBinaryCheck, "Should check if String is binary")
        assertFalse(stringBinaryCheck.result, "String should NOT be binary")
    }

    @Test
    fun `test mapTypeToKColumnType with mixed types`() {
        val debugPath = File(tempDir, "test_mixed/debug.json").absolutePath
        
        val result = IRVerificationUtils.compileWithCustomExtension(
            IRVerificationUtils.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                import java.time.LocalDateTime
                import java.math.BigDecimal
                
                data class ComplexEntity(
                    val id: Long,
                    val name: String,
                    val active: Boolean,
                    val score: Double,
                    val amount: BigDecimal,
                    val createdAt: LocalDateTime,
                    val data: ByteArray
                ) : KPojo
            """),
            extension = UtilsVerificationExtension(debugPath),
            dumpIrPath = File(tempDir, "test_mixed").absolutePath
        )

        result.assertSuccess()
        
        // Verify all type mappings
        val longMapping = result.findTypeJudgment("Long", "mapTypeToKColumnType")
        assertNotNull(longMapping, "Should map Long type")
        assertTrue(longMapping.reason.contains("BIGINT"), "Long should map to BIGINT")
        
        val stringMapping = result.findTypeJudgment("String", "mapTypeToKColumnType")
        assertNotNull(stringMapping, "Should map String type")
        assertTrue(stringMapping.reason.contains("VARCHAR"), "String should map to VARCHAR")
        
        val booleanMapping = result.findTypeJudgment("Boolean", "mapTypeToKColumnType")
        assertNotNull(booleanMapping, "Should map Boolean type")
        assertTrue(booleanMapping.reason.contains("BIT"), "Boolean should map to BIT")
        
        val doubleMapping = result.findTypeJudgment("Double", "mapTypeToKColumnType")
        assertNotNull(doubleMapping, "Should map Double type")
        assertTrue(doubleMapping.reason.contains("DOUBLE"), "Double should map to DOUBLE")
        
        val bigDecimalMapping = result.findTypeJudgment("BigDecimal", "mapTypeToKColumnType")
        assertNotNull(bigDecimalMapping, "Should map BigDecimal type")
        assertTrue(bigDecimalMapping.reason.contains("DECIMAL"), "BigDecimal should map to DECIMAL")
        
        val localDateTimeMapping = result.findTypeJudgment("LocalDateTime", "mapTypeToKColumnType")
        assertNotNull(localDateTimeMapping, "Should map LocalDateTime type")
        assertTrue(localDateTimeMapping.reason.contains("DATETIME"), "LocalDateTime should map to DATETIME")
        
        val byteArrayMapping = result.findTypeJudgment("ByteArray", "mapTypeToKColumnType")
        assertNotNull(byteArrayMapping, "Should map ByteArray type")
        assertTrue(byteArrayMapping.reason.contains("BLOB"), "ByteArray should map to BLOB")
    }
}
