package com.kotlinorm.functions

import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.FunctionTransformer

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/10/21 15:35
 **/
object FunctionManager {
    internal val registedFunctionTransformers = mutableListOf<FunctionTransformer>(
        BasicFunctionTransformer
    )

    fun registerValueTransformer(transformer: FunctionTransformer) {
        registedFunctionTransformers.add(0, transformer)
    }

    fun getMethodTransformed(
        func: FunctionField,
        dbType: DBType,
        showTable: Boolean = false
    ): String {
        val funcName = func.functionName

        if (registedFunctionTransformers.none { it.support(funcName, dbType) }) {
            throw IllegalArgumentException("Method $funcName not found")
        }

        return registedFunctionTransformers.first { it.support(funcName, dbType) }.transform(func, dbType, showTable)
    }
}