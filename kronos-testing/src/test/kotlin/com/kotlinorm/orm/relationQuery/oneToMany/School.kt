package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.annotations.SelectIgnore
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.beans.dsl.KPojo

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/6/20 15:38
 **/
data class School(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    @SelectIgnore
    var groupClass: List<GroupClass>? = null
) : KPojo