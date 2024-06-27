package com.kotlinorm.orm.tableoperationbeans

import com.kotlinorm.annotations.*
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.KColumnType.TINYINT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.enums.Postgres
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("idx_username", ["username"], Postgres.KIndexType.HASH)
@TableIndex(name = "idx_multi", columns = ["id", "username"], type = "BTREE", method = Postgres.KIndexMethod.UNIQUE)
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
) : KPojo()