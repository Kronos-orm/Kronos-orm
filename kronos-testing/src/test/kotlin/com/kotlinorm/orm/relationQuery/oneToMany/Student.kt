package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.Reference
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.CascadeAction.Companion.CASCADE

data class Student(
    var id: Int? = null,
    var name: String? = null,
    var studentNo: String? = null,
    var groupClassId: Int? = null,
    @Reference(["group_class_id"], ["id"], CASCADE, mapperBy = GroupClass::class)
    var groupClass: GroupClass? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    val createTime: String? = null
) : KPojo()
