package com.kotlinorm.orm.beans

import com.kotlinorm.annotations.*
import com.kotlinorm.beans.dsl.KPojo
import java.sql.Date

@Table(name = "tb_user")
data class User(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null,
    @CreateTime
    var createTime: String? = null,
    @UpdateTime
    @DateTimeFormat("YYYY-MM-dd HH:mm:ss")
    var updateTime: Date? = null,
    @LogicDelete
    var deleted: Boolean? = null
) : KPojo()