package com.kotlinorm.methods

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForFunction
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.MethodTransformer

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/10/21 15:35
 **/
object MethodManager {
    internal val registedMethodTransformers = mutableListOf<MethodTransformer>(
        BasicMethodTransformer
    )

    fun registerValueTransformer(transformer: MethodTransformer) {
        registedMethodTransformers.add(0, transformer)
    }

    fun getMethodTransformed(
        func: KTableForFunction,
        dbType: DBType
    ): Field {
        val funcName = func.functionName

        if (registedMethodTransformers.none { it.support(funcName, dbType) }) {
            throw IllegalArgumentException("Method $funcName not found")
        }

        return registedMethodTransformers.first { it.support(funcName, dbType) }.transform(func, dbType)
    }
}