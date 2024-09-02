/**
 * Copyright 2022-2024 kronos-orm
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

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.dsl.KTableConditional.Companion.conditionalRun
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.database.SqlManager.getUpdateSql
import com.kotlinorm.database.SqlManager.quoted
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeUpdateClause
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KProperty

/**
 * Update Clause
 *
 * Creates an update clause for the given pojo.
 *
 * @param T the type of the pojo
 *
 * @property pojo the pojo for the update
 * @property isExcept whether to exclude the fields from the update
 * @param setUpdateFields the fields to update
 * @author yf, OUSC
 */
class UpdateClause<T : KPojo>(
    private val pojo: T,
    private var isExcept: Boolean = false,
    private var setUpdateFields: KTableField<T, Any?> = null
) {
    private var paramMap = pojo.toDataMap()
    private var tableName = pojo.kronosTableName()
    private var createTimeStrategy = pojo.kronosCreateTime()
    private var updateTimeStrategy = pojo.kronosUpdateTime()
    private var logicDeleteStrategy = pojo.kronosLogicDelete()
    private var optimisticStrategy = pojo.kronosOptimisticLock()
    internal var allFields = pojo.kronosColumns().toLinkedSet()
    internal var toUpdateFields = linkedSetOf<Field>()
    internal var condition: Criteria? = null
    internal var paramMapNew = mutableMapOf<Field, Any?>()
    private var cascadeEnabled = true
    private var cascadeAllowed: Array<out KProperty<*>> = arrayOf() // 级联查询的深度限制, 默认为不限制，即所有级联查询都会执行

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
            pojo.tableRun {
                setUpdateFields!!(it) // 配置更新字段
                toUpdateFields += fields // 将当前字段添加到更新字段列表
            }
            // 为每个更新字段在参数映射表中创建"New"版本的映射
            toUpdateFields.forEach {
                paramMapNew[it + "New"] = paramMap[it.name]
            }
        }
    }

    /**
     * Sets the new value for the update clause.
     *
     * @param newValue the new value to be set
     * @throws NeedFieldsException if the new value is null
     * @return the updated UpdateClause object
     */
    fun set(newValue: KTableField<T, Unit>): UpdateClause<T> {
        if (newValue == null) throw NeedFieldsException()
        pojo.tableRun {
            newValue(it)
            if (isExcept) {
                toUpdateFields -= fields.toSet()
            } else {
                toUpdateFields += fields
            }
            paramMapNew.putAll(fieldParamMap.map { e -> e.key + "New" to e.value })
        }
        return this
    }

    fun cascade(vararg props: KProperty<*>, enabled: Boolean = true): UpdateClause<T> {
        this.cascadeEnabled = enabled
        this.cascadeAllowed = props
        return this
    }

    /**
     * Sets the condition for the update clause based on the provided fields.
     *
     * @param someFields the fields to set the condition for
     * @throws NeedFieldsException if the provided fields are null
     * @return the updated UpdateClause object
     */
    fun by(someFields: KTableField<T, Any?>): UpdateClause<T> {
        if (someFields == null) throw NeedFieldsException()
        pojo.tableRun {
            someFields(it)
            condition = fields.map { it.eq(paramMap[it.name]) }.toCriteria()
        }
        return this
    }

    /**
     * Sets the condition for the update clause based on the provided update condition.
     *
     * @param updateCondition the condition for the update clause. Defaults to null.
     * @return the updated UpdateClause object.
     */
    fun where(updateCondition: KTableConditionalField<T, Boolean?> = null): UpdateClause<T> {
        if (updateCondition == null) return this
            .apply {
                // 获取所有字段 且去除null
                condition = allFields.filter { it.isColumn }.mapNotNull { field ->
                    field.eq(paramMap[field.name]).takeIf { it.value != null }
                }.toCriteria()
            }
        pojo.conditionalRun {
            propParamMap = paramMap // 更新 propParamMap
            updateCondition(it)
            condition = criteria
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

        if (condition == null) {
            // 当未指定删除条件时，构建一个默认条件，即删除所有字段都不为null的记录
            condition = allFields.filter { it.isColumn }.mapNotNull { field ->
                field.eq(paramMap[field.name]).takeIf { it.value != null }
            }.toCriteria()
        }

        // 处理字段更新逻辑，如果isExcept为true，则移除特定字段，否则更新所有字段
        if (isExcept) {
            // 移除指定字段并处理"create_time"字段的特殊情况
            toUpdateFields = (allFields.filter { it.isColumn } - toUpdateFields.toSet()).toLinkedSet()
            // 为更新的字段生成新的参数映射
            toUpdateFields.forEach {
                paramMapNew[it + "New"] = paramMap[it.name]
            }
        }

        // 如果没有指定字段需要更新，则更新所有字段
        if (toUpdateFields.isEmpty()) {
            toUpdateFields = allFields
            // 为所有字段生成新的参数映射
            toUpdateFields.forEach {
                paramMapNew[it + "New"] = paramMap[it.name]
            }
        }

        // 设置逻辑删除策略，将被逻辑删除的字段从更新字段中移除，并更新条件语句
        setCommonStrategy(logicDeleteStrategy) { field, value ->
            toUpdateFields -= field
            paramMapNew -= field + "New"
            // 构建逻辑删除的条件SQL
            condition = listOfNotNull(
                condition, "${logicDeleteStrategy.field.quoted(wrapper.orDefault())} = $value".asSql()
            ).toCriteria()
        }

        setCommonStrategy(createTimeStrategy) { field, _ ->
            toUpdateFields -= field
            paramMapNew -= field + "New"
        }

        // 设置更新时间策略，将更新时间字段添加到更新字段列表，并更新参数映射
        setCommonStrategy(updateTimeStrategy, true) { field, value ->
            toUpdateFields += field
            paramMapNew[field + "New"] = value
        }

        toUpdateFields = toUpdateFields.distinctBy { it.columnName }.filter { it.isColumn }.toLinkedSet()

        var versionField: String? = null
        setCommonStrategy(optimisticStrategy) { field, _ ->
            versionField = field.columnName
            if (toUpdateFields.any { it.columnName == versionField }) {
                throw IllegalArgumentException("The version field cannot be updated manually.")
            }
        }

        // 构建完整的更新SQL语句，包括条件部分
        val (whereClauseSql, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(wrapper, condition)
            .toWhereClause()

        val sql = getUpdateSql(wrapper.orDefault(), tableName, toUpdateFields.toList(), versionField, whereClauseSql)

        // 合并参数映射，准备执行SQL所需的参数
        paramMap.putAll(paramMapNew.map { it.key.name to it.value }.toMap())
        // 返回构建好的KronosAtomicTask实例

        val rootTask = KronosAtomicActionTask(
            sql,
            paramMap,
            operationType = KOperationType.UPDATE
        )

        return CascadeUpdateClause.build(
            cascadeEnabled,
            cascadeAllowed,
            pojo,
            paramMap.toMap(),
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
        fun <T : KPojo> List<UpdateClause<T>>.set(rowData: KTableField<T, Unit>): List<UpdateClause<T>> {
            return map { it.set(rowData) }
        }

        fun <T : KPojo> List<UpdateClause<T>>.cascade(
            vararg props: KProperty<*>,
            enabled: Boolean = true
        ): List<UpdateClause<T>> {
            return map { it.cascade(*props, enabled = enabled) }
        }

        /**
         * Applies the `by` operation to each update clause in the list based on the provided fields.
         *
         * @param someFields the fields to set the condition for
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> List<UpdateClause<T>>.by(someFields: KTableField<T, Any?>): List<UpdateClause<T>> {
            return map { it.by(someFields) }
        }

        /**
         * Applies the `where` operation to each update clause in the list based on the provided update condition.
         *
         * @param updateCondition the condition for the update clause. Defaults to null.
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> List<UpdateClause<T>>.where(updateCondition: KTableConditionalField<T, Boolean?> = null): List<UpdateClause<T>> {
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