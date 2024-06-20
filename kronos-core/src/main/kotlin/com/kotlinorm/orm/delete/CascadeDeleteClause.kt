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
    data class ValidRef(
        val references: List<KReference>, val refPojo: KPojo
    ) {
        fun generateTask(
            original: KPojo, whereClauseSql: String?, logic: Boolean, paramMap: MutableMap<String, Any?>
        ): List<KronosAtomicActionTask> {
            val tasks = mutableListOf<KronosAtomicActionTask>()
            references.forEach { ref ->
                val refColumns = ref.referenceColumns
                val targetColumns = ref.targetColumns
                val tableName = original.kronosTableName()
                val refTableName = refPojo.kronosTableName()
                val selectClause = listOfNotNull(
                    "SELECT",
                    targetColumns.joinToString(", ") { "`$tableName`.`${it}`" },
                    "FROM",
                    "`$tableName`",
                    "WHERE".takeIf { whereClauseSql != null },
                    whereClauseSql
                ).joinToString(" ")
                val newWhereClauseSql =
                    "${refColumns.joinToString(", ") { "`$refTableName`.`${it}`" }} IN ($selectClause LIMIT 1)"
                //TODO: LIMIT 1 is not supported in all databases, need to be fixed
                val validReferences = findValidRefs(refPojo.kronosColumns().filter { !it.isColumn })
                tasks.addAll(validReferences.map { it.generateTask(refPojo, newWhereClauseSql, logic, paramMap) }
                    .flatten().toTypedArray())
                tasks.add(
                    generateReferenceDeleteSql(
                        refPojo, newWhereClauseSql, ref, logic, paramMap
                    ) ?: return@forEach
                )
            }
            return tasks
        }
    }

    /**
     * Build a cascade delete clause.
     * 构建级联删除子句。
     *
     * @param pojo The pojo to be deleted.
     * @param condition The condition to be met.
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

        return (validReferences.map { it.generateTask(pojo, whereClauseSql, logic, paramMap) }
            .flatten() + deleteTask).toTypedArray()
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
        pojo: K, whereClauseSql: String?, reference: KReference, logic: Boolean, paramMap: MutableMap<String, Any?>
    ): KronosAtomicActionTask? {
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
                }
            }

            RESTRICT -> {
                if (logic) {
                    KronosAtomicActionTask("UPDATE `${pojo.kronosTableName()}` SET ${
                        toUpdateFields.joinToString {
                            it.equation(
                                true
                            )
                        }
                    } WHERE (select count(1) from ($newWhereClauseSql) LIMIT 1) == 0",
                        paramMap,
                        KOperationType.DELETE)
                } else {
                    KronosAtomicActionTask(
                        "DELETE FROM `${pojo.kronosTableName()}` WHERE  (select count(1) from ($newWhereClauseSql) LIMIT 1) == 0",
                        paramMap,
                        KOperationType.DELETE
                    )
                }
            }

            SET_NULL -> {
                KronosAtomicActionTask("UPDATE `${pojo.kronosTableName()}` SET ${
                    reference.referenceColumns.joinToString(", ") {
                        "`${pojo.kronosTableName()}`.`$it` = null"
                    }
                } WHERE $newWhereClauseSql", paramMap, KOperationType.DELETE)
            }

            SET_DEFAULT -> {
                val randomKey = {
                    var key = "defaultVal${(0..10).random()}"
                    while (paramMap.containsKey(key)) {
                        key = "defaultVal${(0..10).random()}"
                    }
                    key
                }
                KronosAtomicActionTask("UPDATE `${pojo.kronosTableName()}` SET ${
                    toUpdateFields.joinToString(", ") {
                        reference.referenceColumns.joinToString(", ") {
                            "`${pojo.kronosTableName()}`.`$it` = :defaultVal$randomKey"
                        }
                    }
                } WHERE $newWhereClauseSql", paramMap.apply {
                    put(randomKey(), reference.defaultValue)
                })
            }

            else -> null
        }
    }
}