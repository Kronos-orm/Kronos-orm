package com.kotlinorm.orm.beans

import com.kotlinorm.annotations.*
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.KColumnType.CHAR
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.TEXT
import com.kotlinorm.enums.Mysql
import com.kotlinorm.enums.SQLite
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("aaa", ["username"], SQLite.KIndexType.BINARY, SQLite.KIndexMethod.UNIQUE)
@TableIndex(  "bbb",columns = ["username","gender"], type = SQLite.KIndexType.NOCASE, method = SQLite.KIndexMethod.UNIQUE)
@TableIndex(  "ccc",columns = ["gender"])
data class SqlliteUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var username: String? = null,
    @Column("gender")
    @ColumnType(INT)
    @Default("0")
    var gender: Int? = null,
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