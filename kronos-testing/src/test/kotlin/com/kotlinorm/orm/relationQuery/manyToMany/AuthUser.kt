package com.kotlinorm.orm.relationQuery.manyToMany

import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.beans.dsl.KPojo

data class AuthUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var roleId: Int? = null,
    @Cascade(["roleId"], ["id"])
    var role: Role? = null
): KPojo