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

package com.kotlinorm.beans.parser

import com.kotlinorm.cache.namedSqlCache
import com.kotlinorm.exceptions.InvalidDataAccessApiUsageException
import com.kotlinorm.exceptions.InvalidParameterException
import com.kotlinorm.interfaces.KPojo

/**
 * SQL 命名参数解析工具
 * 
 * 提供 SQL 语句中命名参数的解析和替换功能，支持复杂的路径表达式。
 * 
 * ## 支持的参数格式
 * - 简单参数：`:name` 或 `&name`
 * - 括号参数：`:{name}`
 * - 路径参数：`:user.name`、`:array[0]`、`:records[0].id`
 * - 复杂路径：`:array[0].list[1].map[key].property`
 * 
 * ## 特殊操作符处理
 * - PostgreSQL 类型转换：`::timestamp` 不会被识别为参数
 * - PostgreSQL JSON 操作符：`??`、`?|`、`?&` 不会被识别为参数
 * - 转义冒号：`\:` 会被转换为 `:`
 * 
 * ## 性能优化
 * - 使用缓存避免重复解析相同的 SQL 语句
 * - 迭代实现避免递归调用栈开销
 * - 简单路径快速查找优化
 * 
 * @author OUSC
 * @since 1.0.0
 */
object NamedParameterUtils {
    /** SQL 中的引号和注释配对 */
    private val QUOTE_PAIRS = listOf(
        "'" to "'",      // 单引号字符串
        "\"" to "\"",    // 双引号字符串
        "--" to "\n",    // 单行注释
        "/*" to "*/",    // 多行注释
        "`" to "`"       // 反引号标识符
    )

    /** 参数分隔符字符集 */
    private const val PARAMETER_SEPARATORS = "\"':&,;()|=+-*%/\\<>^"
    
    /** 分隔符索引数组大小 */
    private const val SEPARATOR_INDEX_SIZE = 128

    /** 快速查找分隔符的布尔数组索引 */
    private val separatorIndex = BooleanArray(SEPARATOR_INDEX_SIZE).apply {
        PARAMETER_SEPARATORS.forEach { char ->
            this[char.code] = true
        }
    }

    /**
     * 解析 SQL 语句，定位占位符和命名参数
     * 
     * 将 SQL 语句中的命名参数（如 `:name`）解析为 JDBC 占位符（`?`），
     * 并记录参数的位置和名称信息。支持缓存以提高重复解析的性能。
     * 
     * @param sql 原始 SQL 语句
     * @param paramMap 参数映射表，用于展开集合参数
     * @return 解析后的 SQL 对象，包含 JDBC SQL 和参数信息
     * 
     * @throws InvalidParameterException 当参数声明格式错误时
     * 
     * Example:
     * ```kotlin
     * val sql = "SELECT * FROM users WHERE id = :id AND name = :name"
     * val parsed = parseSqlStatement(sql, mapOf("id" to 1, "name" to "John"))
     * // parsed.jdbcSql = "SELECT * FROM users WHERE id = ? AND name = ?"
     * // parsed.jdbcParamList = [1, "John"]
     * ```
     */
    fun parseSqlStatement(sql: String, paramMap: Map<String, Any?> = emptyMap()): ParsedSql {
        namedSqlCache[sql]?.let { cached ->
            return ParsedSql(
                sql, paramMap,
                cached.parameterNames,
                cached.parameterIndexes,
                cached.namedParameterCount,
                cached.unnamedParameterCount,
                cached.totalParameterCount,
                ""
            ).apply {
                jdbcSql = substituteNamedParameters(this, paramMap)
            }
        }

        val context = ParseContext(sql)
        parseStatementIterative(context)

        return ParsedSql(context.sqlToUse.toString(), paramMap).apply {
            context.parameterList.forEach { addNamedParameter(it.name, it.startIndex, it.endIndex) }
            namedParameterCount = context.namedParameters.size
            unnamedParameterCount = context.unnamedParameterCount
            totalParameterCount = context.totalParameterCount
            jdbcSql = substituteNamedParameters(this, paramMap)
            namedSqlCache[sql] = this
        }
    }

