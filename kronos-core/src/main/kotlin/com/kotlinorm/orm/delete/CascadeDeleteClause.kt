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

package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KReference
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.CascadeDeleteAction.*
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.ConditionSqlBuilder.toWhereSql
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.setCommonStrategy
import java.util.*
import kotlin.reflect.full.createInstance

/**
 * Used to build a cascade delete clause.
 *
 * 构建级联删除子句。
 *
 * This object is used to construct a cascade delete clause for a database operation.
 * It contains a nested Counter class for counting operations, a nested ValidRef data class for storing references and referenced POJOs,
 * and several functions for building the cascade delete clause and generating SQL statements.
 *
 * The main function is build, which takes a POJO, a SQL where clause, a logic flag, a parameter map, and a delete task,
 * and returns an array of KronosAtomicActionTask objects representing the cascade delete operations.
 *
 * The other functions are helper functions used by build. They include findValidRefs, which finds valid references in a list of fields,
 * generateReferenceDeleteSql, which generates a delete SQL statement for a referenced POJO, and getDefaultUpdates, which generates a default update SQL clause.
 *
 */
object CascadeDeleteClause {
    class Counter {
        private var _counter = 0
        val counter get() = _counter++
    }

    data class ValidRef(
        val references: List<KReference>, val refPojo: KPojo
    ) {
        /**
         * Generates a list of tasks for each reference in the ValidRef object.
         *
         * This function generates a list of KronosAtomicActionTask objects for each reference in the ValidRef object.
         * It iterates over each reference, generates a delete SQL statement for the referenced POJO, and adds the SQL statement to the list of tasks.
         * If the reference has valid references of its own, it recursively generates tasks for those references as well.
         *
         * 此函数为 ValidRef 对象中的每个引用生成一个 KronosAtomicActionTask 对象列表。
         * 它遍历每个引用，为引用的 POJO 生成一个删除 SQL 语句，并将该 SQL 语句添加到任务列表中。
         * 如果引用有自己的有效引用，它也会为这些引用递归生成任务。
         *
         * @param original The original POJO that is being deleted.
         * @param whereClauseSql The SQL where clause for the delete operation.
         * @param logic The logic flag for the delete operation.
         * @param paramMap The map of parameters for the delete operation.
         * @param rootTask The root task of the delete operation.
         * @param stackOfTask The stack of tasks for the delete operation.
         * @param tempTableIndex The temporary table index for the delete operation.
         * @return List<KronosAtomicActionTask> returns a list of KronosAtomicActionTask objects representing the delete operations for the references.
         */
        fun generateTask(
            original: KPojo,
            whereClauseSql: String?,
            logic: Boolean,
            paramMap: MutableMap<String, Any?>,
            rootTask: KronosAtomicActionTask,
            stackOfTask: Stack<KronosAtomicActionTask> = Stack(), //用于防止循环引用
            tempTableIndex: Counter,
            layerCnt: Int = 0
        ): List<KronosAtomicActionTask> {
            // 生成删除任务列表
            val tasks = mutableListOf<KronosAtomicActionTask>()
            references.forEach { ref ->
                val tableName = original.kronosTableName() // 获取本表名
                val refTableName = refPojo.kronosTableName() // 获取引用表名
                val refColumnSql = ref.referenceColumns.joinToString(", ") { "`$refTableName`.`${it}`" } // 获取引用表本表的引用列
                val targetColumnSql = ref.targetColumns.joinToString(", ") { "`$tableName`.`${it}`" } // 获取目标表对应的引用列的SQL
                val subSelectClause = listOfNotNull(
                    "SELECT",
                    targetColumnSql,
                    "FROM",
                    "`$tableName`",
                    toWhereSql(whereClauseSql)
                ).joinToString(" ")// 构建子查询语句

                val newWhereClauseSql = // 生成新的 where 子句
                    "$refColumnSql IN ( SELECT ${ref.targetColumns.joinToString { "`$it`" }} from ($subSelectClause) as K_TMP_TB_${tempTableIndex.counter})"

                val validReferences = //递归获取下一级的引用
                    findValidRefs(
                        refPojo.kronosColumns().filter { !it.isColumn })
                val nextStepTask = generateReferenceDeleteSql( // 递归生成下一级的级联删除语句
                    refPojo, newWhereClauseSql, ref, logic, paramMap, rootTask
                ).toMutableList()

                if (stackOfTask.none { it.sql == nextStepTask.first().sql }) { // 若出现循环引用，不再递归
                    if (nextStepTask.size > 1) { // 如果级联删除的sql只有一个任务，说明为RESTRICT，不需要再次递归
                        tasks.addAll(
                            validReferences.map {
                                it.generateTask(
                                    refPojo,
                                    newWhereClauseSql,
                                    logic,
                                    paramMap,
                                    nextStepTask.first(),
                                    stackOfTask,
                                    tempTableIndex,
                                    layerCnt + 1
                                )
                            }.flatten().toTypedArray()
                        )
                    }
                    if (nextStepTask.isNotEmpty() && layerCnt > 0) nextStepTask.removeLast() // 防止生成重复任务
                    tasks.addAll(nextStepTask) // 添加任务
                }
                stackOfTask.addAll(tasks) // 添加任务到栈
            }
            return tasks
        }
    }

