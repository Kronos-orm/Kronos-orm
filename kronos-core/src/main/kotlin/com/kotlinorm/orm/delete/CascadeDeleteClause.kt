package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KReference
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.CascadeAction.CASCADE
import com.kotlinorm.enums.CascadeAction.RESTRICT
import com.kotlinorm.enums.CascadeAction.SET_DEFAULT
import com.kotlinorm.enums.CascadeAction.SET_NULL
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.setCommonStrategy
import kotlin.reflect.full.createInstance

/**
 * Used to build a cascade delete clause.
 * 用于构建级联删除子句。
 */
object CascadeDeleteClause {
    data class ValidRef(
        val references: List<KReference>, val refPojo: KPojo
    ) {
        private fun generateRandomCode(): String {
            val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            return (1..4)
                .map { characters.random() }
                .joinToString("")
        }

        /**
         * Generates a list of KronosAtomicActionTask for the given original KPojo, whereClauseSql, logic, and paramMap.
         *
         * This function iterates over each reference in the references list and generates a SQL select clause for each reference.
         * It then creates a new where clause by checking if the target columns in the referenced table are in the select clause.
         * It finds the valid references for the referenced KPojo and generates tasks for each valid reference.
         * It also generates a delete SQL for the referenced KPojo and adds it to the tasks list.
         * Finally, it returns the list of tasks.
         *
         * 此函数遍历引用列表中的每个引用，并为每个引用生成一个 SQL select 子句。
         * 然后，它通过检查引用表中的目标列是否在 select 子句中来创建一个新的 where 子句。
         * 它找到引用的 KPojo 的有效引用，并为每个有效引用生成任务。
         * 它还为引用的 KPojo 生成一个删除 SQL，并将其添加到任务列表中。
         * 最后，它返回任务列表。
         *
         * @param original KPojo the original KPojo for which to generate the tasks.
         * @param whereClauseSql String? the SQL where clause to use in the select clause. If it is null, the where clause is not included in the select clause.
         * @param logic Boolean the logic to use when generating the tasks. If it is true, a logic delete is performed. If it is false, a physical delete is performed.
         * @param paramMap Map<String, Any?> the map of parameters to use in the SQL statements.
         * @return List<KronosAtomicActionTask> returns a list of KronosAtomicActionTask generated for the original KPojo.
         */
        fun generateTask(
            original: KPojo,
            whereClauseSql: String?,
            logic: Boolean,
            paramMap: MutableMap<String, Any?>,
            rootTask: KronosAtomicActionTask
        ): List<KronosAtomicActionTask> {
            val tasks = mutableListOf<KronosAtomicActionTask>()
            references.forEach { ref ->
                val refColumns = ref.referenceColumns
                val targetColumns = ref.targetColumns
                val tableName = original.kronosTableName()
                val refTableName = refPojo.kronosTableName()
                val targetColumnSql = targetColumns.joinToString(", ") { "`$tableName`.`${it}`" }
                val refColumnSql = refColumns.joinToString(", ") { "`$refTableName`.`${it}`" }
                val selectClause = listOfNotNull(
                    "SELECT",
                    targetColumnSql,
                    "FROM",
                    "`$tableName`",
                    "WHERE".takeIf { whereClauseSql != null },
                    whereClauseSql
                ).joinToString(" ")
                val randomTempTableId = generateRandomCode()
                val newWhereClauseSql =
                    "$refColumnSql IN ( SELECT $targetColumnSql from ($selectClause) as KRONOS_TEMP_TABLE_$randomTempTableId)"
                val validReferences = findValidRefs(refPojo.kronosColumns().filter { !it.isColumn })
                val nextStepTask =
                    generateReferenceDeleteSql(
                        refPojo, newWhereClauseSql, ref, logic, paramMap, rootTask
                    ) ?: return@forEach
                if (nextStepTask.size > 1) {
                    tasks.addAll(validReferences.map {
                        it.generateTask(
                            refPojo,
                            newWhereClauseSql,
                            logic,
                            paramMap,
                            nextStepTask.first()
                        )
                    }
                        .flatten().toTypedArray())
                }
                tasks.addAll(nextStepTask)
            }
            return tasks
        }
    }

