package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType.BIGINT
import com.kotlinorm.enums.KColumnType.BIT
import com.kotlinorm.enums.KColumnType.DATETIME
import com.kotlinorm.enums.KColumnType.DECIMAL
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo
import java.math.BigDecimal
import java.time.LocalDateTime

@Table("kt_integration_typed_value")
data class IntegrationTypedValue(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(BIGINT)
    var longValue: Long? = null,

    @ColumnType(VARCHAR, 80)
    var textValue: String? = null,

    @ColumnType(BIT)
    var flagValue: Boolean? = null,

    @ColumnType(DECIMAL, 12, 2)
    var decimalValue: BigDecimal? = null,

    @ColumnType(DATETIME)
    var createdAt: LocalDateTime? = null,

    @ColumnType(INT)
    var optionalScore: Int? = null,
) : KPojo

data class IntegrationTypedValueRecord(
    val id: Int?,
    val longValue: Long?,
    val textValue: String?,
    val flagValue: Boolean?,
    val decimalValue: String?,
    val createdAt: LocalDateTime?,
    val optionalScore: Int?,
)

data class IntegrationFunctionRecord(
    val id: Int?,
    val nameLength: Int?,
    val upperName: String?,
    val lowerName: String?,
    val scoreMod: Int?,
)

data class IntegrationAggregateRecord(
    val total: Int?,
    val scoreSum: Long?,
    val minScore: Int?,
    val maxScore: Int?,
)
