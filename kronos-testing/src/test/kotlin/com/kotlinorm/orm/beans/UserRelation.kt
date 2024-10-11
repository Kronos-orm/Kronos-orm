package com.kotlinorm.orm.beans

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.interfaces.KPojo

data class UserRelation(
    @PrimaryKey() var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null,
    var id2: Int? = null
) : KPojo