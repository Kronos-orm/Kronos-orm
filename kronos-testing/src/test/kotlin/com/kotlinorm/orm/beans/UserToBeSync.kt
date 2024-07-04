package com.kotlinorm.orm.beans

import com.kotlinorm.annotations.*
import com.kotlinorm.beans.dsl.KPojo

@Table(name = "tb_user")
data class UserToBeSync(
    @PrimaryKey
    var id: Int? = null,
    var username: String? = null,
    var gender: String? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    var createTime: String? = null,
    @UpdateTime
    var updateTime: String? = null,
    @LogicDelete
    var deleted: String? = null
) : KPojo