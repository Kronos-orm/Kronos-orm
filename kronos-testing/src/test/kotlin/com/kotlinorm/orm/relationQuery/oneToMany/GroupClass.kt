package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.annotations.ReferenceType
import com.kotlinorm.beans.dsl.KPojo

data class GroupClass(
    var id: Int? = null,
    val name: String? = null,
    val groupNo: String? = null,
    @ReferenceType(Student::class)
    var students: List<Student>? = null
) : KPojo()
