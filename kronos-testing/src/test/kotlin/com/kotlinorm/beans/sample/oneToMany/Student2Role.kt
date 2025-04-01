package com.kotlinorm.beans.sample.oneToMany

import com.kotlinorm.annotations.Cascade
import com.kotlinorm.enums.CascadeDeleteAction
import com.kotlinorm.interfaces.KPojo

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