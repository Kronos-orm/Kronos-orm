package com.kotoframework.utils

import com.kotoframework.beans.dsl.Criteria
import com.kotoframework.beans.dsl.Field
import com.kotoframework.enums.*

/**
 * 工具类，用于根据条件对象构建 SQL 查询中的 WHERE 条件部分。
 */
object ConditionSqlBuilder {

    data class KotoBuildResultSet(
        val sql: String?,
        val paramMap: MutableMap<String, Any?>
    )

    data class KeyCounter(
        var initialized: Boolean = false,
        var metaOfMap: MutableMap<String, MutableMap<Int, Any?>> = mutableMapOf()
    )

    /**
     * 构建 SQL 查询中的 WHERE 条件部分，并返回 SQL 字符串及对应的参数映射。
     *
     * @param condition 条件对象，可能为 null。如果非 null，则根据条件类型构建相应的 SQL 条件语句。
     * @return Pair<String?, MutableMap<String, Any?>>，第一个元素是 SQL 条件字符串，第二个元素是参数映射。
     *         如果输入条件为 null，则返回 Pair(null, emptyMap()).
     */
    fun buildConditionSqlWithParams(
        condition: Criteria?,
        paramMap: MutableMap<String, Any?>,
        needBrackets: Boolean = false,
        keyCounters: KeyCounter = KeyCounter()
    ): KotoBuildResultSet {
        if (condition == null) { // 如果条件为 null，则直接返回
            return KotoBuildResultSet(null, paramMap)
        }

        if (condition.value == null && condition.valueAcceptable) { // 如果值为 null，且条件不允许值为 null，则进入无值策略处理
            when (condition.noValueStrategy) {
                ignore -> return KotoBuildResultSet(null, paramMap) // 直接返回
                judgeNull -> {
                    condition.type = ISNULL
                } // 条件转为 ISNULL

                alwaysTrue -> {
                    condition.type = SQL
                    condition.value = "true"
                } // 条件转为 TRUE

                alwaysFalse -> {
                    condition.type = SQL
                    condition.value = "false"
                } // 条件转为 FALSE

                smart -> {/*
                    *   无值策略：根据条件类型进行转换
                    *   当条件类型为“Equal”时，将其修改为“ISNULL”。
                    *   当条件类型为“Like”、“In”或者“BETWEEN”时，返回相应的操作描述和参数映射表。
                    *   当条件类型为“GT”、“GE”、“LT”或者“LE”时，返回false值和参数映射表。
                    *   对于其他条件类型(SQL)，返回null值和参数映射表。
                    *
                    * */
                    when (condition.type) {
                        Equal -> condition.type = ISNULL
                        Like, In, BETWEEN -> return KotoBuildResultSet((!condition.not).toString(), paramMap)
                        GT, GE, LT, LE -> return KotoBuildResultSet("false", paramMap)
                        else -> return KotoBuildResultSet(null, paramMap)
                    }
                }

                else -> throw UnsupportedOperationException()
            }
        }

        // 这个函数用于生成一个安全的键名，以避免与现有的键名冲突
        fun getSafeKey(keyName: String): String {
            // 如果 keyCounters 尚未初始化，则进行初始化
            if (!keyCounters.initialized) {
                keyCounters.initialized = true
                // 遍历paramMap中的所有键
                paramMap.keys.forEach { key ->
                    // 使用解构声明从字符串中提取键和计数器
                    val (k, c) = if (key.contains("@")) {
                        val split = key.split("@")
                        split[0] to split[1].toInt()
                    } else {
                        key to 0
                    }
                    // 使用getOrPut函数简化向metaOfMap中添加新条目的过程
                    keyCounters.metaOfMap.getOrPut(k) { mutableMapOf() }[c] = paramMap[key]
                }
            }

            // 获取与条件值匹配的键计数对
            val keyCount = keyCounters.metaOfMap[keyName]?.toList()?.firstOrNull { it.second == condition.value }

            // 如果没有匹配的键计数对，则创建新的键计数对
            return if (keyCount == null) {
                // 获取键的最大计数器值，如果不存在则默认为-1
                val counter = keyCounters.metaOfMap[keyName]?.keys?.maxOrNull() ?: -1
                // 添加新的键计数对
                keyCounters.metaOfMap.getOrPut(keyName) { mutableMapOf() }[counter + 1] = condition.value
                if (counter + 1 == 0) keyName else "${keyName}@${counter + 1}"
            } else {
                // 如果存在匹配的键计数对，则返回相应的键名
                if (keyCount.first == 0) keyName else "${keyName}@${keyCount.first}"
            }
        }

        val sql = when (condition.type) {
            Root -> {
                listOf(
                    buildConditionSqlWithParams(condition.children.firstOrNull(), paramMap).sql
                )
            }

            Equal -> {
                val safeKey = getSafeKey(condition.field.name)
                paramMap[safeKey] = condition.value
                listOfNotNull(condition.field.quotedColumnName(), "!=".takeIf { condition.not } ?: "=", ":$safeKey")
            }

            ISNULL -> listOfNotNull(
                condition.field.quotedColumnName(), "IS", "NOT".takeIf { condition.not }, "NULL"
            )

            SQL -> listOf(condition.value.toString())

            Like -> {
                val safeKey = getSafeKey(condition.field.name)
                paramMap[safeKey] = condition.value
                listOfNotNull(
                    condition.field.quotedColumnName(), "NOT".takeIf { condition.not }, "LIKE", ":${safeKey}"
                )
            }

            In -> {
                val safeKey = getSafeKey(condition.field.name + "List")
                paramMap[safeKey] = condition.value
                listOfNotNull(
                    condition.field.quotedColumnName(), "NOT".takeIf { condition.not }, "IN", "(:${safeKey})"
                )
            }

            GT, GE, LT, LE -> {
                val suffix = "Min".takeIf { condition.type in listOf(GT, GE) } ?: "Max"
                val safeKey = getSafeKey(condition.field.name + suffix)
                val sign = mapOf(
                    GT to ">", GE to ">=", LT to "<", LE to "<="
                )
                paramMap[safeKey] = condition.value
                listOf(
                    condition.field.quotedColumnName(), sign[condition.type], ":${safeKey}"
                )
            }

            BETWEEN -> {
                val safeKeyMin = getSafeKey(condition.field.name + "Min")
                val safeKeyMax = getSafeKey(condition.field.name + "Max")
                val rangeValue = condition.value as ClosedRange<*>
                paramMap[safeKeyMin] = rangeValue.start
                paramMap[safeKeyMax] = rangeValue.endInclusive
                listOfNotNull(
                    condition.field.quotedColumnName(), "NOT".takeIf { condition.not }, "BETWEEN", ":${safeKeyMin}", "AND", ":${safeKeyMax}"
                )
            }

            /**
             * 根据给定的条件类型构建对应的SQL查询条件。这里处理的是逻辑操作符（AND, OR）的情况。
             *
             * @param condition 当前处理的条件对象，包含逻辑操作类型和子条件。
             * @param paramMap 查询参数映射表。
             * @param needBrackets 判断是否需要在子条件周围添加括号。当子条件为逻辑操作符且与当前条件类型不同时，需要添加括号。
             * @return 返回构建好的SQL条件字符串，如果条件为空则返回null。
             */
            AND, OR -> {
                // 将子条件转换为SQL字符串，并根据需要添加括号。
                val branches = condition.children.mapNotNull { child ->
                    val (childSql, _) = buildConditionSqlWithParams(
                        child,
                        paramMap,
                        needBrackets = child?.type.isLogicalOperator() && child?.type != condition.type,
                        keyCounters
                    )
                    childSql
                }

                // 当没有子条件时返回null，否则根据逻辑操作类型（AND, OR）连接所有子条件SQL。
                val joinKeyword = " ${condition.type.name} "
                listOf(
                    branches.joinToString(joinKeyword).let {
                        "($it)".takeIf { needBrackets } ?: it
                    }
                )
            }

            else -> throw IllegalArgumentException("Unsupported condition type: ${condition.type}")
        }.joinToString(" ")

        return KotoBuildResultSet(sql, paramMap)
    }

    // 辅助扩展函数，用于判断是否为逻辑操作符类型
    private fun ConditionType?.isLogicalOperator(): Boolean = this == AND || this == OR
    private fun Field.quotedColumnName(): String = "`$columnName`"
}