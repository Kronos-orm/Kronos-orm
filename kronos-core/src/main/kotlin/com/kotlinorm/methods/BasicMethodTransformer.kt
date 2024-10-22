package com.kotlinorm.methods

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForFunction
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.MethodTransformer

object BasicMethodTransformer : MethodTransformer {

    private val definedMethods = listOf(
        "count"
    )

    override fun support(funcName: String, dbType: DBType): Boolean {
        return funcName in definedMethods
    }

    override fun transform(func: KTableForFunction, dbType: DBType): Field {
        TODO("Not yet implemented")
    }

    private fun mysqlMethods(func: KTableForFunction): Field {
        return when (func.functionName) {
            "count" -> Field(func.functionName, func.field.columnName, KColumnType.INT)
            else -> throw IllegalArgumentException("Method ${func.functionName} not found")
        }
    }

}