package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.annotations.Cascade
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
    @Cascade(["studentId"], ["id"], CascadeDeleteAction.SET_NULL)
    val student: Student? = null,
) : KPojo