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

package com.kotlinorm.functions.bundled.builders

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.database.SqlManager.quoted
import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.UnSupportedFunctionException
import com.kotlinorm.interfaces.FunctionBuilder
import com.kotlinorm.interfaces.KronosDataSourceWrapper

object MathFunctionBuilder : FunctionBuilder {
    private val all = arrayOf(
        DBType.Mysql, DBType.Postgres, DBType.SQLite, DBType.Oracle, DBType.Mssql
    )

    private val common = arrayOf(DBType.Mysql, DBType.Postgres, DBType.Oracle)

    override val supportFunctionNames: (String) -> Array<DBType> = {
        when (it) {/*
            *  返回一个数的绝对值
            *  return the absolute value of a number
            *  exp: abs(-32) => 32
            *  Mysql: ABS(x) SQLite: ABS(x) Oracle: ABS(x) Postgres: ABS(x) Mssql: ABS(x)
            */
            "abs" -> all
            /**
             *  返回大于或等于指定表达式的最小整数（向上取整）
             *  return the smallest integer greater than or equal to a number
             *  exp: ceil(12.3) => 13
             *  Mysql: CEIL(x) SQLite: CEIL(x) Oracle: CEIL(x) Postgres: CEIL(x) Mssql: CEILING(x)
             */
            "ceil" -> all
            /**
             *  返回小于或等于指定表达式的最大整数（向下取整）
             *  return the largest integer less than or equal to a number
             *  exp: floor(12.3) => 12
             *  Mysql: FLOOR(x) SQLite: FLOOR(x) Oracle: FLOOR(x) Postgres: FLOOR(x) Mssql: FLOOR(x)
             */
            "floor" -> all
            /**
             *  返回e的x次幂
             *  return e raised to the power of a number
             *  exp: exp(1) => 2.718281828459045
             *  Mysql: EXP(x) SQLite: EXP(x) Oracle: EXP(x) Postgres: EXP(x) Mssql: EXP(x)
             */
            "exp" -> all
            /**
             *  返回一组值中的最大值
             *  return the largest value
             *  exp: greatest(1, 2, 3) => 3
             *  Mysql: GREATEST(x1, x2, ...) Oracle: GREATEST(x1, x2, ...) Postgres: GREATEST(x1, x2, ...)
             */
            "greatest" -> common
            /**
             *  返回一组值中的最小值
             *  return the smallest value
             *  exp: least(1, 2, 3) => 1
             *  Mysql: LEAST(x1, x2, ...) Oracle: LEAST(x1, x2, ...) Postgres: LEAST(x1, x2, ...)
             */
            "least" -> common
            /**
             *  返回一个数的自然对数
             *  return the natural logarithm of a number
             *  exp: ln(2) => 0.6931471805599453
             *  Mysql: LN(x) SQLite: LN(x) Oracle: LN(x) Postgres: LN(x) Mssql: LOG(x, EXP(1))
             */
            "ln" -> all
            /**
             *  返回一个数的对数
             *  return the logarithm of a number
             *  exp: log(2, 10) => 0.3010299956639812
             *  Mysql: LOG(x, base) SQLite: LOG(x, base) Oracle: LOG(base, x) Postgres: LOG(base, x) Mssql: LOG(base, x)
             */
            "log" -> all
            /**
             *  返回两个数相除的余数
             *  return the remainder of a division operation
             *  exp: mod(5, 2) => 1
             *  Mysql: x % y SQLite: x % y Oracle: MOD(x, y) Postgres: x % y Mssql: x % y
             */
            "mod" -> all
            /**
             *  返回圆周率
             *  return the value of pi
             *  exp: pi() => 3.141592653589793
             *  Mysql: PI() SQLite: PI() Oracle: PI() Postgres: PI() Mssql: PI()
             */
            "pi" -> all
            /**
             *  返回一个随机数
             *  return a random number
             *  exp: rand() => 0.123456789
             *  Mysql: RAND() SQLite: RANDOM() Oracle: DBMS_RANDOM.VALUE Postgres: RANDOM() Mssql: RAND()
             */
            "rand" -> all
            /**
             *  返回一个数四舍五入的值
             *  return the value of a number rounded to the nearest integer
             *  exp: round(12.3) => 12
             *  Mysql: ROUND(x) SQLite: ROUND(x) Oracle: ROUND(x) Postgres: ROUND(x) Mssql: ROUND(x)
             */
            "round" -> all
            /**
             *  返回一个数的符号
             *  return the sign of a number
             *  exp: sign(-12) => -1
             *  Mysql: SIGN(x) SQLite: SIGN(x) Oracle: SIGN(x) Postgres: SIGN(x) Mssql: SIGN(x)
             */
            "sign" -> all
            /**
             *  返回一个数的平方根
             *  return the square root of a number
             *  exp: sqrt(9) => 3
             *  Mysql: SQRT(x) SQLite: SQRT(x) Oracle: SQRT(x) Postgres: SQRT(x) Mssql: SQRT(x)
             */
            "sqrt" -> all
            /**
             *  返回一个数截断到指定的小数位数
             *  return a number truncated to a certain number of decimal places
             *  exp: truncate(12.345, 1) => 12.3
             *  Mysql: TRUNCATE(x, d) SQLite: TRUNC(x, d) Oracle: TRUNC(x, d) Postgres: TRUNC(x, d) Mssql: ROUND(x, d)
             */
            "trunc" -> all
            /**
             * 返回一组值的和
             * return the sum of a set of values
             * exp: add(1, 2, 3) => 6
             * Mysql: (x1 + x2 + ...) SQLite: (x1 + x2 + ...) Oracle: (x1 + x2 + ...) Postgres: (x1 + x2 + ...) Mssql: (x1 + x2 + ...)
             */
            "add" -> all
            /**
             * 返回一组值的差
             * return the difference of a set of values
             * exp: sub(1, 2, 3) => -4
             * Mysql: (x1 - x2 - ...) SQLite: (x1 - x2 - ...) Oracle: (x1 - x2 - ...) Postgres: (x1 - x2 - ...) Mssql: (x1 - x2 - ...)
             */
            "sub" -> all
            /**
             * 返回一组值的积
             * return the product of a set of values
             * exp: mul(1, 2, 3) => 6
             * Mysql: (x1 * x2 * ...) SQLite: (x1 * x2 * ...) Oracle: (x1 * x2 * ...) Postgres: (x1 * x2 * ...) Mssql: (x1 * x2 * ...)
             */
            "mul" -> all
            /**
             * 返回一组值的商
             * return the quotient of a set of values
             * exp: div(6, 2, 3) => 1
             * Mysql: (x1 / x2 / ...) SQLite: (x1 / x2 / ...) Oracle: (x1 / x2 / ...) Postgres: (x1 / x2 / ...) Mssql: (x1 / x2 / ...)
             */
            "div" -> all
            else -> emptyArray()
        }
    }

