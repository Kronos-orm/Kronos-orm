package com.kotlinorm.orm.cascade.oneToMany

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.CascadeDeleteAction

data class GroupClass(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    val name: String? = null, // 班级名
    val groupNo: String? = null,
    var schoolName: String? = null,
    @Cascade(["schoolName"], ["name"], CascadeDeleteAction.SET_NULL)
    var school: School? = null,
    var students: List<Student>? = null
) : KPojo