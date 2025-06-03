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
import com.kotlinorm.enums.DBType
import com.kotlinorm.functions.bundled.builders.MathFunctionBuilder.buildAlias
import com.kotlinorm.functions.bundled.builders.MathFunctionBuilder.buildField
import com.kotlinorm.functions.bundled.builders.MathFunctionBuilder.buildFields
import com.kotlinorm.functions.bundled.builders.MathFunctionBuilder.buildOperations
import com.kotlinorm.interfaces.FunctionBuilder
import com.kotlinorm.interfaces.KronosDataSourceWrapper

object StringFunctionBuilder : FunctionBuilder {
    private val all = arrayOf(
        DBType.Mysql, DBType.Postgres, DBType.SQLite, DBType.Oracle, DBType.Mssql
    )

    override val supportFunctionNames: (String) -> Array<DBType> = {
        when (it) {
            /**
             * 返回字符串的长度
             * return the length of a string
             * exp: length("hello") => 5
             * Mysql: LENGTH(x) SQLite: LENGTH(x) Oracle: LENGTH(x) Postgres: LENGTH(x) Mssql: LEN(x)
             */
            "length" -> all
            /**
             * 返回字符串的大写形式
             * return the uppercase form of a string
             * exp: upper("hello") => "HELLO"
             * Mysql: UPPER(x) SQLite: UPPER(x) Oracle: UPPER(x) Postgres: UPPER(x) Mssql: UPPER(x)
             */
            "upper" -> all
            /**
             * 返回字符串的小写形式
             * return the lowercase form of a string
             * exp: lower("HELLO") => "hello"
             * Mysql: LOWER(x) SQLite: LOWER(x) Oracle: LOWER(x) Postgres: LOWER(x) Mssql: LOWER(x)
             */
            "lower" -> all
            /**
             * 返回字符串的子串
             * return a substring of a string
             * exp: substr("hello", 2, 3) => "ell"
             * Mysql: SUBSTRING(x, y, z) SQLite: SUBSTR(x, y, z) Oracle: SUBSTR(x, y, z) Postgres: SUBSTRING(x FROM y FOR z) Mssql: SUBSTRING(x, y, z)
             */
            "substr" -> all
            /**
             * 替换字符串中的子串
             * replace a substring in a string
             * exp: replace("hello", "l", "L") => "heLLo"
             * Mysql: REPLACE(x, y, z) SQLite: REPLACE(x, y, z) Oracle: REPLACE(x, y, z) Postgres: REPLACE(x, y, z) Mssql: REPLACE(x, y, z)
             */
            "replace" -> all
            /**
             * 返回字符串的左边部分
             * return the leftmost part of a string
             * exp: left("hello", 2) => "he"
             * Mysql: LEFT(x, y) SQLite: LEFT(x, y) Oracle: SUBSTR(x, 1, y) Postgres: SUBSTRING(x FROM 1 FOR y) Mssql: LEFT(x, y)
             */
            "left" -> all
            /**
             * 返回字符串的右边部分
             * return the rightmost part of a string
             * exp: right("hello", 2) => "lo"
             * Mysql: RIGHT(x, y) SQLite: RIGHT(x, y) Oracle: SUBSTR(x, -y) Postgres: SUBSTRING(x FROM -y) Mssql: RIGHT(x, y)
             */
            "right" -> all
            /**
             * 返回字符串的重复
             * return a repeated string
             * exp: repeat("hello", 2) => "hellohello"
             * Mysql: REPEAT(x, y) SQLite: REPEAT(x, y) Oracle: RPAD(x, LENGTH(x) * y， x) Postgres: REPEAT(x, y) Mssql: REPLICATE(x, y)
             */
            "repeat" -> all
            /**
             * 返回字符串的逆序
             * return the reverse of a string
             * exp: reverse("hello") => "olleh"
             * Mysql: REVERSE(x) SQLite: REVERSE(x) Oracle: REVERSE(x) Postgres: REVERSE(x) Mssql: REVERSE(x)
             */
            "reverse" -> all
            /**
             * 返回字符串的去除空格
             * return a string with leading and trailing whitespace removed
             * exp: trim(" hello ") => "hello"
             * Mysql: TRIM(x) SQLite: TRIM(x) Oracle: TRIM(x) Postgres: TRIM(x) Mssql: TRIM(x)
             */
            "trim" -> all
            /**
             * 返回字符串的去除左空格
             * return a string with leading whitespace removed
             * exp: trimStart(" hello ") => "hello "
             * Mysql: LTRIM(x) SQLite: LTRIM(x) Oracle: LTRIM(x) Postgres: LTRIM(x) Mssql: LTRIM(x)
             */
            "ltrim" -> all
            /**
             * 返回字符串的去除右空格
             * return a string with trailing whitespace removed
             * exp: trimEnd(" hello ") => " hello"
             * Mysql: RTRIM(x) SQLite: RTRIM(x) Oracle: RTRIM(x) Postgres: RTRIM(x) Mssql: RTRIM(x)
             */
            "rtrim" -> all
            /**
             * 连接字符串
             * concatenate strings
             * join(x1, x2, ...)
             * exp: join("a", "b", "c") => "a, b, c"
             * Mysql: CONCAT(x, y, z) SQLite: CONCAT(x, y, z) Oracle: CONCAT(x, y, z) Postgres: CONCAT(x, y, z) Mssql: CONCAT(x, y, z)
             */
            "concat" -> all
            /**
             * 连接字符串
             * concatenate strings
             * join(separator, x1, x2, ...)
             * exp: join(", ", "a", "b", "c") => "a, b, c"
             * Mysql: CONCAT_WS(separator, x, y, z) SQLite: CONCAT_WS(separator, x, y, z) Oracle: x || separator || y || separator || z Postgres: CONCAT_WS(separator, x, y, z) Mssql: CONCAT_WS(separator, x, y, z)
             */
            "join" -> all
            else -> emptyArray()
        }
    }

