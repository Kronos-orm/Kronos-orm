/**
 * Copyright 2022-2025 kronos-orm
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
package com.kotlinorm.interfaces

import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * ValueTransformer
 *
 * Interface for Value Transformer
 *
 * @author OUSC
 */
interface ValueTransformer {
    fun isMatch(targetKotlinType: String, superTypesOfValue: List<String>, kClassOfValue: KClass<*>): Boolean

    fun isMatch(targetKotlinType: KType, kClassOfValue: KClass<*>): Boolean {
        val targetKClass = targetKotlinType.classifier as? KClass<*> ?: return false
        val targetName = targetKClass.qualifiedName ?: return false
        return isMatch(targetName, emptyList(), kClassOfValue)
    }

    fun transform(
        targetKotlinType: String,
        value: Any,
        superTypesOfValue: List<String> = [],
        dateTimeFormat: String? = null,
        kClassOfValue: KClass<*> = value::class
    ): Any

    fun transform(
        targetKotlinType: KType,
        value: Any,
        dateTimeFormat: String? = null,
        kClassOfValue: KClass<*> = value::class
    ): Any {
        val targetKClass = targetKotlinType.classifier as? KClass<*>
            ?: throw IllegalArgumentException("Invalid type: $targetKotlinType")
        val targetName = targetKClass.qualifiedName ?: targetKClass.simpleName
            ?: throw IllegalArgumentException("Invalid type: $targetKotlinType")
        return transform(targetName, value, emptyList(), dateTimeFormat, kClassOfValue)
    }
}
