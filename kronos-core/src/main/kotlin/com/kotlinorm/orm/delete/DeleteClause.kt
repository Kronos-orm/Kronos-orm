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

package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.dsl.KTableConditional.Companion.conditionalRun
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.database.SqlManager.getDeleteSql
import com.kotlinorm.database.SqlManager.getUpdateSql
import com.kotlinorm.database.SqlManager.quoted
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeDeleteClause
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.ConditionSqlBuilder.buildConditionSqlWithParams
import com.kotlinorm.utils.ConditionSqlBuilder.toWhereSql
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KProperty

class DeleteClause<T : KPojo>(private val pojo: T) {
    private var paramMap = pojo.toDataMap()
    private var tableName = pojo.kronosTableName()
    private var updateTimeStrategy = pojo.kronosUpdateTime()
    private var logicDeleteStrategy = pojo.kronosLogicDelete()
    private var optimisticStrategy = pojo.kronosOptimisticLock()
    private var logic = false
    private var condition: Criteria? = null
    private var allFields = pojo.kronosColumns().toLinkedSet()
    private var cascadeEnabled = true
    private var cascadeAllowed: Array<out KProperty<*>> = arrayOf() // 级联查询的深度限制, 默认为不限制，即所有级联查询都会执行

    fun logic(enabled: Boolean = true): DeleteClause<T> {
        // 若logicDeleteStrategy.enabled为false则抛出异常
        // 若updateTimeStrategy.enabled为false则不更新updateTime
        if (!this.logicDeleteStrategy.enabled && enabled) {
            throw NeedFieldsException()
        }
        this.logic = enabled
        return this
    }

    /**
     * 根据指定的字段构建删除语句的条件部分。
     *
     * @param someFields KTableField类型，表示要用于删除条件的字段。不可为null。
     * @return DeleteClause类型，表示构建完成的删除语句实例。
     * @throws NeedFieldsException 如果someFields为空或者最终没有有效的字段用于构建条件时抛出。
     */
    fun by(someFields: KTableField<T, Any?>): DeleteClause<T> {
        // 检查传入的someFields是否为null，若为null则抛出异常
        if (someFields == null) throw NeedFieldsException()
        pojo.tableRun {
            someFields(it)
            // 若fields为空，则抛出异常，表示需要至少一个字段来构建删除条件
            if (fields.isEmpty()) {
                throw NeedFieldsException()
            }

            // 根据fields中的字段及其值构建删除条件
            condition = fields.map { field -> field.eq(paramMap[field.name]) }.toCriteria()
        }
        return this
    }

    fun cascade(vararg props: KProperty<*>, enabled: Boolean = true): DeleteClause<T> {
        this.cascadeEnabled = enabled
        this.cascadeAllowed = props
        return this
    }

    /**
     * 构建删除语句的条件部分。
     *
     * 该函数允许用户指定一个删除条件，用于过滤需要被删除的数据。如果未指定条件，则默认删除所有匹配的数据。
     *
     * @param deleteCondition 一个函数，用于定义删除操作的条件。该函数接收一个 [KTableConditionalField] 类型的参数，
     *                        并返回一个 [Boolean?] 类型的值，用于指示是否满足删除条件。如果为 null，则表示删除所有数据。
     * @return [DeleteClause] 类型的实例，用于链式调用其它删除操作。
     */
    fun where(deleteCondition: KTableConditionalField<T, Boolean?> = null): DeleteClause<T> {
        if (deleteCondition == null) return this
        // 如果指定了删除条件，执行条件函数，并设置条件
        pojo.conditionalRun {
            propParamMap = paramMap
            deleteCondition(it)
            condition = criteria
        }
        return this
    }


    /**
     * 构建并返回一个KronosAtomicTask对象，用于执行数据库的原子操作。
     * 该方法根据设定的条件构建对应的UPDATE或DELETE SQL语句，并封装必要的参数与操作类型。
     *
     * @return [KronosAtomicActionTask] 一个包含SQL语句、参数映射以及操作类型的原子任务对象。
     */
    fun build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
        if (condition == null) {
            // 当未指定删除条件时，构建一个默认条件，即删除所有字段都不为null的记录
            condition = allFields.filter { it.isColumn }.mapNotNull { field ->
                field.eq(paramMap[field.name]).takeIf { it.value != null }
            }.toCriteria()
        }

        // 设置逻辑删除的策略
        if (logic) {
            setCommonStrategy(logicDeleteStrategy) { field, value ->
                condition = listOfNotNull(
                    condition, "${field.quoted(wrapper.orDefault())} = $value".asSql()
                ).toCriteria()
            }
        }

        // 构建条件SQL语句及参数映射
        val (whereClauseSql, paramMap) = buildConditionSqlWithParams(wrapper, condition)

