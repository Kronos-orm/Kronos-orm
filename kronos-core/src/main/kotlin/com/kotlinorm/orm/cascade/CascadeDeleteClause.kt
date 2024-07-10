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

package com.kotlinorm.orm.cascade

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.CascadeDeleteAction.RESTRICT
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.orm.cascade.NodeOfKPojo.Companion.toTreeNode
import com.kotlinorm.orm.delete.DeleteClause.Companion.build
import com.kotlinorm.orm.delete.DeleteClause.Companion.cascade
import com.kotlinorm.orm.delete.DeleteClause.Companion.logic
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.KStack
import com.kotlinorm.utils.pop
import com.kotlinorm.utils.push

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
    /**
     * Build a cascade delete clause.
     * 构建级联删除子句。
     *
     * @param cascadeEnabled Whether the cascade is enabled.
     * @param pojo The pojo to be deleted.
     * @param whereClauseSql The condition to be met.
     * @param logic The logic to be used.
     * @param rootTask The delete task.
     * @return The list of atomic tasks.
     */
    fun <T : KPojo> build(
        cascadeEnabled: Boolean,
        pojo: T,
        whereClauseSql: String?,
        logic: Boolean,
        rootTask: KronosAtomicActionTask
    ): KronosActionTask {
        if (!cascadeEnabled) return rootTask.toKronosActionTask()
        return generateDeleteTask(pojo, whereClauseSql, pojo.kronosColumns(), logic, rootTask)
    }

    private fun <T : KPojo> generateDeleteTask(
        pojo: T,
        whereClauseSql: String?,
        columns: List<Field>,
        logic: Boolean,
        rootTask: KronosAtomicActionTask
    ): KronosActionTask {
        val toDeleteRecords: MutableList<KPojo> = mutableListOf()
        val validReferences = findValidRefs(columns, KOperationType.DELETE)
        return rootTask.toKronosActionTask().apply {
            doBeforeExecute { wrapper ->
                toDeleteRecords.addAll(pojo.select().where { whereClauseSql.asSql() }.queryList(wrapper))
                if (toDeleteRecords.isEmpty()) return@doBeforeExecute
                val restrictReferences = validReferences.filter { it.reference.onDelete == RESTRICT }
                toDeleteRecords.forEach { record ->
                    restrictReferences.forEach { reference ->
                        val valueOfPojo = record.toDataMap()[reference.field.name]
                        if (valueOfPojo != null && !(valueOfPojo is Collection<*> && valueOfPojo.isEmpty())) {
                            throw UnsupportedOperationException(
                                "The record cannot be deleted because it is restricted by a reference." +
                                        "${record.kronosTableName()}.${reference.reference.referenceColumns} is restricted by ${reference.reference.targetColumns}, " +
                                        "and the value is ${valueOfPojo}."
                            )
                        }
                    }
                }

                val forestOfKPojo = toDeleteRecords.map { it.toTreeNode() }
                if (forestOfKPojo.any { it.children.isNotEmpty() }) {
                    this.atomicTasks.clear() // 清空原有的任务
                    val list = mutableListOf<NodeOfKPojo>()
                    forestOfKPojo.forEach { tree ->
                        val stack = KStack<NodeOfKPojo>() // 用于深度优先遍历
                        val all = KStack<NodeOfKPojo>() // 用于存储所有的节点
                        stack.push(tree) // 将根节点压入栈
                        var tmp: NodeOfKPojo
                        while (!stack.isEmpty()) { // 深度优先遍历
                            tmp = stack.pop()
                            all.push(tmp)
                            tmp.children.forEach {
                                stack.push(it) // 将子节点压入栈
                            }
                        }
                        while (!all.isEmpty()) {
                            list.add(all.pop()) // 将所有节点压入list
                        }
                    }
                    atomicTasks.addAll(
                        list.map { it.kPojo }.delete().logic(logic).cascade(false).build().atomicTasks
                    )
                }
            }
        }
    }
}