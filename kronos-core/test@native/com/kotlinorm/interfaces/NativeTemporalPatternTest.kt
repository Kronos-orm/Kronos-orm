package com.kotlinorm.interfaces

import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.utils.StaticTypeRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class NativeTemporalPatternTest {
    @Test
    fun unsupportedLocalePatternsFailWithContextEvenForOtherwiseValidIsoText() {
        val pattern = "EEE, dd MMM yyyy HH:mm:ss"
        val context = StaticValueCodecContext(ValueCodecDirection.DECODE, ValueCodecOrigin.DATABASE,
            targetType = StaticTypeRef("kotlin.time.Instant", false), dateFormat = pattern, timeZone = "UTC")
        for (text in listOf("Fri, 05 Jun 2026 11:23:17", "2026-06-05T11:23:17Z")) {
            val failure = assertFailsWith<StaticValueCodecException> {
                KronosValueCodecs.decode(KronosPhysicalValue.Text(text), context)
            }
            assertEquals(ValueCodecDirection.DECODE, failure.direction)
            assertEquals(ValueCodecOrigin.DATABASE, failure.origin)
            assertEquals(context.targetType, failure.targetType)
            assertEquals("unsupported or invalid temporal pattern '$pattern'", failure.detail)
        }
        val failure = assertFailsWith<StaticValueCodecException> {
            KronosValueCodecs.encode(Instant.parse("2026-06-05T11:23:17Z"), context.copy(direction = ValueCodecDirection.ENCODE))
        }
        assertEquals(ValueCodecDirection.ENCODE, failure.direction)
        assertEquals("unsupported or invalid temporal pattern '$pattern'", failure.detail)
    }
}
