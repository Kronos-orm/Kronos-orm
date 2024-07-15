package com.kotlinorm.orm.cascade

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.orm.cascade.NodeOfKPojo.Companion.toTreeNode
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.utils.KStack
import com.kotlinorm.utils.pop
import com.kotlinorm.utils.push

object CascadeUpdateClause {

    fun <T : KPojo> build(
        cascade: Boolean,
        limit: Int,
        pojo: T,
        paramMap: Map<String, Any?>,
        toUpdateFields: LinkedHashSet<Field>,
        whereClauseSql: String?,
        rootTask: KronosAtomicActionTask
    ) =
        if (cascade && limit != 0) generateTask(
            limit, pojo, paramMap, toUpdateFields, whereClauseSql, rootTask
        ) else rootTask.toKronosActionTask()

    private fun <T : KPojo> generateTask(
        limit: Int,
        pojo: T,
        paramMap: Map<String, Any?>,
        toUpdateFields: LinkedHashSet<Field>,
        whereClauseSql: String?,
        rootTask: KronosAtomicActionTask
    ): KronosActionTask {
        val toUpdateRecords: MutableList<KPojo> = mutableListOf()

        return rootTask.toKronosActionTask().apply {
            doBeforeExecute { wrapper ->
                toUpdateRecords.addAll(
                    pojo.select()
                        .cascade(true, limit)
                        .where { whereClauseSql.asSql() }
                        .patch(*paramMap.toList().toTypedArray())
                        .queryList(wrapper)
                )
                if (toUpdateRecords.isEmpty()) return@doBeforeExecute
                val forest = toUpdateRecords.map { record ->
                    record.toTreeNode(
                        NodeInfo(true),
                        operationType = KOperationType.UPDATE,
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
            node.updateParams.forEach { (key, value) ->
                val updateField = allFields.first { it.name == key }
                toUpdateFields += updateField
                paramMapNew[updateField + "New"] = paramMap[value + "New"]
            }
        }.build()
    }

}