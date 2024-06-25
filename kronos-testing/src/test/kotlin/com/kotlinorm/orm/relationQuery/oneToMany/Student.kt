package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.Reference
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.CascadeDeleteAction

data class Student(
    var id: Int? = null,
    var name: String? = null,
    var studentNo: String? = null,
    var groupClassId: Int? = null,
//    @Reference(["group_class_id"], ["id"], mapperBy = GroupClass::class)
//    @Reference(["group_class_id"], ["id"], CascadeAction.CASCADE, mapperBy = GroupClass::class)
//    @Reference(["group_class_id"], ["id"], CascadeAction.RESTRICT, mapperBy = GroupClass::class)
    @Reference(
        ["group_class_id"],
        ["id"],
        CascadeDeleteAction.SET_DEFAULT,
        defaultValue = ["9"],
        mapperBy = GroupClass::class
    )
//    @Reference(["group_class_id"], ["id"], CascadeAction.SET_NULL, ["9"], mapperBy = GroupClass::class)
    var groupClass: GroupClass? = null,

    var roleId: Int? = null,
    @Reference(["role_id"], ["role_id"], CascadeDeleteAction.SET_NULL, mapperBy = Student2Role::class)
    var role: Student2Role? = null,

    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    val createTime: String? = null
) : KPojo()
