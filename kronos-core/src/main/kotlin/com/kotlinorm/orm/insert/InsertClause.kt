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

package com.kotlinorm.orm.insert

import com.kotlinorm.Kronos.serializeProcessor
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.generator.SnowflakeIdGenerator
import com.kotlinorm.beans.generator.UUIDGenerator
import com.kotlinorm.beans.generator.customIdGenerator
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.cache.insertSqlCache
import com.kotlinorm.database.SqlManager.getInsertSql
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeInsertClause
import com.kotlinorm.types.ToReference
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.toLinkedSet

class InsertClause<T : KPojo>(val pojo: T) {
    private val paramMap = pojo.toDataMap()
    private val tableName = pojo.kronosTableName()
    private val createTimeStrategy = pojo.kronosCreateTime().bind(tableName)
    private val updateTimeStrategy = pojo.kronosUpdateTime().bind(tableName)
    private val logicDeleteStrategy = pojo.kronosLogicDelete().bind(tableName)
    private val optimisticStrategy = pojo.kronosOptimisticLock().bind(tableName)
    private val allFields = pojo.kronosColumns().toLinkedSet()
    private var cascadeEnabled = true

    /**
     * cascadeAllowed
     *
     * Fields that are allowed to use cascade, if not set, all fields are allowed to use cascade
     *
     * 允许级联的字段，若为空则允许所有字段级联
     */
    internal var cascadeAllowed: Set<Field>? = null

    fun cascade(enabled: Boolean): InsertClause<T> {
        cascadeEnabled = enabled
        return this
    }

    fun cascade(someFields: ToReference<T, Any?>): InsertClause<T> {
        if (someFields == null) throw NeedFieldsException()
        cascadeEnabled = true
        pojo.afterReference {
            someFields(it)
            if (fields.isEmpty()) throw NeedFieldsException()
            cascadeAllowed = fields.toSet()
        }
        return this
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
        var useIdentity = false
        val paramMapNew = mutableMapOf<String, Any?>()
        val fieldsMap = mutableMapOf<String, Field>()
        val toInsertFields = mutableListOf<Field>()
        allFields.forEach {
            when (it.primaryKey) {
                PrimaryKeyType.UUID -> paramMap[it.name] = UUIDGenerator.nextId()
                PrimaryKeyType.SNOWFLAKE -> paramMap[it.name] = SnowflakeIdGenerator.nextId()
                PrimaryKeyType.CUSTOM -> paramMap[it.name] = customIdGenerator?.nextId()
                PrimaryKeyType.IDENTITY -> useIdentity = true
                else -> {}
            }
            if (it.defaultValue != null && paramMap[it.name] == null) {
                paramMap[it.name] = it.defaultValue
            }
            fieldsMap[it.name] = it
            if (it.isColumn) {
                toInsertFields.add(it)
            }
        }
        arrayOf(
            createTimeStrategy to true,
            updateTimeStrategy to true,
            logicDeleteStrategy to false,
            optimisticStrategy to false
        ).forEach {
            setCommonStrategy(it.first, linkedSetOf(it.first.field), it.second) { field, value ->
                paramMap[field.name] = value
            }
        }
        paramMap.forEach { (key, value) ->
            val field = fieldsMap[key]
            if (field != null && value != null) {
                if (field.serializable) {
                    paramMapNew[key] = serializeProcessor.serialize(value)
                } else {
                    paramMapNew[key] = value
                }
            }
        }

        val kClass = pojo.kClass()
        val sql = insertSqlCache[kClass] ?: getInsertSql(
            wrapper.orDefault(),
            tableName,
            toInsertFields
        ).also {
            insertSqlCache[kClass] = it
        }

        return CascadeInsertClause.build(
            cascadeEnabled,
            cascadeAllowed,
            pojo,
            KronosAtomicActionTask(
                sql,
                paramMapNew,
                operationType = KOperationType.INSERT,
                useIdentity = useIdentity
            )
        )

    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build().execute(wrapper)
    }

    companion object {
        fun <T : KPojo> Iterable<InsertClause<T>>.cascade(
            enabled: Boolean
        ): Iterable<InsertClause<T>> {
            return this.onEach { it.cascade(enabled) }
        }

        fun <T : KPojo> Iterable<InsertClause<T>>.cascade(
            someFields: ToReference<T, Any?>
        ): Iterable<InsertClause<T>> {
            return this.onEach { it.cascade(someFields) }
        }

        /**
         * Builds a KronosActionTask for each InsertClause in the list.
         *
         * This function maps each InsertClause in the Iterable to a KronosActionTask by calling the build function of the InsertClause.
         * It then merges all the KronosActionTasks into a single KronosActionTask using the merge function and returns it.
         *
         * @return KronosActionTask returns a single KronosActionTask that represents the merged tasks for all the InsertClauses in the Iterable.
         */
        fun <T : KPojo> Iterable<InsertClause<T>>.build(): KronosActionTask {
            return this.map { it.build() }.merge()
        }

        /**
         * Executes the KronosActionTask built for each InsertClause in the Iterable.
         *
         * This function first builds a KronosActionTask for each InsertClause in the Iterable by calling the build function.
         * It then executes the built KronosActionTask and returns the result.
         *
         * @param wrapper KronosDataSourceWrapper? (optional) the data source wrapper to use for the execution. If not provided, the default data source wrapper is used.
         * @return KronosOperationResult returns the result of the execution of the KronosActionTask.
         */
        fun <T : KPojo> Iterable<InsertClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }

        fun <T : KPojo> Array<InsertClause<T>>.cascade(enabled: Boolean): Array<out InsertClause<T>> {
            return this.onEach { it.cascade(enabled) }
        }

        fun <T : KPojo> Array<InsertClause<T>>.cascade(someFields: ToReference<T, Any?>): Array<out InsertClause<T>> {
            return this.onEach { it.cascade(someFields) }
        }

        /**
         * Builds a KronosActionTask for each InsertClause in the Array.
         *
         * This function maps each InsertClause in the Iterable to a KronosActionTask by calling the build function of the InsertClause.
         * It then merges all the KronosActionTasks into a single KronosActionTask using the merge function and returns it.
         *
         * @return KronosActionTask returns a single KronosActionTask that represents the merged tasks for all the InsertClauses in the Iterable.
         */
        fun <T : KPojo> Array<InsertClause<T>>.build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
            return this.map { it.build(wrapper) }.merge()
        }


        /**
         * Executes the KronosActionTask built for each InsertClause in the array.
         *
         * This function first builds a KronosActionTask for each InsertClause in the Iterable by calling the build function.
         * It then executes the built KronosActionTask and returns the result.
         *
         * @param wrapper KronosDataSourceWrapper? (optional) the data source wrapper to use for the execution. If not provided, the default data source wrapper is used.
         * @return KronosOperationResult returns the result of the execution of the KronosActionTask.
         */
        fun <T : KPojo> Array<InsertClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }
    }
}