package com.kotlinorm.interfaces

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForFunction
import com.kotlinorm.enums.DBType

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/10/21 16:05
 **/
interface MethodTransformer {

    fun support(funcName: String, dbType: DBType): Boolean

    fun transform(func: KTableForFunction, dbType: DBType): Field

}