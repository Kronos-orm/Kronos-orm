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

package com.kotlinorm.compiler.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class TypeUtilsStringMappingTest {
    @Test
    fun mapsPrimitiveAndCommonJvmTypes() {
        val mappings = mapOf(
            "kotlin.Boolean" to "BIT",
            "kotlin.Byte" to "TINYINT",
            "kotlin.Short" to "SMALLINT",
            "kotlin.Int" to "INT",
            "kotlin.Long" to "BIGINT",
            "kotlin.Float" to "FLOAT",
            "kotlin.Double" to "DOUBLE",
            "java.math.BigDecimal" to "DECIMAL",
            "kotlin.Char" to "CHAR",
            "kotlin.String" to "VARCHAR",
            "kotlin.ByteArray" to "BLOB",
        )

        mappings.forEach { (typeName, columnType) ->
            assertEquals(columnType, mapTypeNameToKColumnType(typeName), typeName)
        }
    }

    @Test
    fun mapsTemporalTypes() {
        val mappings = mapOf(
            "java.util.Date" to "DATE",
            "java.sql.Date" to "DATE",
            "java.time.LocalDate" to "DATE",
            "kotlinx.datetime.LocalDate" to "DATE",
            "java.time.LocalTime" to "TIME",
            "kotlinx.datetime.LocalTime" to "TIME",
            "java.time.LocalDateTime" to "DATETIME",
            "kotlinx.datetime.LocalDateTime" to "DATETIME",
            "java.sql.Timestamp" to "TIMESTAMP",
        )

        mappings.forEach { (typeName, columnType) ->
            assertEquals(columnType, mapTypeNameToKColumnType(typeName), typeName)
        }
    }

    @Test
    fun mapsCustomSqlAndUnknownFallback() {
        assertEquals("CUSTOM_CRITERIA_SQL", mapTypeNameToKColumnType("CUSTOM_CRITERIA_SQL"))
        assertEquals("VARCHAR", mapTypeNameToKColumnType("com.example.Unknown"))
    }
}
