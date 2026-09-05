package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType.DATETIME
import com.kotlinorm.enums.KColumnType.DECIMAL
import com.kotlinorm.enums.KColumnType.FLOAT
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.TIME
import com.kotlinorm.enums.KColumnType.TIMESTAMP
import com.kotlinorm.interfaces.KPojo
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime

@Table("kt_h2_precision_value")
data class H2PrecisionValue(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(FLOAT)
    var approximateValue: Double? = null,

    @ColumnType(DECIMAL, 12, 4)
    var exactValue: BigDecimal? = null,

    @ColumnType(TIME, 0, 3)
    var localTime: LocalTime? = null,

    @ColumnType(DATETIME, 0, 6)
    var localDateTime: LocalDateTime? = null,

    @ColumnType(TIMESTAMP, 0, 6)
    var timestampValue: LocalDateTime? = null,
) : KPojo
