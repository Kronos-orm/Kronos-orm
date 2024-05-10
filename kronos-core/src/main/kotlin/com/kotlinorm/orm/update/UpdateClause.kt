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

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.dsl.KTableConditional.Companion.conditionalRun
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.Extensions.toMap
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.setCommonStrategy

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
    private val pojo: T, private var isExcept: Boolean = false, setUpdateFields: KTableField<T, Any?> = null
) {
    internal lateinit var tableName: String
    internal lateinit var updateTimeStrategy: KronosCommonStrategy
    internal lateinit var logicDeleteStrategy: KronosCommonStrategy
    internal var allFields: LinkedHashSet<Field> = linkedSetOf()
    private var toUpdateFields: LinkedHashSet<Field> = linkedSetOf()
    private var condition: Criteria? = null
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private var paramMapNew: MutableMap<Field, Any?> = mutableMapOf()

    init {
        paramMap.putAll(pojo.toMap().filter { it.value != null })
        if (setUpdateFields != null) {
            pojo.tableRun {
                setUpdateFields()
                toUpdateFields += fields
            }
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
            newValue()
            if (isExcept) {
                toUpdateFields -= fields.toSet()
            } else {
                toUpdateFields += fields
            }
            paramMapNew.putAll(fieldParamMap.map { it.key + "New" to it.value })
        }
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
            someFields()
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
                condition = paramMap.keys.mapNotNull { propName ->
                    allFields.first { it.name == propName }.eq(paramMap[propName]).takeIf { it.value != null }
                }.toCriteria()
            }
        pojo.conditionalRun {
            propParamMap = paramMap // 更新 propParamMap
            updateCondition()
            condition = criteria
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
    fun build(): KronosAtomicTask {
        // 如果 isExcept 为 true，则将 toUpdateFields 中的字段从 allFields 中移除
        if (isExcept) {
            toUpdateFields = (allFields - toUpdateFields.toSet()) as LinkedHashSet
            toUpdateFields.forEach {
                paramMapNew[it + "New"] = paramMap[it.name]
            }
        }

        if (toUpdateFields.isEmpty()) {
            // 全都更新
            toUpdateFields = allFields
            toUpdateFields.forEach {
                paramMapNew[it + "New"] = paramMap[it.name]
            }
        }

        // 设置逻辑删除
        setCommonStrategy(logicDeleteStrategy) { field, value ->
            toUpdateFields -= field
            paramMapNew -= field + "New"
            condition = listOfNotNull(
                condition, "${logicDeleteStrategy.field.quotedColumnName()} = $value".asSql()
            ).toCriteria()
        }

        // 设置更新时间
        setCommonStrategy(updateTimeStrategy, true) { field, value ->
            paramMapNew[field + "New"] = value
        }

        val updateFields = toUpdateFields.joinToString(", ") { "${it.quotedColumnName()} = :${it + "New"}" }

        val (conditionSql, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(condition, mutableMapOf())

        val sql = listOfNotNull(
            "UPDATE",
            "`$tableName`",
            "SET",
            updateFields,
            "WHERE".takeIf { !conditionSql.isNullOrEmpty() },
            conditionSql?.ifEmpty { null }
        ).joinToString(" ")

        // 合并
        paramMap.putAll(paramMapNew.map { it.key.name to it.value }.toMap())
        return KronosAtomicTask(
            sql,
            paramMap,
            operationType = KOperationType.UPDATE
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
        fun <T : KPojo> List<UpdateClause<T>>.build(): KronosAtomicBatchTask {
            val tasks = this.map { it.build() }
            return KronosAtomicBatchTask(
                sql = tasks.first().sql,
                paramMapArr = tasks.map { it.paramMap }.toTypedArray(),
                operationType = KOperationType.UPDATE
            )
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