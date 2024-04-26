package com.kotoframework.orm.update

import com.kotoframework.beans.dsl.Criteria
import com.kotoframework.beans.dsl.Field
import com.kotoframework.beans.dsl.KTable
import com.kotoframework.beans.dsl.KTableConditional
import com.kotoframework.enums.*
import com.kotoframework.interfaces.KPojo
import com.kotoframework.interfaces.KotoDataSourceWrapper
import com.kotoframework.types.KTableConditionalField
import com.kotoframework.types.KTableField
import com.kotoframework.utils.Extensions.toMap
import kotlin.reflect.full.createInstance

class UpdateClause<T : KPojo>(
    private val pojo: T,
    setUpdateFields: KTableField<T, Any?> = null
) {
    lateinit var tableName: String
    private var toUpdateFields: MutableSet<Field> = mutableSetOf()
    private var condition: Criteria? = null
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private var paramMapNew: MutableMap<Field, Any?> = mutableMapOf()

    init {
        paramMap.putAll(pojo.toMap().filter { it.value != null })
        if (setUpdateFields != null) {
            KTable(pojo::class.createInstance()).apply {
                setUpdateFields.invoke(this)
                toUpdateFields.addAll(fields)
            }
        }
    }

    fun set(rowData: KTableField<T, Unit>): UpdateClause<T> {
        KTable(pojo::class.createInstance()).apply {
            rowData?.invoke(this)
            fields.let { toUpdateFields.addAll(it) }
            paramMapNew.putAll(this.fieldParamMap)
        }
        return this
    }

    fun by(someFields: KTableField<T, Any?>): UpdateClause<T> {
        KTable(pojo::class.createInstance()).apply {
            someFields?.invoke(this)
            if (fields.isEmpty()) {
//                throw NeedUpdateConditionException(needUpdateConditionMessage)
            } else {
                condition = Criteria(
                    type = AND,
                ).apply {
                    children = fields.map {
                        Criteria(
                            type = Equal,
                            parameterName = it,
                            value = paramMap[it.propertyName]
                        )
                    }.toMutableList()
                }
            }
        }
        return this
    }

    fun where(updateCondition: KTableConditionalField<T, Boolean?> = null): UpdateClause<T> {
        KTableConditional(pojo::class.createInstance()).apply {
            updateCondition?.invoke(this)
            condition = criteria ?: Criteria(
                type = AND
            ).apply {
                children = paramMap.keys.map { propName ->
                    Criteria(
                        type = Equal,
                        parameterName = toUpdateFields.first { it.propertyName == propName },
                        value = paramMap[propName]
                    )
                }.toMutableList()
            }
        }
        return this
    }

    fun build(): Pair<String, Map<String, Any?>> {
        val updateFields =
            toUpdateFields.joinToString(", ")
            { "${it.propertyName} = :${it.propertyName + "New"}" }
        var conditionSql = buildConditionSql(condition)
        if (conditionSql != null) {
            conditionSql = "WHERE $conditionSql"
        }
        val sql = listOfNotNull("UPDATE", tableName, "SET", updateFields, conditionSql).joinToString(" ")
        // 合并 paramMap和paramMapNew
        paramMap.apply {
            putAll(paramMapNew.map { it.key.propertyName + "New" to it.value })
        }
        return Pair(sql, paramMap)
    }

    fun execute(wrapper: KotoDataSourceWrapper? = null) {
//        wrapper.orDefault().update("update xxx set xxx where xxx", paramMap)
    }

    operator fun component1() = build().first

    operator fun component2() = build().second

    /**
     * 根据条件对象构建 SQL 查询中的 WHERE 条件部分。
     *
     * @param condition 条件对象，可能为 null。如果非 null，则根据条件类型构建相应的 SQL 条件语句。
     * @return 对应的 SQL 条件语句字符串。如果输入条件为 null，则返回 null。
     */
    private fun buildConditionSql(condition: Criteria?): String? {
        if (condition == null) {
            return null
        }
        return when (condition.type) {
            ConditionType.EQUAL -> "${condition.parameterName} = :${condition.parameterName}"
            ConditionType.ISNULL -> "${condition.parameterName} IS NULL"
            ConditionType.SQL -> condition.sql

            // 处理 LIKE 条件类型，支持左右匹配、完全匹配和不匹配
            ConditionType.LIKE ->
                when (condition.pos) {
                    MatchPosition.Left -> "%${condition.parameterName}"
                    MatchPosition.Right -> "${condition.parameterName}%"
                    MatchPosition.Both -> "%${condition.parameterName}%"
                    MatchPosition.Never -> condition.parameterName.propertyName
                    else -> throw IllegalArgumentException("Invalid MatchPosition: ${condition.pos}")
                }

            ConditionType.IN -> "${condition.parameterName} IN (${
                condition.value.let { it as List<*> }.joinToString(", ")
            })"

            ConditionType.GT -> "${condition.parameterName} > :${condition.parameterName}"
            ConditionType.GE -> "${condition.parameterName} >= :${condition.parameterName}"
            ConditionType.LT -> "${condition.parameterName} < :${condition.parameterName}"
            ConditionType.LE -> "${condition.parameterName} <= :${condition.parameterName}"
            ConditionType.BETWEEN -> {
                // 处理 BETWEEN 条件类型，将范围值存储为参数，供 SQL 使用
                val rangeValue = condition.value.let { it as ClosedRange<*> }
                paramMap["${condition.parameterName}Min"] = rangeValue.start
                paramMap["${condition.parameterName}Max"] = rangeValue.endInclusive

                "${condition.parameterName} BETWEEN :${condition.parameterName}Min AND :${condition.parameterName}Max"
            }

            ConditionType.AND -> {
                // 处理逻辑与（AND）条件，将子条件连接成字符串
                condition.children.map { buildConditionSql(it) }
                    .joinToString(" AND ")
            }

            ConditionType.OR -> {
                // 处理逻辑或（OR）条件，将子条件连接成字符串
                condition.children.map { buildConditionSql(it) }
                    .joinToString(" OR ")
            }

            // 其他条件类型的处理逻辑...
            else -> error("Unsupported condition type: ${condition.type}")
        }
    }


}