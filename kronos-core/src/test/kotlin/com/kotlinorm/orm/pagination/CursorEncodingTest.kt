package com.kotlinorm.orm.pagination

import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

class CursorEncodingTest {

    @Test
    fun `cursor encodes supported scalar value types`() {
        val values = mapOf<String, Any?>(
            "nullValue" to null,
            "intValue" to 1,
            "longValue" to 2L,
            "shortValue" to 3.toShort(),
            "byteValue" to 4.toByte(),
            "floatValue" to 5.5f,
            "doubleValue" to 6.25,
            "booleanValue" to true,
            "string value" to "a=b\nc"
        )

        val decoded = CursorSpec(values.toCursor(), pageSize = 10).values

        assertEquals(values, decoded)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `cursor preserves numeric uuid and date-time value types`() {
        val bigDecimal = BigDecimal("1.2300E+7")
        val bigInteger = BigInteger("123456789012345678901234567890")
        val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val sqlDate = java.sql.Date.valueOf("2026-07-20")
        val sqlTime = Time.valueOf("12:34:56")
        val sqlTimestamp = Timestamp.valueOf("2026-07-20 12:34:56.123456789")
        val utilDate = java.util.Date.from(Instant.parse("2026-07-20T12:34:56.123Z"))
        val localDate = LocalDate.parse("2026-07-20")
        val localTime = LocalTime.parse("12:34:56.123456789")
        val localDateTime = LocalDateTime.parse("2026-07-20T12:34:56.123456789")
        val instant = Instant.parse("2026-07-20T12:34:56.123456789Z")
        val zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("Asia/Shanghai"))
        val offsetDateTime = OffsetDateTime.parse("2026-07-20T12:34:56.123456789+08:00")
        val kotlinInstant = kotlin.time.Instant.parse("2026-07-20T12:34:56.123456789Z")
        val values = linkedMapOf<String, Any?>(
            "bigDecimal" to bigDecimal,
            "bigInteger" to bigInteger,
            "uuid" to uuid,
            "sqlDate" to sqlDate,
            "sqlTime" to sqlTime,
            "sqlTimestamp" to sqlTimestamp,
            "utilDate" to utilDate,
            "localDate" to localDate,
            "localTime" to localTime,
            "localDateTime" to localDateTime,
            "instant" to instant,
            "zonedDateTime" to zonedDateTime,
            "offsetDateTime" to offsetDateTime,
            "kotlinInstant" to kotlinInstant
        )

        val cursor = values.toCursor()
        val body = String(java.util.Base64.getUrlDecoder().decode(cursor.value), Charsets.UTF_8)
        val decoded = CursorSpec(cursor, pageSize = 10).values

        assertEquals(
            listOf(
                "bigDecimal=bd:1.2300E+7",
                "bigInteger=bi:123456789012345678901234567890",
                "uuid=uuid:123e4567-e89b-12d3-a456-426614174000",
                "sqlDate=sqlDate:2026-07-20",
                "sqlTime=sqlTime:12:34:56",
                "sqlTimestamp=sqlTimestamp:2026-07-20T12:34:56.123456789",
                "utilDate=utilDate:2026-07-20T12:34:56.123Z",
                "localDate=localDate:2026-07-20",
                "localTime=localTime:12:34:56.123456789",
                "localDateTime=localDateTime:2026-07-20T12:34:56.123456789",
                "instant=instant:2026-07-20T12:34:56.123456789Z",
                "zonedDateTime=zonedDateTime:2026-07-20T12:34:56.123456789+08:00[Asia/Shanghai]",
                "offsetDateTime=offsetDateTime:2026-07-20T12:34:56.123456789+08:00",
                "kotlinInstant=kotlinInstant:2026-07-20T12:34:56.123456789Z"
            ).joinToString("\n"),
            body
        )
        assertEquals(values, decoded)
        assertIs<BigDecimal>(decoded.getValue("bigDecimal"))
        assertIs<BigInteger>(decoded.getValue("bigInteger"))
        assertIs<UUID>(decoded.getValue("uuid"))
        assertIs<java.sql.Date>(decoded.getValue("sqlDate"))
        assertIs<Time>(decoded.getValue("sqlTime"))
        assertIs<Timestamp>(decoded.getValue("sqlTimestamp"))
        val decodedUtilDate = assertNotNull(decoded.getValue("utilDate"))
        assertEquals(java.util.Date::class, decodedUtilDate::class)
        assertIs<LocalDate>(decoded.getValue("localDate"))
        assertIs<LocalTime>(decoded.getValue("localTime"))
        assertIs<LocalDateTime>(decoded.getValue("localDateTime"))
        assertIs<Instant>(decoded.getValue("instant"))
        assertIs<ZonedDateTime>(decoded.getValue("zonedDateTime"))
        assertIs<OffsetDateTime>(decoded.getValue("offsetDateTime"))
        assertIs<kotlin.time.Instant>(decoded.getValue("kotlinInstant"))
    }

    @Test
    fun `blank cursor decodes to empty values`() {
        val blank = Cursor(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(0)))

        assertEquals(emptyMap(), CursorSpec(blank, pageSize = 1).values)
    }

    @Test
    fun `cursor rejects lines without a key separator`() {
        val cursor = "missing-separator".encodedCursor()

        val error = assertFailsWith<IllegalArgumentException> {
            CursorSpec(cursor, pageSize = 1)
        }

        assertEquals("Invalid cursor token.", error.message)
    }

    @Test
    fun `cursor rejects unknown encoded value type`() {
        val cursor = "id=x:1".encodedCursor()

        val error = assertFailsWith<IllegalStateException> {
            CursorSpec(cursor, pageSize = 1)
        }

        assertEquals("Invalid cursor token.", error.message)
    }

    @Test
    fun `cursor spec without cursor has empty values`() {
        val spec = CursorSpec(cursor = null, pageSize = 0)

        assertNull(spec.cursor)
        assertEquals(emptyMap(), spec.values)
    }

    private fun String.encodedCursor(): Cursor =
        Cursor(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(Charsets.UTF_8)))
}
