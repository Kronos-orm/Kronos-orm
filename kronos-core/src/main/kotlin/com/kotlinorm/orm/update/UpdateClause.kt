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

package com.kotlinorm.orm.update

import com.kotlinorm.ast.Assignment
import com.kotlinorm.ast.ColumnReference
import com.kotlinorm.ast.CriteriaToAstConverter
import com.kotlinorm.ast.FieldToExpressionConverter
import com.kotlinorm.ast.Parameter
import com.kotlinorm.ast.TableName
import com.kotlinorm.ast.UpdateStatement
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.cache.kPojoAllColumnsCache
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.cache.kPojoCreateTimeCache
import com.kotlinorm.cache.kPojoLogicDeleteCache
import com.kotlinorm.cache.kPojoOptimisticLockCache
import com.kotlinorm.cache.kPojoUpdateTimeCache
import com.kotlinorm.database.SqlManager.quoted
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeUpdateClause
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSet
import com.kotlinorm.database.RegisteredDBTypeManager.getDBSupport
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.getDefaultBoolean
import com.kotlinorm.utils.processParams
import com.kotlinorm.utils.toLinkedSet

/**
 * Update Clause
 *
 * Creates an update clause for the given pojo.
 *
 * @param T the type of the pojo
 *
 * @property pojo the pojo for the update
 * @param setUpdateFields the fields to update
 * @author yf, OUSC
 */
