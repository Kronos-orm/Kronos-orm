package com.kotlinorm.orm.relationQuery.manyToMany

import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.beans.dsl.KPojo

data class RolePermissionRelation(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var roleId: Int? = null,
    var permissionId: Int? = null,
    @Cascade(["roleId"], ["id"])
    var role: Role? = null,
    @Cascade(["permissionId"], ["id"])
    var permission: Permission? = null
): KPojo