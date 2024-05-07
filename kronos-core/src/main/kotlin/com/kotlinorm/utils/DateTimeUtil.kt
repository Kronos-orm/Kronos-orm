package com.kotlinorm.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateTimeUtil {
    val currentDateTime =
        { format: String -> LocalDateTime.now().format(DateTimeFormatter.ofPattern(format)) }
}