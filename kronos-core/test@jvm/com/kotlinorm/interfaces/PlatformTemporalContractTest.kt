package com.kotlinorm.interfaces

import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.utils.StaticTypeRef
import java.time.ZoneId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class PlatformTemporalContractTest {
    @Test
    fun localTemporalValuesEncodeAsIsoTextAndPreserveTheirComponents() {
        val values = listOf(Instant.parse("2026-10-18T01:02:03.123Z"), LocalDate(2026, 10, 18),
            LocalDateTime(2026, 10, 18, 9, 2, 3, 123000000), LocalTime(9, 2, 3, 123000000))
        for (value in values) {
            assertEquals(KronosPhysicalValue.Text(value.toString()), encodePlatformTemporal(value, context("kotlin.String")))
        }
        assertNull(encodePlatformTemporal("not temporal", context("kotlin.String")))
    }

    @Test
    fun instantDecodingUsesExplicitZoneForLocalTextAndEpochMillisecondsForIntegers() {
        val local = KronosPhysicalValue.Text("2026-10-18T09:02:03.123456789")
        val expected = "2026-10-18T01:02:03.123456789Z"
        assertEquals(Instant.parse(expected), decodePlatformTemporal(local, context("kotlinx.datetime.Instant")))
        assertEquals(Instant.parse(expected), decodePlatformTemporal(local, context("kotlin.time.Instant")))
        for (key in listOf("kotlinx.datetime.Instant", "kotlin.time.Instant")) {
            assertEquals("1970-01-01T00:00:01Z", decodePlatformTemporal(KronosPhysicalValue.Integer(1000), context(key)).toString())
            assertEquals(expected, decodePlatformTemporal(KronosPhysicalValue.Text(expected), context(key)).toString())
            assertNull(decodePlatformTemporal(KronosPhysicalValue.Blob(byteArrayOf()), context(key)))
        }
    }

    @Test
    fun localTargetsAcceptBothLocalTextAndZonedInstantText() {
        val instant = KronosPhysicalValue.Text("2026-10-18T01:02:03.123Z")
        val expected = listOf("LocalDate" to LocalDate(2026, 10, 18),
            "LocalDateTime" to LocalDateTime(2026, 10, 18, 9, 2, 3, 123000000),
            "LocalTime" to LocalTime(9, 2, 3, 123000000))
        for ((key, value) in expected) {
            val context = context("kotlinx.datetime.$key")
            assertEquals(value, decodePlatformTemporal(instant, context))
            assertEquals(value, decodePlatformTemporal(KronosPhysicalValue.Text(value.toString()), context))
            assertNull(decodePlatformTemporal(KronosPhysicalValue.Integer(1000), context))
        }
    }

    @Test
    fun missingZoneUsesSystemZoneAndUnrelatedTargetsAreNotClaimed() {
        val instant = Instant.parse("2026-10-18T01:02:03Z")
        val expected = instant.toLocalDateTime(TimeZone.of(ZoneId.systemDefault().id))
        assertEquals(expected, decodePlatformTemporal(KronosPhysicalValue.Text(instant.toString()),
            context("kotlinx.datetime.LocalDateTime").copy(timeZone = null)))
        assertNull(decodePlatformTemporal(KronosPhysicalValue.Text("value"), context("example.Text")))
        assertNull(decodePlatformTemporal(KronosPhysicalValue.Text("value"), context("kotlinx.datetime.Unknown")))
    }

    private fun context(key: String) = StaticValueCodecContext(ValueCodecDirection.DECODE,
        ValueCodecOrigin.DATABASE, targetType = StaticTypeRef(key, false), timeZone = "Asia/Shanghai")
}
