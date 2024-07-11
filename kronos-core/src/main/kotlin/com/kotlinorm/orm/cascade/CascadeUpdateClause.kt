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

object CascadeUpdateClause {

    fun <T : KPojo> build(
        cascade: Boolean,
        limit: Int,
        pojo: T,
        paramMap: Map<String, Any?>,
        whereClauseSql: String?,
        rootTask: KronosAtomicActionTask
    ) =
        if (cascade && limit != 0) generateTask(
            limit, pojo, paramMap, whereClauseSql, pojo.kronosColumns(), rootTask
        ) else rootTask.toKronosActionTask()

    private fun <T : KPojo> generateTask(
        limit: Int,
        pojo: T,
        paramMap: Map<String, Any?>,
        whereClauseSql: String?,
        columns: List<Field>,
        rootTask: KronosAtomicActionTask
    ): KronosActionTask {
        val toUpdateRecords: MutableList<KPojo> = mutableListOf()
        val validReferences = findValidRefs(columns, KOperationType.UPDATE).filter { ref ->
            ref.reference.targetFields.all { paramMap.contains(it + "New") }
        }
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
                val forest = toUpdateRecords.map { it.toTreeNode() }

                forest.forEach { tree ->
                    tree.children.forEach { child ->
                        val ref = validReferences.find { it.field == child.data?.fieldOfParent }
                            ?: throw NeedFieldsException("The field corresponding to the annotation could not be found in the entity")

                        child.kPojo.update().apply {
                            ref.reference.referenceFields.forEachIndexed { index, referenceField ->
                                val targetField = ref.reference.targetFields[index]

                                val updateField = this.allFields.find { it.name == referenceField }
                                    ?: throw NeedFieldsException("The field corresponding to the annotation could not be found in the entity")

                                this.toUpdateFields += updateField
                                this.paramMapNew[updateField + "New"] = paramMap[targetField + "New"]

                                println()
                            }
                        }.execute()
                    }
                }
            }
        }
    }

}