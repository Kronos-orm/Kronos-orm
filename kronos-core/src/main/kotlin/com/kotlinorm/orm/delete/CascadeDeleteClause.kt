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
        fun generateTask(
            original: KPojo, whereClauseSql: String?, logic: Boolean, paramMap: Map<String, Any?>
        ): List<KronosAtomicActionTask> {
            val tasks = mutableListOf<KronosAtomicActionTask>()
            references.forEach { ref ->
                val refColumns = ref.referenceColumns
                val targetColumns = ref.targetColumns
                val tableName = original.kronosTableName()
                val refTableName = refPojo.kronosTableName()
                val selectClause = listOfNotNull(
                    "SELECT",
                    refColumns.joinToString(", ") { "`$tableName`.`${it}`" },
                    "FROM",
                    "`$tableName`",
                    "WHERE".takeIf { whereClauseSql != null },
                    whereClauseSql
                ).joinToString(" ")
                val newWhereClauseSql =
                    "${targetColumns.joinToString(", ") { "`$refTableName`.`${it}`" }} IN ($selectClause LIMIT 1)"
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
        paramMap: Map<String, Any?>,
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
        pojo: K, whereClauseSql: String?, reference: KReference, logic: Boolean, paramMap: Map<String, Any?>
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
                    toUpdateFields.joinToString(", ") {
                        "${
                            it.quoted(
                                true
                            )
                        } = null"
                    }
                } WHERE $newWhereClauseSql", paramMap, KOperationType.DELETE)
            }

            SET_DEFAULT -> {
                KronosAtomicActionTask("UPDATE `${pojo.kronosTableName()}` SET ${
                    toUpdateFields.joinToString(", ") {
                        "${
                            it.quoted(
                                true
                            )
                        } = ${reference.defaultValue}"
                    }
                } WHERE $newWhereClauseSql", paramMap)
            }

            else -> null
        }
    }
}