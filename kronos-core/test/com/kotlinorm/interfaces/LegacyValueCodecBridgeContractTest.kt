package com.kotlinorm.interfaces

import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.utils.StaticTypeRef
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LegacyValueCodecBridgeContractTest {
    @Test
    fun legacyCallbacksReceiveTheExactCompatibilityContextOnce() {
        val legacy = ValueCodecContext(ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE, null, typeOf<Int>())
        val calls = mutableListOf<String>()
        val bridge = valueCodec(supports = { value, context ->
            assertSame(legacy, context)
            calls += "supports:$value"
            true
        }, convert = { value, context ->
            assertSame(legacy, context)
            calls += "convert:$value"
            value.toString().toInt()
        }).asStaticValueCodec()
        val context = context().apply { platformContext = LegacyValueCodecPlatformContext(legacy) }
        assertTrue(bridge.supports("42", context))
        assertEquals(42, bridge.convert("42", context))
        assertEquals(listOf("supports:42", "convert:42"), calls)
    }

    @Test
    fun descriptorOnlyCallsDoNotSelectTheKTypeBridge() {
        var calls = 0
        val bridge = valueCodec(supports = { _, _ -> calls++; true }, convert = { _, _ -> calls++; 1 }).asStaticValueCodec()
        assertFalse(bridge.supports("42", context()))
        assertFailsWith<IllegalStateException> { bridge.convert("42", context()) }
        assertEquals(0, calls)
        val unrelated = context().apply { platformContext = "foreign context" }
        assertFalse(bridge.supports("42", unrelated))
    }

    @Test
    fun compatibilityRegistrationDelegatesLifetimeToItsOriginalHandle() {
        var calls = 0
        val handle = object : StaticValueCodecRegistration { override fun close() { calls++ } }.asValueCodecRegistration()
        handle.close()
        assertEquals(1, calls)
    }

    private fun context() = StaticValueCodecContext(ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE,
        targetType = StaticTypeRef("kotlin.Int", false))
}
