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
package com.kotlinorm.orm.insert

import com.kotlinorm.ast.InsertStatement
import com.kotlinorm.ast.NamedParam
import com.kotlinorm.ast.ValuesSource
import com.kotlinorm.ast.table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.generator.SnowflakeIdGenerator
import com.kotlinorm.beans.generator.UUIDGenerator
import com.kotlinorm.beans.generator.customIdGenerator
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.cache.insertSqlCache
import com.kotlinorm.cache.kPojoAllColumnsCache
import com.kotlinorm.cache.kPojoCreateTimeCache
import com.kotlinorm.cache.kPojoLogicDeleteCache
import com.kotlinorm.cache.kPojoOptimisticLockCache
import com.kotlinorm.cache.kPojoPrimaryKeyCache
import com.kotlinorm.cache.kPojoUpdateTimeCache
import com.kotlinorm.database.RegisteredDBTypeManager.getDBSupport
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeInsertClause
import com.kotlinorm.types.ToReference
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.processParams

class InsertClause<T : KPojo>(val pojo: T) {
    // 直接存储AST结构
    private var insertStatement: InsertStatement? = null
    private val paramMap = pojo.toDataMap()
    private val tableName = pojo.kronosTableName()
    private var kClass = pojo.kClass()
    private var createTimeStrategy = kPojoCreateTimeCache[kClass]
    private var updateTimeStrategy = kPojoUpdateTimeCache[kClass]
    private var logicDeleteStrategy = kPojoLogicDeleteCache[kClass]
    private var optimisticStrategy = kPojoOptimisticLockCache[kClass]
    internal var allColumns = kPojoAllColumnsCache[kClass]!!
    private var cascadeEnabled = true
    var stash = mutableMapOf<String, Any?>()

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
        someFields ?: throw EmptyFieldsException()
        cascadeEnabled = true
        pojo.afterReference {
            someFields(it)
            if (fields.isEmpty()) throw EmptyFieldsException()
            cascadeAllowed = fields.toSet()
        }
        return this
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
        var useIdentity = false
        val paramMapNew = mutableMapOf<String, Any?>()
        val fieldsMap = fieldsMapCache[kClass]!!
        val toInsertFields = mutableListOf<Field>()
        val primaryKeyField = kPojoPrimaryKeyCache[kClass]!!
        when (primaryKeyField.primaryKey) {
            PrimaryKeyType.UUID -> paramMap[primaryKeyField.name] = UUIDGenerator.nextId()
            PrimaryKeyType.SNOWFLAKE ->
                    paramMap[primaryKeyField.name] = SnowflakeIdGenerator.nextId()
            PrimaryKeyType.CUSTOM -> paramMap[primaryKeyField.name] = customIdGenerator?.nextId()
            PrimaryKeyType.IDENTITY -> useIdentity = true
            else -> {}
        }
        if (paramMap[primaryKeyField.name] != null || primaryKeyField.defaultValue != null) {
            useIdentity = false
        }
        stash["useIdentity"] = useIdentity
        allColumns.forEach {
            if (it.defaultValue != null && paramMap[it.name] == null) {
                paramMap[it.name] = it.defaultValue
            }
            if (it.isColumn &&
                            !(it.primaryKey == PrimaryKeyType.IDENTITY && paramMap[it.name] == null)
            ) {
                toInsertFields.add(it)
            }
        }
        if (useIdentity && !paramMap.containsKey(primaryKeyField.name)) {
            toInsertFields.remove(primaryKeyField)
        }
        arrayOf(
                        createTimeStrategy to true,
                        updateTimeStrategy to true,
                        logicDeleteStrategy to false,
                        optimisticStrategy to false
                )
                .forEach {
                    it.first?.execute(it.second) { field, value -> paramMap[field.name] = value }
                }
        paramMap.forEach { (key, value) ->
            val field = fieldsMap[key]
            if (field != null && value != null) {
                paramMapNew[key] = processParams(wrapper.orDefault(), field, value)
            } else {
                paramMapNew[key] = value
            }
        }

