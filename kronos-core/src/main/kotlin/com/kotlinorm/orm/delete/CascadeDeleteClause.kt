package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KReference
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.KOperationType

/**
 * Used to build a cascade delete clause.
 * 用于构建级联删除子句。
 */
object CascadeDeleteClause {
    /**
     * Build a cascade delete clause.
     * 构建级联删除子句。
     *
     * @param pojo The pojo to be deleted.
     * @param condition The condition to be met.
     * @return The list of atomic tasks.
     */
    fun <T : KPojo> build(pojo: T, condition: Criteria?): Array<KronosAtomicActionTask> {
        val allReferences = pojo.kronosColumns().mapNotNull { it.reference }
        return allReferences.map { ref ->
            val propVal = pojo.toDataMap()[ref.propName]
            if (propVal is Iterable<*>) {
                propVal.map {
                    generateReferenceUpdateSql(it as KPojo, ref, condition)
                }
            } else {
                listOf(generateReferenceUpdateSql(propVal as KPojo, ref, condition))
            }
        }.flatten().toTypedArray()
    }

    private fun <K : KPojo> generateReferenceUpdateSql(
        pojo: K,
        reference: KReference,
        condition: Criteria?
    ): KronosAtomicActionTask {
        return KronosAtomicActionTask("", mapOf(), KOperationType.UPDATE)
    }
}