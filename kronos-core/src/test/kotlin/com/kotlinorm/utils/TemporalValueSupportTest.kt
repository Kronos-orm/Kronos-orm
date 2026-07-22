/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.utils

import com.kotlinorm.utils.codec.convertTemporalValue
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalValueSupportTest {
    @Test
    fun `JDBC temporal subclasses use native values instead of toString`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val localDate = LocalDate.of(2026, 7, 22)
        val localTime = LocalTime.of(12, 34, 56)
        val localDateTime = LocalDateTime.of(localDate, localTime).withNano(123_456_789)
        val sqlDate = object : java.sql.Date(java.sql.Date.valueOf(localDate).time) {
            override fun toString(): String = error("SQL Date text must not be read")
        }
        val sqlTime = object : Time(Time.valueOf(localTime).time) {
            override fun toString(): String = error("SQL Time text must not be read")
        }
        val timestamp = object : Timestamp(Timestamp.valueOf(localDateTime).time) {
            init {
                nanos = localDateTime.nano
            }

            override fun toString(): String = error("SQL Timestamp text must not be read")
        }
        val unusedFormatter = lazy<DateTimeFormatter> { error("Native conversion must not read dateFormat") }

        assertEquals(
            localDate,
            convertTemporalValue(sqlDate, typeOf<LocalDate>(), zone, unusedFormatter)
        )
        assertEquals(
            localTime,
            convertTemporalValue(sqlTime, typeOf<LocalTime>(), zone, unusedFormatter)
        )
        assertEquals(
            localDateTime,
            convertTemporalValue(timestamp, typeOf<LocalDateTime>(), zone, unusedFormatter)
        )
        assertEquals(
            localDate.atStartOfDay(zone).toInstant(),
            convertTemporalValue(sqlDate, typeOf<Instant>(), zone, unusedFormatter)
        )
        assertEquals(
            LocalDate.ofEpochDay(0).atTime(localTime).atZone(zone).toInstant(),
            convertTemporalValue(sqlTime, typeOf<Instant>(), zone, unusedFormatter)
        )
    }

    @Test
    fun `Timestamp native conversions preserve nanoseconds`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val expectedInstant = Instant.ofEpochSecond(1_753_000_000L, 123_456_789)
        val timestamp = object : Timestamp(Timestamp.from(expectedInstant).time) {
            init {
                nanos = expectedInstant.nano
            }

            override fun toString(): String = error("SQL Timestamp text must not be read")
        }
        val unusedFormatter = lazy<DateTimeFormatter> { error("Native conversion must not read dateFormat") }

        assertEquals(
            expectedInstant,
            convertTemporalValue(timestamp, typeOf<Instant>(), zone, unusedFormatter)
        )
        assertEquals(
            expectedInstant,
            (convertTemporalValue(timestamp, typeOf<Timestamp>(), zone, unusedFormatter) as Timestamp).toInstant()
        )
        assertEquals(
            ZonedDateTime.ofInstant(expectedInstant, zone),
            convertTemporalValue(timestamp, typeOf<ZonedDateTime>(), zone, unusedFormatter)
        )
    }
}
