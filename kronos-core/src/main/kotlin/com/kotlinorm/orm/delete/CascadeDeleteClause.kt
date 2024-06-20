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
import com.kotlinorm.utils.ConditionSqlBuilder.toWhereSql
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.setCommonStrategy
import java.util.*
import kotlin.reflect.full.createInstance

/**
 * Used to build a cascade delete clause.
 *
 * 构建级联删除子句。
 *
 * This object is used to construct a cascade delete clause for a database operation.
 * It contains a nested Counter class for counting operations, a nested ValidRef data class for storing references and referenced POJOs,
 * and several functions for building the cascade delete clause and generating SQL statements.
 *
 * The main function is build, which takes a POJO, a SQL where clause, a logic flag, a parameter map, and a delete task,
 * and returns an array of KronosAtomicActionTask objects representing the cascade delete operations.
 *
 * The other functions are helper functions used by build. They include findValidRefs, which finds valid references in a list of fields,
 * generateReferenceDeleteSql, which generates a delete SQL statement for a referenced POJO, and getDefaultUpdates, which generates a default update SQL clause.
 *
 */
object CascadeDeleteClause {
    class Counter {
        private var _counter = 0
        val counter get() = _counter++
    }

    data class ValidRef(
        val references: List<KReference>, val refPojo: KPojo
    ) {
        /**
         * Generates a list of tasks for each reference in the ValidRef object.
         *
         * This function generates a list of KronosAtomicActionTask objects for each reference in the ValidRef object.
         * It iterates over each reference, generates a delete SQL statement for the referenced POJO, and adds the SQL statement to the list of tasks.
         * If the reference has valid references of its own, it recursively generates tasks for those references as well.
         *
         * 此函数为 ValidRef 对象中的每个引用生成一个 KronosAtomicActionTask 对象列表。
         * 它遍历每个引用，为引用的 POJO 生成一个删除 SQL 语句，并将该 SQL 语句添加到任务列表中。
         * 如果引用有自己的有效引用，它也会为这些引用递归生成任务。
         *
         * @param original The original POJO that is being deleted.
         * @param whereClauseSql The SQL where clause for the delete operation.
         * @param logic The logic flag for the delete operation.
         * @param paramMap The map of parameters for the delete operation.
         * @param rootTask The root task of the delete operation.
         * @param stackOfTask The stack of tasks for the delete operation.
         * @param tempTableIndex The temporary table index for the delete operation.
         * @return List<KronosAtomicActionTask> returns a list of KronosAtomicActionTask objects representing the delete operations for the references.
         */
        fun generateTask(
            original: KPojo,
            whereClauseSql: String?,
            logic: Boolean,
            paramMap: MutableMap<String, Any?>,
            rootTask: KronosAtomicActionTask,
            stackOfTask: Stack<KronosAtomicActionTask> = Stack(),
            tempTableIndex: Counter,
            layerCnt: Int = 0
        ): List<KronosAtomicActionTask> {
            val tasks = mutableListOf<KronosAtomicActionTask>()
            references.forEach { ref ->
                val tableName = original.kronosTableName() // 获取原始表名
                val refTableName = refPojo.kronosTableName() // 获取引用表名
                val targetColumnSql = ref.targetColumns.joinToString(", ") { "`$tableName`.`${it}`" } // 获取目标列的SQL
                val refColumnSql = ref.referenceColumns.joinToString(", ") { "`$refTableName`.`${it}`" } // 获取引用列的SQL
                val subSelectClause = listOfNotNull(
                    "SELECT",
                    targetColumnSql,
                    "FROM",
                    "`$tableName`",
                    toWhereSql(whereClauseSql)
                ).joinToString(" ")

                val newWhereClauseSql = // 生成新的 where 子句
                    "$refColumnSql IN ( SELECT ${ref.targetColumns.joinToString { "`$it`" }} from ($subSelectClause) as KRONOS_TEMP_TABLE_${tempTableIndex.counter})"

                val validReferences = findValidRefs(refPojo.kronosColumns().filter { !it.isColumn })
                val nextStepTask = generateReferenceDeleteSql(
                    refPojo, newWhereClauseSql, ref, logic, paramMap, rootTask , layerCnt + 1
                )

                if (stackOfTask.none { it.sql == nextStepTask.first().sql }) { // 若出现循环引用，不再递归
                    if (nextStepTask.size > 1) { // 如果级联删除的sql只有一个任务，说明为RESTRICT，不需要再次递归
                        tasks.addAll(
                            validReferences.map {
                                it.generateTask(
                                    refPojo,
                                    newWhereClauseSql,
                                    logic,
                                    paramMap,
                                    nextStepTask.first(),
                                    stackOfTask,
                                    tempTableIndex,
                                    layerCnt + 1
                                )
                            }.flatten().toTypedArray()
                        )
                    }
                    tasks.addAll(nextStepTask)
                }
                stackOfTask.addAll(tasks)
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
    ): List<KronosAtomicActionTask>? {
        val counter = Counter()
        val validReferences = findValidRefs(pojo.kronosColumns().filter { !it.isColumn })
        if (validReferences.isEmpty()) {
            return null
        }

        return validReferences.map {
            it.generateTask(
                pojo, whereClauseSql, logic, paramMap, deleteTask, tempTableIndex = counter
            )
        }.flatten()
    }

    /**
     * Finds valid references in a list of fields.
     *
     * This function iterates over the provided list of fields and for each field, it creates a ValidRef object.
     * A ValidRef object contains a list of references and a referenced POJO.
     * If the field has a cascade mapper, the list of references contains only the reference of the field.
     * Otherwise, the list of references contains all the references in the columns of the referenced POJO that have a cascade mapper for the table of the field.
     * The referenced POJO is created from the class name specified in the field.
     *
     * @param columns List<Field> the list of fields for which to find valid references.
     * @return List<ValidRef> returns a list of ValidRef objects representing the valid references in the fields.
     * @throws UnsupportedOperationException if the reference class is not supported.
     */
    private fun findValidRefs(columns: List<Field>): List<ValidRef> {
        return columns.map { col ->
            val ref = Class.forName(
                col.referenceKClassName ?: throw UnsupportedOperationException("The reference class is not supported!")
            ).kotlin.createInstance() as KPojo

            ValidRef(if (col.cascadeMapperBy()) {
                listOf(col.reference!!)
            } else {
                ref.kronosColumns().filter { it.cascadeMapperBy(col.tableName) }.map { it.reference!! }
            }, ref)
        }
    }

    /**
     * Generates a delete SQL statement for a referenced POJO.
     *
     * This function generates a delete SQL statement for a referenced POJO based on the onDelete action specified in the reference.
     * If the onDelete action is CASCADE, it generates an update or delete SQL statement.
     * If the onDelete action is RESTRICT, it adds a condition to the previous task's SQL statement to ensure that there are no references to the POJO.
     * If the onDelete action is SET_NULL, it generates an update SQL statement that sets the reference columns to null.
     * If the onDelete action is SET_DEFAULT, it generates an update SQL statement that sets the reference columns to their default values.
     *
     * @param pojo K the POJO that is being referenced.
     * @param whereClauseSql String? the SQL where clause for the delete operation.
     * @param reference KReference the reference for which to generate the delete SQL statement.
     * @param logic Boolean the logic flag for the delete operation.
     * @param paramMap MutableMap<String, Any?> the map of parameters for the delete operation.
     * @param prevTask KronosAtomicActionTask the previous task in the delete operation.
     * @return List<KronosAtomicActionTask>? returns a list of KronosAtomicActionTask objects representing the delete operations for the reference, or null if the onDelete action is not recognized.
     */
    private fun <K : KPojo> generateReferenceDeleteSql(
        pojo: K,
        whereClauseSql: String?,
        reference: KReference,
        logic: Boolean,
        paramMap: MutableMap<String, Any?>,
        prevTask: KronosAtomicActionTask,
        layerCnt: Int = 0
    ): List<KronosAtomicActionTask> {
        val toUpdateFields = mutableListOf<Field>()
        var condition = whereClauseSql?.asSql()
        val params = mutableMapOf<String, Any?>().apply { putAll(paramMap) } // 复制参数
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

        val tasks = when (reference.onDelete) {
            CASCADE -> {
                mutableListOf(if (logic) {
                    KronosAtomicActionTask("UPDATE `${pojo.kronosTableName()}` SET ${
                        toUpdateFields.joinToString {
                            it.equation(
                                true
                            )
                        }
                    } WHERE $newWhereClauseSql", params, KOperationType.DELETE)
                } else {
                    KronosAtomicActionTask(
                        "DELETE FROM `${pojo.kronosTableName()}` WHERE $newWhereClauseSql",
                        params,
                        KOperationType.DELETE
                    )
                })
            }

            RESTRICT -> {
                mutableListOf(prevTask.apply {
                    sql += " AND (SELECT count(1) FROM `${pojo.kronosTableName()}` WHERE $newWhereClauseSql LIMIT 1) = 0"
                })
            }

            SET_NULL -> {
                mutableListOf(KronosAtomicActionTask("UPDATE `${pojo.kronosTableName()}` SET ${
                    reference.referenceColumns.joinToString(", ") {
                        "`${pojo.kronosTableName()}`.`$it` = null"
                    }
                } WHERE $newWhereClauseSql", params, KOperationType.DELETE))
            }

            SET_DEFAULT -> {
                mutableListOf(
                    KronosAtomicActionTask(
                        "UPDATE `${pojo.kronosTableName()}` SET ${
                            getDefaultUpdates(pojo.kronosTableName(), reference, params)
                        } WHERE $newWhereClauseSql", params
                    )
                )
            }

            else -> mutableListOf()
        }

        return tasks.apply {
            if (layerCnt <= 1) {
                tasks.add(prevTask)
            }
        }
    }

    /**
     * Generates the default update SQL clause for the given table and reference.
     *
     * This function generates the default update SQL clause for the given table and reference.
     * It iterates over each column in the reference and checks if the column is in the paramMap.
     * If the column is not in the paramMap, it adds the column to the paramMap with the default value from the reference.
     * It then creates a pair of the column name and the key in the paramMap for the column.
     * Finally, it joins the pairs into a string with the format "`tableName`.`columnName` = :key" and returns the string.
     *
     * @param tableName String the name of the table for which to generate the default update SQL clause.
     * @param reference KReference the reference for which to generate the default update SQL clause.
     * @param paramMap MutableMap<String, Any?> the map of parameters to use in the SQL clause.
     * @return String returns the default update SQL clause for the given table and reference.
     */
    private fun getDefaultUpdates(
        tableName: String, reference: KReference, paramMap: MutableMap<String, Any?>
    ): String {
        var counter = 0
        var key = "defaultVal${counter++}"
        return reference.referenceColumns.mapIndexed { i, _ ->
            while (paramMap.containsKey(key)) {
                key = "defaultVal${counter++}"
            }
            paramMap[key] = reference.defaultValue[i]
            reference.referenceColumns[i] to key
        }.joinToString(", ") {
            "`${tableName}`.`${it.first}` = :${it.second}"
        }
    }
}