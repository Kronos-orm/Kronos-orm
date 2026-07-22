package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType.BIGINT
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.TEXT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
enum class IntegrationValueMappingStatus {
    READY,
    ARCHIVED,
}

@Table("kt_integration_value_mapping")
data class IntegrationValueMappingRow(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(VARCHAR, 32)
    var status: IntegrationValueMappingStatus = IntegrationValueMappingStatus.READY,

    @ColumnType(VARCHAR, 32)
    var nullableStatus: IntegrationValueMappingStatus? = null,

    @Serialize
    @ColumnType(TEXT)
    var history: List<IntegrationValueMappingStatus?>? = null,

    @DateTimeFormat("dd/MM/yyyy HH:mm:ss")
    @ColumnType(VARCHAR, 32)
    var occurredAt: LocalDateTime? = null,
) : KPojo

data class IntegrationValueMappingRecord(
    val id: Int?,
    val status: IntegrationValueMappingStatus,
    val nullableStatus: IntegrationValueMappingStatus?,
    val history: List<IntegrationValueMappingStatus?>?,
    val occurredAt: LocalDateTime?,
)

enum class IntegrationNumericEnumStatus {
    ACTIVE,
    ARCHIVED,
}

@Table("kt_integration_numeric_enum")
data class IntegrationNumericEnumRow(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(INT)
    var intStatus: IntegrationNumericEnumStatus = IntegrationNumericEnumStatus.ACTIVE,

    @ColumnType(BIGINT)
    var bigStatus: IntegrationNumericEnumStatus? = null,
) : KPojo
