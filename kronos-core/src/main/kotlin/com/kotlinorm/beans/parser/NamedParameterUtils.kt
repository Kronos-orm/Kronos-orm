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

package com.kotlinorm.beans.parser

import com.kotlinorm.exceptions.InvalidDataAccessApiUsageException
import com.kotlinorm.exceptions.InvalidParameterException
import com.kotlinorm.interfaces.KPojo


/**
 * Created by OUSC on 2022/11/4 11:32
 *
 * Codes based on <a href="https://github.com/spring-projects/spring-framework/blob/main/spring-jdbc/src/main/java/org/springframework/jdbc/core/namedparam/NamedParameterUtils.java">NamedParameterUtils</a>
 *
 * Add path parse support for NamedParameterParameterSource
 *
 * Such as `:array[0].list[1].map[KPojo].id`
 *
 * All rights reserved.
 */
object NamedParameterUtils {
    private val START_SKIP = arrayOf("'", "\"", "--", "/*", "`")

    /**
     * Set of characters that at are the corresponding comment or quotes ending characters.
     */
    private val STOP_SKIP = arrayOf("'", "\"", "\n", "*/", "`")

    /**
     * Set of characters that qualify as parameter separators,
     * indicating that a parameter name in an SQL String has ended.
     */
    private const val PARAMETER_SEPARATORS = "\"':&,;()|=+-*%/\\<>^"

    private const val SEPARATOR_INDEX_SIZE = 128

    /**
     * An index with separator flags per character code.
     * Technically only needed between 34 and 124 at this point.
     */
    private val separatorIndex = BooleanArray(SEPARATOR_INDEX_SIZE)

    init {
        for (i in PARAMETER_SEPARATORS.indices) {
            separatorIndex[PARAMETER_SEPARATORS[i].code] = true
        }
    }

    //-------------------------------------------------------------------------
    // Core methods used by NamedParameterJdbcTemplate and SqlQuery/SqlUpdate
    //-------------------------------------------------------------------------

    //-------------------------------------------------------------------------
    // Core methods used by NamedParameterJdbcTemplate and SqlQuery/SqlUpdate
    //-------------------------------------------------------------------------
    /**
     * Parse the SQL statement and locate any placeholders or named parameters.
     * Named parameters are substituted for a JDBC placeholder.
     * @param sql the SQL statement
     * @return the parsed statement, represented as com.kotlinorm.beans.parser.ParsedSql instance
     */
    fun parseSqlStatement(sql: String, paramMap: Map<String, Any?> = mapOf()): ParsedSql {
        val namedParameters: MutableSet<String> = HashSet()
        val sqlToUse = StringBuilder(sql)
        val parameterList: MutableList<ParameterHolder> = ArrayList()
        val statement = sql.toCharArray()
        var namedParameterCount = 0
        var unnamedParameterCount = 0
        var totalParameterCount = 0
        var escapes = 0
        var i = 0
        while (i < statement.size) {
            var skipToPosition: Int
            while (i < statement.size) {
                skipToPosition = skipCommentsAndQuotes(statement, i)
                if (i == skipToPosition) {
                    break
                } else {
                    i = skipToPosition
                }
            }
            if (i >= statement.size) {
                break
            }
            var c = statement[i]
            if (c == ':' || c == '&') {
                var j = i + 1
                if ((c == ':') && j < statement.size && statement[j] == ':') {
                    // Postgres-style "::" casting operator should be skipped
                    i += 2
                    continue
                }
                var parameter: String?
                if ((c == ':' && j < statement.size) && statement[j] == '{') {
                    // :{x} style parameter
                    while (statement[j] != '}') {
                        j++
                        if (j >= statement.size) {
                            throw InvalidParameterException(
                                "Non-terminated named parameter declaration " +
                                        "at position " + i + " in statement: " + sql
                            )
                        }
                        if (statement[j] == ':' || statement[j] == '{') {
                            throw InvalidParameterException(
                                ("Parameter name contains invalid character '" +
                                        statement[j] + "' at position " + i + " in statement: " + sql)
                            )
                        }
                    }
                    if (j - i > 2) {
                        parameter = sql.substring(i + 2, j)
                        namedParameterCount = addNewNamedParameter(namedParameters, namedParameterCount, parameter)
                        totalParameterCount = addNamedParameter(
                            parameterList, totalParameterCount, escapes, i, j + 1, parameter
                        )
                    }
                    j++
                } else {
                    var paramWithSquareBrackets = false
                    while (j < statement.size) {
                        c = statement[j]
                        if (isParameterSeparator(c)) {
                            break
                        }
                        if (c == '[') {
                            paramWithSquareBrackets = true
                        } else if (c == ']') {
                            if (!paramWithSquareBrackets) {
                                break
                            }
                            paramWithSquareBrackets = false
                        }
                        j++
                    }
                    if (j - i > 1) {
                        parameter = sql.substring(i + 1, j)
                        namedParameterCount = addNewNamedParameter(namedParameters, namedParameterCount, parameter)
                        totalParameterCount = addNamedParameter(
                            parameterList, totalParameterCount, escapes, i, j, parameter
                        )
                    }
                }
                i = j - 1
            } else {
                if (c == '\\') {
                    val j = i + 1
                    if (j < statement.size && statement[j] == ':') {
                        // escaped ":" should be skipped
                        sqlToUse.deleteCharAt(i - escapes)
                        escapes++
                        i += 2
                        continue
                    }
                }
                if (c == '?') {
                    val j = i + 1
                    if (j < statement.size && ((statement[j] == '?') || (statement[j] == '|') || (statement[j] == '&'))) {
                        // Postgres-style "??", "?|", "?&" operator should be skipped
                        i += 2
                        continue
                    }
                    unnamedParameterCount++
                    totalParameterCount++
                }
            }
            i++
        }
        val parsedSql = ParsedSql(sqlToUse.toString(), paramMap)
        for (ph: ParameterHolder in parameterList) {
            parsedSql.addNamedParameter(ph.parameterName, ph.startIndex, ph.endIndex)
        }
        parsedSql.namedParameterCount = namedParameterCount
        parsedSql.unnamedParameterCount = unnamedParameterCount
        parsedSql.totalParameterCount = totalParameterCount
        return parsedSql
    }

