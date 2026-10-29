package com.kotlinorm.orm.pagination
import kotlin.jvm.JvmInline
import kotlin.time.ExperimentalTime

@JvmInline
value class Cursor(val value: String)

internal data class CursorSpec(
    val cursor: Cursor?,
    val pageSize: Int,
    val values: Map<String, Any?> = cursor?.decodeCursorValues().orEmpty()
)

internal fun Map<String, Any?>.toCursor(): Cursor {
    val body = entries.joinToString("\n") { (name, value) ->
        "${escape(name)}=${encodeValue(value)}"
    }
    return Cursor(kotlin.io.encoding.Base64.UrlSafe.withPadding(kotlin.io.encoding.Base64.PaddingOption.ABSENT).encode(body.encodeToByteArray()))
}

private fun Cursor.decodeCursorValues(): Map<String, Any?> {
    val body = kotlin.io.encoding.Base64.UrlSafe.withPadding(kotlin.io.encoding.Base64.PaddingOption.ABSENT_OPTIONAL).decode(value).decodeToString()
    if (body.isBlank()) return emptyMap()
    return body.lineSequence().associate { line ->
        val index = line.indexOf('=')
        require(index > 0) { "Invalid cursor token." }
        unescape(line.substring(0, index)) to decodeValue(line.substring(index + 1))
    }
}

@OptIn(ExperimentalTime::class)
private fun encodeValue(value: Any?): String = when (value) {
    null -> "n:"
    is Int -> "i:$value"
    is Long -> "l:$value"
    is Short -> "h:$value"
    is Byte -> "y:$value"
    is Float -> "f:$value"
    is Double -> "d:$value"
    is Boolean -> "b:$value"
    is kotlin.time.Instant -> "kotlinInstant:$value"
    else -> platformEncodeCursorValue(value) ?: "s:${escape(value.toString())}"
}

@OptIn(ExperimentalTime::class)
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
        "kotlinInstant" -> kotlin.time.Instant.parse(raw)
        "s" -> unescape(raw)
        else -> platformDecodeCursorValue(type, raw)
    }
}

private fun escape(value: String): String = buildString {
    value.encodeToByteArray().forEach { byte ->
        val code = byte.toInt() and UNSIGNED_BYTE_MASK
        when {
            code == ' '.code -> append('+')
            code in '0'.code..'9'.code || code in 'A'.code..'Z'.code || code in 'a'.code..'z'.code || code.toChar() in "-_.*" -> append(code.toChar())
            else -> { append('%'); append(code.toString(HEX_RADIX).uppercase().padStart(2, '0')) }
        }
    }
}
private fun unescape(value: String): String {
    val bytes = mutableListOf<Byte>()
    var index = 0
    while (index < value.length) {
        when (val char = value[index++]) {
            '+' -> bytes += ' '.code.toByte()
            '%' -> { require(index + 2 <= value.length) { "Invalid cursor escape" }; bytes += value.substring(index, index + 2).toInt(HEX_RADIX).toByte(); index += 2 }
            else -> bytes += char.code.toByte()
        }
    }
    return bytes.toByteArray().decodeToString()
}
internal expect fun platformEncodeCursorValue(value: Any): String?
internal expect fun platformDecodeCursorValue(type: String, raw: String): Any

private const val UNSIGNED_BYTE_MASK = 0xff
private const val HEX_RADIX = 16
