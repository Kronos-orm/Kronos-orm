package com.kotlinorm.methods

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.MethodTransformer

object BasicMethodTransformer : MethodTransformer {

    private val definedMethod = listOf(
        "count"
    )

    override fun existMethod(funcName: String) = funcName in definedMethod

    private fun count(field: Field): Field {
        return Field("COUNT(${field.columnName})")
    }

    override fun transform(funcName: String, field: Field, args: List<Any?>): Field {
        return when(funcName) {
            "count" -> count(field)
            else -> throw IllegalArgumentException("Method $funcName not found")
        }
    }
}