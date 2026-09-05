package com.kotlinorm.beans.serialize

import com.google.gson.Gson
import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.ValueCodecRegistration
import com.kotlinorm.interfaces.serializedValueCodec
import com.kotlinorm.interfaces.valueCodec
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SerializableTest {
    internal data class Payload(val name: String, val enabled: Boolean)

    internal enum class Status {
        READY,
        CLOSED
    }

    internal data class DelegateHolder(
        var objectJson: String? = null,
        var listJson: String? = null,
        var nestedListJson: String? = null,
        var enumListJson: String? = null
    ) : KPojo {
        var payload: Payload? by serialize(::objectJson)
        var names: List<String>? by serialize(::listJson)
        var matrix: List<List<Int>>? by serialize(::nestedListJson)
        var statuses: List<Status>? by serialize(::enumListJson)
    }

    private val gson = Gson()
    private val encodedTypes = mutableListOf<KType>()
    private val decodedTypes = mutableListOf<KType>()
    private lateinit var registration: ValueCodecRegistration

    @BeforeTest
    fun registerSerializedCodec() {
        registration = Kronos.registerValueCodec(serializedValueCodec(
            encode = { value, type ->
                encodedTypes += type
                gson.toJson(value, type.javaType)
            },
            decode = { text, type ->
                decodedTypes += type
                gson.fromJson(text, type.javaType)
            }
        ))
    }

    @AfterTest
    fun unregisterSerializedCodec() {
        registration.close()
        encodedTypes.clear()
        decodedTypes.clear()
    }

    @Test
    fun delegateRoundTripsObjectAndGenericCollectionsWithCompleteKType() {
        val holder = DelegateHolder()
        val payload = Payload("codec", true)
        val names = listOf("alpha", "beta")
        val matrix = listOf(listOf(1, 2), listOf(3))
        val statuses = listOf(Status.READY, Status.CLOSED)

        holder.payload = payload
        holder.names = names
        holder.matrix = matrix
        holder.statuses = statuses

        assertEquals("""{"name":"codec","enabled":true}""", holder.objectJson)
        assertEquals("""["alpha","beta"]""", holder.listJson)
        assertEquals("""[[1,2],[3]]""", holder.nestedListJson)
        assertEquals("""["READY","CLOSED"]""", holder.enumListJson)
        assertEquals(payload, holder.payload)
        assertEquals(names, holder.names)
        assertEquals(matrix, holder.matrix)
        assertEquals(statuses, holder.statuses)
        assertEquals(
            listOf(
                typeOf<Payload?>(),
                typeOf<List<String>?>(),
                typeOf<List<List<Int>>?>(),
                typeOf<List<Status>?>()
            ),
            encodedTypes
        )
        assertEquals(encodedTypes, decodedTypes)
    }

    @Test
    fun delegatePassesNullWithoutInvokingSerializedCodec() {
        val holder = DelegateHolder(objectJson = "stale")

        holder.payload = null

        assertNull(holder.objectJson)
        assertNull(holder.payload)
        assertEquals(emptyList(), encodedTypes)
        assertEquals(emptyList(), decodedTypes)
    }

    @Test
    fun delegateDecodesSerializedNullWithItsNullableTargetType() {
        val holder = DelegateHolder(objectJson = "null")

        assertNull(holder.payload)
        assertEquals(emptyList(), encodedTypes)
        assertEquals(listOf(typeOf<Payload?>()), decodedTypes)
    }

    @Test
    fun delegateEncodePassesItsCompleteDeclaredSourceKType() {
        var seenSourceType: KType? = null
        val override = Kronos.registerValueCodec(valueCodec(
            supports = { _, context ->
                if (context.origin == com.kotlinorm.enums.ValueCodecOrigin.DELEGATE &&
                    context.direction == com.kotlinorm.enums.ValueCodecDirection.ENCODE
                ) {
                    seenSourceType = context.sourceType
                    true
                } else {
                    false
                }
            },
            convert = { _, _ -> "delegate-json" }
        ))
        try {
            val holder = DelegateHolder()
            holder.payload = Payload("source-type", true)

            assertEquals("delegate-json", holder.objectJson)
            assertEquals(typeOf<Payload?>(), seenSourceType)
        } finally {
            override.close()
        }
    }
}
