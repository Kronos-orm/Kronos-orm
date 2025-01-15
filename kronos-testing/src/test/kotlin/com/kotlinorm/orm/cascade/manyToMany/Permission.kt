package com.kotlinorm.orm.cascade.manyToMany

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.beans.dsl.KCascade.Companion.manyToMany
import com.kotlinorm.interfaces.KPojo

data class Permission(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var rolePermissions: List<RolePermissionRelation>? = null
) : KPojo {
    val roles: List<Role> by manyToMany(::rolePermissions)
}

