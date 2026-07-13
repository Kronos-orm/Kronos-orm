package com.kotlinorm.orm.pagination

@JvmInline
value class Cursor(val value: String)

internal data class CursorSpec(
    val cursor: Cursor?,
    val offset: Int,
    val values: Map<String, Any?> = cursor?.decodeCursorValues().orEmpty()
)

internal fun Map<String, Any?>.toCursor(): Cursor {
    val body = entries.joinToString("\n") { (name, value) ->
        "${escape(name)}=${encodeValue(value)}"
    }
    return Cursor(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(body.toByteArray(Charsets.UTF_8)))
}

private fun Cursor.decodeCursorValues(): Map<String, Any?> {
    val body = String(java.util.Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
    if (body.isBlank()) return emptyMap()
    return body.lineSequence().associate { line ->
        val index = line.indexOf('=')
        require(index > 0) { "Invalid cursor token." }
        unescape(line.substring(0, index)) to decodeValue(line.substring(index + 1))
    }
}

private fun encodeValue(value: Any?): String = when (value) {
    null -> "n:"
    is Int -> "i:$value"
    is Long -> "l:$value"
    is Short -> "h:$value"
    is Byte -> "y:$value"
    is Float -> "f:$value"
    is Double -> "d:$value"
    is Boolean -> "b:$value"
    else -> "s:${escape(value.toString())}"
}

private fun decodeValue(encoded: String): Any? {
    val type = encoded.substringBefore(':')
    val raw = encoded.substringAfter(':', "")
    return when (type) {
        "n" -> null
        "i" -> raw.toInt()
        "l" -> raw.toLong()
        "h" -> raw.toShort()
        "y" -> raw.toByte()
        "f" -> raw.toFloat()
        "d" -> raw.toDouble()
        "b" -> raw.toBooleanStrict()
        "s" -> unescape(raw)
        else -> error("Invalid cursor token.")
    }
}

private fun escape(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

private fun unescape(value: String): String = java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
