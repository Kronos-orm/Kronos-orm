package com.kotlinorm.orm.cascade.oneToMany

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.interfaces.KPojo

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/6/20 15:38
 **/
data class School(
    // id，学校id
    @PrimaryKey(identity = true)
    var id: Int? = null,
    /* 学校名 */
    var name: String? = null,
    var groupClass: List<GroupClass>? = null
) : KPojo