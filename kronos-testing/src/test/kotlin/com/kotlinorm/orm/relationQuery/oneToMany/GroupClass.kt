package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.annotations.NotNull
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.CascadeDeleteAction

data class GroupClass(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    val name: String? = null,
    val groupNo: String? = null,
    @NotNull
    var schoolName: String? = null,
    @Cascade(["schoolName"], ["name"], CascadeDeleteAction.CASCADE)
    var school: School? = null,

    var students: List<Student>? = null
) : KPojo