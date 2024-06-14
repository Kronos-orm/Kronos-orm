package com.kotlinorm.orm.relationQuery

import com.kotlinorm.annotations.Reference
import com.kotlinorm.beans.dsl.KPojo

data class Student(
    var id: Int? = null,
    var name: String? = null,
    var studentNo: String? = null,
    var groupClassId: Int? = null
) : KPojo() {
    @Reference(column = ["group_class_id"], target = ["id"], cascade = false)
    lateinit var groupClass: GroupClass
}