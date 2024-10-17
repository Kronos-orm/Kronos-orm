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

package com.kotlinorm.transformers

import com.kotlinorm.interfaces.ValueTransformer
import kotlin.reflect.KClass

/**
 * Manager for transformers.
 *
 * @author OUSC
 */
object TransformerManager {
    private val registeredValueTransformers = mutableListOf(
        JvmDateTimeTransformer,
        BasicTypeTransformer
    )

    fun registerValueTransformer(transformer: ValueTransformer) {
        registeredValueTransformers.add(0, transformer)
    }

    fun getValueTransformed(
        targetKotlinType: String,
        value: Any,
        superTypes: List<String> = listOf(),
        dateTimeFormat: String? = null,
        kClassOfVal: KClass<*> = value::class
    ): Any {
        if (targetKotlinType in kClassOfVal.qualifiedName + superTypes) return value
        val transformer = (registeredValueTransformers + ToStringTransformer).firstOrNull {
            it.isMatch(targetKotlinType, superTypes, kClassOfVal)
        } ?: return value

        return transformer.transform(
            targetKotlinType,
            value,
            superTypes,
            dateTimeFormat,
            kClassOfVal
        )
    }
}