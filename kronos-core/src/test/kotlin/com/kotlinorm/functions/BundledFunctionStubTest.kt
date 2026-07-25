package com.kotlinorm.functions

import com.kotlinorm.functions.bundled.exts.MathFunctions
import com.kotlinorm.functions.bundled.exts.PostgresFunctions
import com.kotlinorm.functions.bundled.exts.StringFunctions
import kotlin.test.Test
import kotlin.test.assertEquals

class BundledFunctionStubTest {

    @Test
    fun `bin stub has a binary text result type`() {
        val value: String? = with(MathFunctions) { FunctionHandler.bin(1) }

        assertEquals(null, value)
    }

    @Test
    fun `math function runtime stubs return null`() {
        val values = with(MathFunctions) {
            listOf(
                FunctionHandler.abs(1),
                FunctionHandler.bin(1),
                FunctionHandler.ceil(1),
                FunctionHandler.exp(1),
                FunctionHandler.floor(1),
                FunctionHandler.greatest(1, 2, 3),
                FunctionHandler.least(1, 2, 3),
                FunctionHandler.ln(1),
                FunctionHandler.log(10, 2),
                FunctionHandler.mod(10, 3),
                FunctionHandler.pi(),
                FunctionHandler.rand(),
                FunctionHandler.round(10, 2),
                FunctionHandler.sign(-1),
                FunctionHandler.sqrt(4),
                FunctionHandler.trunc(10, 2),
                FunctionHandler.add(1, 2, 3),
                FunctionHandler.sub(1, 2, 3),
                FunctionHandler.mul(1, 2, 3),
                FunctionHandler.div(1, 2, 3)
            )
        }

        assertEquals(List(20) { null }, values)
    }

    @Test
    fun `string function runtime stubs return null`() {
        val values = with(StringFunctions) {
            listOf(
                FunctionHandler.length("abc"),
                FunctionHandler.upper("abc"),
                FunctionHandler.lower("ABC"),
                FunctionHandler.substr("abc", 1, 2),
                FunctionHandler.replace("abc", "a", "b"),
                FunctionHandler.left("abc", 1),
                FunctionHandler.right("abc", 1),
                FunctionHandler.repeat("abc", 2),
                FunctionHandler.reverse("abc"),
                FunctionHandler.trim(" abc "),
                FunctionHandler.ltrim(" abc"),
                FunctionHandler.rtrim("abc "),
                FunctionHandler.concat("a", "b", "c"),
                FunctionHandler.join(",", "a", "b", "c")
            )
        }

        assertEquals(List(14) { null }, values)
    }

    @Test
    fun `postgres any all runtime stubs return null`() {
        val values = with(PostgresFunctions) {
            listOf(
                FunctionHandler.any(listOf(1)),
                FunctionHandler.all(listOf(1)),
                FunctionHandler.any(arrayOf(1)),
                FunctionHandler.all(arrayOf(1)),
                FunctionHandler.any(intArrayOf(1)),
                FunctionHandler.all(intArrayOf(1)),
                FunctionHandler.any(longArrayOf(1L)),
                FunctionHandler.all(longArrayOf(1L)),
                FunctionHandler.any(shortArrayOf(1)),
                FunctionHandler.all(shortArrayOf(1)),
                FunctionHandler.any(1.toByte()),
                FunctionHandler.all(1.toByte()),
                FunctionHandler.any(floatArrayOf(1f)),
                FunctionHandler.all(floatArrayOf(1f)),
                FunctionHandler.any(doubleArrayOf(1.0)),
                FunctionHandler.all(doubleArrayOf(1.0)),
                FunctionHandler.any(booleanArrayOf(true)),
                FunctionHandler.all(booleanArrayOf(true))
            )
        }

        assertEquals(List(18) { null }, values)
    }
}
