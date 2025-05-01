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
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.orm.cascade.NodeOfKPojo.Companion.toTreeNode
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.utils.KStack
import com.kotlinorm.utils.pop
import com.kotlinorm.utils.push
import kotlin.reflect.KClass

object CascadeUpdateClause {

    fun <T : KPojo> build(
        cascade: Boolean,
        cascadeAllowed: Set<Field>? = null,
        pojo: T,
        kClass: KClass<KPojo>,
        paramMap: Map<String, Any?>,
        toUpdateFields: LinkedHashSet<Field>,
        whereClauseSql: String?,
        rootTask: KronosAtomicActionTask
    ) =
        if (cascade) generateTask(
            cascadeAllowed, pojo, kClass, paramMap, toUpdateFields, whereClauseSql, rootTask
        ) else rootTask.toKronosActionTask()

    private fun <T : KPojo> generateTask(
        cascadeAllowed: Set<Field>? = null,
        pojo: T,
        kClass: KClass<KPojo>,
        paramMap: Map<String, Any?>,
        toUpdateFields: LinkedHashSet<Field>,
        whereClauseSql: String?,
        rootTask: KronosAtomicActionTask
    ): KronosActionTask {
        val toUpdateRecords: MutableList<KPojo> = mutableListOf()
        val validCascades = findValidRefs( // 获取有效的引用
            kClass,
            kPojoAllFieldsCache[kClass],
            KOperationType.UPDATE,
            cascadeAllowed?.filter { it.tableName == pojo.kronosTableName()}?.map { it.name }?.toSet(), // 获取当前Pojo内允许级联的属性
            cascadeAllowed.isNullOrEmpty() // 是否允许所有属性级联
        ).filter { !it.mapperByThis }

        return rootTask.toKronosActionTask().apply {
            doBeforeExecute { wrapper ->
                if (validCascades.isEmpty()) return@doBeforeExecute // 如果没有级联，直接返回
                toUpdateRecords.addAll(
                    pojo.select()
                        .where { whereClauseSql.asSql() }
                        .patch(*paramMap.toList().toTypedArray())
                        .apply {
                            this.cascadeAllowed = cascadeAllowed
                            this.operationType = KOperationType.UPDATE
                        }
                        .queryList(wrapper)
                )
                if (toUpdateRecords.isEmpty()) return@doBeforeExecute
                val forest = toUpdateRecords.map { record ->
                    record.toTreeNode(
                        NodeInfo(true),
                        operationType = KOperationType.UPDATE,
                        cascadeAllowed = cascadeAllowed,
                        updateParams = toUpdateFields.associateTo(mutableMapOf()) { it.name to it.name }
                    )
                }

                if (forest.any { it.children.isNotEmpty() }) {
                    this.atomicTasks.clear() // 清空原有的任务
                    val list = mutableListOf<NodeOfKPojo>()
                    forest.forEach { tree ->
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
                        list.mapNotNull {
                            getTask(it, paramMap)?.atomicTasks
                        }.flatten()
                    )
                }
            }
        }
    }

    private fun getTask(
        node: NodeOfKPojo,
        paramMap: Map<String, Any?>
    ): KronosActionTask? {
        if (null == node.data) return null
        return node.kPojo.update().apply {
            node.updateParams.forEach { (_, value) ->
                val updateField = allFields.first { it.name == value }
                toUpdateFields += updateField
                paramMapNew[updateField + "New"] = paramMap[value + "New"]
            }
        }.build()
    }

}