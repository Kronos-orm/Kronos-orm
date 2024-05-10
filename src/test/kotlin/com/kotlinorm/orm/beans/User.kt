package com.kotlinorm.orm.beans

import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.interfaces.KPojo

@Table(name = "tb_user")
data class User(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null,
    @CreateTime
    var createTime: String? = null,
    @UpdateTime
    var updateTime: String? = null,
    @LogicDelete
    var deleted: Boolean? = null
) : KPojo