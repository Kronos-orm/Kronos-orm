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

package com.kotlinorm.orm.cascade

import com.kotlinorm.annotations.Cascade.Companion.RESERVED
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.CascadeDeleteAction.*
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.orm.cascade.NodeOfKPojo.Companion.toTreeNode
import com.kotlinorm.orm.delete.deleteWithType
import com.kotlinorm.orm.select.selectWithType
import com.kotlinorm.orm.sql.toSqlParameterEq
import com.kotlinorm.orm.statement.ParameterSource
import com.kotlinorm.orm.update.updateWithType
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.utils.KStack
import com.kotlinorm.utils.pop
import com.kotlinorm.utils.push
import com.kotlinorm.utils.resolveRuntimeMetadata
import kotlin.reflect.KType

/**
 * Used to build a cascade delete clause.
 *
 * 构建级联删除子句。
 *
 * This object is used to construct a cascade delete clause for a database operation.
 * It contains a nested Counter class for counting operations, a nested ValidCascade data class for storing cascades and cascaded POJOs,
 * and several functions for building the cascade delete clause and generating SQL statements.
 *
 * The main function is build, which takes a POJO, a SQL where clause, a logic flag, a parameter map, and a delete task,
 * and returns an array of KronosAtomicActionTask objects representing the cascade delete operations.
 *
 * The other functions are helper functions used by build. They include findValidRefs, which finds valid cascades in a list of fields,
 * generateCascadeDeleteSql, which generates a delete SQL statement for a cascaded POJO, and getDefaultUpdates, which generates a default update SQL clause.
 *
 */
object CascadeDeleteClause {
    /**
     * Build a cascade delete clause.
     * 构建级联删除子句。
     *
     * @param cascade Whether the cascade is enabled.
     * @param cascadeAllowed The properties that are allowed to cascade.
     * @param pojo The pojo to be deleted.
     * @param where The condition to be met.
     * @param paramMap The map of parameters.
     * @param logic The logic to be used.
     * @param rootTask The delete task.
     * @return The list of atomic tasks.
     */
    fun <T : KPojo> build(
        cascade: Boolean,
        cascadeAllowed: Set<Field>?,
        targetType: KType,
        pojo: T,
        where: SqlExpr?,
        paramMap: Map<String, Any?>,
        logic: Boolean,
        rootTask: KronosAtomicActionTask
    ) =
        if (cascade) generateTask(
            cascadeAllowed,
            targetType,
            pojo,
            where,
            paramMap,
            pojo.resolveRuntimeMetadata().allFields.toList(),
            logic,
            rootTask
        ) else rootTask.toKronosActionTask()

