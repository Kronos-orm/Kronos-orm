package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.beans.dsl.KPojo
import org.jetbrains.annotations.NotNull

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/6/20 15:38
 **/
data class School(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @NotNull
    var name: String? = null,
    var groupClass: List<GroupClass>? = null
) : KPojo()