    /**
     * Build a cascade delete clause.
     * 构建级联删除子句。
     *
     * @param pojo The pojo to be deleted.
     * @param whereClauseSql The condition to be met.
     * @param logic The logic to be used.
     * @param paramMap The map of parameters.
     * @param rootTask The delete task.
     * @return The list of atomic tasks.
     */
    fun <T : KPojo> build(
        pojo: T,
        whereClauseSql: String?,
        logic: Boolean,
        paramMap: MutableMap<String, Any?>,
        rootTask: KronosAtomicActionTask
    ): KronosActionTask {
        val counter = Counter() //创建一个计数器，用于生成临时表的表名，防止临时表名发生重名
        val validReferences =
            findValidRefs(
                pojo.kronosColumns()
                    .filter { !it.isColumn }) // 获取所有的非数据库列、有关联注解且用于删除操作
        if (validReferences.isEmpty()) {
            // 若没有关联信息，返回空（在deleteClause的build中，有对null值的判断和默认值处理）
            // 为何不直接返回deleteTask: 因为此处的deleteTask构建sql语句时带有表名，而普通的deleteTask不带表名，因此需要重新构建
            return rootTask.toKronosActionTask()
        }

        return validReferences.map {
            // 生成删除任务列表
            // 为何要传入最初的deleteTask: 若cascade删除为RESTRICT，需要在deleteTask的sql语句中加入限制条件
            it.generateTask(
                pojo, whereClauseSql, logic, paramMap, rootTask, tempTableIndex = counter
            )
        }.flatten().toKronosActionTask()
    }

    /**
     * Finds valid references in a list of fields.
     *
     * This function iterates over the provided list of fields and for each field, it creates a ValidRef object.
     * A ValidRef object contains a list of references and a referenced POJO.
     * If the field has a cascade mapper, the list of references contains only the reference of the field.
     * Otherwise, the list of references contains all the references in the columns of the referenced POJO that have a cascade mapper for the table of the field.
     * The referenced POJO is created from the class name specified in the field.
     *
     * @param columns List<Field> the list of fields for which to find valid references.
     * @return List<ValidRef> returns a list of ValidRef objects representing the valid references in the fields.
     * @throws UnsupportedOperationException if the reference class is not supported.
     */
    private fun findValidRefs(columns: List<Field>): List<ValidRef> {
        //columns 为的非数据库列、有关联注解且用于删除操作的Field
        return columns.map { col ->
            val ref = Class.forName(
                col.referenceKClassName ?: throw UnsupportedOperationException("The reference class is not supported!")
            ).kotlin.createInstance() as KPojo // 通过反射创建引用的类的POJO，支持类型为KPojo/Collections<KPojo>
            ValidRef(if (col.cascadeMapperBy() && col.refUseFor(KOperationType.DELETE)) {
                listOf(col.reference!!) // 若有级联映射，返回引用
            } else {
                ref.kronosColumns().filter { it.cascadeMapperBy(col.tableName) && it.refUseFor(KOperationType.DELETE) }
                    .map { it.reference!! } // 若没有级联映射，返回引用的所有关于本表级联映射
            }, ref)
        }
    }

