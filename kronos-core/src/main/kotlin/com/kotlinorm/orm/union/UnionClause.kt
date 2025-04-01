package com.kotlinorm.orm.union

import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.SelectClause
import com.kotlinorm.utils.ConditionSqlBuilder

open class UnionClause(tasks: List<SelectClause<out KPojo>>) {
    private val tasks = tasks.toMutableList()
    private var builtTasks: List<KronosQueryTask> = listOf()
    private var safeKeyList: List<String> = listOf()
    private val fieldNameMapping = mutableMapOf<String, MutableMap<String, String>>()

    fun query(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any?>> {
        return executeQuery { it.query(wrapper) }
    }

    fun queryMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?> {
        return executeQuery { listOf(it.queryMap(wrapper)) }.first()
    }

    fun queryMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?>? {
        return executeQuery { listOfNotNull(it.queryMapOrNull(wrapper)) }.firstOrNull()
    }

    private fun executeQuery(
        queryFunction: (KronosQueryTask) -> List<Map<String, Any?>>
    ): List<Map<String, Any?>> {
        prepareTasks()
        val safeKeyList = getUniqueTask(ConditionSqlBuilder.KeyCounter())

        val resultMap = safeKeyList.mapIndexed { index, safeKey ->
            val result = queryFunction(builtTasks[index])
            safeKey to result
        }.toMap()

        return if (resultMap.isEmpty()) emptyList() else union(resultMap)
    }

    private fun prepareTasks() {
        builtTasks = tasks.map { it.build() }
    }

    private fun getUniqueTask(keyCounters: ConditionSqlBuilder.KeyCounter): List<String> {
        val fieldCountMap = mutableMapOf<String, Int>()
        safeKeyList = tasks.map { task ->
            val safeKey = ConditionSqlBuilder.getSafeKey(
                task.pojo::class.simpleName.toString(), keyCounters, mutableMapOf(), task
            )
            task.selectFields.forEach { field ->
                fieldCountMap[field.name] = fieldCountMap.getOrDefault(field.name, 0) + 1
            }
            safeKey
        }

        fieldNameMapping.clear()
        tasks.forEachIndexed { index, task ->
            val safeKey = safeKeyList[index]
            fieldNameMapping[safeKey] = task.selectFields.associate { field ->
                val fieldName = field.name
                fieldName to if (fieldCountMap[fieldName]!! > 1) "$safeKey${capitalizeFirstLetter(fieldName)}" else fieldName
            }.toMutableMap()
        }
        return safeKeyList
    }

    private fun union(resultMap: Map<String, List<Map<String, Any?>>>): List<Map<String, Any?>> {
        val maxRowNum = resultMap.values.maxOfOrNull { it.size } ?: 0

        val allFields = resultMap.flatMap { (safeKey, list) ->
            list.flatMap { it.keys.map { key -> fieldNameMapping[safeKey]!![key]!! } }
        }.toSet()

        return List(maxRowNum) { index ->
            mutableMapOf<String, Any?>().apply {
                allFields.forEach { this[it] = null }

                resultMap.forEach { (safeKey, list) ->
                    val fieldNameMap = fieldNameMapping[safeKey]!!
                    list.getOrNull(index)?.forEach { (key, value) ->
                        this[fieldNameMap[key]!!] = value
                    }
                }
            }
        }
    }

    private fun capitalizeFirstLetter(s: String): String {
        return s.replaceFirstChar { it.uppercaseChar() }
    }
}