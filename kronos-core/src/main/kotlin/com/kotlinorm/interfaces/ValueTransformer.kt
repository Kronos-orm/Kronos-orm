package com.kotlinorm.interfaces

import kotlin.reflect.KClass

interface ValueTransformer {
    fun isMatch(targetKotlinType: String, superTypesOfValue: List<String>, kClassOfValue: KClass<*>): Boolean

    fun transform(
        targetKotlinType: String,
        value: Any,
        superTypesOfValue: List<String> = listOf(),
        dateTimeFormat: String? = null,
        kClassOfValue: KClass<*> = value::class
    ): Any
}