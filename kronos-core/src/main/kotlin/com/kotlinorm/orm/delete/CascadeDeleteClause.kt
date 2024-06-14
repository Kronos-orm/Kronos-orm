package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KReference
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.KOperationType
import kotlin.reflect.full.createInstance

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
        val columns = pojo.kronosColumns().filter { !it.isColumn }
        return columns.map { col ->
            val propVal = pojo.toDataMap()[col.name]
            val referenceType = col.referenceKClass
            val references = if (col.reference != null) {
                //自己维护的关系
                listOf(col.reference)
            } else {
                val ref = if (propVal is Iterable<*> && referenceType != null) {
                    referenceType.createInstance()
                } else {
                    propVal
                } as KPojo
                ref.kronosColumns().mapNotNull { it.reference }.filter { col.tableName in it.mapperBy }
            }
            generateReferenceUpdateSql(pojo, references, condition)
        }.toTypedArray()
    }

    private fun <K : KPojo> generateReferenceUpdateSql(
        pojo: K,
        reference: List<KReference>,
        condition: Criteria?
    ): KronosAtomicActionTask {
        return KronosAtomicActionTask("", mapOf(), KOperationType.UPDATE)
    }
}