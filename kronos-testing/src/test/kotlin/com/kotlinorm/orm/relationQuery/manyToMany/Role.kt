package com.kotlinorm.orm.relationQuery.manyToMany

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.beans.dsl.KCascade.Companion.manyToMany
import com.kotlinorm.beans.dsl.KPojo

data class Role(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var rolePermissions: List<RolePermissionRelation>? = null,
): KPojo{
    var permissions: List<Permission> by manyToMany(::rolePermissions)
}