    /**
     * Generate a task for a cascade delete operation.
     *
     * @param cascadeAllowed The properties that are allowed to cascade.
     * @param pojo The pojo to be deleted.
     * @param where The condition to be met.
     * @param paramMap The parameter map.
     * @param columns The columns of the pojo.
     * @param logic The logic to be used.
     * @param rootTask The delete task.
     *
     * **/
    private fun <T : KPojo> generateTask(
        cascadeAllowed: Set<Field>?,
        targetType: KType,
        pojo: T,
        where: SqlExpr?,
        paramMap: Map<String, Any?>,
        columns: List<Field>,
        logic: Boolean,
        rootTask: KronosAtomicActionTask
    ): KronosActionTask {
        val tableName = pojo.__tableName
        val validCascades = findValidRefs( // 获取有效的引用
            targetType,
            columns,
            KOperationType.DELETE,
            cascadeAllowed?.filter { it.tableName == tableName }?.map { it.name }?.toSet(), // 获取当前Pojo内允许级联的属性
            cascadeAllowed.isNullOrEmpty() // 是否允许所有属性级联
        ).filter { !it.mapperByThis }

        return rootTask.toKronosActionTask().apply {
            doBeforeExecute { wrapper -> // 在执行前检查是否有引用
                if (validCascades.isEmpty()) return@doBeforeExecute // 如果没有级联，直接返回
                val inheritedWhere = where
                val inheritedParamMap = paramMap
                val inheritedCascadeAllowed = cascadeAllowed
                val selectClause = pojo.selectWithType(targetType).apply {
                    with(context) {
                        andWhere(inheritedWhere, inheritedParamMap)
                        this.cascadeAllowed = inheritedCascadeAllowed
                        operationType = KOperationType.DELETE
                        logicDeleteStrategy = null
                    }
                }
                val toDeleteRecords = selectClause.toList(wrapper) //先查询出要删除的记录
                if (toDeleteRecords.isEmpty()) return@doBeforeExecute // 如果没有要删除的记录，直接返回

                validateRestrictCascades(toDeleteRecords, validCascades)

                // 生成树结构，后序遍历所有的子节点，将所有的子节点压入list，最后由子到父执行删除操作
                val forestOfKPojo = toDeleteRecords.map {
                    it.toTreeNode(
                        operationType = KOperationType.DELETE,
                        cascadeAllowed = cascadeAllowed
                    )
                }
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
                    atomicTasks.addAll(list.mapNotNull { node ->
                        val nodeType = node.data?.fieldOfParent?.let { field ->
                            field.elementKType ?: field.kType
                        } ?: targetType
                        requireNotNull(nodeType) {
                            "Missing Kotlin type metadata for cascade field ${node.data?.fieldOfParent?.name}."
                        }
                        when (node.data?.kCascade?.onDelete) {
                            NO_ACTION, RESTRICT -> null
                            CASCADE, null -> node.kPojo.deleteWithType(nodeType)
                                .logic(logic)
                                .cascade(enabled = false)
                                .apply {
                                    with(context) {
                                        andWhereAll(this.fields.mapNotNull { field ->
                                            sourceValues[field.name]?.let { value ->
                                                bind(field.name, value, field, ParameterSource.Condition)
                                                field.toSqlParameterEq(field.name)
                                            }
                                        })
                                    }
                                }
                                .build().atomicTasks
                            SET_NULL -> {
                                val updateClause = node.kPojo.updateWithType(nodeType)
                                val listOfValidCascade = node.data.parent?.validCascades?.filter { cascade-> cascade.field == node.data.fieldOfParent }
                                listOfValidCascade?.forEach { validCascade->
                                    validCascade.kCascade.properties.forEach{ property ->
                                        updateClause.patch(property to null)
                                    }
                                }
                                updateClause.apply {
                                    with(context) {
                                        andWhereAll(this.fields.mapNotNull { field ->
                                            sourceValues[field.name]?.let { value ->
                                                bind(field.name, value, field, ParameterSource.Condition)
                                                field.toSqlParameterEq(field.name)
                                            }
                                        })
                                    }
                                }.build().atomicTasks
                            }

                            SET_DEFAULT -> {
                                val updateClause = node.kPojo.updateWithType(nodeType)
                                val listOfValidCascade = node.data.parent?.validCascades?.filter { cascade-> cascade.field == node.data.fieldOfParent }
                                listOfValidCascade?.forEach { validCascade->
                                    validCascade.kCascade.properties.forEachIndexed{ index, property ->
                                        val defaultValue = validCascade.kCascade.defaultValue.getOrNull(index)
                                        if(defaultValue != null && defaultValue != RESERVED) {
                                            updateClause.patch(property to defaultValue)
                                        }
                                    }
                                }
                                updateClause.apply {
                                    with(context) {
                                        andWhereAll(this.fields.mapNotNull { field ->
                                            sourceValues[field.name]?.let { value ->
                                                bind(field.name, value, field, ParameterSource.Condition)
                                                field.toSqlParameterEq(field.name)
                                            }
                                        })
                                    }
                                }.build().atomicTasks
                            }
                        }
                    }.flatten()) // 生成删除任务
                }
            }
        }
    }

    /**
     * Rejects deletion when a record still contains a value governed by a
     * `RESTRICT` cascade relationship.
     *
     * @param records records selected for deletion
     * @param validCascades cascade relationships applicable to the deletion
     * @throws UnsupportedOperationException when a restricted relationship has data
     */
    private fun validateRestrictCascades(records: List<KPojo>, validCascades: List<ValidCascade>) {
        val restrictCascades = validCascades.filter { it.kCascade.onDelete == RESTRICT }
        records.forEach { record ->
            restrictCascades.forEach { cascade ->
                val valueOfPojo = record.toDataMap()[cascade.field.name]
                if (valueOfPojo != null && !(valueOfPojo is Collection<*> && valueOfPojo.isEmpty())) {
                    throw UnsupportedOperationException(
                        "The record cannot be deleted because it is restricted by a cascade." +
                            "${record.__tableName}.${cascade.kCascade.properties} is restricted by " +
                            "${cascade.kCascade.targetProperties}, and the value is $valueOfPojo."
                    )
                }
            }
        }
    }
}