    override fun transform(
        field: FunctionField, dataSource: KronosDataSourceWrapper, showTable: Boolean, showAlias: Boolean
    ): String {
        return getFunctionSql(field, dataSource, showTable, showAlias)
    }

    private fun getFunctionSql(
        field: FunctionField, dataSource: KronosDataSourceWrapper, showTable: Boolean, showAlias: Boolean
    ): String {
        val alias = if (showAlias) field.name else ""
        return when (field.functionName) {
            "add", "sub", "mul", "div" -> {
                val operator = when (field.functionName) {
                    "add" -> "+"
                    "sub" -> "-"
                    "mul" -> "*"
                    "div" -> "/"
                    else -> throw UnSupportedFunctionException(dataSource.dbType, field.functionName)
                }
                buildOperations(operator, alias, field.fields, dataSource, showTable)
            }

            else -> {
                val functionName = when (field.functionName to dataSource.dbType) {
                    "ceil" to DBType.Mssql -> "CEILING"
                    "ln" to DBType.Mssql -> {
                        field.fields = listOf(
                            field.fields.first(), Pair(null, "EXP(1)")
                        )
                        "LOG"
                    }

                    "rand" to DBType.Oracle -> return buildAlias("DBMS_RANDOM.VALUE", alias)
                    "rand" to DBType.SQLite, "rand" to DBType.Postgres -> "RANDOM"
                    "trunc" to DBType.Mysql -> "TRUNCATE"
                    "trunc" to DBType.Mssql -> "ROUND"
                    else -> field.functionName
                }
                buildFields(functionName.uppercase(), alias, field.fields, dataSource, showTable)
            }
        }
    }

    fun buildField(it: Pair<Field?, Any?>, dataSource: KronosDataSourceWrapper, showTable: Boolean): String {
        return it.first?.quoted(
            dataSource, showTable
        ) ?: if (it.second is String) "'${it.second}'" else it.second.toString()
    }

    fun buildOperations(
        operator: String,
        alias: String,
        fields: List<Pair<Field?, Any?>>,
        dataSource: KronosDataSourceWrapper,
        showTable: Boolean
    ): String {
        return buildAlias("(${
            fields.joinToString(" $operator ") {
                buildField(it, dataSource, showTable)
            }
        })", alias)
    }

    fun buildFields(
        functionName: String,
        alias: String,
        fields: List<Pair<Field?, Any?>>,
        dataSource: KronosDataSourceWrapper,
        showTable: Boolean
    ): String {
        return buildAlias("${functionName}(${
            fields.joinToString(", ") {
                buildField(it, dataSource, showTable)
            }
        })", alias)
    }

    fun buildAlias(field: String, alias: String): String {
        return "$field${
            if (alias.isNotEmpty()) " AS $alias" else ""
        }"
    }

}