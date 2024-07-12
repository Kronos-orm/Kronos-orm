package com.kotlinorm.orm.cascade

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.orm.cascade.NodeOfKPojo.Companion.toTreeNode
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.utils.KStack
import com.kotlinorm.utils.pop
import com.kotlinorm.utils.push
import kotlin.collections.LinkedHashSet

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
            limit, pojo, paramMap, toUpdateFields, whereClauseSql, pojo.kronosColumns(), rootTask
        ) else rootTask.toKronosActionTask()

    private fun <T : KPojo> generateTask(
        limit: Int,
        pojo: T,
        paramMap: Map<String, Any?>,
        toUpdateFields: LinkedHashSet<Field>,
        whereClauseSql: String?,
        columns: List<Field>,
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
                val forest = toUpdateRecords.map { it.toTreeNode(operationType = KOperationType.UPDATE) }

                if (forest.any { it.children.isNotEmpty() }) {
                    val rootTask = this.atomicTasks.first()
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
                    atomicTasks.add(rootTask)
                }
            }
        }
    }

    private fun getTask(
        child: NodeOfKPojo,
        paramMap: Map<String, Any?>
    ): KronosActionTask? {
        if (null == child.data) return null
        val validReferences = child.data.parent?.validRefs ?: return null
        val ref = validReferences.find { it.field == child.data.fieldOfParent }
            ?: throw NeedFieldsException("The field corresponding to the annotation could not be found in the entity")

        return  child.kPojo.update().apply {
            ref.reference.referenceFields.forEachIndexed { index, referenceField ->
                val targetField = ref.reference.targetFields[index]

                val updateField = this.allFields.find { it.name == referenceField }
                    ?: throw NeedFieldsException("The field corresponding to the annotation could not be found in the entity")

                this.toUpdateFields += updateField
                this.paramMapNew[updateField + "New"] = paramMap[targetField + "New"]
            }
        }.build()
    }

}