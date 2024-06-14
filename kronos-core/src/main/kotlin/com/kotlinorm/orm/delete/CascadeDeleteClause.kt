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
        val dataMap = pojo.toDataMap()
        return columns.map { col -> // 对于其中的每一个属性
            val propVal = dataMap[col.name]  //获取其值
            val referenceKClass = col.referenceKClass //获取其引用的类
            val ref = if (propVal is Iterable<*>) { //如果是集合
                if (referenceKClass != null) { //如果引用的类不为空
                    referenceKClass //返回引用的类
                } else { // 否则抛出异常，引用类未指定：因为引用是一个集合。使用@ReferenceType指定引用类。
                    throw IllegalArgumentException("The reference class is not specified because the reference is a collection. Use @ReferenceType to specify the reference class.")
                }
            } else {
                (propVal as KPojo)::class //否则返回其类
            }.createInstance()
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