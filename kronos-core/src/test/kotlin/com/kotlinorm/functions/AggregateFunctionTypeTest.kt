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

package com.kotlinorm.functions

import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.avg
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.max
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.min
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies aggregate function Kotlin signatures used by IDE type inference and FIR projection generation.
 */
class AggregateFunctionTypeTest {
    @Test
    fun `aggregate functions expose precise kotlin result types`() {
        val byteValue: Byte? = 1
        val shortValue: Short? = 1
        val intValue: Int? = 1
        val longValue: Long? = 1L
        val floatValue: Float? = 1.0f
        val doubleValue: Double? = 1.0
        val decimalValue: BigDecimal? = BigDecimal.ONE

        val count: Long? = FunctionHandler.count(1)
        val sumByte: Long? = FunctionHandler.sum(byteValue)
        val sumShort: Long? = FunctionHandler.sum(shortValue)
        val sumInt: Long? = FunctionHandler.sum(intValue)
        val sumLong: Long? = FunctionHandler.sum(longValue)
        val sumFloat: Double? = FunctionHandler.sum(floatValue)
        val sumDouble: Double? = FunctionHandler.sum(doubleValue)
        val sumDecimal: BigDecimal? = FunctionHandler.sum(decimalValue)
        val avg: BigDecimal? = FunctionHandler.avg(intValue)
        val max: Int? = FunctionHandler.max(intValue)
        val min: Int? = FunctionHandler.min(intValue)

        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        assertTrue(
            listOf(
                count,
                sumByte,
                sumShort,
                sumInt,
                sumLong,
                sumFloat,
                sumDouble,
                sumDecimal,
                avg,
                max,
                min
            ).all { it == null }
        )
    }
}
