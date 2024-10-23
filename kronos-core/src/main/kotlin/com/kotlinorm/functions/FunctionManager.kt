package com.kotlinorm.functions

import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.exceptions.UnSupportedFunctionException
import com.kotlinorm.interfaces.FunctionTransformer
import com.kotlinorm.interfaces.KronosDataSourceWrapper

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/10/21 15:35
 **/
object FunctionManager {
    private val registeredFunctionTransformers = mutableListOf<FunctionTransformer>(
        BasicFunctionTransformer
    )

    fun registerValueTransformer(transformer: FunctionTransformer) {
        registeredFunctionTransformers.add(0, transformer)
    }

    fun getFunctionTransformed(
        field: FunctionField, dataSource: KronosDataSourceWrapper,
        showTable: Boolean = false
    ): String {
        return registeredFunctionTransformers.firstOrNull { it.support(field.functionName, dataSource.dbType) }
            ?.transform(field, dataSource, showTable)
            ?: throw UnSupportedFunctionException(dataSource.dbType, field.functionName)
    }
}