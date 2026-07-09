package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.enums.KColumnType.BIGINT
import com.kotlinorm.enums.KColumnType.DATETIME
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo
import java.time.LocalDateTime

@Table("kt_schema_sync_user")
data class SchemaSyncUserV1(
    @PrimaryKey(custom = true)
    @ColumnType(VARCHAR, 64)
    var id: String? = null,
    @ColumnType(VARCHAR, 80)
    var name: String? = null,
    @CreateTime
    @ColumnType(DATETIME)
    var createTime: LocalDateTime? = null,
    @UpdateTime
    @ColumnType(DATETIME)
    var updateTime: LocalDateTime? = null,
) : KPojo

@Table("kt_schema_sync_user")
data class SchemaSyncUserV2(
    @PrimaryKey(custom = true)
    @ColumnType(VARCHAR, 64)
    var id: String? = null,
    @ColumnType(VARCHAR, 80)
    var name: String? = null,
    @ColumnType(BIGINT)
    var age: Long? = null,
    @CreateTime
    @ColumnType(DATETIME)
    var createTime: LocalDateTime? = null,
    @UpdateTime
    @ColumnType(DATETIME)
    var updateTime: LocalDateTime? = null,
) : KPojo

@Table("kt_schema_sync_shape")
data class SchemaSyncShapeV1(
    @PrimaryKey(custom = true)
    @ColumnType(VARCHAR, 64)
    var id: String? = null,
    @ColumnType(INT)
    var legacyScore: Int? = null,
    @ColumnType(INT)
    var mutableScore: Int? = null,
    @ColumnType(VARCHAR, 40)
    @Default("'draft'")
    var status: String? = null,
) : KPojo

@Table("kt_schema_sync_shape")
data class SchemaSyncShapeV2(
    @PrimaryKey(custom = true)
    @ColumnType(VARCHAR, 64)
    var id: String? = null,
    @ColumnType(BIGINT)
    var mutableScore: Long? = null,
    @ColumnType(VARCHAR, 40)
    @Default("'active'")
    var status: String? = null,
) : KPojo
