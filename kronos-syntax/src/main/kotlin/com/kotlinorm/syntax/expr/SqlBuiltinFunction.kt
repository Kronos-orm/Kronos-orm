/*
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

package com.kotlinorm.syntax.expr

/**
 * Semantic identifiers for the functions supplied by Kronos.
 *
 * The SQL spelling is the standard rendering. Dialect renderers may lower a
 * function to a different spelling or expression while custom functions keep
 * using [SqlExpr.Function.name] without this marker.
 */
enum class SqlBuiltinFunction(
    val dslName: String,
    val standardSqlName: String
) {
    Count("count", "COUNT"),
    Sum("sum", "SUM"),
    Average("avg", "AVG"),
    Maximum("max", "MAX"),
    Minimum("min", "MIN"),
    GroupConcat("groupconcat", "GROUP_CONCAT"),

    Absolute("abs", "ABS"),
    Binary("bin", "BIN"),
    Ceiling("ceil", "CEIL"),
    Exponential("exp", "EXP"),
    Floor("floor", "FLOOR"),
    Greatest("greatest", "GREATEST"),
    Least("least", "LEAST"),
    NaturalLog("ln", "LN"),
    Log("log", "LOG"),
    Pi("pi", "PI"),
    Random("rand", "RAND"),
    Round("round", "ROUND"),
    Sign("sign", "SIGN"),
    SquareRoot("sqrt", "SQRT"),
    Truncate("trunc", "TRUNC"),

    Length("length", "LENGTH"),
    Uppercase("upper", "UPPER"),
    Lowercase("lower", "LOWER"),
    Substring("substr", "SUBSTR"),
    Replace("replace", "REPLACE"),
    Left("left", "LEFT"),
    Right("right", "RIGHT"),
    Repeat("repeat", "REPEAT"),
    Reverse("reverse", "REVERSE"),
    Trim("trim", "TRIM"),
    LeftTrim("ltrim", "LTRIM"),
    RightTrim("rtrim", "RTRIM"),
    Concatenate("concat", "CONCAT"),
    JoinWithSeparator("join", "CONCAT_WS"),

    Any("any", "ANY"),
    All("all", "ALL"),
    RowNumber("rownumber", "ROW_NUMBER");

    companion object {
        private val byDslName = entries.associateBy(SqlBuiltinFunction::dslName)

        fun fromDslName(name: String): SqlBuiltinFunction? = byDslName[name.lowercase()]
    }
}
