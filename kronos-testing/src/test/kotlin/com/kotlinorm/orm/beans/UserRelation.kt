package com.kotlinorm.orm.beans

import com.kotlinorm.beans.dsl.KPojo

data class UserRelation(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null,
    var id2: Int? = null
) : KPojo()