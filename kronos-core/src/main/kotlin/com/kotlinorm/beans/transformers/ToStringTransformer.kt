package com.kotlinorm.beans.transformers

import com.kotlinorm.interfaces.ValueTransformer
import kotlin.reflect.KClass

object ToStringTransformer : ValueTransformer {
    override fun isMatch(targetKotlinType: String, superTypesOfValue: List<String>, kClassOfValue: KClass<*>) =
        targetKotlinType == "kotlin.String"

    override fun transform(
        targetKotlinType: String,
        value: Any,
        superTypesOfValue: List<String>,
        dateTimeFormat: String?,
        kClassOfValue: KClass<*>
    ): Any {
        return value.toString()
    }
}