package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Reference
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.CascadeDeleteAction
import org.jetbrains.annotations.NotNull

data class GroupClass(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    val name: String? = null,
    val groupNo: String? = null,
    @NotNull
    var schoolName: String? = null,
    @Reference(["school_name"], ["name"], CascadeDeleteAction.CASCADE, mapperBy = School::class)
    var school: School? = null,
    var students: List<Student>? = null
) : KPojo()