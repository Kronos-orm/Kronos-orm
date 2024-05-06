package com.kotoframework.beans.dsw

import com.kotoframework.beans.exceptions.InvalidParameterException

/**
 * Created by ousc on 2022/11/4 11:32
 * Codes from https://github.com/spring-projects/spring-framework/blob/main/spring-jdbc/src/main/java/org/springframework/jdbc/support/NamedParameterUtils.java
 * All rights reserved.
 */
object NamedParameterUtils {
    private val START_SKIP = arrayOf("'", "\"", "--", "/*")

    /**
     * Set of characters that at are the corresponding comment or quotes ending characters.
     */
    private val STOP_SKIP = arrayOf("'", "\"", "\n", "*/")

    /**
     * Set of characters that qualify as parameter separators,
     * indicating that a parameter name in an SQL String has ended.
     */
    private const val PARAMETER_SEPARATORS = "\"':&,;()|=+-*%/\\<>^"

    /**
     * An index with separator flags per character code.
     * Technically only needed between 34 and 124 at this point.
     */
    private val separatorIndex = BooleanArray(128)

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
     * @return the parsed statement, represented as com.kotoframework.beans.dsw.ParsedSql instance
     */
    fun parseSqlStatement(sql: String, paramMap: Map<String, Any?>): ParsedSql {
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
            val c = statement[i]
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
                    while (j < statement.size && !isParameterSeparator(statement[j])) {
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
        val parsedSql = ParsedSql(sqlToUse.toString())
        for (ph: ParameterHolder in parameterList) {
            parsedSql.addNamedParameter(ph.parameterName, ph.startIndex, ph.endIndex)
        }
        parsedSql.namedParameterCount = namedParameterCount
        parsedSql.unnamedParameterCount = unnamedParameterCount
        parsedSql.totalParameterCount = totalParameterCount
        parsedSql.paramMap = paramMap
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
            namedParameters.add(parameter)
        }
        return if (namedParameters.contains(parameter)) {
            namedParameterCount + 1
        } else {
            namedParameterCount
        }
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

}