    override fun transform(
        field: FunctionField,
        dataSource: KronosDataSourceWrapper,
        showTable: Boolean,
        showAlias: Boolean
    ): String {
        val alias = if (showAlias) field.name else ""
        field.functionName = when (field.functionName) {
            "join" -> {
                if (dataSource.dbType == DBType.Oracle) {
                    val separator = field.fields.first().second
                    val fields: List<Pair<Field?, Any?>> =
                        field.fields.drop(1).flatMap { listOf(Pair(null, separator), it) }.drop(1)
                    return buildOperations("||", alias, fields, dataSource, showTable)
                } else {
                    field.fields = field.fields.drop(1)
                    "CONCAT_WS"
                }
            }

            "repeat" -> {
                when (dataSource.dbType) {
                    DBType.Oracle -> {
                        val f = buildField(field.fields[0], dataSource, showTable)
                        val times = buildField(field.fields[1], dataSource, showTable)
                        return buildAlias(
                            "RPAD($f, $times * LENGTH($f, $times), $f)",
                            alias
                        )
                    }

                    DBType.Mssql -> "REPLICATE"
                    else -> "REPEAT"
                }
            }

            "right" -> {
                when (dataSource.dbType) {
                    DBType.Oracle -> {
                        val f = buildField(field.fields[0], dataSource, showTable)
                        val length = buildField(field.fields[1], dataSource, showTable)
                        return buildAlias("SUBSTR($f, -$length)", alias)
                    }

                    DBType.Postgres -> {
                        val f = buildField(field.fields[0], dataSource, showTable)
                        val length = buildField(field.fields[1], dataSource, showTable)
                        return buildAlias("SUBSTRING($f FROM -$length)", alias)
                    }

                    else -> "RIGHT"
                }
            }

            "left" -> {
                when (dataSource.dbType) {
                    DBType.Oracle -> {
                        val f = buildField(field.fields[0], dataSource, showTable)
                        val length = buildField(field.fields[1], dataSource, showTable)
                        return buildAlias("SUBSTR($f, 1, $length)", alias)
                    }

                    DBType.Postgres -> {
                        val f = buildField(field.fields[0], dataSource, showTable)
                        val length = buildField(field.fields[1], dataSource, showTable)
                        return buildAlias("SUBSTRING($f FROM 1 FOR $length)", alias)
                    }

                    else -> "LEFT"
                }
            }

            "substr" -> {
                when (dataSource.dbType) {
                    DBType.Postgres -> {
                        val f = buildField(field.fields[0], dataSource, showTable)
                        val start = buildField(field.fields[1], dataSource, showTable)
                        val length = buildField(field.fields[2], dataSource, showTable)
                        return buildAlias("SUBSTRING(${f} FROM $start FOR $length)", alias)
                    }

                    else -> "SUBSTR"
                }
            }

            "length" -> when (dataSource.dbType) {
                DBType.Mssql -> "LEN"
                else -> "LENGTH"
            }


            else -> field.functionName.uppercase()
        }
        return buildFields(field.functionName, alias, field.fields, dataSource, showTable)
    }
}