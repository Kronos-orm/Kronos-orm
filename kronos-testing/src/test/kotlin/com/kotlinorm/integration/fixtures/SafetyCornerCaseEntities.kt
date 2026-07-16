package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.annotations.Version
import com.kotlinorm.enums.KColumnType.BIT
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo

@Table("kt_safety_guarded_row")
data class SafetyGuardedRow(
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

@Table("kt_safety_versioned_account")
@TableIndex("uk_safety_versioned_account_external_id", ["external_id"], type = "UNIQUE")
data class SafetyVersionedAccount(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(VARCHAR, 80)
    var externalId: String? = null,

    @ColumnType(VARCHAR, 80)
    var name: String? = null,

    @ColumnType(INT)
    var score: Int? = null,

    @LogicDelete
    @ColumnType(BIT)
    var deleted: Boolean? = null,

    @Version
    @ColumnType(INT)
    var version: Int? = null,
) : KPojo

data class SafetyGuardedRowRecord(
    val id: Int?,
    val name: String?,
    val score: Int?,
    val status: Int?,
)

data class SafetyVersionedAccountRecord(
    val id: Int?,
    val externalId: String?,
    val name: String?,
    val score: Int?,
    val deleted: Boolean?,
    val version: Int?,
)
