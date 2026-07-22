/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm.utils.codec

import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.interfaces.ValueCodecContext
import com.kotlinorm.utils.GeneratedTypeMetadataSnapshot
import com.kotlinorm.utils.KTypeKey
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.typeOf

/**
 * Handles built-in numeric, boolean, character, decimal and string coercion.
 *
 * This codec is available only for [com.kotlinorm.enums.ValueStorage.NONE]. It
 * accepts a non-assignable value only where the request permits implicit
 * coercion; serialized values and strict DECODE requests are left to user or
 * other built-in codecs.
 */
internal object BasicValueCodec : RegistryCodec {
    override val description: String = "basic ValueCodec"

    private enum class TargetKind {
        INT,
        LONG,
        SHORT,
        FLOAT,
        DOUBLE,
        BYTE,
        CHAR,
        BOOLEAN,
        BIG_DECIMAL,
        BIG_INTEGER,
        STRING
    }

    private val targetKinds = mapOf(
        typeKey<Int>() to TargetKind.INT,
        typeKey<Long>() to TargetKind.LONG,
        typeKey<Short>() to TargetKind.SHORT,
        typeKey<Float>() to TargetKind.FLOAT,
        typeKey<Double>() to TargetKind.DOUBLE,
        typeKey<Byte>() to TargetKind.BYTE,
        typeKey<Char>() to TargetKind.CHAR,
        typeKey<Boolean>() to TargetKind.BOOLEAN,
        typeKey<BigDecimal>() to TargetKind.BIG_DECIMAL,
        typeKey<BigInteger>() to TargetKind.BIG_INTEGER,
        typeKey<String>() to TargetKind.STRING
    )

    /**
     * Matches supported scalar targets only when conversion is required.
     * Strict mode disables implicit DECODE coercion but not required ENCODE preparation.
     *
     * @param value non-null scalar source
     * @param request request containing strict mode and effective target
     * @param context logical request context
     * @return whether one built-in scalar coercion is required and allowed
     */
    override fun supports(value: Any, request: ValueConversionRequest, context: ValueCodecContext): Boolean {
        if (request.direction == ValueCodecDirection.DECODE && request.strict) return false
        val targetType = request.effectiveTargetType()
        val target = KTypeKey.from(targetType, ignoreTopLevelNullability = true)
        val targetKind = targetKinds[target] ?: return false
        if (targetType.accepts(value)) return false
        return targetKind != TargetKind.STRING || value.isBindableScalar()
    }

    /**
     * Converts one value to the effective scalar target exactly once.
     *
     * ENCODE may use the physical JDBC target, while DECODE uses the logical
     * target. The registry validates the returned scalar and adds request context
     * when a conversion throws.
     *
     * @param value source previously accepted by [supports]
     * @param request request supplying the exact effective scalar target
     * @param context logical request context
     * @param generatedTypes unused generated metadata snapshot
     * @return scalar assignable to the effective target
     */
    override fun convert(
        value: Any,
        request: ValueConversionRequest,
        context: ValueCodecContext,
        generatedTypes: GeneratedTypeMetadataSnapshot?
    ): Any {
        val targetType = request.effectiveTargetType()
        val target = KTypeKey.from(targetType, ignoreTopLevelNullability = true)
        return when (targetKinds[target]) {
            TargetKind.INT -> value.convertNumber(Number::toInt, String::toInt)
            TargetKind.LONG -> value.convertNumber(Number::toLong, String::toLong)
            TargetKind.SHORT -> value.convertNumber(Number::toShort, String::toShort)
            TargetKind.FLOAT -> value.convertNumber(Number::toFloat, String::toFloat)
            TargetKind.DOUBLE -> value.convertNumber(Number::toDouble, String::toDouble)
            TargetKind.BYTE -> value.convertNumber(Number::toByte, String::toByte)
            TargetKind.CHAR -> if (value is Number) value.toInt().toChar() else value.toString().first()
            TargetKind.BOOLEAN -> value.toBooleanValue()
            TargetKind.BIG_DECIMAL -> value.toBigDecimalValue()
            TargetKind.BIG_INTEGER -> value.toBigIntegerValue()
            TargetKind.STRING -> value.toString()
            null -> error("Unsupported basic target $targetType")
        }
    }

    private inline fun <reified T> typeKey(): KTypeKey =
        KTypeKey.from(typeOf<T>(), ignoreTopLevelNullability = true)

    private fun <T> Any.convertNumber(fromNumber: Number.() -> T, fromString: String.() -> T): T =
        when (this) {
            is Number -> fromNumber()
            is Boolean -> (if (this) 1 else 0).fromNumber()
            else -> toString().fromString()
        }

    private fun Any.toBooleanValue(): Boolean = when (this) {
        is BigDecimal -> compareTo(BigDecimal.ZERO) != 0
        is BigInteger -> this != BigInteger.ZERO
        is Byte, is Short, is Int, is Long -> (this as Number).toLong() != 0L
        is Float, is Double -> (this as Number).toDouble() != 0.0
        is Number -> BigDecimal(toString()).compareTo(BigDecimal.ZERO) != 0
        else -> toString().trim().let { text ->
            when (text) {
                "1" -> true
                "0", "" -> false
                else -> text.toBoolean()
            }
        }
    }

    private fun Any.toBigDecimalValue(): BigDecimal = when (this) {
        is BigDecimal -> this
        is BigInteger -> BigDecimal(this)
        is Boolean -> if (this) BigDecimal.ONE else BigDecimal.ZERO
        is Byte, is Short, is Int, is Long -> BigDecimal.valueOf((this as Number).toLong())
        is Float, is Double -> BigDecimal.valueOf((this as Number).toDouble())
        else -> BigDecimal(toString())
    }

    private fun Any.toBigIntegerValue(): BigInteger = when (this) {
        is BigInteger -> this
        is BigDecimal -> toBigInteger()
        is Boolean -> if (this) BigInteger.ONE else BigInteger.ZERO
        is Byte, is Short, is Int, is Long -> BigInteger.valueOf((this as Number).toLong())
        is Number -> BigDecimal(toString()).toBigInteger()
        else -> BigInteger(toString())
    }
}
