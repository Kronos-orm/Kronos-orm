package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo

@Table("kt_integration_unique_upsert_user")
@TableIndex("uk_unique_upsert_user_email", ["email"], type = "UNIQUE")
data class IntegrationUniqueUpsertUser(
    @PrimaryKey(identity = true)
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(VARCHAR, 120)
    var email: String? = null,

    @ColumnType(VARCHAR, 80)
    var name: String? = null,

    @ColumnType(INT)
    var score: Int? = null,
) : KPojo

@Table("kt_integration_composite_upsert_user")
@TableIndex("uk_composite_upsert_user_tenant_email", ["tenant_id", "email"], type = "UNIQUE")
data class IntegrationCompositeUpsertUser(
    @PrimaryKey(identity = true)
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(INT)
    var tenantId: Int? = null,

    @ColumnType(VARCHAR, 120)
    var email: String? = null,

    @ColumnType(VARCHAR, 80)
    var name: String? = null,

    @ColumnType(INT)
    var score: Int? = null,
) : KPojo
