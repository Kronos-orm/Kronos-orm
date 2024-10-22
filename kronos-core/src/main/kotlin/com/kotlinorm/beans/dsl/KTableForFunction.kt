package com.kotlinorm.beans.dsl

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/10/22 10:36
 **/
class KTableForFunction (
    val functionName: String,
    val field: Field,
    val args: List<Any?> = listOf()
)