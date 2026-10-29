package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.interfaces.valueCodec
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.codec.ValueConversionRequest
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NativeValueCodecFacadeTest {
    @Test
    fun strictModeStillAllowsExplicitUserCodecsAndNumericNormalization() {
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { value, context -> value == "one" && context.targetType == typeOf<Int>() },
            convert = { _, _ -> 1 }
        ))
        try {
            assertEquals(1, ValueCodecRegistry.convert(ValueConversionRequest("one", ValueCodecDirection.DECODE,
                ValueCodecOrigin.DATABASE, typeOf<Int>(), strict = true)))
            assertEquals(2, ValueCodecRegistry.convert(ValueConversionRequest(2L, ValueCodecDirection.DECODE,
                ValueCodecOrigin.DATABASE, typeOf<Int>(), strict = true)))
        } finally { registration.close() }
    }

    @Test
    fun incompatibleDeclaredGenericInputNeverReachesSerializedCodec() {
        var invocations = 0
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, context -> context.storage == ValueStorage.SERIALIZED },
            convert = { _, _ -> invocations++; "encoded" }
        ))
        try {
            assertFailsWith<ValueMappingException> {
                ValueCodecRegistry.convert(ValueConversionRequest(Box("value"), ValueCodecDirection.ENCODE,
                    ValueCodecOrigin.PARAMETER, typeOf<Box<Int>>(), sourceType = typeOf<Box<String>>(),
                    storage = ValueStorage.SERIALIZED))
            }
            assertEquals(0, invocations)
        } finally { registration.close() }
    }

    private class Box<T>(val value: T)
}
