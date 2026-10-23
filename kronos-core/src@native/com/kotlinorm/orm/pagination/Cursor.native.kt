package com.kotlinorm.orm.pagination
internal actual fun platformEncodeCursorValue(value: Any): String? = when(value) {
    is kotlinx.datetime.LocalDate -> "localDate:$value"
    is kotlinx.datetime.LocalTime -> "localTime:$value"
    is kotlinx.datetime.LocalDateTime -> "localDateTime:$value"
    else -> null
}
internal actual fun platformDecodeCursorValue(type: String, raw: String): Any = when(type) {
    "localDate" -> kotlinx.datetime.LocalDate.parse(raw)
    "localTime" -> kotlinx.datetime.LocalTime.parse(raw)
    "localDateTime" -> kotlinx.datetime.LocalDateTime.parse(raw)
    else -> error("Invalid cursor token.")
}
