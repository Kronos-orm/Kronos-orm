package com.kotlinorm.utils

import com.kotlinorm.Kronos.timeZone
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime

object DateTimeUtil {
    @OptIn(FormatStringsInDatetimeFormats::class)
    val currentDateTime =
        { format: String ->
            Clock.System.now().toLocalDateTime(timeZone).format(
                LocalDateTime.Format {
                    byUnicodePattern(format)
                }
            )
        }
}