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

package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.toLinkedSet

/**
 * Update Clause
 *
 * Creates an update clause for the given pojo.
 *
 * @param T the type of the pojo
 *
 * @property pojo the pojo for the update
 * @property isExcept whether to exclude the fields from the update
 * @param setUpsertFields the fields to update
 * @author Jieyao Lu, OUSC
 */
class UpsertClause<T : KPojo>(
    private val pojo: T,
    private var isExcept: Boolean = false,
    private var setUpsertFields: KTableField<T, Any?> = null
) {
    private var paramMap = pojo.toDataMap()
    private var tableName = pojo.kronosTableName()
    private var optimisticStrategy = pojo.kronosOptimisticLock()
    private var allFields = pojo.kronosColumns().toLinkedSet()
    private var onConflict = false
    private var toInsertFields = linkedSetOf<Field>()
    private var toUpdateFields = linkedSetOf<Field>()
    private var onFields = linkedSetOf<Field>()
    private var cascadeEnabled = true
    private var cascadeLimit = -1 // 级联查询的深度限制, -1表示无限制，0表示不查询级联，1表示只查询一层级联，以此类推
    private var lock:PessimisticLock? = null

    init {
        if (setUpsertFields != null) {
            pojo.tableRun {
                setUpsertFields!!(it)
                toUpdateFields += fields
            }
        }
    }

    private fun updateUpsertFields(updateOnFields: Boolean = false): (Field, Any?) -> Unit {
        return { field: Field, value: Any? ->
            toInsertFields += field
            toUpdateFields -= field
            if (updateOnFields) onFields += field
            paramMap[field.name] = value
        }
    }

    /**
     * Set the fields on which the update clause will be applied.
     *
     * @param someFields on which the update clause will be applied
     * @throws NeedFieldsException if the new value is null
     * @return the upsert UpdateClause object
     */
    fun on(someFields: KTableField<T, Any?>): UpsertClause<T> {
        if (null == someFields) throw NeedFieldsException()
        pojo.tableRun {
            someFields(it)
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

    fun cascade(enabled: Boolean = true, depth: Int = -1): UpsertClause<T> {
        this.cascadeEnabled = enabled
        this.cascadeLimit = depth
        return this
    }

    fun lock(lock: PessimisticLock = PessimisticLock.X): UpsertClause<T> {
        optimisticStrategy.enabled = false
        this.lock = lock
        return this
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build(wrapper).execute(wrapper)
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
        val dataSource = wrapper.orDefault()

        if (isExcept) {
            toUpdateFields = (allFields - toUpdateFields.toSet()) as LinkedHashSet<Field>
        }

        if (toInsertFields.isEmpty()) {
            toInsertFields = allFields.filter { null != paramMap[it.name] }.toLinkedSet()
        }

        if (toUpdateFields.isEmpty()) {
            toUpdateFields = allFields.toLinkedSet()
        }

        paramMap = paramMap.filter { it ->
            it.key in (toUpdateFields + toInsertFields + onFields).map { it.name }
        }.toMutableMap()

        if (onConflict) {
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

                lock = lock ?: PessimisticLock.X.takeIf { !optimisticStrategy.enabled }

                if ((pojo.select { "COUNT(1)".asSql() }
                        .cascade(false)
                        .lock(lock)
                        .apply {
                            condition = onFields.filter { it.isColumn && it.name in paramMap.keys }
                                .map {
                                    it.eq(paramMap[it.name])
                                }.toCriteria()
                        }.queryOneOrNull<Int>() ?: 0)
                    > 0
                ) {
                    pojo.update().cascade(cascadeEnabled, cascadeLimit)
                        .apply {
                            condition = onFields.filter { it.isColumn && it.name in paramMap.keys }
                                .map { it.eq(paramMap[it.name]) }.toCriteria()
                            this@UpsertClause.toUpdateFields = this.toUpdateFields
                        }
                        .execute(wrapper)
                } else {
                    setCommonStrategy(optimisticStrategy, false, callBack = updateUpsertFields())
                    pojo.insert().cascade(cascadeEnabled, cascadeLimit).execute(wrapper)
                }
            }
        }
    }

    companion object {
        fun <T : KPojo> List<UpsertClause<T>>.on(someFields: KTableField<T, Unit>): List<UpsertClause<T>> {
            return map { it.on(someFields) }
        }

        fun <T : KPojo> List<UpsertClause<T>>.onConflict(): List<UpsertClause<T>> {
            return map { it.onConflict() }
        }

        fun <T : KPojo> List<UpsertClause<T>>.cascade(enabled: Boolean = true, depth: Int = -1): List<UpsertClause<T>> {
            return map { it.cascade(enabled, depth) }
        }

        fun <T : KPojo> List<UpsertClause<T>>.build(): KronosActionTask {
            return map { it.build() }.merge()
        }

        fun <T : KPojo> List<UpsertClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }
    }

}