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

import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.interfaces.ValueCodecContext
import com.kotlinorm.syntax.render.SqlDialectFamily
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
 * coercion. Strict DECODE still permits database Number normalization because
 * JDBC drivers may expose the same SQL numeric column through different Number
 * implementations. Oracle-family BIT columns also normalize exact numeric 0/1;
 * other strict conversions remain user-codec concerns.
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
        STRING;

        val isNumeric: Boolean
            get() = when (this) {
                INT, LONG, SHORT, FLOAT, DOUBLE, BYTE, BIG_DECIMAL, BIG_INTEGER -> true
                CHAR, BOOLEAN, STRING -> false
            }
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
     * Strict mode disables implicit DECODE coercion but retains required JDBC
     * numeric normalization and ENCODE preparation.
     *
     * @param value non-null scalar source
     * @param request request containing strict mode and effective target
     * @param context logical request context
     * @return whether one built-in scalar coercion is required and allowed
     */
    override fun supports(value: Any, request: ValueConversionRequest, context: ValueCodecContext): Boolean {
        val targetType = request.effectiveTargetType()
        val target = KTypeKey.from(targetType, ignoreTopLevelNullability = true)
        val targetKind = targetKinds[target] ?: return false
        if (targetType.accepts(value)) return false
        if (request.direction == ValueCodecDirection.DECODE && request.strict &&
            !request.allowsStrictDatabaseNormalization(value, targetKind)
        ) {
            return false
        }
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
        val targetKind = targetKinds.getValue(target)
        val strictDatabaseNumber = (value as? Number)
            ?.takeIf { request.isStrictDatabaseNumericNormalization(targetKind) }
        return when (targetKind) {
            TargetKind.INT -> strictDatabaseNumber?.toIntExact()
                ?: value.convertNumber(Number::toInt, String::toInt)
            TargetKind.LONG -> strictDatabaseNumber?.toLongExact()
                ?: value.convertNumber(Number::toLong, String::toLong)
            TargetKind.SHORT -> strictDatabaseNumber?.toShortExact()
                ?: value.convertNumber(Number::toShort, String::toShort)
            TargetKind.FLOAT -> strictDatabaseNumber?.toFloatChecked()
                ?: value.convertNumber(Number::toFloat, String::toFloat)
            TargetKind.DOUBLE -> strictDatabaseNumber?.toDoubleChecked()
                ?: value.convertNumber(Number::toDouble, String::toDouble)
            TargetKind.BYTE -> strictDatabaseNumber?.toByteExact()
                ?: value.convertNumber(Number::toByte, String::toByte)
            TargetKind.CHAR -> if (value is Number) value.toInt().toChar() else value.toString().first()
            TargetKind.BOOLEAN -> value.toBooleanValue()
            TargetKind.BIG_DECIMAL -> strictDatabaseNumber?.toBigDecimalExact() ?: value.toBigDecimalValue()
            TargetKind.BIG_INTEGER -> strictDatabaseNumber?.toBigIntegerExact() ?: value.toBigIntegerValue()
            TargetKind.STRING -> value.toString()
        }
    }

    private inline fun <reified T> typeKey(): KTypeKey =
        KTypeKey.from(typeOf<T>(), ignoreTopLevelNullability = true)

    /**
     * Distinguishes required JDBC representation normalization from optional
     * logical coercion while strict DECODE is active.
     */
    private fun ValueConversionRequest.allowsStrictDatabaseNormalization(
        value: Any,
        targetKind: TargetKind
    ): Boolean {
        if (origin != ValueCodecOrigin.DATABASE || value !is Number) return false
        if (targetKind.isNumeric) return true
        return targetKind == TargetKind.BOOLEAN &&
            field?.type == KColumnType.BIT &&
            dialect?.family == SqlDialectFamily.Oracle &&
            value.isExactBinaryValue()
    }

    /**
     * Selects the range-checked conversion path only for strict JDBC numeric reads.
     */
    private fun ValueConversionRequest.isStrictDatabaseNumericNormalization(targetKind: TargetKind?): Boolean =
        direction == ValueCodecDirection.DECODE &&
            strict &&
            origin == ValueCodecOrigin.DATABASE &&
            targetKind?.isNumeric == true

    private fun <T> Any.convertNumber(fromNumber: Number.() -> T, fromString: String.() -> T): T =
        when (this) {
            is Number -> fromNumber()
            is Boolean -> (if (this) 1 else 0).fromNumber()
            else -> toString().fromString()
        }

    private fun Number.toIntExact(): Int = toBigIntegerExact().intValueExact()

    private fun Number.toLongExact(): Long = toBigIntegerExact().longValueExact()

    private fun Number.toShortExact(): Short = toBigIntegerExact().shortValueExact()

    private fun Number.toByteExact(): Byte = toBigIntegerExact().byteValueExact()

    private fun Number.toFloatChecked(): Float = toFloat().also { result ->
        if (isFiniteValue() && !result.isFinite()) {
            throw ArithmeticException("$this is outside the kotlin.Float range")
        }
    }

    private fun Number.toDoubleChecked(): Double = toDouble().also { result ->
        if (isFiniteValue() && !result.isFinite()) {
            throw ArithmeticException("$this is outside the kotlin.Double range")
        }
    }

    private fun Number.isFiniteValue(): Boolean = when (this) {
        is Float -> isFinite()
        is Double -> isFinite()
        else -> true
    }

    private fun Number.isExactBinaryValue(): Boolean = when (this) {
        is BigDecimal -> compareTo(BigDecimal.ZERO) == 0 || compareTo(BigDecimal.ONE) == 0
        is BigInteger -> this == BigInteger.ZERO || this == BigInteger.ONE
        is Byte, is Short, is Int, is Long -> toLong() == 0L || toLong() == 1L
        is Float -> this == 0F || this == 1F
        is Double -> this == 0.0 || this == 1.0
        else -> false
    }

    private fun Number.toBigIntegerExact(): BigInteger = when (this) {
        is BigInteger -> this
        is BigDecimal -> toBigIntegerExact()
        is Byte, is Short, is Int, is Long -> BigInteger.valueOf(toLong())
        is Float, is Double -> toBigDecimalExact().toBigIntegerExact()
        else -> BigDecimal(toString()).toBigIntegerExact()
    }

    private fun Number.toBigDecimalExact(): BigDecimal = when (this) {
        is BigDecimal -> this
        is BigInteger -> BigDecimal(this)
        is Byte, is Short, is Int, is Long -> BigDecimal.valueOf(toLong())
        is Float, is Double -> BigDecimal.valueOf(toDouble())
        else -> BigDecimal(toString())
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