    /**
     * SQL 解析上下文
     * 
     * 保存解析过程中的状态信息
     * 
     * @property originalSql 原始 SQL 字符串
     * @property statement SQL 字符数组，用于快速访问
     * @property sqlToUse 处理转义后的 SQL
     * @property namedParameters 已发现的唯一参数名集合
     * @property parameterList 参数列表，包含位置信息
     * @property totalParameterCount 总参数数量（包括重复的）
     * @property unnamedParameterCount 未命名参数（?）数量
     * @property escapes 转义字符数量
     */
    private data class ParseContext(
        val originalSql: String,
        val statement: CharArray = originalSql.toCharArray(),
        val sqlToUse: StringBuilder = StringBuilder(originalSql),
        val namedParameters: MutableSet<String> = mutableSetOf(),
        val parameterList: MutableList<Parameter> = mutableListOf(),
        var totalParameterCount: Int = 0,
        var unnamedParameterCount: Int = 0,
        var escapes: Int = 0
    )

    /**
     * 参数信息
     * 
     * @property name 参数名称
     * @property startIndex 在 SQL 中的起始位置
     * @property endIndex 在 SQL 中的结束位置
     */
    private data class Parameter(val name: String, val startIndex: Int, val endIndex: Int)

    /**
     * 迭代方式解析 SQL 语句
     * 
     * 使用迭代而非递归，避免长 SQL 语句导致的栈溢出问题
     * 
     * @param context 解析上下文
     */
    private fun parseStatementIterative(context: ParseContext) {
        var position = 0
        
        while (position < context.statement.size) {
            // 跳过注释和引号内容
            val nextPos = skipCommentsAndQuotes(context.statement, position)
            if (nextPos != position) {
                position = nextPos
                continue
            }

            when (val char = context.statement[position]) {
                ':', '&' -> {
                    position = parseParameter(context, position, char)
                }
                '\\' -> {
                    position = if (handleEscapedColon(context, position)) {
                        position + 2
                    } else {
                        position + 1
                    }
                }
                '?' -> {
                    position = if (handleQuestionMark(context, position)) {
                        position + 2
                    } else {
                        context.unnamedParameterCount++
                        context.totalParameterCount++
                        position + 1
                    }
                }
                else -> position++
            }
        }
    }

    /**
     * 解析参数
     * 
     * 识别并解析命名参数，支持两种格式：
     * - 括号格式：`:{name}`
     * - 常规格式：`:name` 或 `:array[0].property`
     * 
     * @param context 解析上下文
     * @param start 参数起始位置
     * @param prefix 参数前缀字符（`:` 或 `&`）
     * @return 参数结束位置
     */
    private fun parseParameter(context: ParseContext, start: Int, prefix: Char): Int {
        val next = start + 1
        
        // 跳过 Postgres 的 :: 类型转换操作符
        if (prefix == ':' && next < context.statement.size && context.statement[next] == ':') {
            return start + 2
        }

        val (paramName, endPos) = when {
            prefix == ':' && next < context.statement.size && context.statement[next] == '{' ->
                parseBracketParameter(context, start, next)
            else ->
                parseRegularParameter(context, start, next)
        }

        if (paramName.isNotEmpty()) {
            context.namedParameters.add(paramName)
            context.parameterList.add(
                Parameter(paramName, start - context.escapes, endPos - context.escapes)
            )
            context.totalParameterCount++
        }

        return endPos
    }

