package com.kotlinorm.beans.sample.databases

import com.kotlinorm.annotations.*
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.enums.Postgres
import java.time.Instant
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("idx_username", ["username"], method = Postgres.KIndexMethod.HASH, concurrently = true)
@TableIndex(
    name = "idx_multi",
    columns = ["id", "username"],
    type = Postgres.KIndexType.UNIQUE,
    method = Postgres.KIndexMethod.BTREE
)
data class PgUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @ColumnType(VARCHAR, 254)
    var username: String? = null,
    @Column("gender1")
    @ColumnType(KColumnType.BIT)
    @Default("true")
    @Necessary
    var gender: Int? = null,
    var age: Int? = 0,
    @ColumnType(type = KColumnType.TIMESTAMP)
    var regTime: Instant? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    @Necessary
    var createTime: String? = null,
    @UpdateTime
    @Necessary
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    @Necessary
    var deleted: Boolean? = null
) : KPojo