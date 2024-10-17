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
 * Transformer for basic types.
 *
 * @author OUSC
 */
object BasicTypeTransformer : ValueTransformer {
    private val basicTypes = listOf(
        "kotlin.Int",
        "kotlin.Long",
        "kotlin.Double",
        "kotlin.Float",
        "kotlin.Boolean",
        "kotlin.Char",
        "kotlin.Byte",
        "kotlin.Short"
    )

    /**
     * Safely casts the value to the target type.
     *
     * @param fromNumber A lambda to convert from a Number.
     * @param fromStr A lambda to convert from a String.
     * @return The value cast to the target type.
     */
    private fun <T> Any.safeCast(fromNumber: Number.() -> T, fromStr: String.() -> T): T {
        return if (this is Number) {
            fromNumber()
        } else {
            toString().fromStr()
        }
    }

    private fun Any.toChar(): Char {
        return if (this is Number) {
            toInt().toChar()
        } else {
            toString().first()
        }
    }

    override fun isMatch(targetKotlinType: String, superTypesOfValue: List<String>, kClassOfValue: KClass<*>): Boolean {
        return targetKotlinType in basicTypes
    }

    override fun transform(
        targetKotlinType: String,
        value: Any,
        superTypesOfValue: List<String>,
        dateTimeFormat: String?,
        kClassOfValue: KClass<*>
    ): Any {
        return when (targetKotlinType) {
            "kotlin.Int" -> value.safeCast(Number::toInt, String::toInt)
            "kotlin.Long" -> value.safeCast(Number::toLong, String::toLong)
            "kotlin.Short" -> value.safeCast(Number::toShort, String::toShort)
            "kotlin.Float" -> value.safeCast(Number::toFloat, String::toFloat)
            "kotlin.Double" -> value.safeCast(Number::toDouble, String::toDouble)
            "kotlin.Byte" -> value.safeCast(Number::toByte, String::toByte)
            "kotlin.Char" -> value.toChar()
            "kotlin.Boolean" -> (value is Number && value != 0) || value.toString().ifBlank { "false" }.toBoolean()
            else -> null
        } ?: throw IllegalArgumentException("Invalid type: $targetKotlinType")
    }
}