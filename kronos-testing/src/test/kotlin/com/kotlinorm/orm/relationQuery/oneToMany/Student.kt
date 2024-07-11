package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Reference
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.CascadeDeleteAction

data class Student(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var studentNo: String? = null,
    var schoolName: Int? = null,
    var groupClassName: Int? = null,
    @Reference(
        ["groupClassName", "schoolName"],
        ["name", "schoolName"],
        CascadeDeleteAction.SET_DEFAULT,
        defaultValue = ["9"],
        mapperBy = GroupClass::class
    )
    var groupClass: GroupClass? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    val createTime: String? = null
) : KPojo
