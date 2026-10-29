package com.kotlinorm.orm.insert

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.JdbcParameterTypeHints
import com.kotlinorm.enums.KColumnType.DATE
import com.kotlinorm.enums.KColumnType.DATETIME
import com.kotlinorm.enums.KColumnType.BLOB
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.TEXT
import com.kotlinorm.enums.KColumnType.TIME
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.wrappers.SampleSqlServerJdbcWrapper
import java.sql.Types
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalJdbcTypeHintsTest {
    @Test
    fun `insert task carries jdbc null type hints from field metadata`() {
        val tasks = TemporalHintValue(id = 1)
            .insert()
            .build(SampleSqlServerJdbcWrapper)
            .component3()

        val hints = JdbcParameterTypeHints.from(tasks.single().stash)

        assertEquals(
            mapOf(
                "localDate" to Types.DATE,
                "localTime" to Types.TIME,
                "localDateTime" to Types.TIMESTAMP,
                "binaryPayload" to Types.LONGVARBINARY,
                "longText" to Types.LONGVARCHAR,
            ),
            hints
        )
    }

    @Test
    fun `insert task does not carry temporal jdbc type hints for non-null values`() {
        val tasks = TemporalHintValue(
            id = 1,
            localDate = LocalDate.of(2026, 7, 14),
            localTime = LocalTime.of(12, 34, 56),
            localDateTime = LocalDateTime.of(2026, 7, 14, 12, 34, 56),
            binaryPayload = byteArrayOf(1, 2, 3),
            longText = "stored"
        )
            .insert()
            .build(SampleSqlServerJdbcWrapper)
            .component3()

        assertEquals(emptyMap(), JdbcParameterTypeHints.from(tasks.single().stash))
    }
}

@Table("kt_temporal_hint")
data class TemporalHintValue(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(DATE)
    var localDate: LocalDate? = null,

    @ColumnType(TIME)
    var localTime: LocalTime? = null,

    @ColumnType(DATETIME)
    var localDateTime: LocalDateTime? = null,

    @ColumnType(BLOB)
    var binaryPayload: ByteArray? = null,

    @ColumnType(TEXT)
    var longText: String? = null,
) : KPojo
