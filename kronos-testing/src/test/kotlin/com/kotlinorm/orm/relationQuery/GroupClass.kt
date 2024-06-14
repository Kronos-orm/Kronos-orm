package com.kotlinorm.orm.relationQuery

import com.kotlinorm.beans.dsl.KPojo

data class GroupClass(
    var id: Int? = null,
    val name: String? = null,
    val groupNo: String? = null,
) : KPojo() {
    lateinit var students: List<Student>
}
