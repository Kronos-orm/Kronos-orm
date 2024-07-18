package com.kotlinorm.orm.relationQuery.oneToMany

import com.kotlinorm.beans.dsl.KPojo

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/6/20 17:48
 **/
data class Role(
    var id: Int,
    var name: String
) : KPojo