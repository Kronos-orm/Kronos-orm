package com.kotlinorm.orm.pagination

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

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

        val decoded = CursorSpec(values.toCursor(), offset = 10).values

        assertEquals(values, decoded)
    }

    @Test
    fun `blank cursor decodes to empty values`() {
        val blank = Cursor(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(0)))

        assertEquals(emptyMap(), CursorSpec(blank, offset = 1).values)
    }

    @Test
    fun `cursor rejects lines without a key separator`() {
        val cursor = "missing-separator".encodedCursor()

        val error = assertFailsWith<IllegalArgumentException> {
            CursorSpec(cursor, offset = 1)
        }

        assertEquals("Invalid cursor token.", error.message)
    }

    @Test
    fun `cursor rejects unknown encoded value type`() {
        val cursor = "id=x:1".encodedCursor()

        val error = assertFailsWith<IllegalStateException> {
            CursorSpec(cursor, offset = 1)
        }

        assertEquals("Invalid cursor token.", error.message)
    }

    @Test
    fun `cursor spec without cursor has empty values`() {
        val spec = CursorSpec(cursor = null, offset = 0)

        assertNull(spec.cursor)
        assertEquals(emptyMap(), spec.values)
    }

    private fun String.encodedCursor(): Cursor =
        Cursor(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(Charsets.UTF_8)))
}
