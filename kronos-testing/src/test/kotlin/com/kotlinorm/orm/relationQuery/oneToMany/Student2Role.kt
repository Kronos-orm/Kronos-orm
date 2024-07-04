package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.annotations.Reference
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.CascadeDeleteAction

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/6/20 17:49
 **/
data class Student2Role(
    val id: Int? = null,

    val studentId: Int? = null,
    @Reference(["student_id"], ["id"], CascadeDeleteAction.SET_NULL, mapperBy = Student2Role::class)
    val student: Student? = null,
) : KPojo