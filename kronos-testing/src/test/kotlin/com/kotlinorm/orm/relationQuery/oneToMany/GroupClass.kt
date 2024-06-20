package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.annotations.Reference
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.CascadeAction

data class GroupClass(
    var id: Int? = null,
    val name: String? = null,
    val groupNo: String? = null,

    var schoolId: Int? = null,

    @Reference(["school_id"], ["id"], CascadeAction.CASCADE, mapperBy = School::class)
    var school: School? = null,

    var students: List<Student>? = null
) : KPojo()