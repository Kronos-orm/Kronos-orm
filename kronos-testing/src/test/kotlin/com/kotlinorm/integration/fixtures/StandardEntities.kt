package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo

@Table("kt_integration_user")
data class IntegrationUser(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(VARCHAR, 80)
    var name: String? = null,

    @ColumnType(INT)
    var score: Int? = null,

    @ColumnType(INT)
    var status: Int? = null,
) : KPojo

@Table("kt_integration_order")
data class IntegrationOrder(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(INT)
    var userId: Int? = null,

    @ColumnType(INT)
    var status: Int? = null,

    @ColumnType(INT)
    var amount: Int? = null,
) : KPojo

@Table("kt_integration_archive")
data class IntegrationArchive(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(INT)
    var userId: Int? = null,

    @ColumnType(INT)
    var amount: Int? = null,

    @ColumnType(INT)
    var status: Int? = null,
) : KPojo
