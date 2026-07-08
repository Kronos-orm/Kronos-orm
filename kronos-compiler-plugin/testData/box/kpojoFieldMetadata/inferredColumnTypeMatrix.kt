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

// Verifies inferred KColumnType metadata for Kotlin and Java field types.

import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class InferredTypeUser(
    var boolValue: Boolean? = null,
    var byteValue: Byte? = null,
    var shortValue: Short? = null,
    var intValue: Int? = null,
    var longValue: Long? = null,
    var floatValue: Float? = null,
    var doubleValue: Double? = null,
    var decimalValue: BigDecimal? = null,
    var charValue: Char? = null,
    var stringValue: String? = null,
    var utilDateValue: java.util.Date? = null,
    var localDateValue: LocalDate? = null,
    var localTimeValue: LocalTime? = null,
    var localDateTimeValue: LocalDateTime? = null,
    var bytesValue: ByteArray? = null,
    var unknownValue: UUID? = null,
) : KPojo

fun box(): String {
    val actual = InferredTypeUser().kronosColumns().associate { it.name to it.type }
    val expected = linkedMapOf(
        "boolValue" to KColumnType.BIT,
        "byteValue" to KColumnType.TINYINT,
        "shortValue" to KColumnType.SMALLINT,
        "intValue" to KColumnType.INT,
        "longValue" to KColumnType.BIGINT,
        "floatValue" to KColumnType.FLOAT,
        "doubleValue" to KColumnType.DOUBLE,
        "decimalValue" to KColumnType.DECIMAL,
        "charValue" to KColumnType.CHAR,
        "stringValue" to KColumnType.VARCHAR,
        "utilDateValue" to KColumnType.DATE,
        "localDateValue" to KColumnType.DATE,
        "localTimeValue" to KColumnType.TIME,
        "localDateTimeValue" to KColumnType.DATETIME,
        "bytesValue" to KColumnType.BLOB,
        "unknownValue" to KColumnType.VARCHAR,
    )

    expected.forEach { (name, type) ->
        val observed = actual[name] ?: return "Fail: missing $name"
        if (observed != type) return "Fail: $name type was $observed"
    }

    return "OK"
}
