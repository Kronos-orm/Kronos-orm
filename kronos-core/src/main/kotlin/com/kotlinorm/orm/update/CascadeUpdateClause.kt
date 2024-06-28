package com.kotlinorm.orm.update

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.utils.query
import com.kotlinorm.utils.setCommonStrategy
import java.util.*
import kotlin.reflect.full.createInstance

object CascadeUpdateClause {
    /**
     * Build a cascade update clause.
     * 构建级联更新子句。
     *
     * @param pojo The pojo to be deleted.
     * @param condition The condition to be met.
     * @return The list of atomic tasks.
     */
    fun <T : KPojo> build(
        pojo: T,
        whereClauseSql: String?,
        paramMap: MutableMap<String, Any?>,
        rootTask: KronosAtomicActionTask
    ): KronosActionTask {

        // 筛选出所有被修改的列
        val columns = pojo.kronosColumns()
        val updatedColumns = mutableListOf<Field>()
        paramMap.forEach { param ->
            val updatedData = columns.find { it.name + "New" == param.key }
            if (updatedData != null) updatedColumns.add(updatedData)
        }

        return generateTask(pojo, paramMap, columns.filter {!it.isColumn }, updatedColumns, rootTask, whereClauseSql)
    }

    private fun generateTask(
        originalPojo: KPojo,
        paramMap: MutableMap<String, Any?>,
        columns: List<Field>,
        updatedColumns: List<Field>,
        prevTask: KronosAtomicActionTask,
        whereClauseSql: String?
    ): KronosActionTask {

        return prevTask.toKronosActionTask().doBeforeExecute {

            val selectFields = updatedColumns.joinToString(", ") { it.quoted() }
            val updatedColumnRecords = KronosAtomicQueryTask(
                listOfNotNull(
                    "SELECT",
                    selectFields,
                    "FROM",
                    "`${originalPojo.kronosTableName()}`",
                    whereClauseSql
                ).joinToString(" "),
                paramMap
            ).query()

//            val updatedColumnVals: MutableMap<String, MutableList<Any?>> = mutableMapOf()
//            updatedColumnRecords.forEach { item ->
//                item.keys.forEach { key ->
//                    val newKey = key + "List"
//                    if (updatedColumnVals.contains(newKey)) updatedColumnVals[key]!!.add(item[key])
//                    else updatedColumnVals[newKey] = mutableListOf(item[key])
//                }
//            }

            updatedColumnRecords.forEachIndexed { i, r ->
                r.keys.forEach {
                    paramMap["${it}RecordValue${i}"] = r[it]
                }
            }

            columns.map { col ->

                val ref = Class.forName(
                    col.referenceKClassName
                        ?: throw UnsupportedOperationException("The reference class is not supported!")
                ).kotlin.createInstance() as KPojo

                val cascadeRefs = ref.kronosColumns()
                    .filter { it.cascadeMapperBy(col.tableName) && it.refUseFor(KOperationType.UPDATE) }

                val logicDeleteStrategy = ref.kronosLogicDelete()
                val updateTimeStrategy = ref.kronosUpdateTime()

                fun generateUpdateTask(
                    refColumns: Array<String>,
                    targetColumns: Array<String>
                ): KronosAtomicActionTask {

                    val paramMapNew: MutableMap<String, Any?> = mutableMapOf()

                    //主表关联字段（实体属性名）
                    val referred = refColumns.map { item ->
                        val column = originalPojo.kronosColumns().find { it.columnName == item }
                        column!!.name
                    }

                    var updateColumn: Field? = null
                    var logicColumn: Field? = null

                    setCommonStrategy(logicDeleteStrategy, true) { field, value ->
                        logicColumn = field
                        paramMapNew[field.name] = value
                    }
                    setCommonStrategy(updateTimeStrategy, true) { field, value ->
                        updateColumn = field
                        paramMapNew[field.name + "New"] = value
                    }

                    var updateFields = refColumns.mapIndexed { i, _ ->
                        if (paramMap.contains(refColumns[i] + "New") && null != paramMap[refColumns[i] + "New"]) {
                            "`${targetColumns[i]}` = :${referred[i] + "New"}"
                        } else null
                    }.joinToString(", ")
                    if (updateTimeStrategy.enabled) {
                        updateFields += ", ${updateColumn!!.quoted()} = :${updateColumn!!.name}New"
                    }

                    val whereSql =
                        // 具体帅选条件+逻辑删除条件
                        "WHERE " + listOfNotNull(
                            //可能的所有取值的条件并列
                            "( " + updatedColumnRecords.mapIndexed { i , r ->
                                //单种取值方式需要满足的条件
                                "( " + targetColumns.mapIndexed { j, _ ->
                                    "`${targetColumns[j]}` = :${referred[j]}RecordValue${i}"
                                }.joinToString(" AND ") + " )"
                            }.joinToString(" OR ") + " )",
                            if (logicDeleteStrategy.enabled) "${logicColumn!!.quoted()} = :${logicColumn}" else null
                        ).joinToString(" AND ")

                    val sql = listOfNotNull(
                        "UPDATE",
                        "`${ref.kronosTableName()}`",
                        "SET",
                        updateFields,
                        whereSql
                    ).joinToString(" ")

                   paramMapNew += paramMap
                    return KronosAtomicActionTask(
                        sql,
                        paramMapNew,
                        KOperationType.UPDATE
                    )
                }

                return@map if ((col.cascadeMapperBy() && col.refUseFor(KOperationType.UPDATE)) || cascadeRefs.isNotEmpty()) {

                    if (cascadeRefs.isEmpty()) { //维护关系定义在主表
                        //主表关联字段
                        val refColumns = col.reference!!.referenceColumns
                        //子表关联字段
                        val targetColumns = col.reference.targetColumns

                        atomicTasks.add(generateUpdateTask(refColumns, targetColumns))

                    } else {                    //维护关系定义在子表
                        cascadeRefs.forEach {
                            //主表关联字段
                            val refColumns = it.reference!!.targetColumns
                            //子表关联字段
                            val targetColumns = it.reference.referenceColumns

                            atomicTasks.add(generateUpdateTask(refColumns, targetColumns))
                        }
                    }
                } else null
            }
        }
    }
}