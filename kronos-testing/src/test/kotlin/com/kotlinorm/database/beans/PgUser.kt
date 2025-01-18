package com.kotlinorm.database.beans

import com.kotlinorm.annotations.*
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.KColumnType.TINYINT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.enums.Postgres
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
    @ColumnType(TINYINT)
    @Default("0")
    @Necessary
    var gender: Int? = null,
    var age: Int? = 0,
//    @ColumnType(INT)
//    var age: Int? = null,
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