package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo

@Table("kt_edge_account")
data class EdgeAccount(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,
    @ColumnType(VARCHAR, 80)
    var name: String? = null,
    @ColumnType(INT)
    var balance: Int? = null,
    @ColumnType(VARCHAR, 20)
    @Default("'active'")
    var state: String? = null,
) : KPojo

data class EdgeAccountRecord(
    val id: Int?,
    val name: String?,
    val balance: Int?,
    val state: String?,
)

@Table("kt_edge_nullable_only")
data class EdgeNullableOnly(
    @ColumnType(VARCHAR, 80)
    var note: String? = null,
) : KPojo

@Table("kt_edge_default_only")
data class EdgeDefaultOnly(
    @PrimaryKey
    @ColumnType(INT)
    @Default("1")
    var id: Int? = null,
    @ColumnType(VARCHAR, 20)
    @Default("'active'")
    var state: String? = null,
    @ColumnType(INT)
    @Default("0")
    var retries: Int? = null,
) : KPojo

@Table("kt_edge_wide_insert_select")
data class EdgeWideInsertSelect(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,
    @ColumnType(VARCHAR, 80)
    var name: String? = null,
    @ColumnType(INT)
    var balance: Int? = null,
    @ColumnType(VARCHAR, 20)
    var state: String? = null,
    @ColumnType(VARCHAR, 80)
    var extraNote: String? = null,
) : KPojo

@Table("kt_edge_tuple_insert_select")
data class EdgeTupleInsertSelect(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,
    @ColumnType(INT)
    var age: Int? = null,
    @ColumnType(VARCHAR, 80)
    var name: String? = null,
) : KPojo

@Table("kt_edge_tail_null_insert_select")
data class EdgeTailNullInsertSelect(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,
    @ColumnType(INT)
    var firstNull: Int? = null,
    @ColumnType(INT)
    var secondNull: Int? = null,
    @ColumnType(VARCHAR, 80)
    var thirdNull: String? = null,
) : KPojo
