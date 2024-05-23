package com.kotlinorm.orm.beans

import com.kotlinorm.annotations.*
import com.kotlinorm.beans.dsl.KPojo
import java.util.*

@Table(name = "tb_user")
data class User(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null,
    @UseSerializeResolver
    var habbits: List<String>? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    var createTime: String? = null,
    @UpdateTime
    var updateTime: Date? = null,
    @LogicDelete
    var deleted: Boolean? = null
) : KPojo()