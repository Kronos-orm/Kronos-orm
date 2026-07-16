package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType.BIGINT
import com.kotlinorm.enums.KColumnType.BIT
import com.kotlinorm.enums.KColumnType.BLOB
import com.kotlinorm.enums.KColumnType.DATE
import com.kotlinorm.enums.KColumnType.DATETIME
import com.kotlinorm.enums.KColumnType.DECIMAL
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.TEXT
import com.kotlinorm.enums.KColumnType.TIME
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Table("kt_type_dialect_corner")
data class TypeDialectCornerValue(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @Column("select")
    @ColumnType(VARCHAR, 120)
    var reservedWord: String? = null,

    @Column("CamelCase")
    @ColumnType(VARCHAR, 80)
    var mixedCaseName: String? = null,

    @Column("under_score")
    @ColumnType(VARCHAR, 80)
    var underScoreName: String? = null,

    @ColumnType(BIGINT)
    var externalId: Long? = null,

    @ColumnType(BIT)
    var enabled: Boolean? = null,

    @ColumnType(DECIMAL, 18, 4)
    var exactAmount: BigDecimal? = null,

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

    @ColumnType(VARCHAR, 20)
    @Default("'fallback'")
    var defaultedText: String? = null,

    @ColumnType(INT)
    @Default("7")
    var defaultedNumber: Int? = null,
) : KPojo

data class TypeDialectCornerRecord(
    val id: Int?,
    val reservedWord: String?,
    val mixedCaseName: String?,
    val underScoreName: String?,
    val externalId: Long?,
    val enabled: Boolean?,
    val exactAmount: String?,
    val localDate: LocalDate?,
    val localTime: LocalTime?,
    val localDateTime: LocalDateTime?,
    val binaryPayload: List<Int>?,
    val longText: String?,
    val defaultedText: String?,
    val defaultedNumber: Int?,
)

@Table("kt_type_dialect_sync_corner")
data class TypeDialectSyncCornerV1(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @Column("order")
    @ColumnType(VARCHAR, 40)
    var orderValue: String? = null,
) : KPojo

@Table("kt_type_dialect_sync_corner")
data class TypeDialectSyncCornerV2(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @Column("order")
    @ColumnType(VARCHAR, 40)
    var orderValue: String? = null,

    @ColumnType(VARCHAR, 40)
    @Default("'fresh'")
    var addedDefault: String? = null,

    @ColumnType(INT)
    var addedNullable: Int? = null,

    @Column("snake_value")
    @ColumnType(BIGINT)
    @Default("42")
    var mappedSnakeValue: Long? = null,
) : KPojo

data class TypeDialectSyncCornerRecord(
    val id: Int?,
    val orderValue: String?,
    val addedDefault: String?,
    val addedNullable: Int?,
    val snakeValue: Long?,
)
