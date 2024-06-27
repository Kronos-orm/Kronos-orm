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

package com.kotlinorm.beans.dsw

/**
 * Created by OUSC on 2022/11/4 14:09
 *
 * Codes based on <a href="https://github.com/spring-projects/spring-framework/blob/main/spring-jdbc/src/main/java/org/springframework/jdbc/support/ParsedSql.java">ParsedSql</a>
 *
 * All rights reserved.
 */
class ParsedSql(private var originalSql: String = "") {

    private val parameterNames: MutableList<String> = ArrayList()

    private val parameterIndexes: MutableList<IntArray> = ArrayList()

    var namedParameterCount = 0

    var unnamedParameterCount = 0

    var totalParameterCount = 0

    /**
     * Add a named parameter parsed from this SQL statement.
     * @param parameterName the name of the parameter
     * @param startIndex the start index in the original SQL String
     * @param endIndex the end index in the original SQL String
     */
    fun addNamedParameter(parameterName: String, startIndex: Int, endIndex: Int) {
        parameterNames.add(parameterName)
        parameterIndexes.add(intArrayOf(startIndex, endIndex))
    }

    var paramMap: Map<String, Any?> = mapOf()

    var jdbcSql: String = ""
    val jdbcParamList: MutableList<Any?> = mutableListOf()

    fun executeParse() {
        var sql = originalSql
        for (parameterName in parameterNames) {
            sql = if (paramMap[parameterName] is Collection<*>) {
                jdbcParamList.addAll(paramMap[parameterName] as Collection<*>)
                sql.replace(
                    ":$parameterName",
                    (paramMap[parameterName] as Collection<*>).joinToString(",") { _ -> "?" })
            } else {
                jdbcParamList.add(paramMap[parameterName])
                sql.replace(":$parameterName", "?")
            }
        }
        jdbcSql = sql
    }

    /**
     * Exposes the original SQL String.
     */
    override fun toString(): String {
        return originalSql
    }

    operator fun component1(): String {
        return jdbcSql
    }

    operator fun component2(): List<Any?> {
        return jdbcParamList
    }

}