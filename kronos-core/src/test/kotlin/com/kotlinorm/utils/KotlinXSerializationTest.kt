package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.ValueCodecRegistration
import com.kotlinorm.interfaces.serializedValueCodec
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import kotlin.reflect.KType
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
        var payload: SerializableEnvelope? = null,
        @Serialize
        var profileData: SerializableProfile? = null,
        @Serialize
        var listData: List<Int>? = null,
        @Serialize
        var matrixData: List<List<String>>? = null
    ) : KPojo

    private object KotlinXSerializationCodec {
        private val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        fun decode(serializedStr: String, kType: KType): Any {
            return json.decodeFromString(serializer(kType), serializedStr)
                ?: error("Kotlinx serialization returned null for $kType")
        }

        fun encode(obj: Any, kType: KType): String {
            @Suppress("UNCHECKED_CAST")
            val valueSerializer = serializer(kType) as KSerializer<Any>
            return json.encodeToString(valueSerializer, obj)
        }
    }

    private lateinit var codecRegistration: ValueCodecRegistration

    inline fun <reified T : KPojo> newKPojo(): T {
        return createKPojo<T>()
    }

    @BeforeTest
    fun installKotlinXSerializationCodec() {
        codecRegistration = Kronos.registerValueCodec(
            serializedValueCodec(
                encode = KotlinXSerializationCodec::encode,
                decode = KotlinXSerializationCodec::decode
            )
        )
    }

    @AfterTest
    fun resetSerializationCodec() {
        if (::codecRegistration.isInitialized) {
            codecRegistration.close()
        }
    }

    @Test
    fun testKotlinXSerialization() {
        val testPojo = newKPojo<TestPojo>()
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
        val payloadField = holder.__columns.single { it.name == "payload" }
        val profileDataField = holder.__columns.single { it.name == "profileData" }
        val listDataField = holder.__columns.single { it.name == "listData" }
        val matrixDataField = holder.__columns.single { it.name == "matrixData" }

        assertTrue(payloadField.serializable)
        assertEquals(
            """{"profile":{"name":"Ada","tags":["orm","json"]},"active":false}""",
            toDatabaseValue(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper, payloadField, payload)
        )
        assertTrue(profileDataField.serializable)
        assertEquals(
            """{"name":"Ada","tags":["orm","json"]}""",
            toDatabaseValue(
                SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper,
                profileDataField,
                SerializableProfile("Ada", listOf("orm", "json"))
            )
        )
        assertTrue(listDataField.serializable)
        assertEquals(
            """[1,2,3]""",
            toDatabaseValue(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper, listDataField, listOf(1, 2, 3))
        )
        assertTrue(matrixDataField.serializable)
        assertEquals(
            """[["read","write"],["sync"]]""",
            toDatabaseValue(
                SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper,
                matrixDataField,
                listOf(listOf("read", "write"), listOf("sync"))
            )
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
            mapOf(
                "id" to 7,
                "payload" to serialized,
                "profileData" to """{"name":"Lin","tags":["db"]}""",
                "listData" to "[4,5,6]",
                "matrixData" to """[["north","south"],["east"]]"""
            )
        )

        assertEquals(7, hydrated.id)
        assertNotNull(hydrated.payload)
        assertEquals(payload, hydrated.payload)
        assertEquals(SerializableProfile("Lin", listOf("db")), hydrated.profileData)
        assertEquals(listOf(4, 5, 6), hydrated.listData)
        assertEquals(listOf(listOf("north", "south"), listOf("east")), hydrated.matrixData)
    }

    @Test
    fun kotlinxSerializationProcessorCanDeserializeDataClassFieldFromGeneratedMapHydration() {
        val hydrated = SerializableHolder().safeFromMapData<SerializableHolder>(
            mapOf(
                "id" to 8,
                "profileData" to """{"name":"Grace","tags":["compiler","orm"]}"""
            )
        )

        assertEquals(8, hydrated.id)
        assertEquals(SerializableProfile("Grace", listOf("compiler", "orm")), hydrated.profileData)
    }
}
