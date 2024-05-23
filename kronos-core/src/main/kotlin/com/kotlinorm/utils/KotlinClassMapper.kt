package com.kotlinorm.utils

import kotlin.reflect.KClass

object KotlinClassMapper {
    internal val kotlinBuiltInClassMap = mapOf(
        "kotlin.Int" to Int::class,
        "kotlin.Long" to Long::class,
        "kotlin.Short" to Short::class,
        "kotlin.Boolean" to Boolean::class,
        "kotlin.String" to String::class,
        "kotlin.Float" to Float::class,
        "kotlin.Double" to Double::class,
        "kotlin.Any" to Any::class,
        "kotlin.collections.List" to List::class,
        "kotlin.collections.Map" to Map::class,
        "kotlin.collections.Set" to Set::class,
        "kotlin.collections.MutableList" to MutableList::class,
        "kotlin.collections.MutableMap" to MutableMap::class,
        "kotlin.collections.MutableSet" to MutableSet::class,
        "kotlin.Array" to Array::class,
        "kotlin.Char" to Char::class
    )

    internal fun String.toKClass(): KClass<*> {
        return kotlinBuiltInClassMap[this] ?: Any::class
    }
}