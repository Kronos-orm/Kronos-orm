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

package com.kotlinorm.compiler.fir.utils.kTableForSelect

import com.kotlinorm.compiler.helpers.referenceClass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.getSimpleFunction


const val KTABLE_FOR_SELECT_CLASS = "com.kotlinorm.beans.dsl.KTableForSelect"

context(IrPluginContext)
private val kTableForSelectSymbol
    get() = referenceClass(KTABLE_FOR_SELECT_CLASS)!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val addFieldSymbol
    get() = kTableForSelectSymbol.getSimpleFunction("addField")!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val aliasSymbol
    get() = kTableForSelectSymbol.getSimpleFunction("setAlias")!!

internal val builtinFunctions =
    arrayOf(
        "count", "average", "sum", "max", "min",
        "abs", "bin", "ceiling", "exp", "floor",
        "greatest", "least", "ln", "log", "mod",
        "pi", "rand", "round", "sign", "sqrt",
        "truncate", "groupConcat", "ascii", "bitLength", "concat",
        "concatWs", "insert", "findInSet", "lcase", "left",
        "length", "ltrim", "position", "quote", "repeat",
        "reverse", "right", "rtrim", "strcmp", "trim",
        "ucase", "curdate", "curtime", "dateAdd", "dateFormat",
        "dateSub", "dayOfWeek", "dayOfMonth", "dayOfYear", "dayName",
        "fromUnixTime", "hour", "minute", "month", "monthName",
        "now", "quarter", "week", "year", "periodDiff",
        "calculateAge", "aesEncrypt", "aesDecrypt", "decode", "encrypt",
        "encode", "md5", "password", "sha"
    )