package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.beans.serialize.NoneSerializeProcessor
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosSerializeProcessor
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.starProjectedType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotlinXSerializationTest {
    @Serializable
    data class TestPojo(
        var id: JsonPrimitive = JsonNull,
        var name: String? = "test",
        var age: Int? = 104,
        var version: Int = 1
    ): KPojo

    @Serializable
    data class SerializableProfile(
        val name: String,
        val tags: List<String> = emptyList()
    )

    @Serializable
    data class SerializableEnvelope(
        val profile: SerializableProfile,
        val active: Boolean = true
    )

    data class SerializableHolder(
        var id: Int? = null,
        @Serialize
        var payload: SerializableEnvelope? = null
    ) : KPojo

    private object KotlinXSerializeProcessor : KronosSerializeProcessor {
        private val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        override fun deserialize(serializedStr: String, kClass: KClass<*>): Any {
            return json.decodeFromString(serializer(kClass.starProjectedType), serializedStr)
                ?: error("Kotlinx serialization returned null for ${kClass.qualifiedName}")
        }

        override fun serialize(obj: Any): String {
            @Suppress("UNCHECKED_CAST")
            val valueSerializer = serializer(obj::class.starProjectedType) as KSerializer<Any>
            return json.encodeToString(valueSerializer, obj)
        }
    }

    inline fun <reified T: KPojo> createInstance(): T {
        return T::class.createInstance()
    }

    @BeforeTest
    fun installKotlinXSerializeProcessor() {
        Kronos.serializeProcessor = KotlinXSerializeProcessor
    }

    @AfterTest
    fun resetSerializeProcessor() {
        Kronos.serializeProcessor = NoneSerializeProcessor
    }

    @Test
    fun testKotlinXSerialization() {
        val testPojo = createInstance<TestPojo>()
        assertEquals(JsonNull, testPojo.id)
        assertEquals("test", testPojo.name)
        assertEquals(104, testPojo.age)
        assertEquals(1, testPojo.version)
    }

    @Test
    fun kotlinxSerializationProcessorCanBackKronosDatabaseValueConversion() {
        val payload = SerializableEnvelope(
            profile = SerializableProfile("Ada", listOf("orm", "json")),
            active = false
        )
        val holder = SerializableHolder(id = 1, payload = payload)
        val payloadField = holder.kronosColumns().single { it.name == "payload" }

        assertTrue(payloadField.serializable)
        assertEquals(
            """{"profile":{"name":"Ada","tags":["orm","json"]},"active":false}""",
            toDatabaseValue(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper, payloadField, payload)
        )
    }

    @Test
    fun kotlinxSerializationProcessorCanBackKronosGeneratedMapHydration() {
        val payload = SerializableEnvelope(
            profile = SerializableProfile("Lin", listOf("db")),
            active = true
        )
        val serialized = """{"profile":{"name":"Lin","tags":["db"]},"active":true}"""
        val hydrated = SerializableHolder().safeFromMapData<SerializableHolder>(
            mapOf("id" to 7, "payload" to serialized)
        )

        assertEquals(7, hydrated.id)
        assertNotNull(hydrated.payload)
        assertEquals(payload, hydrated.payload)
    }
}
