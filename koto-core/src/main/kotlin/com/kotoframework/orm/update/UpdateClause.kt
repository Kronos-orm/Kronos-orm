package com.kotoframework.orm.update

import com.kotoframework.beans.dsl.Criteria
import com.kotoframework.beans.dsl.Field
import com.kotoframework.beans.dsl.KTable
import com.kotoframework.beans.dsl.KTableConditional
import com.kotoframework.enums.*
import com.kotoframework.exceptions.NeedUpdateConditionException
import com.kotoframework.interfaces.KPojo
import com.kotoframework.interfaces.KotoDataSourceWrapper
import com.kotoframework.orm.utils.ConditionSqlBuilder
import com.kotoframework.types.KTableConditionalField
import com.kotoframework.types.KTableField
import com.kotoframework.utils.Extensions.toMap
import kotlin.reflect.full.createInstance

class UpdateClause<T : KPojo>(
    private val pojo: T, setUpdateFields: KTableField<T, Any?> = null
) {
    var isExcept: Boolean = false
    lateinit var tableName: String
    var allFields: MutableSet<Field> = mutableSetOf()
    private var toUpdateFields: MutableSet<Field> = mutableSetOf()
    private var condition: Criteria? = null
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private var paramMapNew: MutableMap<Field, Any?> = mutableMapOf()

    init {
        paramMap.putAll(pojo.toMap().filter { it.value != null })
        if (setUpdateFields != null) {
            with(KTable(pojo::class.createInstance())) {
                setUpdateFields()
                toUpdateFields.addAll(fields)
                toUpdateFields.forEach {
                    paramMapNew[
                        Field(
                            it.columnName,
                            it.name + "New"
                        )
                    ] = paramMap[it.name]
                }
            }
        }
    }

    fun set(rowData: KTableField<T, Unit>): UpdateClause<T> {
        if (rowData == null) return this
        with(KTable(pojo::class.createInstance())) {
            rowData()
            toUpdateFields.addAll(fields)
            paramMapNew.putAll(fieldParamMap)
        }
        return this
    }

    fun by(someFields: KTableField<T, Any?>): UpdateClause<T> {
        if (someFields == null) return this
        with(KTable(pojo::class.createInstance())) {
            someFields()
            if (fields.isEmpty()) {
                throw NeedUpdateConditionException()
            }
            condition = Criteria(
                type = AND,
            ).apply {
                children = fields.map {
                    Criteria(
                        type = Equal,
                        field = it,
                        value = paramMap[it.name]
                    )
                }.toMutableList()
            }
        }
        return this
    }

    fun where(updateCondition: KTableConditionalField<T, Boolean?> = null): UpdateClause<T> {
        if (updateCondition == null) return this
        with(KTableConditional(pojo::class.createInstance())) {
            propParamMap = paramMap
            updateCondition()
            condition = criteria ?: Criteria(
                type = AND
            ).apply {
                children = paramMap.keys.map { propName ->
                    Criteria(
                        type = Equal,
                        field = toUpdateFields.first { it.name == propName },
                        value = paramMap[propName]
                    )
                }.toMutableList()
            }
        }
        return this
    }

    fun build(): Pair<String, Map<String, Any?>> {
        // 如果 isExcept 为 true，则将 toUpdateFields 中的字段从 allFields 中移除
        if (isExcept) {
            toUpdateFields =
                allFields.filter { !toUpdateFields.any { f -> f.columnName == it.columnName } }.toMutableSet()
            toUpdateFields.forEach {
                paramMapNew[Field(
                    it.columnName, it.name + "New"
                )] = paramMap[it.name]
            }
        }

        if (toUpdateFields.size == 0) {
            // 全都更新
            toUpdateFields = allFields.toMutableSet()
        }

        val updateFields = toUpdateFields.joinToString(", ") { "${it.name} = :${it.name + "New"}" }

        var (conditionSql,paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, paramMap)
        if (conditionSql != null) {
            conditionSql = "WHERE $conditionSql"
        }
        val sql = listOfNotNull("UPDATE", tableName, "SET", updateFields, conditionSql).joinToString(" ")
        // 合并
        paramMap.apply {
            putAll(paramMapNew.map { entry ->
                // 如果 key 不以 "New" 结尾，则添加 "New" 后缀
                val keyWithSuffix = if (!entry.key.name.endsWith("New")) entry.key.name + "New" else entry.key.name
                keyWithSuffix to entry.value
            })
        }
        return Pair(sql, paramMap)
    }

    fun execute(wrapper: KotoDataSourceWrapper? = null) {
//        wrapper.orDefault().update("update xxx set xxx where xxx", paramMap)
    }

    operator fun component1() = build().first

    operator fun component2() = build().second

//    /**
//     * 根据条件对象构建 SQL 查询中的 WHERE 条件部分。
//     *
//     * @param condition 条件对象，可能为 null。如果非 null，则根据条件类型构建相应的 SQL 条件语句。
//     * @return 对应的 SQL 条件语句字符串。如果输入条件为 null，则返回 null。
//     */
//    private fun buildConditionSql(condition: Criteria?): String? {
//        if (condition == null) {
//            return null
//        }
//        return when (condition.type) {
//            ConditionType.ROOT -> {
//                val childrenSql = condition.children.map { buildConditionSql(it) }.joinToString(" AND ")
//                if (childrenSql.isEmpty()) {
//                    null
//                } else {
//                    childrenSql
//                }
//            }
//
//            ConditionType.NOT -> {
//                val childrenSql = condition.children.map { buildConditionSql(it) }.joinToString(" AND ")
//                if (childrenSql.isEmpty()) {
//                    null
//                } else {
//                    "NOT ($childrenSql)"
//                }
//            }
//
//            ConditionType.EQUAL -> "${condition.field} ${if (condition.not) "!=" else "="} :${condition.field}"
//
//            ConditionType.ISNULL -> "${condition.field} IS${if (condition.not) " NOT " else " "}NULL"
//
//            ConditionType.SQL -> condition.sql
//
//            // 处理 LIKE 条件类型，支持左右匹配、完全匹配和不匹配
//            ConditionType.LIKE -> {
//                "${condition.field}${if (condition.not) " NOT" else ""} LIKE :${condition.field}".apply {
//                    paramMap.put("${condition.field}", condition.value)
//                }
//            }
//
//            ConditionType.IN -> "${condition.field} ${if (condition.not) "NOT IN" else "IN"} (:${condition.field}List)".apply {
//                paramMap.put("${condition.field}List", condition.value)
//            }
//
//
//            ConditionType.GT -> "${condition.field} > " + ":${condition.field}Min".apply {
//                paramMap.put("${condition.field}Min", condition.value)
//            }
//
//            ConditionType.GE -> "${condition.field} >= " + ":${condition.field}Min".apply {
//                paramMap.put("${condition.field}Min", condition.value)
//            }
//
//            ConditionType.LT -> "${condition.field} < " + ":${condition.field}Max".apply {
//                paramMap.put("${condition.field}Max", condition.value)
//            }
//
//            ConditionType.LE -> "${condition.field} <= " + ":${condition.field}Max".apply {
//                paramMap.put("${condition.field}Max", condition.value)
//            }
//
//
//            ConditionType.BETWEEN -> {
//                "${condition.field} ${if (condition.not) "NOT BETWEEN" else "BETWEEN"} :${condition.field}Min AND :${condition.field}Max".apply {
//                    // 处理 BETWEEN 条件类型，将范围值存储为参数，供 SQL 使用
//                    val rangeValue = condition.value.let { it as ClosedRange<*> }
//                    paramMap["${condition.field}Min"] = rangeValue.start
//                    paramMap["${condition.field}Max"] = rangeValue.endInclusive
//                }
//            }
//
//            ConditionType.AND -> {
//                // 处理逻辑与（AND）条件，将子条件连接成字符串
//                condition.children.map { buildConditionSql(it) }.joinToString(" AND ")
//            }
//
//            ConditionType.OR -> {
//                // 处理逻辑或（OR）条件，将子条件连接成字符串
//                condition.children.map { buildConditionSql(it) }.joinToString(" OR ")
//            }
//
//            // 其他条件类型的处理逻辑...
//            else -> error("Unsupported condition type: ${condition.type}")
//        }
//    }


}