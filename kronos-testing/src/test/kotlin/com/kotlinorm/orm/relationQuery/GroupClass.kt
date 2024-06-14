package com.kotlinorm.orm.relationQuery

import com.kotlinorm.annotations.ReferenceType
import com.kotlinorm.beans.dsl.KPojo

data class GroupClass(
    var id: Int? = null,
    val name: String? = null,
    val groupNo: String? = null,
) : KPojo() {
    @ReferenceType(Student::class)
    lateinit var students: List<Student>
}
