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

package com.kotlinorm.functions.bundled.exts

import com.kotlinorm.functions.FunctionHandler
import java.math.BigDecimal

@Suppress("unused")
object PolymerizationFunctions {
    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.count(x: Any?): Long? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.sum(x: Byte?): Long? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.sum(x: Short?): Long? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.sum(x: Int?): Long? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.sum(x: Long?): Long? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.sum(x: Float?): Double? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.sum(x: Double?): Double? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.sum(x: BigDecimal?): BigDecimal? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.sum(x: Number?): BigDecimal? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.sum(x: Any?): BigDecimal? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.avg(x: Any?): BigDecimal? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun <T : Comparable<T>> FunctionHandler.max(x: T?): T? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.max(x: Any?): Any? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun <T : Comparable<T>> FunctionHandler.min(x: T?): T? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.min(x: Any?): Any? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.groupConcat(x: Any?): Any? = null
}
