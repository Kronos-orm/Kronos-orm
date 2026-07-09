/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
import com.kotlinorm.beans.dsl.KronosFunctionExpr
import com.kotlinorm.beans.dsl.rawSqlSelectItem
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.cache.kPojoCreateTimeCache
import com.kotlinorm.cache.kPojoLogicDeleteCache
import com.kotlinorm.cache.kPojoOptimisticLockCache
import com.kotlinorm.cache.kPojoUpdateTimeCache
import com.kotlinorm.database.SqlManager.renderStatement
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.sql.materializeSqlQuery
import com.kotlinorm.orm.sql.toSqlParameterEq
import com.kotlinorm.orm.statement.ParameterSource
import com.kotlinorm.orm.update.update
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlAssignmentTarget
import com.kotlinorm.syntax.statement.SqlConflictTarget
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlLock
import com.kotlinorm.syntax.statement.SqlUpdateSetPair
import com.kotlinorm.syntax.statement.SqlUpsertAction
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSet
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.LinkedHashSet
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.toDatabaseBooleanValue
import com.kotlinorm.utils.toDatabaseParameterValue
import com.kotlinorm.utils.toLinkedSet

/**
 * Update Clause
 *
 * Creates an update clause for the given pojo.
 *
 * @param T the type of the pojo
 *
 * @property pojo the pojo for the update
 * @param setUpsertFields the fields to update
 * @author Jieyao Lu, OUSC
 */
