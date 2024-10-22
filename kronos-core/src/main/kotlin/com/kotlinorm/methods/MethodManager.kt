package com.kotlinorm.methods

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.MethodTransformer

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/10/21 15:35
 **/
object MethodManager {
    private val registedMethodTransformers = mutableListOf<MethodTransformer>(
        BasicMethodTransformer
    )

    fun registerValueTransformer(transformer: MethodTransformer) {
        Pair(1,2)
        registedMethodTransformers.add(0, transformer)
    }

    fun getMethodTransformed(
        funcName: String,
        field: Field,
        args: List<Any?>
    ): Field {
        if (registedMethodTransformers.none { it.existMethod(funcName) }) {
            throw IllegalArgumentException("Method $funcName not found")
        }

        return registedMethodTransformers.first { it.existMethod(funcName) }.transform(funcName, field, args)
    }
}