    private fun addNamedParameter(
        parameterList: MutableList<ParameterHolder>,
        totalParameterCount: Int,
        escapes: Int,
        i: Int,
        j: Int,
        parameter: String
    ): Int {
        parameterList.add(ParameterHolder(parameter, i - escapes, j - escapes))
        return totalParameterCount + 1
    }

    private fun addNewNamedParameter(
        namedParameters: MutableSet<String>,
        namedParameterCount: Int,
        parameter: String
    ): Int {
        if (!namedParameters.contains(parameter)) {
            namedParameters.add(parameter);
            return namedParameterCount + 1
        }
        return namedParameterCount
    }

    /**
     * Skip over comments and quoted names present in an SQL statement.
     * @param statement character array containing SQL statement
     * @param position current position of statement
     * @return next position to process after any comments or quotes are skipped
     */
    private fun skipCommentsAndQuotes(statement: CharArray, position: Int): Int {
        for (i in START_SKIP.indices) {
            if (statement[position] == START_SKIP[i][0]) {
                var match = true
                for (j in 1 until START_SKIP[i].length) {
                    if (statement[position + j] != START_SKIP[i][j]) {
                        match = false
                        break
                    }
                }
                if (match) {
                    val offset: Int = START_SKIP[i].length
                    for (m in position + offset until statement.size) {
                        if (statement[m] == STOP_SKIP[i][0]) {
                            var endMatch = true
                            var endPos = m
                            for (n in 1 until STOP_SKIP[i].length) {
                                if (m + n >= statement.size) {
                                    // last comment not closed properly
                                    return statement.size
                                }
                                if (statement[m + n] != STOP_SKIP[i][n]) {
                                    endMatch = false
                                    break
                                }
                                endPos = m + n
                            }
                            if (endMatch) {
                                // found character sequence ending comment or quote
                                return endPos + 1
                            }
                        }
                    }
                    // character sequence ending comment or quote not found
                    return statement.size
                }
            }
        }
        return position
    }

    /**
     * Determine whether a parameter name ends at the current position,
     * that is, whether the given character qualifies as a separator.
     */
    private fun isParameterSeparator(c: Char): Boolean {
        return (c.code < 128 && separatorIndex[c.code]) || Character.isWhitespace(c)
    }


    private class ParameterHolder(val parameterName: String, val startIndex: Int, val endIndex: Int)

