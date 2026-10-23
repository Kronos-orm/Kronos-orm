package com.kotlinorm.testfixtures.ddl

import com.kotlinorm.annotations.*
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.KColumnType.INT
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("aaa", ["username"], "UNIQUE")
@TableIndex(
    "bbb",
    columns = ["username", "gender1"],
    type = "UNIQUE"
)
@TableIndex("ccc", columns = ["gender1"])
data class SqliteDdlUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var username: String? = null,
    @Column("gender1")
    @ColumnType(INT)
    @Default("0")
    var gender: Int? = null,
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