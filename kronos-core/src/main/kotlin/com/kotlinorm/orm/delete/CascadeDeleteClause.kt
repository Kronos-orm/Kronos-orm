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
        // 获取所有Kotlin 属性（非数据库对应的列）
        val columns = pojo.kronosColumns().filter { !it.isColumn }
        return columns.map { col -> // 对于其中的每一个属性
            val ref = Class.forName(
                col.referenceKClassName ?: throw UnsupportedOperationException("The reference class is not supported!")
            ).kotlin.createInstance() as KPojo
            val references = if (col.reference != null) {
                //当前属性通过@Reference维护的关联关系
                listOf(col.reference)
            } else {
                //否则要去找到当前属性所在的表的所有@Reference维护的关联关系，判断当前属性是否在mapperBy中
                ref.kronosColumns().mapNotNull { it.reference }
                    .filter { "${col.tableName}.${col.columnName}" in it.mapperBy }
            }
            generateReferenceUpdateSql(pojo, ref, references, condition) //生成删除语句
        }.flatten().toTypedArray()
    }

    private fun <T : KPojo, K : KPojo> generateReferenceUpdateSql(
        pojo: T,
        ref: K,
        reference: List<KReference>,
        condition: Criteria?
    ): List<KronosAtomicActionTask> {
        return listOf(KronosAtomicActionTask("", mapOf(), KOperationType.UPDATE))
    }
}