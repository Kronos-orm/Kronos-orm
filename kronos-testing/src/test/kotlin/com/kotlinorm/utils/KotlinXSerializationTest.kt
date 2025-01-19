package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KPojo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinXSerializationTest {
    @Serializable
    data class TestPojo(
        var id: JsonPrimitive = JsonNull,
        var name: String? = "test",
        var age: Int? = 104,
        var version: Int = 1
    ): KPojo

    inline fun <reified T: KPojo> createInstance(): T {
        return T::class.createInstance()
    }

    init {
        Kronos.init{}
    }

    @Test
    fun testKotlinXSerialization() {
        val testPojo = createInstance<TestPojo>()
        assertEquals(JsonNull, testPojo.id)
        assertEquals("test", testPojo.name)
        assertEquals(104, testPojo.age)
        assertEquals(1, testPojo.version)
    }
}