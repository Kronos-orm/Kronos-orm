package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo

@Table("kt_mutation_dialect_account")
@TableIndex("uk_mutation_dialect_account_external_code", ["external_code"], type = "UNIQUE")
@TableIndex("uk_mutation_dialect_account_tenant_region_email", ["tenant_id", "region", "email"], type = "UNIQUE")
data class MutationDialectCornerCaseAccount(
    @PrimaryKey(identity = true)
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(VARCHAR, 64)
    var externalCode: String? = null,

    @ColumnType(INT)
    var tenantId: Int? = null,

    @ColumnType(VARCHAR, 32)
    var region: String? = null,

    @ColumnType(VARCHAR, 120)
    var email: String? = null,

    @ColumnType(VARCHAR, 80)
    var label: String? = null,

    @ColumnType(INT)
    var score: Int? = null,

    @ColumnType(INT)
    var quota: Int? = null,

    @ColumnType(INT)
    var status: Int? = null,
) : KPojo

@Table("kt_mutation_dialect_ledger")
data class MutationDialectCornerCaseLedger(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(VARCHAR, 64)
    var externalCode: String? = null,

    @ColumnType(INT)
    var tenantId: Int? = null,

    @ColumnType(VARCHAR, 32)
    var region: String? = null,

    @ColumnType(VARCHAR, 120)
    var email: String? = null,

    @ColumnType(INT)
    var amount: Int? = null,

    @ColumnType(INT)
    var score: Int? = null,

    @ColumnType(INT)
    var status: Int? = null,
) : KPojo

@Table("kt_mutation_dialect_identity_archive")
data class MutationDialectCornerCaseIdentityArchive(
    @PrimaryKey(identity = true)
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(VARCHAR, 64)
    var externalCode: String? = null,

    @ColumnType(INT)
    var tenantId: Int? = null,

    @ColumnType(VARCHAR, 32)
    var region: String? = null,

    @ColumnType(VARCHAR, 120)
    var email: String? = null,

    @ColumnType(INT)
    var score: Int? = null,

    @ColumnType(INT)
    var status: Int? = null,
) : KPojo

@Table("kt_mutation_dialect_manual_archive")
data class MutationDialectCornerCaseManualArchive(
    @PrimaryKey
    @ColumnType(INT)
    var archiveId: Int? = null,

    @ColumnType(VARCHAR, 64)
    var externalCode: String? = null,

    @ColumnType(INT)
    var tenantId: Int? = null,

    @ColumnType(VARCHAR, 32)
    var region: String? = null,

    @ColumnType(VARCHAR, 120)
    var email: String? = null,

    @ColumnType(INT)
    var score: Int? = null,

    @ColumnType(INT)
    var status: Int? = null,
) : KPojo

@Table("kt_mutation_dialect_ctas_archive")
data class MutationDialectCornerCaseCtasArchive(
    @Column("externalCode")
    @ColumnType(VARCHAR, 64)
    var externalCode: String? = null,

    @Column("tenantId")
    @ColumnType(INT)
    var tenantId: Int? = null,

    @ColumnType(VARCHAR, 120)
    var email: String? = null,

    @ColumnType(INT)
    var status: Int? = null,
) : KPojo

data class MutationDialectCornerCaseAccountRecord(
    val externalCode: String?,
    val tenantId: Int?,
    val region: String?,
    val email: String?,
    val label: String?,
    val score: Int?,
    val quota: Int?,
    val status: Int?,
)

data class MutationDialectCornerCaseArchiveRecord(
    val externalCode: String?,
    val tenantId: Int?,
    val region: String?,
    val email: String?,
    val score: Int?,
    val status: Int?,
)