    /**
     * Build a cascade delete clause.
     * 构建级联删除子句。
     *
     * @param pojo The pojo to be deleted.
     * @param whereClauseSql The condition to be met.
     * @param logic The logic to be used.
     * @param paramMap The map of parameters.
     * @param deleteTask The delete task.
     * @return The list of atomic tasks.
     */
    fun <T : KPojo> build(
        pojo: T,
        whereClauseSql: String?,
        logic: Boolean,
        paramMap: MutableMap<String, Any?>,
        deleteTask: KronosAtomicActionTask
    ): Array<KronosAtomicActionTask> {
        val validReferences = findValidRefs(pojo.kronosColumns().filter { !it.isColumn })

        return (validReferences.map { it.generateTask(pojo, whereClauseSql, logic, paramMap, deleteTask) }
            .flatten()).toTypedArray()
    }

    private fun findValidRefs(columns: List<Field>): List<ValidRef> {
        return columns.map { col ->
            val ref = Class.forName(
                col.referenceKClassName ?: throw UnsupportedOperationException("The reference class is not supported!")
            ).kotlin.createInstance() as KPojo

            ValidRef(if (col.cascadeMapperBy()) {
                listOf(col.reference!!)
            } else {
                ref.kronosColumns()
                    .filter { it.cascadeMapperBy(col.tableName) }
                    .map { it.reference!! }
            }, ref)
        }
    }

    private fun <K : KPojo> generateReferenceDeleteSql(
        pojo: K,
        whereClauseSql: String?,
        reference: KReference,
        logic: Boolean,
        paramMap: MutableMap<String, Any?>,
        prevTask: KronosAtomicActionTask
    ): List<KronosAtomicActionTask>? {
        val toUpdateFields = mutableListOf<Field>()
        var condition = whereClauseSql?.asSql()
        val logicDeleteStrategy = pojo.kronosLogicDelete()

        if (logicDeleteStrategy.enabled) {
            setCommonStrategy(logicDeleteStrategy) { field, value ->
                condition = listOfNotNull(
                    condition, "${field.quoted(true)} = $value".asSql()
                ).toCriteria()
            }
        }

        val (newWhereClauseSql) = ConditionSqlBuilder.buildConditionSqlWithParams(
            condition, mutableMapOf(), showTable = true
        )

        if (logicDeleteStrategy.enabled) {
            val updateInsertFields = { field: Field, _: Any? ->
                toUpdateFields += Field(
                    columnName = field.name, name = field.name + "New", tableName = pojo.kronosTableName()
                )
            }
            setCommonStrategy(logicDeleteStrategy, deleted = true, callBack = updateInsertFields)
        }

        return when (reference.onDelete) {
            CASCADE -> {
                listOf(
                    if (logic) {
                        KronosAtomicActionTask("UPDATE `${pojo.kronosTableName()}` SET ${
                            toUpdateFields.joinToString {
                                it.equation(
                                    true
                                )
                            }
                        } WHERE $newWhereClauseSql", paramMap, KOperationType.DELETE)
                    } else {
                        KronosAtomicActionTask(
                            "DELETE FROM `${pojo.kronosTableName()}` WHERE $newWhereClauseSql",
                            paramMap,
                            KOperationType.DELETE
                        )
                    },
                    prevTask
                )
            }

            RESTRICT -> {
                listOf(
                    prevTask.apply {
                        sql += " AND (SELECT count(1) FROM `${pojo.kronosTableName()}` WHERE$newWhereClauseSql LIMIT 1) = 0"
                    }
                )
            }

            SET_NULL -> {
                listOf(
                    KronosAtomicActionTask("UPDATE `${pojo.kronosTableName()}` SET ${
                        reference.referenceColumns.joinToString(", ") {
                            "`${pojo.kronosTableName()}`.`$it` = null"
                        }
                    } WHERE $newWhereClauseSql", paramMap, KOperationType.DELETE),
                    prevTask
                )
            }

            SET_DEFAULT -> {
                listOf(
                    KronosAtomicActionTask(
                        "UPDATE `${pojo.kronosTableName()}` SET ${
                            getDefaultUpdates(pojo.kronosTableName(), reference, paramMap)
                        } WHERE $newWhereClauseSql", paramMap
                    ),
                    prevTask
                )
            }

            else -> null
        }
    }

    private fun getDefaultUpdates(
        tableName: String,
        reference: KReference,
        paramMap: MutableMap<String, Any?>
    ): String {
        var counter = 0
        var key = "defaultVal${counter++}"
        var randomKey: String
        val defaults = reference.referenceColumns.mapIndexed { i, _ ->
            while (paramMap.containsKey(key)) {
                key = "defaultVal${counter++}"
            }
            randomKey = key
            paramMap[randomKey] = reference.defaultValue[i]
            reference.referenceColumns[i] to key
        }
        return defaults.joinToString(", ") {
            "`${tableName}`.`${it.first}` = :${it.second}"
        }
    }
}