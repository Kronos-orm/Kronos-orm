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

package com.kotlinorm.exceptions

import com.kotlinorm.enums.DBType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExceptionsTest {

    @Test
    fun testEmptyFieldsException() {
        // Test with default message
        val exception1 = EmptyFieldsException()
        assertNotNull(exception1)
        assertEquals("At least one field must be provided!", exception1.message)

        // Test with custom message
        val customMessage = "Custom error message"
        val exception2 = EmptyFieldsException(customMessage)
        assertEquals(customMessage, exception2.message)
    }

    @Test
    fun testInvalidDataAccessApiUsageException() {
        val message = "Invalid API usage"
        val exception = InvalidDataAccessApiUsageException(message)
        assertEquals(message, exception.message)
    }

    @Test
    fun testInvalidParameterException() {
        val message = "Invalid parameter"
        val exception = InvalidParameterException(message)
        assertEquals(message, exception.message)
    }

    @Test
    fun testNoDataSourceException() {
        val message = "No data source configured"
        val exception = NoDataSourceException(message)
        assertEquals(message, exception.message)
    }

    @Test
    fun testNoSerializeProcessorException() {
        val message = "No serialize processor found"
        val exception = NoSerializeProcessorException(message)
        assertEquals(message, exception.message)
    }

    @Test
    fun testUnsupportedDatabaseTypeException() {
        val dbType = DBType.Mysql
        val message = "Unsupported database type"
        val exception = UnsupportedDatabaseTypeException(dbType, message)
        assertNotNull(exception.message)
        assertTrue(exception.message!!.contains(message))
    }

    @Test
    fun testUnSupportedFunctionException() {
        val dbType = DBType.Postgres
        val functionName = "some_function"
        val message = "Function not supported"
        val exception = UnSupportedFunctionException(dbType, functionName, message)
        assertNotNull(exception.message)
        // The exception message contains the function name or database type
        val messageStr = exception.message!!
        assertTrue(
            messageStr.contains(functionName) || 
            messageStr.contains(dbType.name) ||
            messageStr.contains(message),
            "Exception message should contain function name, db type, or custom message"
        )
    }
}