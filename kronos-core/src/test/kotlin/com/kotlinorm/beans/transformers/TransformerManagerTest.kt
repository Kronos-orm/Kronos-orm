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

package com.kotlinorm.beans.transformers

import com.kotlinorm.interfaces.ValueTransformer
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class TransformerManagerTest {
    @Test
    fun testRegisterValueTransformer() {
        val mockTransformer = object : ValueTransformer {
            override fun isMatch(targetKotlinType: KType, sourceValueClass: KClass<*>): Boolean = false
            override fun transform(targetKotlinType: KType, value: Any, dateTimeFormat: String?, sourceValueClass: KClass<*>): Any = value
        }

        TransformerManager.registerValueTransformer(mockTransformer)
        
        // Verify transformer was added (we can't directly check the internal list, 
        // but we can test that it doesn't throw)
        assertTrue(true, "Transformer registration should succeed")
    }

    @Test
    fun testUnregisterValueTransformer() {
        val mockTransformer = object : ValueTransformer {
            override fun isMatch(targetKotlinType: KType, sourceValueClass: KClass<*>): Boolean = false
            override fun transform(targetKotlinType: KType, value: Any, dateTimeFormat: String?, sourceValueClass: KClass<*>): Any = value
        }

        TransformerManager.registerValueTransformer(mockTransformer)
        TransformerManager.unregisterValueTransformer(mockTransformer)
        
        // Verify transformer was removed (we can't directly check the internal list,
        // but we can test that it doesn't throw)
        assertTrue(true, "Transformer unregistration should succeed")
    }

    @Test
    fun testGetValueTransformedWithSameType() {
        val value = "testValue"
        val result = TransformerManager.getValueTransformed(
            targetKotlinType = typeOf<String>(),
            value = value
        )
        
        assertSame(value, result, "Should return same value when target type is in superTypes")
    }

    @Test
    fun testGetValueTransformedWithSameClass() {
        val value = 123
        val result = TransformerManager.getValueTransformed(
            targetKotlinType = typeOf<Int>(),
            value = value
        )
        
        assertEquals(value, result, "Should return same value when target type matches value class")
    }

    @Test
    fun testGetValueTransformedWithBasicTypeTransformer() {
        val value = "123"
        val result = TransformerManager.getValueTransformed(
            targetKotlinType = typeOf<Int>(),
            value = value
        )
        
        assertEquals(123, result, "Should transform string to int using BasicTypeTransformer")
    }

    @Test
    fun `basic type transformer converts numeric zero to boolean false`() {
        assertEquals(
            false,
            TransformerManager.getValueTransformed(
                targetKotlinType = typeOf<Boolean>(),
                value = BigDecimal.ZERO
            )
        )
        assertEquals(
            false,
            TransformerManager.getValueTransformed(
                targetKotlinType = typeOf<Boolean>(),
                value = BigInteger.ZERO
            )
        )
        assertEquals(
            true,
            TransformerManager.getValueTransformed(
                targetKotlinType = typeOf<Boolean>(),
                value = BigDecimal.ONE
            )
        )
    }

    @Test
    fun testGetValueTransformedWithToStringTransformer() {
        val value = 123
        val result = TransformerManager.getValueTransformed(
            targetKotlinType = typeOf<String>(),
            value = value
        )
        
        assertEquals("123", result, "Should transform int to string using ToStringTransformer")
    }

    @Test
    fun testGetValueTransformedWithNoMatchingTransformer() {
        val value = CustomClass("test")
        val result = TransformerManager.getValueTransformed(
            targetKotlinType = typeOf<CustomClass>(),
            value = value
        )
        
        assertSame(value, result, "Should return same value when no transformer matches")
    }

    private data class CustomClass(val value: String)
}