        // 构建AST结构
        insertStatement =
                InsertStatement(
                        target = table(tableName),
                        columns = toInsertFields.map { it.columnName }.toMutableList(),
                        source = ValuesSource(listOf(toInsertFields.map { NamedParam(it.name) }))
                )

        // 通过DatabaseSupport渲染SQL
        val support =
                getDBSupport(wrapper.orDefault().dbType)
                        ?: throw UnsupportedDatabaseTypeException(wrapper.orDefault().dbType)
        val rendered = support.getInsertSqlWithParams(wrapper.orDefault(), insertStatement!!)
        val sql =
                insertSqlCache[
                        kClass to useIdentity,
                        {
                            insertSqlCache[kClass to useIdentity] = rendered.sql
                            rendered.sql
                        }]

        return CascadeInsertClause.build(
                cascadeEnabled,
                cascadeAllowed,
                pojo,
                KronosAtomicActionTask(
                        sql,
                        paramMapNew,
                        operationType = KOperationType.INSERT,
                        stash = stash
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
         * This function maps each InsertClause in the Iterable to a KronosActionTask by calling the
         * build function of the InsertClause. It then merges all the KronosActionTasks into a
         * single KronosActionTask using the merge function and returns it.
         *
         * @return KronosActionTask returns a single KronosActionTask that represents the merged
         * tasks for all the InsertClauses in the Iterable.
         */
        fun <T : KPojo> Iterable<InsertClause<T>>.build(): KronosActionTask {
            return this.map { it.build() }.merge()
        }

        /**
         * Executes the KronosActionTask built for each InsertClause in the Iterable.
         *
         * This function first builds a KronosActionTask for each InsertClause in the Iterable by
         * calling the build function. It then executes the built KronosActionTask and returns the
         * result.
         *
         * @param wrapper KronosDataSourceWrapper? (optional) the data source wrapper to use for the
         * execution. If not provided, the default data source wrapper is used.
         * @return KronosOperationResult returns the result of the execution of the
         * KronosActionTask.
         */
        fun <T : KPojo> Iterable<InsertClause<T>>.execute(
                wrapper: KronosDataSourceWrapper? = null
        ): KronosOperationResult {
            return build().execute(wrapper)
        }

        fun <T : KPojo> Array<InsertClause<T>>.cascade(
                enabled: Boolean
        ): Array<out InsertClause<T>> {
            return this.onEach { it.cascade(enabled) }
        }

        fun <T : KPojo> Array<InsertClause<T>>.cascade(
                someFields: ToReference<T, Any?>
        ): Array<out InsertClause<T>> {
            return this.onEach { it.cascade(someFields) }
        }

        /**
         * Builds a KronosActionTask for each InsertClause in the Array.
         *
         * This function maps each InsertClause in the Iterable to a KronosActionTask by calling the
         * build function of the InsertClause. It then merges all the KronosActionTasks into a
         * single KronosActionTask using the merge function and returns it.
         *
         * @return KronosActionTask returns a single KronosActionTask that represents the merged
         * tasks for all the InsertClauses in the Iterable.
         */
        fun <T : KPojo> Array<InsertClause<T>>.build(
                wrapper: KronosDataSourceWrapper? = null
        ): KronosActionTask {
            return this.map { it.build(wrapper) }.merge()
        }

        /**
         * Executes the KronosActionTask built for each InsertClause in the array.
         *
         * This function first builds a KronosActionTask for each InsertClause in the Iterable by
         * calling the build function. It then executes the built KronosActionTask and returns the
         * result.
         *
         * @param wrapper KronosDataSourceWrapper? (optional) the data source wrapper to use for the
         * execution. If not provided, the default data source wrapper is used.
         * @return KronosOperationResult returns the result of the execution of the
         * KronosActionTask.
         */
        fun <T : KPojo> Array<InsertClause<T>>.execute(
                wrapper: KronosDataSourceWrapper? = null
        ): KronosOperationResult {
            return build().execute(wrapper)
        }
    }
}
