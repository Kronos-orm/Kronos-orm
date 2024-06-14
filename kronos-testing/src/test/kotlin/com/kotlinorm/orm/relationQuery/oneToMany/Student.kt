package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.annotations.Reference
import com.kotlinorm.beans.dsl.KPojo

data class Student(
    var id: Int? = null,
    var name: String? = null,
    var studentNo: String? = null,
    var groupClassId: Int? = null,
    @Reference(["group_class_id"], ["id"])
    var groupClass: GroupClass? = null
) : KPojo()