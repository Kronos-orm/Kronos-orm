package com.kotlinorm.orm.beans

import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.interfaces.KPojo

@Table(name = "tb_user")
data class User(
    var id: Int? = null,
    var username: String? = null,
    @UpdateTime(format = "YYYY-MM-dd") var gender: Int? = null
) : KPojo