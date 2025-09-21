/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *     http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kotlinorm.orm.update

import com.kotlinorm.ast.Assignment
import com.kotlinorm.ast.BinaryOp
import com.kotlinorm.ast.ColumnRef
import com.kotlinorm.ast.CriteriaExpr
import com.kotlinorm.ast.Expression
import com.kotlinorm.ast.NamedParam
import com.kotlinorm.ast.Statement
import com.kotlinorm.ast.TableName
import com.kotlinorm.ast.UpdateStatement
import com.kotlinorm.ast.table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.enums.ConditionType
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
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.cache.kPojoCreateTimeCache
import com.kotlinorm.cache.kPojoLogicDeleteCache
import com.kotlinorm.cache.kPojoOptimisticLockCache
import com.kotlinorm.cache.kPojoUpdateTimeCache
import com.kotlinorm.database.RegisteredDBTypeManager.getDBSupport
import com.kotlinorm.database.SqlManager.quoted
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KActionInfo
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeUpdateClause
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSet
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.getDefaultBoolean
import com.kotlinorm.utils.processParams

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
    // 直接存储AST结构
    private var updateStatement: UpdateStatement? = null
    private var paramMap = pojo.toDataMap()
    private var tableName = pojo.kronosTableName()
    private var kClass = pojo.kClass()
    private var createTimeStrategy = kPojoCreateTimeCache[kClass]
    private var updateTimeStrategy = kPojoUpdateTimeCache[kClass]
    private var logicDeleteStrategy = kPojoLogicDeleteCache[kClass]
    private var optimisticStrategy = kPojoOptimisticLockCache[kClass]
    internal var allFields = kPojoAllFieldsCache[kClass]!!
    internal var paramMapNew = mutableMapOf<Field, Any?>()
    private var cascadeEnabled = true
    internal var cascadeAllowed: Set<Field>? = null

    /**
     * 初始化函数：用于配置更新字段和构建参数映射。 该函数不接受参数，也不返回任何值。 主要完成以下功能：
     * 1. 如果设置了更新字段，则对更新字段进行配置，并添加到更新字段列表中；
     * 2. 遍历更新字段列表，将字段名拼接为"New"格式，并映射到参数映射表中。
     */
    init {
        // Initialize update statement with target table
        updateStatement = UpdateStatement(TableName(table = tableName), mutableListOf())

        // Process update fields if provided
        if (setUpdateFields != null) {
            pojo.afterSelect {
                setUpdateFields!!(it) // Configure update fields
                fields.forEach { field ->
                    val columnName = field.name
                    val paramName = "${columnName}New"
                    updateStatement?.let { stmt ->
                        stmt.set.add(
                            Assignment(
                                ColumnRef(column = columnName, tableAlias = null),
                                NamedParam(paramName)
                            )
                        )
                        paramMapNew[field] = paramMap[columnName]
                    }
                }
            }
        } else {
            // If no fields are specified, update all fields except the ID
            allFields.forEach { field ->
                if (field.name != "id") { // Skip ID field as it's typically not updated
                    val columnName = field.name
                    val paramName = "${columnName}New"
                    updateStatement?.let { stmt ->
                        stmt.set.add(
                            Assignment(
                                ColumnRef(column = columnName, tableAlias = null),
                                NamedParam(paramName)
                            )
                        )
                        paramMapNew[field] = paramMap[columnName]
                    }
                }
            }
        }
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
            val plusAssign = plusAssignFields
            val minusAssign = minusAssignFields

            plusAssign.forEach { assign ->
                val assignField = assign.first
                val assignKey = assignField.name + "2PlusNew"
                updateStatement?.addPlusAssign(assignField, assignKey)
                paramMapNew[assignField + "2PlusNew"] = assign.second
            }
            minusAssign.forEach { assign ->
                val assignField = assign.first
                val assignKey = assignField.name + "2MinusNew"
                updateStatement?.addMinusAssign(assignField, assignKey)
                paramMapNew[assignField + "2MinusNew"] = assign.second
            }

            fields
                    .filter { field ->
                        field !in
                                plusAssign.map { item -> item.first } +
                                        minusAssign.map { item -> item.first }
                    }
                    .forEach { field -> updateStatement?.addUpdateField(field) }
            paramMapNew.putAll(
                    fieldParamMap
                            .filter { field ->
                                field.key !in
                                        plusAssign.map { item -> item.first } +
                                                minusAssign.map { item -> item.first }
                            }
                            .map { e -> e.key + "New" to e.value }
            )
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

            // 根据fields中的字段及其值构建删除条件
            val newCondition = fields.map { field -> field.eq(paramMap[field.name]) }.toCriteria()
            if (newCondition != null) {
                updateStatement?.let { stmt ->
                    if (stmt.condition == null) {
                        stmt.setWhereCriteria(newCondition)
                    } else {
                        stmt.condition!!.children.add(newCondition)
                    }
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
        pojo.afterFilter {
            criteriaParamMap = paramMap // 更新 propParamMap
            updateCondition(it)
            if (criteria == null) return@afterFilter
            updateStatement?.let { stmt ->
                if (stmt.condition == null) {
                    stmt.setWhereCriteria(criteria)
                } else {
                    // 如果已经有条件，则将新条件添加到现有条件中
                    stmt.condition!!.children.addAll(criteria!!.children)
                }
            }
        }
        return this
    }

    fun patch(vararg pairs: Pair<String, Any?>): UpdateClause<T> {
        paramMapNew.putAll(pairs.map { Field(it.first) to it.second })
        return this
    }

    /**
     * Builds a KronosAtomicTask based on the current state of the UpdateClause.
     *
     * This function generates a KronosAtomicTask object based on the current state of the
     * UpdateClause. It performs the following steps:
     * 1. If `isExcept` is true, removes the fields in `toUpdateFields` from `allFields` and updates
     * `paramMapNew` accordingly.
     * 2. If `toUpdateFields` is empty, updates `toUpdateFields` with all the fields in `allFields`
     * and updates `paramMapNew` accordingly.
     * 3. Sets the logic delete strategy by removing the field from `toUpdateFields`, updating
     * `paramMapNew`, and modifying the `condition` accordingly.
     * 4. Sets the update time strategy by updating `paramMapNew` with the new value.
     * 5. Constructs the SQL query string for the update operation.
     * 6. Constructs the parameter map for the SQL query.
     * 7. Merges the `paramMapNew` into the `paramMap`.
     * 8. Returns a KronosAtomicTask object with the constructed SQL query, parameter map, and
     * operation type.
     *
     * @return The constructed KronosAtomicTask.
     */
    fun build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
        if (updateStatement?.condition == null) {
            // 当未指定删除条件时，构建一个默认条件，即删除所有字段都不为null的记录
            val defaultCondition =
                    allFields
                            .asSequence()
                            .filter { it.isColumn }
                            .mapNotNull { field ->
                                field.eq(paramMap[field.name]).takeIf { it.value != null }
                            }
                            .toList()
                            .toCriteria()
            defaultCondition?.let { updateStatement?.setWhereCriteria(it) }
        }

        // 如果没有指定字段需要更新，则更新所有字段
        if (updateStatement?.toUpdateFields?.isEmpty() == true &&
                        updateStatement?.plusAssigns?.isEmpty() == true &&
                        updateStatement?.minusAssigns?.isEmpty() == true
        ) {
            allFields.forEach { field ->
                updateStatement?.addUpdateField(field)
                paramMapNew[field + "New"] =
                        processParams(wrapper.orDefault(), field, paramMap[field.name])
            }
        }

        // 设置逻辑删除策略，将被逻辑删除的字段从更新字段中移除，并更新条件语句
        logicDeleteStrategy?.execute(
                defaultValue = getDefaultBoolean(wrapper.orDefault(), false)
        ) { field, value ->
            updateStatement?.toUpdateFields?.remove(field)
            paramMapNew -= field + "New"
            // 构建逻辑删除的条件SQL
            val logicDeleteCondition = "${field.quoted(wrapper.orDefault())} = $value".asSql()
            updateStatement?.let { stmt ->
                if (stmt.condition == null) {
                    stmt.setWhereCriteria(logicDeleteCondition)
                } else {
                    stmt.condition!!.children.add(logicDeleteCondition)
                }
            }
        }

        createTimeStrategy?.apply {
            updateStatement?.toUpdateFields?.remove(field)
            paramMapNew -= field + "New"
        }

        // 设置更新时间策略，将更新时间字段添加到更新字段列表，并更新参数映射
        updateTimeStrategy?.execute(true) { field, value ->
            updateStatement?.addUpdateField(field)
            paramMapNew[field + "New"] = processParams(wrapper.orDefault(), field, value)
        }

        // 去重和过滤更新字段
        updateStatement?.toUpdateFields?.let { fields ->
            val distinctFields = fields
                    .asSequence()
                    .distinctBy { it.columnName }
                    .filter { it.isColumn }
                    .toList()
            fields.clear()
            fields.addAll(distinctFields)
        }

        optimisticStrategy?.execute { field, _ ->
            if (updateStatement?.toUpdateFields?.any { it.columnName == field.columnName } == true
            ) {
                throw IllegalArgumentException("The version field cannot be updated manually.")
            }

            updateStatement?.addPlusAssign(field, field.name + "2PlusNew")
            paramMapNew[field + "2PlusNew"] = 1
        }

        // 构建完整的更新SQL语句，包括条件部分
        val paramMap = mutableMapOf<String, Any?>()

        // Build AST UpdateStatement
        val assignments = mutableListOf<Assignment>()
        updateStatement?.toUpdateFields?.forEach { field ->
            val paramName = field.name + "New"
            assignments +=
                    Assignment(
                            ColumnRef(column = field.columnName, tableAlias = field.tableName),
                            NamedParam(paramName)
                    )
        }
        updateStatement?.plusAssigns?.forEach { (field, paramKey) ->
            assignments +=
                    Assignment(
                            ColumnRef(tableAlias = field.tableName, column = field.columnName),
                            BinaryOp(
                                    ColumnRef(
                                            tableAlias = field.tableName,
                                            column = field.columnName
                                    ),
                                    "+",
                                    NamedParam(":" + paramKey)
                            )
                    )
        }
        updateStatement?.minusAssigns?.forEach { (field, paramKey) ->
            assignments +=
                    Assignment(
                            ColumnRef(tableAlias = field.tableName, column = field.columnName),
                            BinaryOp(
                                    ColumnRef(
                                            tableAlias = field.tableName,
                                            column = field.columnName
                                    ),
                                    "-",
                                    NamedParam(":" + paramKey)
                            )
                    )
        }
        // 构建AST结构
        updateStatement = UpdateStatement(
                target = table(tableName),
                set = assignments.toMutableList(),
                where = updateStatement?.where
        )

        // 通过DatabaseSupport渲染SQL
        val support = getDBSupport(wrapper.orDefault().dbType)
                ?: throw UnsupportedDatabaseTypeException(wrapper.orDefault().dbType)
        val rendered = support.getUpdateSqlWithParams(wrapper.orDefault(), updateStatement!!)
        val sql = rendered.sql
        paramMap.putAll(rendered.params)

        // 合并参数映射，准备执行SQL所需的参数
        val fieldMap = fieldsMapCache[kClass]!!
        paramMapNew.forEach { (key, value) ->
            val field = fieldMap[key.name]
            if (field != null && value != null) {
                paramMap[key.name] = processParams(wrapper.orDefault(), field, value)
            } else {
                paramMap[key.name] = value
            }
        }

        // 返回构建好的KronosAtomicTask实例
        val rootTask =
                KronosAtomicActionTask(
                        sql,
                        paramMap,
                        operationType = KOperationType.UPDATE,
                        actionInfo =
                                object : KActionInfo {
                                    override val kClass = this@UpdateClause.kClass
                                    override val statement: Statement? = updateStatement
                                    override val tableName: String? = this@UpdateClause.tableName
                                    override val where: Expression? = updateStatement?.where
                                }
                )

        return CascadeUpdateClause.build(
                cascadeEnabled,
                cascadeAllowed,
                pojo,
                kClass,
                paramMap.toMap(),
                (updateStatement?.toUpdateFields ?: emptySet()).toCollection(linkedSetOf()),
                (updateStatement?.where as? CriteriaExpr)?.let { /* legacy prop */ null },
                rootTask
        )
    }

    /**
     * Executes the update operation using the provided data source wrapper.
     *
     * @param wrapper The data source wrapper to use for the update operation. If not provided, a
     * default data source wrapper will be used.
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

        fun <T : KPojo> List<UpdateClause<T>>.cascade(enabled: Boolean): List<UpdateClause<T>> {
            return map { it.cascade(enabled) }
        }

        fun <T : KPojo> List<UpdateClause<T>>.cascade(
                someFields: ToReference<T, Any?>
        ): List<UpdateClause<T>> {
            return map { it.cascade(someFields) }
        }

        /**
         * Applies the `by` operation to each update clause in the list based on the provided
         * fields.
         *
         * @param someFields the fields to set the condition for
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> List<UpdateClause<T>>.by(
                someFields: ToSelect<T, Any?>
        ): List<UpdateClause<T>> {
            return map { it.by(someFields) }
        }

        /**
         * Applies the `where` operation to each update clause in the list based on the provided
         * update condition.
         *
         * @param updateCondition the condition for the update clause. Defaults to null.
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> List<UpdateClause<T>>.where(
                updateCondition: ToFilter<T, Boolean?> = null
        ): List<UpdateClause<T>> {
            return map { it.where(updateCondition) }
        }

        /**
         * Builds a KronosAtomicBatchTask from a list of UpdateClause objects.
         *
         * @param T The type of KPojo objects in the list.
         * @return A KronosAtomicBatchTask object with the SQL and parameter map array from the
         * UpdateClause objects.
         */
        fun <T : KPojo> List<UpdateClause<T>>.build(
                wrapper: KronosDataSourceWrapper? = null
        ): KronosActionTask {
            return map { it.build(wrapper) }.merge()
        }

        /**
         * Executes a list of UpdateClause objects and returns the result of the execution.
         *
         * @param wrapper The KronosDataSourceWrapper to use for the execution. Defaults to null.
         * @return The KronosOperationResult of the execution.
         */
        fun <T : KPojo> List<UpdateClause<T>>.execute(
                wrapper: KronosDataSourceWrapper? = null
        ): KronosOperationResult {
            return build().execute(wrapper)
        }
    }
}
