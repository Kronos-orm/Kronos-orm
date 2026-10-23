package com.kotlinorm.testfixtures.ddl

import com.kotlinorm.annotations.*
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.KColumnType.TINYINT
import com.kotlinorm.enums.KColumnType.VARCHAR
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("idx_username", ["username"], method = "HASH", concurrently = true)
@TableIndex(
    name = "idx_multi",
    columns = ["id", "username"],
    type = "UNIQUE",
    method = "BTREE"
)
data class PostgresDdlUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @ColumnType(VARCHAR, 254)
    var username: String? = null,
    @Column("gender1")
    @ColumnType(TINYINT)
    @Default("0")
    @NonNull
    var gender: Int? = null,
    var age: Int? = 0,
//    @ColumnType(INT)
//    var age: Int? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    @NonNull
    var createTime: String? = null,
    @UpdateTime
    @NonNull
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    @NonNull
    var deleted: Boolean? = null
) : KPojo