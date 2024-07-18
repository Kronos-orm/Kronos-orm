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

package com.kotlinorm.utils

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.enums.ConditionType.Companion.And
import com.kotlinorm.enums.ConditionType.Companion.Between
import com.kotlinorm.enums.ConditionType.Companion.Equal
import com.kotlinorm.enums.ConditionType.Companion.Ge
import com.kotlinorm.enums.ConditionType.Companion.Gt
import com.kotlinorm.enums.ConditionType.Companion.In
import com.kotlinorm.enums.ConditionType.Companion.IsNull
import com.kotlinorm.enums.ConditionType.Companion.Le
import com.kotlinorm.enums.ConditionType.Companion.Like
import com.kotlinorm.enums.ConditionType.Companion.Lt
import com.kotlinorm.enums.ConditionType.Companion.Or
import com.kotlinorm.enums.ConditionType.Companion.Regexp
import com.kotlinorm.enums.ConditionType.Companion.Root
import com.kotlinorm.enums.ConditionType.Companion.Sql
import com.kotlinorm.enums.NoValueStrategy.Companion.alwaysFalse
import com.kotlinorm.enums.NoValueStrategy.Companion.alwaysTrue
import com.kotlinorm.enums.NoValueStrategy.Companion.ignore
import com.kotlinorm.enums.NoValueStrategy.Companion.judgeNull
import com.kotlinorm.enums.NoValueStrategy.Companion.smart

/**
 * 工具类，用于根据条件对象构建 SQL 查询中的 WHERE 条件部分。
 */
object ConditionSqlBuilder {

    data class KotoBuildResultSet(
        val sql: String?,
        val paramMap: MutableMap<String, Any?>
    ) {
        fun toWhereClause(): Pair<String?, MutableMap<String, Any?>> {
            return toWhereSql(sql) to paramMap
        }

        fun toOnClause(): Pair<String?, MutableMap<String, Any?>> {
            return if (sql != null) {
                "ON $sql"
            } else {
                null
            } to paramMap
        }
    }

    data class KeyCounter(
        var initialized: Boolean = false,
        var metaOfMap: MutableMap<String, MutableMap<Int, Any?>> = mutableMapOf()
    )

    fun toWhereSql(sql: String?): String? {
        return if (sql != null) {
            "WHERE $sql"
        } else {
            null
        }
    }

    /**
     * 根据给定的条件类型构建对应的SQL查询条件。这里处理的是逻辑操作符（AND, OR）的情况。
     *
     * @param condition 当前处理的条件对象，包含逻辑操作类型和子条件。
     * @param paramMap 查询参数映射表。
     * @param needBrackets 判断是否需要在子条件周围添加括号。当子条件为逻辑操作符且与当前条件类型不同时，需要添加括号。
     * @param keyCounters 用于生成唯一键名的计数器。
     * @return 返回构建好的SQL条件字符串，如果条件为空则返回null。
     */
    fun buildConditionSqlWithParams(
        condition: Criteria?,
        paramMap: MutableMap<String, Any?>,
        needBrackets: Boolean = false,
        keyCounters: KeyCounter = KeyCounter(),
        showTable: Boolean = false
    ): KotoBuildResultSet {
        if (condition == null) { // 如果条件为 null，则直接返回
            return KotoBuildResultSet(null, paramMap)
        }

        if (condition.value == null && condition.valueAcceptable) { // 如果值为 null，且条件不允许值为 null，则进入无值策略处理
            when (condition.noValueStrategy) {
                ignore -> return KotoBuildResultSet(null, paramMap) // 直接返回
                judgeNull -> {
                    condition.type = IsNull
                } // 条件转为 ISNULL

                alwaysTrue -> {
                    condition.type = Sql
                    condition.value = "true"
                } // 条件转为 TRUE

                alwaysFalse -> {
                    condition.type = Sql
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
                        Equal -> condition.type = IsNull
                        Like, In, Between, Regexp -> return KotoBuildResultSet((!condition.not).toString(), paramMap)
                        Gt, Ge, Lt, Le -> return KotoBuildResultSet("false", paramMap)
                        else -> return KotoBuildResultSet(null, paramMap)
                    }
                }

                else -> throw UnsupportedOperationException()
            }
        }

