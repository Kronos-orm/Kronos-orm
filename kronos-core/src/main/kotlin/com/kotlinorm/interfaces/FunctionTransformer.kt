package com.kotlinorm.interfaces

import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.enums.DBType

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/10/21 16:05
 **/
interface FunctionTransformer {

    fun support(funcName: String, dbType: DBType): Boolean

    fun transform(func: FunctionField, dbType: DBType, showTable: Boolean = false): String

}