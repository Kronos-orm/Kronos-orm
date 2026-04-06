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

package com.kotlinorm.beans.dsl

import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PrimaryKeyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FieldTest {

    @Test
    fun testFieldCreation() {
        val field = Field(
            columnName = "user_name",
            name = "userName",
            type = KColumnType.VARCHAR,
            primaryKey = PrimaryKeyType.NOT,
            nullable = true
        )
        
        assertEquals("user_name", field.columnName)
        assertEquals("userName", field.name)
        assertEquals(KColumnType.VARCHAR, field.type)
        assertEquals(PrimaryKeyType.NOT, field.primaryKey)
        assertTrue(field.nullable)
    }

    @Test
    fun testFieldToString() {
        val field = Field(columnName = "test_field", name = "testField")
        assertEquals("testField", field.toString())
    }

    @Test
    fun testFieldEquals() {
        val field1 = Field(columnName = "id", name = "id", tableName = "users")
        val field2 = Field(columnName = "id", name = "id", tableName = "users")
        val field3 = Field(columnName = "id", name = "id", tableName = "orders")
        
        assertEquals(field1, field2)
        assertNotEquals<Field?>(field1, field3)
    }

    @Test
    fun testFieldHashCode() {
        val field1 = Field(columnName = "id", name = "id", tableName = "users")
        val field2 = Field(columnName = "id", name = "id", tableName = "users")
        
        assertEquals(field1.hashCode(), field2.hashCode())
    }

    @Test
    fun testFieldPlusOperator() {
        val field = Field(columnName = "name", name = "name")
        val newField = field + "_suffix"
        
        assertEquals("name_suffix", newField.name)
        assertEquals("name", newField.columnName)
    }

    @Test
    fun testFieldRefUseFor() {
        val cascade = KCascade(usage = arrayOf(KOperationType.SELECT, KOperationType.INSERT))
        val field = Field(columnName = "items", cascade = cascade)
        
        assertTrue(field.refUseFor(KOperationType.SELECT))
        assertTrue(field.refUseFor(KOperationType.INSERT))
        assertFalse(field.refUseFor(KOperationType.DELETE))
    }

    @Test
    fun testFieldRefUseForNoCascade() {
        val field = Field(columnName = "name")
        
        assertFalse(field.refUseFor(KOperationType.SELECT))
    }

    @Test
    fun testFieldCopy() {
        val field = Field(
            columnName = "id",
            name = "id",
            type = KColumnType.INT,
            primaryKey = PrimaryKeyType.IDENTITY,
            nullable = false
        )
        
        val copied = field.copy(name = "userId")
        
        assertEquals("id", copied.columnName)
        assertEquals("userId", copied.name)
        assertEquals(KColumnType.INT, copied.type)
        assertEquals(PrimaryKeyType.IDENTITY, copied.primaryKey)
        assertFalse(copied.nullable)
    }

    @Test
    fun testFieldWithAllProperties() {
        val field = Field(
            columnName = "email",
            name = "email",
            type = KColumnType.VARCHAR,
            primaryKey = PrimaryKeyType.NOT,
            dateFormat = "yyyy-MM-dd",
            tableName = "users",
            length = 255,
            scale = 0,
            defaultValue = "test@example.com",
            nullable = false,
            serializable = true
        )
        
        assertEquals("email", field.columnName)
        assertEquals("email", field.name)
        assertEquals(KColumnType.VARCHAR, field.type)
        assertEquals("yyyy-MM-dd", field.dateFormat)
        assertEquals("users", field.tableName)
        assertEquals(255, field.length)
        assertEquals("test@example.com", field.defaultValue)
        assertFalse(field.nullable)
        assertTrue(field.serializable)
    }
}