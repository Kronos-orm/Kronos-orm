package com.kotlinorm.beans.sample.databases

import com.kotlinorm.annotations.*
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.SQLite
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("aaa", ["username"], SQLite.KIndexType.UNIQUE)
@TableIndex(
    "bbb",
    columns = ["username", "gender1"],
    type = SQLite.KIndexType.UNIQUE
)
@TableIndex("ccc", columns = ["gender1"])
data class SqlliteUser(
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
    @Necessary
    var createTime: String? = null,
    @UpdateTime
    @Necessary
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    @Necessary
    var deleted: Boolean? = null
) : KPojo