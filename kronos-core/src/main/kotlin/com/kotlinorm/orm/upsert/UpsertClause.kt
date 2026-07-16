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

import com.kotlinorm.Kronos.primaryKeyStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
import com.kotlinorm.beans.dsl.KronosFunctionExpr
import com.kotlinorm.beans.dsl.rawSqlSelectItem
import com.kotlinorm.beans.generator.resolveGeneratedPrimaryKeyValue
import com.kotlinorm.beans.task.JdbcParameterTypeHints
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.beans.task.jdbcNullParameterTypeHints
import com.kotlinorm.database.SqlManager.renderStatement
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.selectWithType
import com.kotlinorm.orm.sql.materializeSqlQuery
import com.kotlinorm.orm.sql.toSqlParameterEq
import com.kotlinorm.orm.statement.ParameterSource
import com.kotlinorm.orm.update.updateWithType
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
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
import com.kotlinorm.utils.resolveRuntimeMetadata
import com.kotlinorm.utils.toDatabaseBooleanValue
import com.kotlinorm.utils.toDatabaseParameterValue
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KType

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
    private val targetType: KType,
    private var setUpsertFields: ToSelect<T, Any?> = null
) {
    private val metadata = pojo.resolveRuntimeMetadata()
    private var paramMap = pojo.toDataMap()
    private var tableName = metadata.tableName
    private var kClass = metadata.kClass
    private var createTimeStrategy = metadata.createTimeStrategy
    private var updateTimeStrategy = metadata.updateTimeStrategy
    private var logicDeleteStrategy = metadata.logicDeleteStrategy
    private var optimisticStrategy = metadata.optimisticLockStrategy
    internal var allFields = metadata.allFields
    private var onConflict = false
    private var toInsertFields: LinkedHashSet<Field> = []
    private var toUpdateFields: LinkedHashSet<Field> = []
    private var onFields: LinkedHashSet<Field> = []
    private var cascadeEnabled = true
    private var cascadeAllowed: Set<Field>? = null
    private var lock: SqlLock? = null
    private var paramMapNew = mutableMapOf<Field, Any?>()
    private var conflictAssignmentValues = mutableMapOf<Field, Any?>()
    private var generatedPrimaryKey: Pair<Field, Any?>? = null

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
        prepareGeneratedPrimaryKey()

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
        val fieldMap = metadata.fieldMap
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
            createTimeStrategy?.execute(true) { field, value ->
                toInsertFields += field
                toUpdateFields -= field
                paramMap[field.name] = value
            }

            updateTimeStrategy?.execute(true) { field, value ->
                toInsertFields += field
                toUpdateFields += field
                paramMap[field.name] = value
            }

            logicDeleteStrategy?.execute(defaultValue = false) { field, _ ->
                toInsertFields += field
                toUpdateFields += field
                paramMap[field.name] = toDatabaseBooleanValue(dataSource, field, false)
            }

            optimisticStrategy?.execute(defaultValue = 0) { field, value ->
                toInsertFields += field
                toUpdateFields += field
                paramMap[field.name] = value
                paramMap["${field.name}2PlusNew"] = 1
                conflictAssignmentValues[field] = SqlExpr.Binary(
                    SqlExpr.Column(tableName = tableName, columnName = field.columnName),
                    SqlBinaryOperator.Plus,
                    SqlExpr.Parameter(SqlParameter.Named("${field.name}2PlusNew"))
                )
            }

            val conflictFields = inferConflictFields(paramMap)
            val statement = toSqlUpsertStatement(paramMap, dataSource, conflictFields, requireConflictTarget = true)
            val rendered = renderStatement(dataSource, statement, paramMap, fieldMap)
            val jdbcTypeHints = (toInsertFields + toUpdateFields + onFields).jdbcNullParameterTypeHints(rendered.parameters)
            return KronosAtomicActionTask(
                rendered.sql,
                rendered.parameters,
                operationType = KOperationType.UPSERT,
                statement = statement,
                stash = JdbcParameterTypeHints.stashFor(jdbcTypeHints),
                listParameterOccurrences = rendered.listParameterOccurrences
            ).toKronosActionTask()
        } else {
            val tasks: List<KronosAtomicActionTask> = []
            val fallbackStatement = toSqlUpsertStatement(paramMap, dataSource, onFields.toList(), requireConflictTarget = false)
            return tasks.toKronosActionTask().doBeforeExecute { dataSource ->

                lock = lock ?: SqlLock.Update().takeIf { optimisticStrategy?.enabled != true }

                val selectClause = pojo.selectWithType(targetType)
                    .cascade(enabled = false)
                    .lock(lock)
                    .apply {
                        with(context) {
                            setProjectionItems(
                                listOf(KTableForSelect.ProjectionItem.SelectItemValue(rawSqlSelectItem("1"))),
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

                val fallbackTask = if (selectClause.firstOrNull<Int>(dataSource) != null) {
                    val updateClause = pojo.updateWithType(targetType).cascade(cascadeEnabled)
                        .apply {
                            with(context) {
                                cascadeAllowed = this@UpsertClause.cascadeAllowed
                                logicEnabled = false
                                restoreLogicDeleteOnUpdate = this@UpsertClause.logicDeleteStrategy?.enabled == true
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
                        }
                    updateClause.build(dataSource)
                } else {
                    pojo.insert().cascade(cascadeEnabled)
                        .apply {
                            this@apply.cascadeAllowed = this@UpsertClause.cascadeAllowed
                            generatedPrimaryKey?.let { (field, value) ->
                                withPreparedPrimaryKey(field.name, value)
                            }
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

    private fun prepareGeneratedPrimaryKey() {
        val primaryKey = metadata.primaryKey ?: return
        val currentValue = paramMap[primaryKey.name]
        val resolvedValue = primaryKey.resolveGeneratedPrimaryKeyValue(currentValue)
        if (resolvedValue != null) {
            paramMap[primaryKey.name] = resolvedValue
        }
        if (currentValue == null && resolvedValue != null) {
            generatedPrimaryKey = primaryKey to resolvedValue
        }
    }

    private fun toSqlUpsertStatement(
        parameterValues: MutableMap<String, Any?>,
        dataSource: KronosDataSourceWrapper,
        conflictFields: List<Field>,
        requireConflictTarget: Boolean
    ): SqlDmlStatement.Upsert {
        require(!requireConflictTarget || conflictFields.isNotEmpty()) {
            "Unable to infer upsert conflict target for $tableName. Use on { ... } or define a valued primary/unique key."
        }
        val insertColumns = toInsertFields.map { field -> SqlIdentifier.of(field.columnName) }
        val insertValues = toInsertFields.map { field ->
            SqlExpr.Parameter(SqlParameter.Named(field.name))
        }
        val conflictColumns = conflictFields.map { field -> SqlIdentifier.of(field.columnName) }
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

    private fun inferConflictFields(parameterValues: Map<String, Any?>): List<Field> {
        if (onFields.isNotEmpty()) return onFields.toList()

        val columns = allFields.filter { it.isColumn }
        val primaryField = columns.firstOrNull { it.primaryKey != PrimaryKeyType.NOT }
            ?: primaryKeyStrategy.takeIf { it.enabled }?.field?.let { strategyField ->
                columns.firstOrNull { it.name == strategyField.name || it.columnName == strategyField.columnName }
            }
        if (primaryField != null &&
            !(primaryField.primaryKey == PrimaryKeyType.IDENTITY && parameterValues[primaryField.name] == null) &&
            parameterValues[primaryField.name] != null
        ) {
            return listOf(primaryField)
        }

        val fieldMap = metadata.fieldMap
        return metadata.tableIndexes
            .asSequence()
            .filter { it.type.equals("UNIQUE", ignoreCase = true) || it.method.equals("UNIQUE", ignoreCase = true) }
            .map { index -> index.columns.mapNotNull { column -> fieldMap[column] }.distinct() }
            .firstOrNull { fields ->
                fields.isNotEmpty() &&
                    fields.size == fields.map { it.columnName }.distinct().size &&
                    fields.all { field -> parameterValues[field.name] != null }
            }
            .orEmpty()
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