    /**
     * Parse the SQL statement and locate any placeholders or named parameters. Named
     * parameters are substituted for a JDBC placeholder, and any select list is expanded
     * to the required number of placeholders. Select lists may contain an array of
     * objects, and in that case the placeholders will be grouped and enclosed with
     * parentheses. This allows for the use of "expression lists" in the SQL statement
     * like: <br></br><br></br>
     * `select id, name, state from table where (name, age) in (('John', 35), ('Ann', 50))`
     *
     * The parameter values passed in are used to determine the number of
     * placeholders to be used for a select list. Select lists should not be empty
     * and should be limited to 100 or fewer elements. An empty list or a larger
     * number of elements is not guaranteed to be supported by the database and
     * is strictly vendor-dependent.
     * @param parsedSql the parsed representation of the SQL statement
     * @param paramSource the source for named parameters
     * @return the SQL statement with substituted parameters
     * @see .parseSqlStatement
     */
    fun substituteNamedParameters(parsedSql: ParsedSql, paramSource: Map<String, Any?>? = null): String {
        val originalSql: String = parsedSql.originalSql
        val paramNames: List<String> = parsedSql.parameterNames
        if (paramNames.isEmpty()) {
            return originalSql
        }

        val actualSql = java.lang.StringBuilder(originalSql.length)
        var lastIndex = 0
        for (i in paramNames.indices) {
            val paramName = paramNames[i]
            val indexes: IntArray = parsedSql.parameterIndexes[i]
            val startIndex = indexes[0]
            val endIndex = indexes[1]
            actualSql.append(originalSql, lastIndex, startIndex)
            if (paramSource != null && paramSource.containsKey(paramName) && paramSource[paramName] != null) {
                val value: Any = paramSource[paramName]!!
                if (value is Iterable<*>) {
                    for ((k, entryItem) in value.withIndex()) {
                        if (k > 0) {
                            actualSql.append(", ")
                        }
                        if (entryItem is Array<*> && entryItem.isArrayOf<Any>()) {
                            actualSql.append('(')
                            for (m in entryItem.indices) {
                                if (m > 0) {
                                    actualSql.append(", ")
                                }
                                actualSql.append('?')
                            }
                            actualSql.append(')')
                        } else {
                            actualSql.append('?')
                        }
                    }
                } else {
                    actualSql.append('?')
                }
            } else {
                actualSql.append('?')
            }
            lastIndex = endIndex
        }
        actualSql.append(originalSql, lastIndex, originalSql.length)
        return actualSql.toString()
    }

    /**
     * Convert a Map of named parameter values to a corresponding array.
     * @param parsedSql the parsed SQL statement
     * @param paramSource the source for named parameters
     * @param declaredParams the List of declared SqlParameter objects
     * (may be `null`). If specified, the parameter metadata will
     * be built into the value array in the form of SqlParameterValue objects.
     * @return the array of values
     */
    fun buildValueArray(
        parsedSql: ParsedSql, paramSource: Map<String, Any?>
    ): Array<Any?> {
        val paramArray = arrayOfNulls<Any>(parsedSql.totalParameterCount)
        if (parsedSql.namedParameterCount > 0 && parsedSql.unnamedParameterCount > 0) {
            throw InvalidDataAccessApiUsageException(
                "Not allowed to mix named and traditional ? placeholders. You have " +
                        parsedSql.namedParameterCount + " named parameter(s) and " +
                        parsedSql.unnamedParameterCount + " traditional placeholder(s) in statement: " +
                        parsedSql.originalSql
            )
        }
        val paramNames: List<String> = parsedSql.parameterNames
        for (i in paramNames.indices) {
            paramArray[i] = getValueFromMap(paramSource, paramNames[i])
        }
        return paramArray
    }

    private fun getValueFromMap(map: Map<String, Any?>, path: String): Any? {
        // 解析路径
        val keys = parsePath(path)

        // 逐级取值
        var current: Any? = map
        for (key in keys) {
            current = when (current) {
                is Map<*, *> -> current[key] // 如果当前值是 Map，取出对应的值
                is KPojo -> current.toDataMap()[key] // 如果当前值是 KPojo，取出对应的值
                is Iterable<*>, is Array<*>, is IntArray, is LongArray, is ShortArray, is ByteArray, is DoubleArray, is FloatArray, is BooleanArray ->
                    // 如果当前值是 List，取出对应的索引
                    key.toIntOrNull()?.let {
                        when (current) {
                            is IntArray -> (current as IntArray)[it]
                            is LongArray -> (current as LongArray)[it]
                            is ShortArray -> (current as ShortArray)[it]
                            is ByteArray -> (current as ByteArray)[it]
                            is DoubleArray -> (current as DoubleArray)[it]
                            is FloatArray -> (current as FloatArray)[it]
                            is BooleanArray -> (current as BooleanArray)[it]
                            is Array<*> -> (current as Array<*>)[it]
                            is Iterable<*> -> (current as Iterable<*>).elementAt(it)
                            else -> throw InvalidDataAccessApiUsageException(
                                "Collection named '$key' in parameter source is not an Iterable or Array"
                            )
                        }
                    } // 如果当前值是 List，取出对应的索引
                else -> null // 其他类型则返回 null
            }
            if (current == null) break // 如果中途遇到 null，停止
        }

        return current
    }

    private fun parsePath(path: String): List<String> {
        // 使用正则表达式解析路径
        val regex = """\.|(\[([0-9]+)])|(?<key>[^.\[\]]+)""".toRegex()
        return regex.findAll(path).mapNotNull {
            it.groups["key"]?.value ?: it.groups[2]?.value
        }.toList()
    }
}