class UpdateClause<T : KPojo>(
    private val pojo: T,
    private var setUpdateFields: ToSelect<T, Any?> = null
) {
    private var paramMap = pojo.toDataMap()
    private var tableName = pojo.__tableName
    private var kClass = pojo.kClass()
    private var createTimeStrategy = kPojoCreateTimeCache[kClass]
    private var updateTimeStrategy = kPojoUpdateTimeCache[kClass]
    internal var logicDeleteStrategy = kPojoLogicDeleteCache[kClass]
    private var optimisticStrategy = kPojoOptimisticLockCache[kClass]
    internal var allFields = kPojoAllFieldsCache[kClass]!!
    internal var allColumns = kPojoAllColumnsCache[kClass]!!
    private var cascadeEnabled = true
    internal var cascadeAllowed: Set<Field>? = null

    // AST UpdateStatement - all data stored here, directly modified without copy()
    internal var statement: UpdateStatement = UpdateStatement(
        table = TableName(table = tableName),
        assignments = mutableListOf()
    )

    // Store parameter values for assignments (needed for parameter binding)
    // Key: Field name, Value: parameter value
    private val assignmentParams = mutableMapOf<String, Any?>()
    
    // Store parameter values extracted from Criteria (WHERE/BY clauses)
    // Key: Parameter name, Value: parameter value
    internal val criteriaParams = mutableMapOf<String, Any?>()
    
    // Track whether where() method was called (even if condition was ignored)
    private var whereCalled = false

    // Store plus/minus assignments (for += and -= operations)
    private val plusAssigns = mutableListOf<Pair<Field, String>>()
    private val minusAssigns = mutableListOf<Pair<Field, String>>()

    /**
     * 初始化函数：用于配置更新字段和构建参数映射。
     * 该函数不接受参数，也不返回任何值。
     * 主要完成以下功能：
     * 1. 如果设置了更新字段，则对更新字段进行配置，并添加到更新字段列表中；
     * 2. 遍历更新字段列表，将字段名拼接为"New"格式，并映射到参数映射表中。
     */
    init {
        // 如果设置了更新字段，则进行字段配置和更新字段列表的构建
        if (setUpdateFields != null) {
            pojo.afterSelect { selectTable ->
                setUpdateFields!!(selectTable) // 配置更新字段
                // Add assignments to statement
                fields.forEach { field ->
                    val columnRef = fieldToColumnReference(field)
                    val paramName = "${field.name}New"
                    val param = Parameter.NamedParameter(paramName)
                    statement.assignments.add(Assignment(columnRef, param))
                    assignmentParams[paramName] = paramMap[field.name]
                }
            }
        }
    }

    /**
     * Converts a Field to a ColumnReference.
     * In single-table operations, we should not use table prefix.
     */
    private fun fieldToColumnReference(field: Field): ColumnReference {
        return FieldToExpressionConverter.fieldToColumnReference(field, useTableAlias = false)
    }

    /**
     * Returns the AST UpdateStatement with all parameters and checks applied.
     * 
     * @param wrapper Optional KronosDataSourceWrapper for processing
     * @param parameterValues Mutable map to collect parameter values from Criteria
     * @return Complete UpdateStatement AST
     */
    fun toStatement(wrapper: KronosDataSourceWrapper? = null, parameterValues: MutableMap<String, Any?> = mutableMapOf()): UpdateStatement {
        val dataSource = wrapper.orDefault()
        val support = getDBSupport(dataSource.dbType)

        // Build condition from paramMap if where is null AND where() was not called
        if (statement.where == null && !whereCalled) {
            // Build default condition from all fields
            val buildCondition = allFields.asSequence().filter { it.isColumn }.mapNotNull { field ->
                field.eq(paramMap[field.name]).takeIf { criteria -> criteria.value != null }
            }.toList().toCriteria()

            statement.where = CriteriaToAstConverter.convert(buildCondition, parameterValues, KOperationType.UPDATE)
            // Note: statement.where can be null if buildCondition is empty
        }

        // If no assignments, add all fields
        if (statement.assignments.isEmpty() && plusAssigns.isEmpty() && minusAssigns.isEmpty()) {
            if (support != null) {
                allFields.filter { it.isColumn }.forEach { field ->
                    val columnRef = fieldToColumnReference(field)
                    val paramName = "${field.name}New"
                    val param = Parameter.NamedParameter(paramName)
                    statement.assignments.add(Assignment(columnRef, param))
                    assignmentParams[paramName] = support.processParams(dataSource, field, paramMap[field.name])
                }
            }
        }

        // Apply create time strategy (remove from assignments)
        createTimeStrategy?.apply {
            statement.assignments.removeAll { it.column.columnName == field.columnName }
            // Also remove from assignmentParams
            assignmentParams.remove("${field.name}New")
        }

        // Remove duplicates by column name (keep first occurrence)
        // This must happen BEFORE update_time is added
        val finalAssignments = mutableListOf<Assignment>()
        val seenColumns = mutableSetOf<String>()
        // Iterate forward to keep the first occurrence
        statement.assignments.forEach { assignment ->
            val columnName = assignment.column.columnName
            if (!seenColumns.contains(columnName)) {
                finalAssignments.add(assignment)
                seenColumns.add(columnName)
            }
        }
        statement.assignments.clear()
        statement.assignments.addAll(finalAssignments)

        // Apply update time strategy (add to assignments after deduplication)
        // Insert update_time after the last non-arithmetic assignment (before +=/-= operations)
        if (support != null) {
            updateTimeStrategy?.execute(true) { field, value ->
                val columnRef = fieldToColumnReference(field)
                val paramName = "${field.name}New"
                val param = Parameter.NamedParameter(paramName)
                // Remove existing assignment if any
                statement.assignments.removeAll { it.column.columnName == field.columnName }
                
                // Find the position to insert: after the last regular assignment, before arithmetic operations
                val insertIndex = statement.assignments.indexOfLast { assignment ->
                    // Check if this is a regular assignment (not arithmetic)
                    assignment.value is Parameter.NamedParameter
                }
                
                if (insertIndex >= 0) {
                    statement.assignments.add(insertIndex + 1, Assignment(columnRef, param))
                } else {
                    statement.assignments.add(Assignment(columnRef, param))
                }
                assignmentParams[paramName] = support.processParams(dataSource, field, value)
            }
        }

        // Apply logic delete strategy (remove from assignments and add to where) - always apply if strategy exists
        logicDeleteStrategy?.execute(defaultValue = getDefaultBoolean(dataSource, false)) { field, value ->
                // Remove from assignments
                statement.assignments.removeAll { it.column.columnName == field.columnName }
                // Also remove from assignmentParams
                assignmentParams.remove("${field.name}New")
                // Build logic delete condition with literal value (not parameter)
                // Logic delete values are fixed (0 or 1), so we use literals
                val logicDeleteExpression = com.kotlinorm.ast.BinaryExpression(
                    com.kotlinorm.ast.ColumnReference(database = null, tableAlias = null, columnName = field.columnName),
                    com.kotlinorm.ast.SqlOperator.EQUAL,
                    com.kotlinorm.ast.Literal.NumberLiteral(value.toString())
                )

                statement.where = if (statement.where == null) {
                    logicDeleteExpression
                } else {
                    com.kotlinorm.ast.BinaryExpression(
                        statement.where!!,
                        com.kotlinorm.ast.SqlOperator.AND,
                        logicDeleteExpression
                    )
                }
        }

        // Apply optimistic lock strategy (add += 1 assignment)
        optimisticStrategy?.execute { field, _ ->
            // Check if field is already in assignments
            if (statement.assignments.any { it.column.columnName == field.columnName }) {
                throw IllegalArgumentException("The version field cannot be updated manually.")
            }

            val columnRef = fieldToColumnReference(field)
            val paramName = "${field.name}2PlusNew"
            val param = Parameter.NamedParameter(paramName)
            val addExpression = com.kotlinorm.ast.BinaryExpression(
                columnRef,
                com.kotlinorm.ast.SqlOperator.ADD,
                param
            )
            statement.assignments.add(Assignment(columnRef, addExpression))
            plusAssigns += field to paramName
            assignmentParams[paramName] = 1
        }

        return statement
    }

    /**
     * Renders the UpdateStatement to SQL with processed parameters.
     */
    private fun renderStatement(wrapper: KronosDataSourceWrapper?): Pair<String, Map<String, Any?>> {
        val dataSource = wrapper.orDefault()
        val support = getDBSupport(dataSource.dbType) ?: throw UnsupportedDatabaseTypeException(dataSource.dbType)

        // Collect parameter values from Criteria during AST conversion in toStatement()
        val criteriaParameterValues = mutableMapOf<String, Any?>()
        
        // Get complete statement with all parameters and checks applied
        val finalStatement = toStatement(wrapper, criteriaParameterValues)

        // Render AST to SQL with parameters
        val renderedSql = support.getUpdateSqlWithParams(dataSource, finalStatement)

        // Process parameters
        val paramMapNew = mutableMapOf<String, Any?>()
        val fieldMap = fieldsMapCache[kClass]!!
        
        // First, add parameter values extracted from Criteria in where()/by() methods
        // These parameters are guaranteed to be used in the WHERE clause
        criteriaParams.forEach { (key, value) ->
            val field = fieldMap[key]
            if (field != null) {
                paramMapNew[key] = support.processParams(dataSource, field, value)
            } else {
                paramMapNew[key] = value
            }
        }
        
        // Second, add parameter values extracted from Criteria during toStatement()
        // These are from default WHERE conditions built from paramMap
        criteriaParameterValues.forEach { (key, value) ->
            if (!paramMapNew.containsKey(key)) {
                val field = fieldMap[key]
                if (field != null) {
                    paramMapNew[key] = support.processParams(dataSource, field, value)
                } else {
                    paramMapNew[key] = value
                }
            }
        }
        
        // Third, add assignment parameters (SET clause) - include null values
        assignmentParams.forEach { (key, value) ->
            val field =
                allColumns.find { "${it.name}New" == key || "${it.name}2PlusNew" == key || "${it.name}2MinusNew" == key }
            if (field != null) {
                paramMapNew[key] = support.processParams(dataSource, field, value)
            } else {
                paramMapNew[key] = value
            }
        }
        
        // Fourth, merge rendered parameters (if any were added by the renderer)
        renderedSql.parameters.forEach { (key, value) ->
            if (!paramMapNew.containsKey(key) && value != null) {  // Only add if not already present
                val field = fieldMap[key]
                if (field != null) {
                    paramMapNew[key] = support.processParams(dataSource, field, value)
                } else {
                    paramMapNew[key] = value
                }
            }
        }

        return Pair(renderedSql.sql, paramMapNew)
    }

    /**
     * Sets the new value for the update clause.
     *
     * @param newValue the new value to be set
     * @throws EmptyFieldsException if the new value is null
     * @return the updated UpdateClause object
     */
    fun set(newValue: ToSet<T, Unit>): UpdateClause<T> {
        newValue ?: throw EmptyFieldsException()
        pojo.afterSet {
            newValue(it)
            val plusAssignMap = plusAssignFields.toMap()
            val minusAssignMap = minusAssignFields.toMap()

            // Process fields in order, checking if each is a plus/minus assign or regular assign
            fields.forEach { field ->
                when {
                    plusAssignMap.containsKey(field) -> {
                        // Handle plus assignment (column = column + value)
                        val columnRef = fieldToColumnReference(field)
                        val paramName = "${field.name}2PlusNew"
                        val param = Parameter.NamedParameter(paramName)
                        val addExpression = com.kotlinorm.ast.BinaryExpression(
                            columnRef,
                            com.kotlinorm.ast.SqlOperator.ADD,
                            param
                        )
                        statement.assignments.add(Assignment(columnRef, addExpression))
                        plusAssigns += field to paramName
                        assignmentParams[paramName] = plusAssignMap[field]!!
                    }
                    minusAssignMap.containsKey(field) -> {
                        // Handle minus assignment (column = column - value)
                        val columnRef = fieldToColumnReference(field)
                        val paramName = "${field.name}2MinusNew"
                        val param = Parameter.NamedParameter(paramName)
                        val subtractExpression = com.kotlinorm.ast.BinaryExpression(
                            columnRef,
                            com.kotlinorm.ast.SqlOperator.SUBTRACT,
                            param
                        )
                        statement.assignments.add(Assignment(columnRef, subtractExpression))
                        minusAssigns += field to paramName
                        assignmentParams[paramName] = minusAssignMap[field]!!
                    }
                    else -> {
                        // Handle regular field assignment
                        val columnRef = fieldToColumnReference(field)
                        val paramName = "${field.name}New"
                        val param = Parameter.NamedParameter(paramName)
                        statement.assignments.add(Assignment(columnRef, param))
                        assignmentParams[paramName] = fieldParamMap[field]
                    }
                }
            }
            
            // Note: update_time will be added in toStatement() after all user fields
        }
        return this
    }

    fun cascade(enabled: Boolean): UpdateClause<T> {
        cascadeEnabled = enabled
        return this
    }

    fun cascade(someFields: ToReference<T, Any?>): UpdateClause<T> {
        someFields ?: throw EmptyFieldsException()
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

    /**
     * Sets the condition for the update clause based on the provided fields.
     *
     * @param someFields the fields to set the condition for
     * @throws EmptyFieldsException if the provided fields are null
     * @return the updated UpdateClause object
     */
    fun by(someFields: ToSelect<T, Any?>): UpdateClause<T> {
        someFields ?: throw EmptyFieldsException()
        pojo.afterSelect {
            someFields(it)
            if (fields.isEmpty()) {
                throw EmptyFieldsException()
            }

            // Build condition from fields and merge with existing where condition
            val newCondition = fields.map { field -> field.eq(paramMap[field.name]) }.toCriteria()
            val localCriteriaParams = mutableMapOf<String, Any?>()
            val newExpression = CriteriaToAstConverter.convert(newCondition, localCriteriaParams, KOperationType.UPDATE)

            // Store criteria parameters for later use in renderStatement
            criteriaParams.putAll(localCriteriaParams)

            // Only update where if newExpression is not null
            if (newExpression != null) {
                statement.where = if (statement.where == null) {
                    newExpression
                } else {
                    com.kotlinorm.ast.BinaryExpression(
                        statement.where!!,
                        com.kotlinorm.ast.SqlOperator.AND,
                        newExpression
                    )
                }
            }
        }
        return this
    }

    /**
     * Sets the condition for the update clause based on the provided update condition.
     *
     * @param updateCondition the condition for the update clause. Defaults to null.
     * @return the updated UpdateClause object.
     */
    fun where(updateCondition: ToFilter<T, Boolean?> = null): UpdateClause<T> {
        if (updateCondition == null) return this
        whereCalled = true  // Mark that where() was called
        pojo.afterFilter { filterTable ->
            criteriaParamMap = paramMap // 更新 propParamMap
            updateCondition(filterTable)
            if (criteria == null) return@afterFilter

            // Convert Criteria to Expression and merge with existing where condition
            val localCriteriaParams = mutableMapOf<String, Any?>()
            val newExpression = CriteriaToAstConverter.convert(criteria!!, localCriteriaParams, KOperationType.UPDATE)
            
            // Store criteria parameters for later use in renderStatement
            criteriaParams.putAll(localCriteriaParams)
            
            // Only update where if newExpression is not null
            if (newExpression != null) {
                statement.where = if (statement.where == null) {
                    newExpression
                } else {
                    com.kotlinorm.ast.BinaryExpression(
                        statement.where!!,
                        com.kotlinorm.ast.SqlOperator.AND,
                        newExpression
                    )
                }
            }
        }
        return this
    }

    fun patch(vararg pairs: Pair<String, Any?>): UpdateClause<T> {
        pairs.forEach { (fieldName, value) ->
            val field = allColumns.find { it.name == fieldName } ?: return@forEach
            val columnRef = fieldToColumnReference(field)
            val paramName = "${fieldName}New"
            val param = Parameter.NamedParameter(paramName)
            // Remove existing assignment for this field if any
            statement.assignments.removeAll { it.column.columnName == field.columnName }
            statement.assignments.add(Assignment(columnRef, param))
            assignmentParams[paramName] = value
        }
        return this
    }

    /**
     * Builds a KronosAtomicTask based on the current state of the UpdateClause.
     *
     * This function generates a KronosAtomicTask object based on the current state of the UpdateClause. It performs the following steps:
     * 1. If `isExcept` is true, removes the fields in `toUpdateFields` from `allFields` and updates `paramMapNew` accordingly.
     * 2. If `toUpdateFields` is empty, updates `toUpdateFields` with all the fields in `allFields` and updates `paramMapNew` accordingly.
     * 3. Sets the logic delete strategy by removing the field from `toUpdateFields`, updating `paramMapNew`, and modifying the `condition` accordingly.
     * 4. Sets the update time strategy by updating `paramMapNew` with the new value.
     * 5. Constructs the SQL query string for the update operation.
     * 6. Constructs the parameter map for the SQL query.
     * 7. Merges the `paramMapNew` into the `paramMap`.
     * 8. Returns a KronosAtomicTask object with the constructed SQL query, parameter map, and operation type.
     *
     * @return The constructed KronosAtomicTask.
     */
    fun build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
        // Render statement to SQL with processed parameters
        val (sql, paramMap) = renderStatement(wrapper)

        // Get where clause SQL for UpdateClauseInfo (for cascade)
        // Extract WHERE clause from SQL string
        val whereClauseSql = if (sql.contains(" WHERE ", ignoreCase = true)) {
            val whereIndex = sql.indexOf(" WHERE ", ignoreCase = true)
            sql.substring(whereIndex + 7) // " WHERE " is 7 characters
        } else {
            null
        }

        // Get toUpdateFields from assignments for cascade compatibility
        val toUpdateFields = statement.assignments.mapNotNull { assignment ->
            allColumns.find { it.columnName == assignment.column.columnName }
        }.toLinkedSet()

        // 返回构建好的KronosAtomicTask实例
        val rootTask = KronosAtomicActionTask(
            sql,
            paramMap,
            operationType = KOperationType.UPDATE,
            UpdateClauseInfo(
                kClass,
                tableName,
                whereClauseSql
            )
        )

        return CascadeUpdateClause.build(
            cascadeEnabled,
            cascadeAllowed,
            pojo,
            kClass,
            paramMap,
            toUpdateFields,
            whereClauseSql,
            rootTask
        )
    }

    /**
     * Executes the update operation using the provided data source wrapper.
     *
     * @param wrapper The data source wrapper to use for the update operation. If not provided, a default data source wrapper will be used.
     * @return The result of the update operation.
     */
    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build().execute(wrapper)
    }

    companion object {
        /**
         * Sets the given row data for each update clause in the list.
         *
         * @param rowData the row data to set
         * @return a list of UpdateClause objects with the updated row data
         */
        fun <T : KPojo> List<UpdateClause<T>>.set(rowData: ToSet<T, Unit>): List<UpdateClause<T>> {
            return map { it.set(rowData) }
        }

        fun <T : KPojo> List<UpdateClause<T>>.cascade(
            enabled: Boolean
        ): List<UpdateClause<T>> {
            return map { it.cascade(enabled) }
        }

        fun <T : KPojo> List<UpdateClause<T>>.cascade(
            someFields: ToReference<T, Any?>
        ): List<UpdateClause<T>> {
            return map { it.cascade(someFields) }
        }

        /**
         * Applies the `by` operation to each update clause in the list based on the provided fields.
         *
         * @param someFields the fields to set the condition for
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> List<UpdateClause<T>>.by(someFields: ToSelect<T, Any?>): List<UpdateClause<T>> {
            return map { it.by(someFields) }
        }

        /**
         * Applies the `where` operation to each update clause in the list based on the provided update condition.
         *
         * @param updateCondition the condition for the update clause. Defaults to null.
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> List<UpdateClause<T>>.where(updateCondition: ToFilter<T, Boolean?> = null): List<UpdateClause<T>> {
            return map { it.where(updateCondition) }
        }

        /**
         * Builds a KronosAtomicBatchTask from a list of UpdateClause objects.
         *
         * @param T The type of KPojo objects in the list.
         * @return A KronosAtomicBatchTask object with the SQL and parameter map array from the UpdateClause objects.
         */
        fun <T : KPojo> List<UpdateClause<T>>.build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
            return map { it.build(wrapper) }.merge()
        }

        /**
         * Executes a list of UpdateClause objects and returns the result of the execution.
         *
         * @param wrapper The KronosDataSourceWrapper to use for the execution. Defaults to null.
         * @return The KronosOperationResult of the execution.
         */
        fun <T : KPojo> List<UpdateClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }
    }
}