    /**
     * 解析括号格式的参数
     * 
     * 格式：`:{paramName}`
     * 
     * @param context 解析上下文
     * @param start 参数起始位置
     * @param bracketStart 左括号位置
     * @return 参数名和结束位置
     * @throws InvalidParameterException 当括号未闭合或包含非法字符时
     */
    private fun parseBracketParameter(context: ParseContext, start: Int, bracketStart: Int): Pair<String, Int> {
        var pos = bracketStart + 1
        
        while (pos < context.statement.size && context.statement[pos] != '}') {
            val char = context.statement[pos]
            if (char == ':' || char == '{') {
                throw InvalidParameterException(
                    "Parameter name contains invalid character '$char' at position $start in statement: ${context.originalSql}"
                )
            }
            pos++
        }

        if (pos >= context.statement.size) {
            throw InvalidParameterException(
                "Non-terminated named parameter declaration at position $start in statement: ${context.originalSql}"
            )
        }

        val paramName = if (pos - bracketStart > 1) {
            context.originalSql.substring(bracketStart + 1, pos)
        } else ""

        return paramName to pos + 1
    }

    /**
     * 解析常规格式的参数
     * 
     * 支持简单参数和路径参数：
     * - 简单：`:name`
     * - 路径：`:user.name`、`:array[0]`、`:records[0].id`
     * 
     * @param context 解析上下文
     * @param start 参数起始位置
     * @param paramStart 参数名起始位置
     * @return 参数名和结束位置
     */
    private fun parseRegularParameter(context: ParseContext, start: Int, paramStart: Int): Pair<String, Int> {
        var pos = paramStart
        var hasSquareBracket = false

        while (pos < context.statement.size) {
            val char = context.statement[pos]
            
            when (char) {
                '[' -> hasSquareBracket = true
                ']' -> {
                    if (!hasSquareBracket) break
                    hasSquareBracket = false
                }
                else -> if (isParameterSeparator(char)) break
            }
            pos++
        }

        val paramName = if (pos - start > 1) {
            context.originalSql.substring(start + 1, pos)
        } else ""

        return paramName to pos
    }

    /**
     * 处理转义的冒号
     * 
     * 将 `\:` 转换为 `:`
     * 
     * @param context 解析上下文
     * @param position 当前位置
     * @return 是否处理了转义
     */
    private inline fun handleEscapedColon(context: ParseContext, position: Int): Boolean {
        val next = position + 1
        return if (next < context.statement.size && context.statement[next] == ':') {
            context.sqlToUse.deleteCharAt(position - context.escapes)
            context.escapes++
            true
        } else false
    }

    /**
     * 处理问号操作符
     * 
     * 识别 PostgreSQL 的特殊操作符：`??`、`?|`、`?&`
     * 
     * @param context 解析上下文
     * @param position 当前位置
     * @return 是否是特殊操作符
     */
    private inline fun handleQuestionMark(context: ParseContext, position: Int): Boolean {
        val next = position + 1
        if (next >= context.statement.size) return false
        val nextChar = context.statement[next]
        return nextChar == '?' || nextChar == '|' || nextChar == '&'
    }

    /**
     * 跳过 SQL 中的注释和引号内容
     * 
     * @param statement SQL 字符数组
     * @param position 当前位置
     * @return 跳过后的位置
     */
    private fun skipCommentsAndQuotes(statement: CharArray, position: Int): Int {
        if (position >= statement.size) return position

        for ((start, end) in QUOTE_PAIRS) {
            if (matchesPattern(statement, position, start)) {
                return findPatternEndIterative(statement, position + start.length, end)
            }
        }
        return position
    }

    /**
     * 匹配字符串模式
     * 
     * @param statement SQL 字符数组
     * @param position 起始位置
     * @param pattern 要匹配的模式
     * @return 是否匹配
     */
    private inline fun matchesPattern(statement: CharArray, position: Int, pattern: String): Boolean {
        if (position + pattern.length > statement.size) return false
        return pattern.indices.all { statement[position + it] == pattern[it] }
    }

