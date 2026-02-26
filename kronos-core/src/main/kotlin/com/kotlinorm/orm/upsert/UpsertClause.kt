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

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.cache.kPojoCreateTimeCache
import com.kotlinorm.cache.kPojoLogicDeleteCache
import com.kotlinorm.cache.kPojoOptimisticLockCache
import com.kotlinorm.cache.kPojoUpdateTimeCache
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.NoValueStrategyType
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.utils.DataSourceUtil.orDefault
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
    private var toInsertFields = linkedSetOf<Field>()
    private var toUpdateFields = linkedSetOf<Field>()
    private var onFields = linkedSetOf<Field>()
    private var cascadeEnabled = true
    private var cascadeAllowed: Set<Field>? = null
    private var lock: PessimisticLock? = null
    private var paramMapNew = mutableMapOf<Field, Any?>()

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

    fun lock(lock: PessimisticLock = PessimisticLock.X): UpsertClause<T> {
        optimisticStrategy?.enabled = false
        this.lock = lock
        return this
    }

    fun patch(vararg pairs: Pair<String, Any?>): UpsertClause<T> {
        paramMapNew.putAll(pairs.map { Field(it.first) to it.second })
        return this
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build(wrapper).execute(wrapper)
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
        val dataSource = wrapper.orDefault()

        if (toInsertFields.isEmpty()) {
            toInsertFields = allFields.filter { null != paramMap[it.name] }.toLinkedSet()
        }

        if (toUpdateFields.isEmpty()) {
            toUpdateFields = allFields
        }

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

        val paramMap = (paramMap.filter { it.key in (toUpdateFields + toInsertFields + onFields).map { f -> f.name } }).toMutableMap()

        if (onConflict) {
            onFields += toUpdateFields
            // 设置逻辑删除策略，将被逻辑删除的字段从更新字段中移除，并更新条件语句
            logicDeleteStrategy?.execute(defaultValue = getDefaultBoolean(wrapper.orDefault(), false)) { field, value ->
                toInsertFields += field
                paramMap[field.name] = value
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
            return KronosAtomicActionTask(
                SqlManager.getOnConflictSql(
                    dataSource, ConflictResolver(
                        tableName,
                        onFields,
                        toUpdateFields,
                        toInsertFields
                    )
                ),
                paramMap,
                operationType = KOperationType.UPSERT
            ).toKronosActionTask()
        } else {
            return listOf<KronosAtomicActionTask>().toKronosActionTask().doBeforeExecute {

                lock = lock ?: PessimisticLock.X.takeIf { optimisticStrategy?.enabled != true }

                if ((pojo.select()
                        .cascade(enabled = false)
                        .lock(lock)
                        .apply {
                            selectFields =
                                linkedSetOf(Field("COUNT(1)", "COUNT(1)", type = KColumnType.CUSTOM_CRITERIA_SQL))
                            selectAll = false
                            condition = onFields.filter { it.isColumn && it.name in paramMap.keys }
                                .map {
                                    it.eq(paramMap[it.name]).apply {
                                        noValueStrategyType = NoValueStrategyType.JudgeNull
                                    }
                                }.toCriteria()
                            logicDeleteStrategy = null
                        }
                        .queryOneOrNull<Int>() ?: 0)
                    > 0
                ) {
                    pojo.update().cascade(cascadeEnabled)
                        .apply {
                            this@apply.cascadeAllowed = this@UpsertClause.cascadeAllowed
                            this@apply.toUpdateFields = this@UpsertClause.toUpdateFields
                            this@UpsertClause.toUpdateFields.forEach {
                                this@apply.paramMapNew[it + "New"] = paramMap[it.name]
                            }
                            this@UpsertClause.logicDeleteStrategy?.execute(
                                defaultValue = getDefaultBoolean(
                                    wrapper.orDefault(),
                                    false
                                )
                            ) { field, value ->
                                this@apply.toUpdateFields += field
                                this@apply.paramMapNew[field + "New"] = value
                            }
                            condition = onFields.filter { it.isColumn && it.name in paramMap.keys }
                                .map { it.eq(paramMap[it.name]) }.toCriteria()
                            logicDeleteStrategy = null
                        }
                        .execute(wrapper)
                } else {
                    pojo.insert().cascade(cascadeEnabled)
                        .apply {
                            this@apply.cascadeAllowed = this@UpsertClause.cascadeAllowed
                        }
                        .execute(wrapper)
                }
            }
        }
    }

    companion object {
        fun <T : KPojo> List<UpsertClause<T>>.on(someFields: ToSelect<T, Any?>): List<UpsertClause<T>> {
            return map { it.on(someFields) }
        }

        fun <T : KPojo> List<UpsertClause<T>>.onConflict(): List<UpsertClause<T>> {
            return map { it.onConflict() }
        }

        fun <T : KPojo> List<UpsertClause<T>>.cascade(
            enabled: Boolean
        ): List<UpsertClause<T>> {
            return map { it.cascade(enabled) }
        }

        fun <T : KPojo> List<UpsertClause<T>>.cascade(
            someFields: ToReference<T, Any?>
        ): List<UpsertClause<T>> {
            return map { it.cascade(someFields) }
        }

        fun <T : KPojo> List<UpsertClause<T>>.build(): KronosActionTask {
            return map { it.build() }.merge()
        }

        fun <T : KPojo> List<UpsertClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }
    }

}