class UpsertClause<T : KPojo>(
    private val pojo: T,
    private var setUpsertFields: ToSelect<T, Any?> = null
) {
    private var paramMap = pojo.toDataMap()
    private var tableName = pojo.__tableName
    private var kClass = pojo.kClass()
    private var createTimeStrategy = kPojoCreateTimeCache[kClass]
    private var updateTimeStrategy = kPojoUpdateTimeCache[kClass]
    private var logicDeleteStrategy = kPojoLogicDeleteCache[kClass]
    private var optimisticStrategy = kPojoOptimisticLockCache[kClass]
    internal var allFields = kPojoAllFieldsCache[kClass]!!
    private var onConflict = false
    private var toInsertFields: LinkedHashSet<Field> = []
    private var toUpdateFields: LinkedHashSet<Field> = []
    private var onFields: LinkedHashSet<Field> = []
    private var cascadeEnabled = true
    private var cascadeAllowed: Set<Field>? = null
    private var lock: SqlLock? = null
    private var paramMapNew = mutableMapOf<Field, Any?>()
    private var conflictAssignmentValues = mutableMapOf<Field, Any?>()

    init {
        if (setUpsertFields != null) {
            pojo.afterSelect {
                setUpsertFields!!(it)
                if (fields.isEmpty()) {
                    throw EmptyFieldsException()
                }
                toUpdateFields += fields
            }
        }
    }

    /**
     * Set the fields on which the update clause will be applied.
     *
     * @param someFields on which the update clause will be applied
     * @throws EmptyFieldsException if the new value is null
     * @return the upsert UpdateClause object
     */
    fun on(someFields: ToSelect<T, Any?>): UpsertClause<T> {
        if (null == someFields) throw EmptyFieldsException()
        pojo.afterSelect {
            someFields(it)
            if (fields.isEmpty()) {
                throw EmptyFieldsException()
            }
            onFields += fields.toSet()
        }
        return this
    }

    /**
     * On duplicate key update
     *
     * **Please define constraints before using onConflict**
     *
     * @return the upsert UpdateClause object
     */
    fun onConflict(): UpsertClause<T> {
        onConflict = true
        return this
    }

    fun cascade(enabled: Boolean): UpsertClause<T> {
        this.cascadeEnabled = enabled
        return this
    }

    fun cascade(someFields: ToReference<T, Any?>): UpsertClause<T> {
        if (someFields == null) throw EmptyFieldsException()
        cascadeEnabled = true
        pojo.afterReference {
            someFields(it)
            if (fields.isEmpty()) {
                throw EmptyFieldsException()
            }
            cascadeAllowed = fields.toSet()
        }
        return this
    }

    fun lock(lock: SqlLock = SqlLock.Update()): UpsertClause<T> {
        optimisticStrategy?.enabled = false
        this.lock = lock
        return this
    }

    fun patch(vararg pairs: Pair<String, Any?>): UpsertClause<T> {
        pairs.forEach { (fieldName, value) ->
            val field = allFields.find { it.name == fieldName } ?: Field(fieldName)
            paramMapNew[field] = value
            conflictAssignmentValues[field] = value
            toUpdateFields += field
        }
        return this
    }

    fun set(newValue: ToSet<T, Unit>): UpsertClause<T> {
        newValue ?: throw EmptyFieldsException()
        pojo.afterSet {
            newValue(it)
            fields.forEach { field ->
                val value = fieldParamMap[field]
                paramMapNew[field] = value
                conflictAssignmentValues[field] = value
                toUpdateFields += field
            }
        }
        return this
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build(wrapper).execute(wrapper)
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
        val dataSource = wrapper.orDefault()

        if (toInsertFields.isEmpty()) {
            toInsertFields = allFields.filter { field ->
                if (field.primaryKey == PrimaryKeyType.IDENTITY && paramMap[field.name] == null) return@filter false
                field.isColumn && (paramMap[field.name] != null || field.defaultValue == null)
            }.toLinkedSet()
        }

        if (toUpdateFields.isEmpty()) {
            toUpdateFields = allFields
        }

        // 合并参数映射，准备执行SQL所需的参数
        val fieldMap = fieldsMapCache[kClass]!!
        paramMapNew.forEach { (key, value) ->
            if (!value.requiresUpsertParameter()) {
                return@forEach
            }
            val field = fieldMap[key.name]
            if (field != null && value != null) {
                paramMap[key.name] = toDatabaseParameterValue(dataSource, fieldMap, key.name, value)
            } else {
                paramMap[key.name] = value
            }
        }

        val paramMap = (paramMap.filter { it.key in (toUpdateFields + toInsertFields + onFields).map { f -> f.name } }).toMutableMap()

        if (onConflict) {
            // 设置逻辑删除策略，将被逻辑删除的字段从更新字段中移除，并更新条件语句
            logicDeleteStrategy?.execute(defaultValue = false) { field, _ ->
                toInsertFields += field
                paramMap[field.name] = toDatabaseParameterValue(dataSource, fieldMap, field.name, false, mapOf(field.name to field))
            }

            createTimeStrategy?.execute{ field, value ->
                onFields -= field
                toInsertFields += field
                paramMap[field.name] = value
            }

            // 设置更新时间策略，将更新时间字段添加到更新字段列表，并更新参数映射
            updateTimeStrategy?.execute(true) { field, value ->
                onFields -= field
                toInsertFields += field
                toUpdateFields += field
                paramMap[field.name] = value
            }
            val statement = toSqlUpsertStatement(paramMap, dataSource)
            val rendered = renderStatement(dataSource, statement, paramMap, fieldMap)
            return KronosAtomicActionTask(
                rendered.sql,
                rendered.parameters,
                operationType = KOperationType.UPSERT,
                statement = statement
            ).toKronosActionTask()
        } else {
            val tasks: List<KronosAtomicActionTask> = []
            val fallbackStatement = toSqlUpsertStatement(paramMap, dataSource)
            return tasks.toKronosActionTask().doBeforeExecute { dataSource ->

                lock = lock ?: SqlLock.Update().takeIf { optimisticStrategy?.enabled != true }

                val selectClause = pojo.select()
                    .cascade(enabled = false)
                    .lock(lock)
                    .apply {
                        with(context) {
                            setProjectionItems(
                                listOf(KTableForSelect.ProjectionItem.SelectItemValue(rawSqlSelectItem("COUNT(1)"))),
                                emptyList()
                            )
                            addFieldConditions(
                                this@UpsertClause.onFields.filter {
                                    it.isColumn && it.name in this@UpsertClause.paramMap.keys
                                },
                                this@UpsertClause.paramMap
                            )
                        }
                    }
                with(selectClause.context) {
                    logicDeleteStrategy = null
                }

                val fallbackTask = if ((selectClause.queryOneOrNull<Int>(dataSource) ?: 0) > 0) {
                    val updateClause = pojo.update().cascade(cascadeEnabled)
                        .apply {
                            with(context) {
                                cascadeAllowed = this@UpsertClause.cascadeAllowed
                                logicEnabled = false
                                andWhereAll(
                                    this@UpsertClause.onFields
                                        .filter { it.isColumn && it.name in paramMap.keys }
                                        .map { field ->
                                            bind(field.name, paramMap[field.name], field, ParameterSource.Condition)
                                            field.toSqlParameterEq(field.name)
                                        }
                                )
                            }
                        }
                        .set {
                            this@UpsertClause.toUpdateFields.forEach { field ->
                                val value = if (conflictAssignmentValues.containsKey(field)) {
                                    conflictAssignmentValues[field]
                                } else {
                                    paramMap[field.name]
                                }
                                setValue(field, value)
                            }
                            this@UpsertClause.logicDeleteStrategy?.execute(defaultValue = false) { field, _ ->
                                this@UpsertClause.toUpdateFields += field
                                setValue(field + "New", toDatabaseBooleanValue(dataSource, field, false))
                            }
                        }
                    updateClause.build(dataSource)
                } else {
                    pojo.insert().cascade(cascadeEnabled)
                        .apply {
                            this@apply.cascadeAllowed = this@UpsertClause.cascadeAllowed
                        }
                        .build(dataSource)
                }
                appendPrepared(fallbackTask, dataSource) { task ->
                    task.copy(
                        operationType = KOperationType.UPSERT,
                        statement = fallbackStatement
                    )
                }
            }
        }
    }

    private fun Any?.requiresUpsertParameter(): Boolean =
        this !is SqlExpr && this !is Field && this !is KronosFunctionExpr && this !is KSelectable<*>

    private fun toSqlUpsertStatement(
        parameterValues: MutableMap<String, Any?>,
        dataSource: KronosDataSourceWrapper
    ): SqlDmlStatement.Upsert {
        val insertColumns = toInsertFields.map { field -> SqlIdentifier.of(field.columnName) }
        val insertValues = toInsertFields.map { field ->
            SqlExpr.Parameter(SqlParameter.Named(field.name))
        }
        val conflictColumns = onFields.map { field -> SqlIdentifier.of(field.columnName) }
        val updatePairs = toUpdateFields.map { field ->
            val targetField = allFields.find { it.name == field.name } ?: field
            val value = conflictAssignmentValues[field]?.toUpsertAssignmentExpr(targetField, parameterValues, dataSource)
                ?: SqlExpr.Parameter(SqlParameter.Named(targetField.name))
            SqlUpdateSetPair(
                SqlAssignmentTarget.Column(SqlIdentifier.of(targetField.columnName)),
                value
            )
        }
        return SqlDmlStatement.Upsert(
            table = SqlTable.Ident(tableName),
            columns = insertColumns,
            values = insertValues,
            primaryKeys = conflictColumns,
            conflictTarget = SqlConflictTarget(columns = conflictColumns),
            action = SqlUpsertAction.Update(updatePairs)
        )
    }

    private fun Any?.toUpsertAssignmentExpr(
        targetField: Field,
        parameterValues: MutableMap<String, Any?>,
        dataSource: KronosDataSourceWrapper
    ): SqlExpr =
        when (this) {
            is SqlExpr -> this
            is KronosFunctionExpr -> expr
            is Field -> SqlExpr.Column(
                tableName = tableName.takeIf { it.isNotBlank() },
                columnName = columnName
            )
            is KSelectable<*> -> SqlExpr.Subquery(materializeSqlQuery(parameterValues, mutableMapOf(), dataSource))
            else -> SqlExpr.Parameter(SqlParameter.Named(targetField.name))
        }

}
