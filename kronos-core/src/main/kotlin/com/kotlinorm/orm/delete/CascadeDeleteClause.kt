package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KReference
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.CascadeAction.NO_ACTION
import com.kotlinorm.enums.CascadeAction.RESTRICT
import com.kotlinorm.enums.CascadeAction.SET_DEFAULT
import com.kotlinorm.enums.CascadeAction.SET_NULL
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.query
import com.kotlinorm.utils.queryOne
import com.kotlinorm.utils.setCommonStrategy
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
            val references = if (col.reference != null && (col.reference.mapperBy.isBlank() || col.tableName == col.reference.mapperBy)) {
                //当前属性通过@Reference维护的关联关系
                listOf(col.reference)
            } else {
                //否则要去找到当前属性所在的表的所有@Reference维护的关联关系，判断当前属性是否在mapperBy中
                ref.kronosColumns().mapNotNull { it.reference }
                    .filter { col.tableName == it.mapperBy || it.mapperBy.isBlank() }
            }
            generateReferenceUpdateSql(pojo, ref, references, condition) //生成删除语句
        }.flatten().toTypedArray()
    }

    private fun <T : KPojo, K : KPojo> generateReferenceUpdateSql(
        pojo: T,
        ref: K,
        reference: List<KReference>,
        originalCondition: Criteria?
    ): List<KronosAtomicActionTask> {

        var kOpType = KOperationType.DELETE
        val toUpdateFields = mutableListOf<Field>()
        var condition = originalCondition
        val pojoLogicDeleteStrategy = pojo.kronosLogicDelete()
        val refLogicDeleteStrategy = ref.kronosLogicDelete()

        if (pojoLogicDeleteStrategy.enabled) {
            setCommonStrategy(pojoLogicDeleteStrategy) { field, value ->
                condition = listOfNotNull(
                    condition, "${field.quoted(true)} = $value".asSql()
                ).toCriteria()
            }
        }

        val (whereClauseSql, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(
            condition,
            mutableMapOf(),
            showTable = true
        ).toWhereClause()

        if (refLogicDeleteStrategy.enabled) {
            kOpType = KOperationType.UPDATE
            val updateInsertFields = { field: Field, value: Any? ->
                toUpdateFields += Field(columnName = field.name , name = field.name + "New" , tableName = ref.kronosTableName())
                paramMap[field.name + "New"] = value
            }
            setCommonStrategy(refLogicDeleteStrategy, deleted = true, callBack = updateInsertFields)
        }

        return reference.map { item ->
            val pojoDbName = "`${pojo.kronosTableName()}`"
            val refDbName = "`${ref.kronosTableName()}`"

            when(item.cascade) {
                SET_NULL -> {
                    kOpType = KOperationType.UPDATE
                    item.targetColumns.forEach {
                        toUpdateFields += Field(columnName = it , name = it + "New" , tableName = ref.kronosTableName())
                        paramMap[it + "New"] = "NULL"
                    }
                }
                SET_DEFAULT -> {
                    kOpType = KOperationType.UPDATE
                    item.targetColumns.forEach {
                        toUpdateFields += Field(columnName = it , name = it + "New" , tableName = ref.kronosTableName())
                        paramMap[it + "New"] = item.defaultValue
                    }
                }
                NO_ACTION -> return listOf(KronosAtomicActionTask("", mapOf(), KOperationType.DELETE))
            }

            val updateFields = toUpdateFields.joinToString(", ") { it.equation(true) }

            val joinClauseSql = "ON " + item.referenceColumns.mapIndexed { i, _ ->
                pojoDbName + "." + "`${item.targetColumns[i]}`" + " = " + refDbName + "." + "`${item.referenceColumns[i]}`"
            }.joinToString(" AND ")

            fun withOtherReferenced(): Boolean {

                val columns = ref.kronosColumns().filter { !it.isColumn }
                return columns.filter { it.referenceKClassName != pojo::class.qualifiedName }.any {col ->

                    //关联的其他类
                    val mapperBy = Class.forName(
                        col.referenceKClassName ?: throw UnsupportedOperationException("The reference class is not supported!")
                    ).kotlin.createInstance() as KPojo

                    val refInfo = mapperBy.kronosColumns()
                        .find { it.referenceKClassName == ref::class.qualifiedName }?.reference ?: throw RuntimeException()

                    //判断维护关系是不是在对方
                    if (col.reference == null || col.reference.mapperBy == mapperBy.kronosTableName() || refInfo.mapperBy == mapperBy.kronosTableName()) {

                        //拼接sql语句，查找关联的记录数
                        var sql = ""

                        val cnt = KronosAtomicQueryTask(sql , mapOf() , KOperationType.SELECT).queryOne<Int>()
                        return cnt > 0
                    }

                    false
                }
            }

            if (item.cascade == RESTRICT && withOtherReferenced()) {
                throw UnsupportedOperationException("The target is referenced by other records!")
            }

            val sql = listOfNotNull(
                when(kOpType) {
                    KOperationType.DELETE -> "DELETE $refDbName FROM"
                    KOperationType.UPDATE -> "UPDATE"
                    else -> throw IllegalStateException("Unexpected operation type: $kOpType")
                },
                refDbName,
                "JOIN",
                pojoDbName,
                joinClauseSql,
                "SET $updateFields".takeIf { kOpType == KOperationType.UPDATE },
                whereClauseSql
            ).joinToString(" ")

            KronosAtomicActionTask(sql , paramMap , KOperationType.DELETE)
        }
    }
}