    /**
     * 查找模式结束位置（迭代实现）
     * 
     * @param statement SQL 字符数组
     * @param startPos 搜索起始位置
     * @param pattern 结束模式
     * @return 模式结束位置
     */
    private fun findPatternEndIterative(statement: CharArray, startPos: Int, pattern: String): Int {
        var position = startPos
        
        while (position < statement.size) {
            if (matchesPattern(statement, position, pattern)) {
                return position + pattern.length
            }
            position++
        }
        
        return statement.size
    }

    /**
     * 判断字符是否为参数分隔符
     * 
     * @param char 要判断的字符
     * @return 是否为分隔符
     */
    private inline fun isParameterSeparator(char: Char): Boolean =
        (char.code < SEPARATOR_INDEX_SIZE && separatorIndex[char.code]) || char.isWhitespace()

    /**
     * 替换命名参数为 JDBC 占位符
     * 
     * 将命名参数（如 `:name`）替换为 JDBC 占位符（`?`）。
     * 对于集合类型的参数，会展开为多个占位符。
     * 
     * @param parsedSql 已解析的 SQL 对象
     * @param paramSource 参数值映射表，用于确定集合参数的展开数量
     * @return 替换后的 JDBC SQL 语句
     * 
     * Example:
     * ```kotlin
     * val parsed = parseSqlStatement("SELECT * FROM users WHERE id IN (:ids)")
     * val sql = substituteNamedParameters(parsed, mapOf("ids" to listOf(1, 2, 3)))
     * // sql = "SELECT * FROM users WHERE id IN (?, ?, ?)"
     * ```
     */
    fun substituteNamedParameters(parsedSql: ParsedSql, paramSource: Map<String, Any?>? = null): String {
        val paramNames = parsedSql.parameterNames
        if (paramNames.isEmpty()) return parsedSql.originalSql

        return buildString(parsedSql.originalSql.length) {
            var lastIndex = 0
            
            paramNames.forEachIndexed { i, paramName ->
                val (startIndex, endIndex) = parsedSql.parameterIndexes[i]
                append(parsedSql.originalSql, lastIndex, startIndex)
                
                val placeholders = generatePlaceholders(paramSource, paramName)
                append(placeholders)
                
                lastIndex = endIndex
            }
            
            append(parsedSql.originalSql, lastIndex, parsedSql.originalSql.length)
        }
    }

    /**
     * 生成参数占位符
     * 
     * 根据参数值类型生成相应数量的占位符：
     * - 单值：`?`
     * - 集合：`?, ?, ?`
     * - 数组元组：`(?, ?), (?, ?)`
     * 
     * @param paramSource 参数值映射表
     * @param paramName 参数名
     * @return 占位符字符串
     */
    private inline fun generatePlaceholders(paramSource: Map<String, Any?>?, paramName: String): String {
        if (paramSource == null) return "?"

        val value = getValueFromMap(paramSource, paramName)
        val list = value?.asListValue() ?: return "?"
        
        if (list.isEmpty()) return "?"

        return list.joinToString(", ") { item ->
            when (item) {
                is Array<*> -> item.joinToString(", ", "(", ")") { "?" }
                else -> "?"
            }
        }
    }

    /**
     * 构建参数值数组
     * 
     * 将命名参数映射表转换为 JDBC 参数数组，展开集合和数组。
     * 
     * @param parsedSql 已解析的 SQL 对象
     * @param paramSource 参数值映射表
     * @return JDBC 参数数组
     * @throws InvalidDataAccessApiUsageException 当混用命名参数和 `?` 占位符时
     * 
     * Example:
     * ```kotlin
     * val parsed = parseSqlStatement("INSERT INTO users VALUES (:id, :name)")
     * val params = buildValueArray(parsed, mapOf("id" to 1, "name" to "John"))
     * // params = [1, "John"]
     * ```
     */
    fun buildValueArray(parsedSql: ParsedSql, paramSource: Map<String, Any?>): Array<Any?> {
        validatePlaceholders(parsedSql)

        return buildList {
            parsedSql.parameterNames.forEach { paramName ->
                val value = getValueFromMap(paramSource, paramName)
                val list = value.asListValue()
                
                list.forEach { item ->
                    when (item) {
                        is Array<*> -> addAll(item)
                        else -> add(item)
                    }
                }
            }
        }.toTypedArray()
    }

