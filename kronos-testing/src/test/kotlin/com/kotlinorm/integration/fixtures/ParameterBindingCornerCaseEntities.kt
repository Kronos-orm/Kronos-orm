package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType.BLOB
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo

@Table("kt_parameter_binding_case")
data class ParameterBindingCornerCaseValue(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(VARCHAR, 120)
    var label: String? = null,

    @ColumnType(INT)
    var score: Int? = null,

    @ColumnType(VARCHAR, 500)
    var note: String? = null,

    @ColumnType(INT)
    var optionalScore: Int? = null,

    @ColumnType(BLOB)
    var binaryPayload: ByteArray? = null,
) : KPojo

data class ParameterBindingCornerCaseRecord(
    val id: Int?,
    val label: String?,
    val score: Int?,
    val note: String?,
    val optionalScore: Int?,
)
