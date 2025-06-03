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