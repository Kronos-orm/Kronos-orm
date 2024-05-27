/**
 * Copyright 2022-2024 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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