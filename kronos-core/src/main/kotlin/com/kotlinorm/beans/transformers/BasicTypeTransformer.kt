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
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

/**
 * Transformer for basic types.
 *
 * @author OUSC
 */
object BasicTypeTransformer : ValueTransformer {
    private val basicTypes = [
        "kotlin.Int",
        "kotlin.Long",
        "kotlin.Double",
        "kotlin.Float",
        "kotlin.Boolean",
        "kotlin.Char",
        "kotlin.Byte",
        "kotlin.Short",
        "java.math.BigDecimal",
        "java.math.BigInteger"
    ]

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

    private fun Any.toBigDecimal(): BigDecimal = when (this) {
        is BigDecimal -> this
        is BigInteger -> BigDecimal(this)
        is Byte,
        is Short,
        is Int,
        is Long -> BigDecimal.valueOf((this as Number).toLong())

        is Float,
        is Double -> BigDecimal.valueOf((this as Number).toDouble())

        is Number -> BigDecimal(toString())
        else -> BigDecimal(toString())
    }

    private fun Any.toBigInteger(): BigInteger = when (this) {
        is BigInteger -> this
        is BigDecimal -> toBigInteger()
        is Byte,
        is Short,
        is Int,
        is Long -> BigInteger.valueOf((this as Number).toLong())

        is Number -> BigDecimal(toString()).toBigInteger()
        else -> BigInteger(toString())
    }

    private fun Number.toBooleanValue(): Boolean = when (this) {
        is BigDecimal -> compareTo(BigDecimal.ZERO) != 0
        is BigInteger -> this != BigInteger.ZERO
        is Byte,
        is Short,
        is Int,
        is Long -> toLong() != 0L

        is Float,
        is Double -> toDouble() != 0.0

        else -> BigDecimal(toString()).compareTo(BigDecimal.ZERO) != 0
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
            "kotlin.Boolean" -> when (value) {
                is Number -> value.toBooleanValue()
                else -> value.toString().trim().let {
                    when {
                        it == "1" -> true
                        it == "0" -> false
                        else -> it.ifBlank { "false" }.toBoolean()
                    }
                }
            }
            "java.math.BigDecimal" -> value.toBigDecimal()
            "java.math.BigInteger" -> value.toBigInteger()
            else -> null
        } ?: throw IllegalArgumentException("Invalid type: $targetKotlinType")
    }
}
