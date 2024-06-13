package com.kotlinorm.orm.beans

import com.kotlinorm.annotations.*
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.KColumnType.CHAR
import com.kotlinorm.enums.Mysql
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("idx_username", ["username"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var username: String? = null,
    @Column("gender")
    @ColumnType(CHAR)
    @Default("0")
    var gender: Int? = null,
//    @ColumnType(INT)
//    var age: Int? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    var createTime: String? = null,
    @UpdateTime
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    var deleted: Boolean? = null
) : KPojo()