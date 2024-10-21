package com.kotlinorm.interfaces

import com.kotlinorm.beans.dsl.Field
import kotlin.reflect.KClass

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/10/21 16:05
 **/
interface MethodTransformer {

    fun existMethod(funcName: String): Boolean

    fun transform(
        funcName: String,
        field: Field,
        args: List<Any?>
    ): String

}