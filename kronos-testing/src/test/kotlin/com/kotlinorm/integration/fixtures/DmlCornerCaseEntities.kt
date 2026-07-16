package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo

@Table("kt_dml_corner_account")
@TableIndex("uk_dml_corner_tenant_email", ["tenant_id", "email"], type = "UNIQUE")
data class DmlCornerAccount(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(INT)
    var tenantId: Int? = null,

    @ColumnType(VARCHAR, 120)
    var email: String? = null,

    @ColumnType(VARCHAR, 80)
    var label: String? = null,

    @ColumnType(INT)
    var balance: Int? = null,

    @ColumnType(INT)
    var quota: Int? = null,

    @ColumnType(INT)
    var priority: Int? = null,

    @ColumnType(VARCHAR, 80)
    var note: String? = null,

    @ColumnType(INT)
    var status: Int? = null,
) : KPojo

@Table("kt_dml_corner_ledger")
data class DmlCornerLedger(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(INT)
    var accountId: Int? = null,

    @ColumnType(INT)
    var tenantId: Int? = null,

    @ColumnType(VARCHAR, 24)
    var code: String? = null,

    @ColumnType(INT)
    var amount: Int? = null,

    @ColumnType(INT)
    var score: Int? = null,
) : KPojo

@Table("kt_dml_corner_snapshot")
data class DmlCornerSnapshot(
    @PrimaryKey(identity = true)
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(INT)
    var accountId: Int? = null,

    @ColumnType(INT)
    var tenantId: Int? = null,

    @ColumnType(VARCHAR, 120)
    var email: String? = null,

    @ColumnType(VARCHAR, 80)
    var label: String? = null,

    @ColumnType(INT)
    var status: Int? = null,

    @ColumnType(VARCHAR, 80)
    @Default("'snapshot-default'")
    var note: String? = null,
) : KPojo

data class DmlCornerAccountRecord(
    val id: Int?,
    val tenantId: Int?,
    val email: String?,
    val label: String?,
    val balance: Int?,
    val quota: Int?,
    val priority: Int?,
    val note: String?,
    val status: Int?,
)

data class DmlCornerSnapshotRecord(
    val id: Int?,
    val accountId: Int?,
    val tenantId: Int?,
    val email: String?,
    val label: String?,
    val status: Int?,
    val note: String?,
)