        // 处理逻辑删除时的更新字段逻辑
        if (logic) {
            val toUpdateFields = mutableListOf<Field>()
            val updateFields = { field: Field, value: Any? ->
                toUpdateFields += (field + "New")
                paramMap[field.name + "New"] = value
            }
            // 设置更新时间和逻辑删除字段的策略
            setCommonStrategy(updateTimeStrategy, true, callBack = updateFields)
            setCommonStrategy(logicDeleteStrategy, defaultValue = 1, callBack = updateFields)

            var versionField: String? = null
            setCommonStrategy(optimisticStrategy) { field, _ ->
                versionField = field.columnName
            }

            return CascadeDeleteClause.build(
                cascadeEnabled, cascadeAllowed, pojo, whereClauseSql, paramMap, true, KronosAtomicActionTask(
                    getUpdateSql(
                        wrapper.orDefault(),
                        tableName,
                        toUpdateFields,
                        versionField,
                        toWhereSql(whereClauseSql)
                    ),
                    paramMap,
                    operationType = KOperationType.DELETE
                )
            )
        } else {
            // 组装UPDATE语句并返回KronosAtomicTask对象
            return CascadeDeleteClause.build(
                cascadeEnabled, cascadeAllowed, pojo, whereClauseSql, paramMap, false, KronosAtomicActionTask(
                    getDeleteSql(wrapper.orDefault(), tableName, toWhereSql(whereClauseSql)),
                    paramMap,
                    operationType = KOperationType.DELETE
                )
            )
        }
    }

    /**
     * 执行Kronos操作的函数。
     *
     * @param wrapper 可选参数，KronosDataSourceWrapper的实例，用于提供数据源配置和上下文。
     *                如果为null，函数将使用默认配置执行操作。
     * @return 返回KronosOperationResult对象，包含操作的结果信息。
     */
    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        // 构建并执行Kronos操作，根据提供的wrapper配置执行，如果没有提供则使用默认配置
        return build(wrapper).execute(wrapper)
    }

    companion object {

        /**
         * Applies the `by` operation to each update clause in the list based on the provided fields.
         *
         * @param someFields the fields to set the condition for
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> Iterable<DeleteClause<T>>.by(someFields: KTableField<T, Any?>): List<DeleteClause<T>> {
            return map { it.by(someFields) }
        }

        /**
         * Applies the `where` operation to each update clause in the list based on the provided update condition.
         *
         * @param updateCondition the condition for the update clause. Defaults to null.
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> Iterable<DeleteClause<T>>.where(updateCondition: KTableConditionalField<T, Boolean?> = null): List<DeleteClause<T>> {
            return map { it.where(updateCondition) }
        }

        fun <T : KPojo> Iterable<DeleteClause<T>>.logic(enabled: Boolean = true): List<DeleteClause<T>> {
            return map { it.logic(enabled) }
        }

        fun <T : KPojo> Iterable<DeleteClause<T>>.cascade(
            enabled: Boolean = true,
            vararg props: KProperty<*>
        ): List<DeleteClause<T>> {
            return map { it.cascade(*props, enabled = enabled) }
        }

        /**
         * Builds a KronosAtomicBatchTask from a list of UpdateClause objects.
         *
         * @param T The type of KPojo objects in the list.
         * @return A KronosAtomicBatchTask object with the SQL and parameter map array from the UpdateClause objects.
         */
        fun <T : KPojo> Iterable<DeleteClause<T>>.build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
            return map { it.build(wrapper) }.merge()
        }

        /**
         * Executes an array of UpdateClause objects and returns the result of the execution.
         *
         * @param wrapper The KronosDataSourceWrapper to use for the execution. Defaults to null.
         * @return The KronosOperationResult of the execution.
         */
        fun <T : KPojo> Iterable<DeleteClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }

        /**
         * Applies the `by` operation to each update clause in the array based on the provided fields.
         *
         * @param someFields the fields to set the condition for
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> Array<DeleteClause<T>>.by(someFields: KTableField<T, Any?>): List<DeleteClause<T>> {
            return map { it.by(someFields) }
        }

        /**
         * Applies the `where` operation to each update clause in the array based on the provided update condition.
         *
         * @param updateCondition the condition for the update clause. Defaults to null.
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> Array<DeleteClause<T>>.where(updateCondition: KTableConditionalField<T, Boolean?> = null): List<DeleteClause<T>> {
            return map { it.where(updateCondition) }
        }

        fun <T : KPojo> Array<DeleteClause<T>>.logic(enabled: Boolean = true): List<DeleteClause<T>> {
            return map { it.logic(enabled) }
        }

        fun <T : KPojo> Array<DeleteClause<T>>.cascade(
            enabled: Boolean = true,
            vararg props: KProperty<*>
        ): List<DeleteClause<T>> {
            return map { it.cascade(*props, enabled = enabled) }
        }


        /**
         * Builds a KronosAtomicBatchTask from an array of UpdateClause objects.
         *
         * @param T The type of KPojo objects in the list.
         * @return A KronosAtomicBatchTask object with the SQL and parameter map array from the UpdateClause objects.
         */
        fun <T : KPojo> Array<DeleteClause<T>>.build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
            return map { it.build(wrapper) }.merge()
        }

        /**
         * Executes an array of UpdateClause objects and returns the result of the execution.
         *
         * @param wrapper The KronosDataSourceWrapper to use for the execution. Defaults to null.
         * @return The KronosOperationResult of the execution.
         */
        fun <T : KPojo> Array<DeleteClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }
    }
}