    /**
     * 验证占位符使用是否合法
     * 
     * 不允许在同一 SQL 中混用命名参数和 `?` 占位符
     * 
     * @param parsedSql 已解析的 SQL 对象
     * @throws InvalidDataAccessApiUsageException 当混用时抛出异常
     */
    private inline fun validatePlaceholders(parsedSql: ParsedSql) {
        if (parsedSql.namedParameterCount > 0 && parsedSql.unnamedParameterCount > 0) {
            throw InvalidDataAccessApiUsageException(
                "Not allowed to mix named and traditional ? placeholders. " +
                "You have ${parsedSql.namedParameterCount} named parameter(s) and " +
                "${parsedSql.unnamedParameterCount} traditional placeholder(s) in statement: ${parsedSql.originalSql}"
            )
        }
    }

    /**
     * 从映射表中获取值，支持路径表达式
     * 
     * 支持的路径格式：
     * - 简单：`name`
     * - 属性：`user.name`
     * - 索引：`array[0]`
     * - 复杂：`records[0].user.name`
     * 
     * @param map 参数映射表
     * @param path 参数路径
     * @return 参数值，如果路径无效则返回 null
     */
    private fun getValueFromMap(map: Map<String, Any?>, path: String): Any? {
        // 简单路径优化：如果没有特殊字符，直接从 map 取值
        if (!path.contains('[') && !path.contains('.')) {
            return map[path]
        }
        
        val keys = parsePath(path)
        return keys.fold(map as Any?) { current, key -> 
            navigateToValue(current, key)
        }
    }

    /**
     * 导航到指定键的值
     * 
     * 根据当前值的类型选择合适的访问方式
     * 
     * @param current 当前值
     * @param key 键或索引
     * @return 导航后的值
     */
    private inline fun navigateToValue(current: Any?, key: String): Any? = when (current) {
        null -> null
        is Map<*, *> -> current[key]
        is KPojo -> current.toDataMap()[key]
        is Iterable<*>, is Array<*>, 
        is IntArray, is LongArray, is ShortArray, is ByteArray,
        is DoubleArray, is FloatArray, is BooleanArray -> 
            key.toIntOrNull()?.let { current.asListValue().getOrNull(it) }
        else -> null
    }

    /** 路径解析正则表达式（延迟初始化） */
    private val PATH_REGEX by lazy { """\.|(\[([0-9]+)])|(?<key>[^.\[\]]+)""".toRegex() }

    /**
     * 解析路径表达式
     * 
     * 将路径字符串解析为键列表
     * 
     * @param path 路径字符串，如 `array[0].property`
     * @return 键列表，如 `["array", "0", "property"]`
     */
    private fun parsePath(path: String): List<String> =
        PATH_REGEX.findAll(path)
            .mapNotNull { it.groups["key"]?.value ?: it.groups[2]?.value }
            .toList()

    /**
     * 将任意值转换为列表
     * 
     * 支持各种集合和数组类型的统一转换
     * 
     * @receiver 要转换的值
     * @return 转换后的列表
     */
    private fun Any?.asListValue(): List<Any?> = when (this) {
        null -> listOf(null)
        is Iterable<*> -> this.toList()
        is Array<*> -> this.toList()
        is IntArray -> this.toList()
        is LongArray -> this.toList()
        is ShortArray -> this.toList()
        is ByteArray -> this.toList()
        is DoubleArray -> this.toList()
        is FloatArray -> this.toList()
        is BooleanArray -> this.toList()
        else -> listOf(this)
    }
}