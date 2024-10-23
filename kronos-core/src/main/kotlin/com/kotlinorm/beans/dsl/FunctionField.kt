package com.kotlinorm.beans.dsl

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/10/22 10:36
 **/
class FunctionField(
    val functionName: String,
    val fields: List<Pair<Field?, Any?>> = listOf()
) : Field(functionName)