    /**
     * Generates a delete SQL statement for a referenced POJO.
     *
     * This function generates a delete SQL statement for a referenced POJO based on the onDelete action specified in the reference.
     * If the onDelete action is CASCADE, it generates an update or delete SQL statement.
     * If the onDelete action is RESTRICT, it adds a condition to the previous task's SQL statement to ensure that there are no references to the POJO.
     * If the onDelete action is SET_NULL, it generates an update SQL statement that sets the reference columns to null.
     * If the onDelete action is SET_DEFAULT, it generates an update SQL statement that sets the reference columns to their default values.
     *
     * @param pojo K the POJO that is being referenced.
     * @param whereClauseSql String? the SQL where clause for the delete operation.
     * @param reference KReference the reference for which to generate the delete SQL statement.
     * @param logic Boolean the logic flag for the delete operation.
     * @param paramMap MutableMap<String, Any?> the map of parameters for the delete operation.
     * @param prevTask KronosAtomicActionTask the previous task in the delete operation.
     * @return List<KronosAtomicActionTask>? returns a list of KronosAtomicActionTask objects representing the delete operations for the reference, or null if the onDelete action is not recognized.
     */
    private fun <K : KPojo> generateReferenceDeleteSql(
        pojo: K,
        whereClauseSql: String?,
        reference: KReference,
        logic: Boolean,
        paramMap: MutableMap<String, Any?>,
        prevTask: KronosAtomicActionTask,
    ): List<KronosAtomicActionTask> {
        val toUpdateFields = mutableListOf<Field>()
        var condition = whereClauseSql?.asSql()
        val params = mutableMapOf<String, Any?>().apply { putAll(paramMap) } // 复制参数
        val logicDeleteStrategy = pojo.kronosLogicDelete()

        if (logicDeleteStrategy.enabled) {
            setCommonStrategy(logicDeleteStrategy) { field, value ->
                condition = listOfNotNull(
                    condition, "${field.quoted(true)} = $value".asSql()
                ).toCriteria()
            }
        }

        val (newWhereClauseSql) = ConditionSqlBuilder.buildConditionSqlWithParams(
            condition, mutableMapOf(), showTable = true
        )

        if (logicDeleteStrategy.enabled) { //判断是否为逻辑删除
            val updateInsertFields = { field: Field, _: Any? ->
                toUpdateFields += Field(
                    columnName = field.name, name = field.name + "New", tableName = pojo.kronosTableName()
                )
            }
            setCommonStrategy(logicDeleteStrategy, deleted = true, callBack = updateInsertFields)
        }

        return when (reference.onDelete) { // 生成删除语句
            CASCADE -> {
                listOf(if (logic) {
                    KronosAtomicActionTask("UPDATE `${pojo.kronosTableName()}` SET ${
                        toUpdateFields.joinToString {
                            it.equation(
                                true
                            )
                        }
                    } WHERE $newWhereClauseSql", params, KOperationType.DELETE)
                } else {
                    KronosAtomicActionTask(
                        "DELETE FROM `${pojo.kronosTableName()}` WHERE $newWhereClauseSql",
                        params,
                        KOperationType.DELETE
                    )
                }, prevTask)
            }

            RESTRICT -> {
                listOf(prevTask.apply {
                    sql += " AND (SELECT count(1) FROM `${pojo.kronosTableName()}` WHERE $newWhereClauseSql LIMIT 1) = 0"
                })
            }

            SET_NULL -> {
                listOf(KronosAtomicActionTask("UPDATE `${pojo.kronosTableName()}` SET ${
                    reference.referenceColumns.joinToString(", ") {
                        "`${pojo.kronosTableName()}`.`$it` = null"
                    }
                } WHERE $newWhereClauseSql", params, KOperationType.DELETE), prevTask)
            }

            SET_DEFAULT -> {
                listOf(
                    KronosAtomicActionTask(
                        "UPDATE `${pojo.kronosTableName()}` SET ${
                            getDefaultUpdates(pojo.kronosTableName(), reference, params)
                        } WHERE $newWhereClauseSql", params
                    ), prevTask
                )
            }

            else -> listOf(prevTask)
        }
    }

    /**
     * Generates the default update SQL clause for the given table and reference.
     *
     * This function generates the default update SQL clause for the given table and reference.
     * It iterates over each column in the reference and checks if the column is in the paramMap.
     * If the column is not in the paramMap, it adds the column to the paramMap with the default value from the reference.
     * It then creates a pair of the column name and the key in the paramMap for the column.
     * Finally, it joins the pairs into a string with the format "`tableName`.`columnName` = :key" and returns the string.
     *
     * @param tableName String the name of the table for which to generate the default update SQL clause.
     * @param reference KReference the reference for which to generate the default update SQL clause.
     * @param paramMap MutableMap<String, Any?> the map of parameters to use in the SQL clause.
     * @return String returns the default update SQL clause for the given table and reference.
     */
    private fun getDefaultUpdates(
        tableName: String, reference: KReference, paramMap: MutableMap<String, Any?>
    ): String {
        var counter = 0
        var key = "defaultVal${counter++}"
        return reference.referenceColumns.mapIndexed { i, _ ->
            while (paramMap.containsKey(key)) {
                key = "defaultVal${counter++}"
            }
            paramMap[key] = reference.defaultValue[i]
            reference.referenceColumns[i] to key
        }.joinToString(", ") {
            "`${tableName}`.`${it.first}` = :${it.second}"
        }
    }
}