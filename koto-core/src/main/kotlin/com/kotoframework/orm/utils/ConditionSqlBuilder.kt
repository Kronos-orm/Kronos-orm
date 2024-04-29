package com.kotoframework.orm.utils

import com.kotoframework.beans.dsl.Criteria
import com.kotoframework.enums.ConditionType

/**
 * 工具类，用于根据条件对象构建 SQL 查询中的 WHERE 条件部分。
 */
object ConditionSqlBuilder {

        /**
         * 构建 SQL 查询中的 WHERE 条件部分，并返回 SQL 字符串及对应的参数映射。
         *
         * @param condition 条件对象，可能为 null。如果非 null，则根据条件类型构建相应的 SQL 条件语句。
         * @return Pair<String?, MutableMap<String, Any?>>，第一个元素是 SQL 条件字符串，第二个元素是参数映射。
         *         如果输入条件为 null，则返回 Pair(null, emptyMap()).
         */
        fun buildConditionSqlWithParams(condition: Criteria?,paramMap:MutableMap<String,Any?>): Pair<String?, MutableMap<String, Any?>> {
            if (condition == null) {
                return Pair(null, mutableMapOf())
            }

            val sql = when (condition.type) {
                ConditionType.ROOT -> {
                    val childrenSqls = condition.children.mapNotNull { buildConditionSqlWithParams(it,paramMap).first }
                    if (childrenSqls.isEmpty()) null else childrenSqls.joinToString(" AND ")
                }

                ConditionType.NOT -> {
                    val childrenSqls = condition.children.mapNotNull { buildConditionSqlWithParams(it,paramMap).first }
                    if (childrenSqls.isEmpty()) null else "NOT (${childrenSqls.joinToString(" AND ")})"
                }

                ConditionType.EQUAL -> "${condition.field} ${if (condition.not) "!=" else "="} :${condition.field}"
                    .apply { paramMap.put("${condition.field}", condition.value) }
                ConditionType.ISNULL -> "${condition.field} IS${if (condition.not) " NOT " else " "}NULL"
                ConditionType.SQL -> condition.value.toString()
                ConditionType.LIKE -> {
                    condition.value?.let { paramMap.put(condition.field.toString(), it) }
                    "${condition.field}${if (condition.not) " NOT" else ""} LIKE :${condition.field}"
                }

                ConditionType.IN -> {
                    condition.value?.let { paramMap.put("${condition.field}List", it as Collection<*>) }
                    "${condition.field} ${if (condition.not) "NOT IN" else "IN"} (:${condition.field}List)"
                }

                ConditionType.GT -> {
                    condition.value?.let { paramMap.put("${condition.field}Min", it) }
                    "${condition.field} > :${condition.field}Min"
                }

                ConditionType.GE -> {
                    condition.value?.let { paramMap.put("${condition.field}Min", it) }
                    "${condition.field} >= :${condition.field}Min"
                }

                ConditionType.LT -> {
                    condition.value?.let { paramMap.put("${condition.field}Max", it) }
                    "${condition.field} < :${condition.field}Max"
                }

                ConditionType.LE -> {
                    condition.value?.let { paramMap.put("${condition.field}Max", it) }
                    "${condition.field} <= :${condition.field}Max"
                }

                ConditionType.BETWEEN -> {
                    val rangeValue = condition.value as ClosedRange<*>
                    paramMap["${condition.field}Min"] = rangeValue.start
                    paramMap["${condition.field}Max"] = rangeValue.endInclusive
                    "${condition.field} ${if (condition.not) "NOT BETWEEN" else "BETWEEN"} :${condition.field}Min AND :${condition.field}Max"
                }

                ConditionType.AND -> {
                    condition.children.mapNotNull { buildConditionSqlWithParams(it,paramMap).first }
                        .joinToString(" AND ")
                }

                ConditionType.OR -> {
                    condition.children.mapNotNull { buildConditionSqlWithParams(it,paramMap).first }
                        .joinToString(" OR ")
                }

                else -> throw IllegalArgumentException("Unsupported condition type: ${condition.type}")
            }

            return Pair(sql, paramMap)
        }
}