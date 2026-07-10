package com.kotlinorm.beans.serialize

import com.kotlinorm.GsonProcessor
import com.kotlinorm.Kronos
import io.mockk.every
import io.mockk.mockk
import kotlin.reflect.KProperty0
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializableTest {

    init {
        with(Kronos) {
            serializeProcessor = GsonProcessor
        }
    }

    @Test
    fun serializeReturnsCorrectDelegateInstance() {
        val property = mockk<KProperty0<String?>> {
            every { name } returns "property"
        }
        val result = serialize<Map<String, String>>(property)
        assertEquals("property", result::class.java.getDeclaredField("toSerialize").apply {
            isAccessible = true
        }.get(result) as String)
        assertEquals(typeOf<Map<String, String>>(), result::class.java.getDeclaredField("targetKType").apply {
            isAccessible = true
        }.get(result) as KType)
    }
}