        val sql = when (condition.type) {
            Root -> {
                listOf(
                    buildConditionSqlWithParams(condition.children.firstOrNull(), paramMap, showTable = showTable).sql
                )
            }

            Equal -> {
                if (condition.value is Field) listOfNotNull(
                    condition.field.quoted(showTable),
                    "!=".takeIf { condition.not } ?: "=",
                    (condition.value as Field).quoted(showTable))
                else {
                    val safeKey = getSafeKey(condition.field.name, keyCounters, paramMap, condition)
                    paramMap[safeKey] = condition.value
                    listOfNotNull(condition.field.quoted(showTable), "!=".takeIf { condition.not } ?: "=", ":$safeKey")
                }
            }

            IsNull -> listOfNotNull(
                condition.field.quoted(showTable), "IS", "NOT".takeIf { condition.not }, "NULL"
            )

            Sql -> listOf(condition.value.toString())

            Like -> {
                val safeKey = getSafeKey(condition.field.name, keyCounters, paramMap, condition)
                paramMap[safeKey] = "${condition.value}"
                listOfNotNull(
                    condition.field.quoted(showTable), "NOT".takeIf { condition.not }, "LIKE", ":${safeKey}"
                )
            }

            In -> {
                val safeKey = getSafeKey(condition.field.name + "List", keyCounters, paramMap, condition)
                paramMap[safeKey] = condition.value
                listOfNotNull(
                    condition.field.quoted(showTable), "NOT".takeIf { condition.not }, "IN", "(:${safeKey})"
                )
            }

            Gt, Ge, Lt, Le -> {
                if (condition.value is Field) listOfNotNull(
                    condition.field.quoted(showTable),
                    condition.type.value,
                    (condition.value as Field).quoted(showTable)
                )
                else {
                    val suffix = "Min".takeIf { condition.type in listOf(Gt, Ge) } ?: "Max"
                    val safeKey = getSafeKey(condition.field.name + suffix, keyCounters, paramMap, condition)
                    paramMap[safeKey] = condition.value
                    listOf(
                        condition.field.quoted(showTable), condition.type.value, ":${safeKey}"
                    )
                }
            }

            Between -> {
                val safeKeyMin = getSafeKey(condition.field.name + "Min", keyCounters, paramMap, condition)
                val safeKeyMax = getSafeKey(condition.field.name + "Max", keyCounters, paramMap, condition)
                val rangeValue = condition.value as ClosedRange<*>
                paramMap[safeKeyMin] = rangeValue.start
                paramMap[safeKeyMax] = rangeValue.endInclusive
                listOfNotNull(
                    condition.field.quoted(showTable),
                    "NOT".takeIf { condition.not },
                    "BETWEEN",
                    ":${safeKeyMin}",
                    "AND",
                    ":${safeKeyMax}"
                )
            }

            Regexp -> {
                val safeKey = getSafeKey(condition.field.name + "Pattern", keyCounters, paramMap, condition)
                paramMap[safeKey] = "${condition.value}"
                listOfNotNull(
                    condition.field.quoted(showTable), "NOT".takeIf { condition.not }, "REGEXP", ":${safeKey}"
                )
            }

            And, Or -> {
                // 将子条件转换为SQL字符串，并根据需要添加括号。
                val branches = condition.children.mapNotNull { child ->
                    val (childSql, _) = buildConditionSqlWithParams(
                        child,
                        paramMap,
                        needBrackets = child?.type.isLogicalOperator() && child?.type != condition.type,
                        keyCounters,
                        showTable = showTable
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
        }.filterNotNull().joinToString(" ")

        return KotoBuildResultSet(sql.ifEmpty { null }, paramMap)
    }

    // 辅助扩展函数，用于判断是否为逻辑操作符类型
    private fun ConditionType?.isLogicalOperator(): Boolean = this == And || this == Or

    // 这个函数用于生成一个安全的键名，以避免与现有的键名冲突
    fun getSafeKey(
        keyName: String,
        keyCounters: KeyCounter,
        dataMap: MutableMap<String, Any?>,
        data: Any
    ): String {
        // 如果 keyCounters 尚未初始化，则进行初始化
        if (!keyCounters.initialized) {
            keyCounters.initialized = true
            // 遍历paramMap中的所有键
            dataMap.keys.forEach { key ->
                // 使用解构声明从字符串中提取键和计数器
                val (k, c) = if (key.contains("@")) {
                    val split = key.split("@")
                    split[0] to split[1].toInt()
                } else {
                    key to 0
                }
                // 使用getOrPut函数简化向metaOfMap中添加新条目的过程
                keyCounters.metaOfMap.getOrPut(k) { mutableMapOf() }[c] = dataMap[key]
            }
        }

        // 获取与条件值匹配的键计数对
        val keyCount = keyCounters.metaOfMap[keyName]?.toList()?.firstOrNull { it.second == getValue(data) }

        // 如果没有匹配的键计数对，则创建新的键计数对
        return if (keyCount == null) {
            // 获取键的最大计数器值，如果不存在则默认为-1
            val counter = keyCounters.metaOfMap[keyName]?.keys?.maxOrNull() ?: -1
            // 添加新的键计数对
            keyCounters.metaOfMap.getOrPut(keyName) { mutableMapOf() }[counter + 1] = getValue(dataMap)
            if (counter + 1 == 0) keyName else "${keyName}@${counter + 1}"
        } else {
            // 如果存在匹配的键计数对，则返回相应的键名
            if (keyCount.first == 0) keyName else "${keyName}@${keyCount.first}"
        }
    }

    private fun getValue(data: Any): Any? {
        return when (data) {
            is Criteria -> data.value
            else -> data
        }
    }
}