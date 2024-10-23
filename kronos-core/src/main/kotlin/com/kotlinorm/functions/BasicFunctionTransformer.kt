package com.kotlinorm.functions

import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.database.SqlManager.getBasicMethodFunction
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.FunctionTransformer

object BasicFunctionTransformer : FunctionTransformer {

    private val definedMethods = listOf(
        "count", "average", "sum", "max", "min"
    )

    override fun support(funcName: String, dbType: DBType): Boolean {
        return funcName in definedMethods
    }

    override fun transform(func: FunctionField, dbType: DBType, showTable: Boolean): String {
        return getBasicMethodFunction(func, dbType, showTable)
    }

}