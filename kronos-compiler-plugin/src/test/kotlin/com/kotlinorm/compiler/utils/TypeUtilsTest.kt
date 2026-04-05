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

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for TypeUtils.kt - Type Mapping Functions
 *
 * Tests the type mapping functions that convert Kotlin type names to KColumnType enum values.
 * These are pure functions that don't require IR compilation.
 */
class TypeUtilsTest {

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
}
