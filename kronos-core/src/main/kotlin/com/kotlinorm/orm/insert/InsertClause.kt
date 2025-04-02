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
    private var paramMap = pojo.toDataMap()
    private var tableName = pojo.kronosTableName()
    private var createTimeStrategy = pojo.kronosCreateTime().bind(tableName)
    private var updateTimeStrategy = pojo.kronosUpdateTime().bind(tableName)
    private var logicDeleteStrategy = pojo.kronosLogicDelete().bind(tableName)
    private var optimisticStrategy = pojo.kronosOptimisticLock().bind(tableName)
    private var allFields = pojo.kronosColumns().toLinkedSet()
    private val toInsertFields = linkedSetOf<Field>()
    private var cascadeEnabled = true

    /**
     * cascadeAllowed
     *
     * Fields that are allowed to use cascade, if not set, all fields are allowed to use cascade
     *
     * 允许级联的字段，若为空则允许所有字段级联
     */
    internal var cascadeAllowed: Set<Field>? = null

    private val updateInsertFields = { field: Field, value: Any? ->
        if (field.isColumn && value != null) {
            toInsertFields += field
            paramMap[field.name] = value
        }
    }

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
        val pk = pojo.kronosColumns()
            .find { it.primaryKey !in setOf(PrimaryKeyType.NOT, PrimaryKeyType.DEFAULT, PrimaryKeyType.IDENTITY) }
        if (pk != null && paramMap[pk.name] == null) {
            paramMap[pk.name] = when (pk.primaryKey) {
                PrimaryKeyType.UUID -> UUIDGenerator.nextId()
                PrimaryKeyType.SNOWFLAKE -> SnowflakeIdGenerator.nextId()
                PrimaryKeyType.CUSTOM -> customIdGenerator?.nextId()
                else -> throw IllegalArgumentException("Primary key type not supported")
            }
        }
        toInsertFields.addAll(allFields.filter { it.isColumn && paramMap[it.name] != null })

        setCommonStrategy(createTimeStrategy, allFields, true, callBack = updateInsertFields)
        setCommonStrategy(updateTimeStrategy, allFields, true, callBack = updateInsertFields)
        setCommonStrategy(logicDeleteStrategy, allFields, false, callBack = updateInsertFields)
        setCommonStrategy(optimisticStrategy, allFields, false, callBack = updateInsertFields)

        val sql = getInsertSql(wrapper.orDefault(), tableName, toInsertFields.toList())
        val paramMapNew = mutableMapOf<String, Any?>()
        paramMap.forEach { (key, value) ->
            val field = toInsertFields.find { it.name == key }
            if (field != null && value != null) {
                if (field.serializable) {
                    paramMapNew[key] = serializeProcessor.serialize(value)
                } else {
                    paramMapNew[key] = value
                }
            }
        }

        return CascadeInsertClause.build(
            cascadeEnabled,
            cascadeAllowed,
            pojo,
            KronosAtomicActionTask(
                sql,
                paramMapNew,
                operationType = KOperationType.INSERT,
                useIdentity = (allFields - toInsertFields).any { it.primaryKey == PrimaryKeyType.IDENTITY }
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