package com.kotlinorm.tableOperation.beans

import com.kotlinorm.annotations.*
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.KColumnType.TINYINT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.enums.Postgres
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("idx_username", ["username"], method = Postgres.KIndexType.HASH, concurrently = true)
@TableIndex(
    name = "idx_multi",
    columns = ["id", "username"],
    type = Postgres.KIndexMethod.UNIQUE,
    method = Postgres.KIndexType.BTREE
)
data class PgUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @ColumnType(VARCHAR, 254)
    var username: String? = null,
    @Column("gender1")
    @ColumnType(TINYINT)
    @Default("0")
    @NotNull
    var gender: Int? = null,
    var age: Int? = 0,
//    @ColumnType(INT)
//    var age: Int? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    @NotNull
    var createTime: String? = null,
    @UpdateTime
    @NotNull
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    @NotNull
    var deleted: Boolean? = null
) : KPojo