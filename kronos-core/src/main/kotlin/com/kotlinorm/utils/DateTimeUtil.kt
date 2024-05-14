package com.kotlinorm.utils

import kotlinx.datetime.*
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern

object DateTimeUtil {
    @OptIn(FormatStringsInDatetimeFormats::class)
    val currentDateTime =
        { format: String ->
            Clock.System.now().toLocalDateTime(TimeZone.UTC).format(
                LocalDateTime.Format {
                    byUnicodePattern(format)
                }
            )
        }
}