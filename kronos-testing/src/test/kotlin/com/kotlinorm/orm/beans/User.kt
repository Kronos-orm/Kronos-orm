package com.kotlinorm.orm.beans

import com.kotlinorm.annotations.*
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.KColumnType.CHAR
import java.util.*

@Table(name = "tb_user")
data class User(
    @PrimaryKey
    var id: Int? = null,
    var username: String? = null,
    @Column("gender")
    @ColumnType(CHAR)
    var gender: Int? = null,
//    @ColumnType(INT)
//    var age: Int? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    var createTime: String? = null,
    @UpdateTime
    var updateTime: Date? = null,
    @LogicDelete
    var deleted: Boolean? = null
) : KPojo()