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

package com.kotlinorm.orm.insert

import com.kotlinorm.ast.ColumnReference
import com.kotlinorm.ast.Expression
import com.kotlinorm.ast.InsertStatement
import com.kotlinorm.ast.Parameter
import com.kotlinorm.ast.TableName
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
import com.kotlinorm.utils.processParams

class InsertClause<T : KPojo>(val pojo: T) {
    private val paramMap = pojo.toDataMap()
    private val tableName = pojo.__tableName
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
        val fieldsMap = fieldsMapCache[kClass]!!
        val toInsertFields = mutableListOf<Field>()
        val primaryKeyField = kPojoPrimaryKeyCache[kClass]!!
        when (primaryKeyField.primaryKey) {
            PrimaryKeyType.UUID -> paramMap[primaryKeyField.name] = UUIDGenerator.nextId()
            PrimaryKeyType.SNOWFLAKE -> paramMap[primaryKeyField.name] = SnowflakeIdGenerator.nextId()
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
            if (it.isColumn && !(it.primaryKey == PrimaryKeyType.IDENTITY && paramMap[it.name] == null)) {
                toInsertFields.add(it)
            }
        }
        if(useIdentity && !paramMap.containsKey(primaryKeyField.name)){
            toInsertFields.remove(primaryKeyField)
        }
        arrayOf(
            createTimeStrategy to true,
            updateTimeStrategy to true,
            logicDeleteStrategy to false,
            optimisticStrategy to false
        ).forEach {
            it.first?.execute(it.second) { field, value ->
                paramMap[field.name] = value
            }
        }

        // Use new AST-based rendering
        val (sql, paramMapNew) = renderStatement(wrapper)

        return CascadeInsertClause.build(
            cascadeEnabled,
            cascadeAllowed,
            pojo,
            KronosAtomicActionTask(
                sql,
                paramMapNew,
                operationType = KOperationType.INSERT,
                actionInfo = InsertClauseInfo(
                    kClass,
                    tableName
                ),
                stash = stash
            )
        )

    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build().execute(wrapper)
    }

    /**
     * Converts the InsertClause to an InsertStatement AST.
     * This method constructs the complete AST representation including:
     * - Table reference
     * - Column list
     * - Value expressions
     * - Conflict resolver (if configured)
     *
     * @param wrapper Optional KronosDataSourceWrapper for processing
     * @return Complete InsertStatement AST
     */
    fun toStatement(wrapper: KronosDataSourceWrapper? = null): InsertStatement {
        val dataSource = wrapper.orDefault()
        var useIdentity = false
        val fieldsMap = fieldsMapCache[kClass]!!
        val toInsertFields = mutableListOf<Field>()
        val primaryKeyField = kPojoPrimaryKeyCache[kClass]!!
        
        // Handle primary key generation
        when (primaryKeyField.primaryKey) {
            PrimaryKeyType.UUID -> paramMap[primaryKeyField.name] = UUIDGenerator.nextId()
            PrimaryKeyType.SNOWFLAKE -> paramMap[primaryKeyField.name] = SnowflakeIdGenerator.nextId()
            PrimaryKeyType.CUSTOM -> paramMap[primaryKeyField.name] = customIdGenerator?.nextId()
            PrimaryKeyType.IDENTITY -> useIdentity = true
            else -> {}
        }
        if (paramMap[primaryKeyField.name] != null || primaryKeyField.defaultValue != null) {
            useIdentity = false
        }
        stash["useIdentity"] = useIdentity
        
        // Collect fields to insert
        allColumns.forEach {
            if (it.defaultValue != null && paramMap[it.name] == null) {
                paramMap[it.name] = it.defaultValue
            }
            if (it.isColumn && !(it.primaryKey == PrimaryKeyType.IDENTITY && paramMap[it.name] == null)) {
                toInsertFields.add(it)
            }
        }
        if (useIdentity && !paramMap.containsKey(primaryKeyField.name)) {
            toInsertFields.remove(primaryKeyField)
        }
        
        // Apply strategies
        arrayOf(
            createTimeStrategy to true,
            updateTimeStrategy to true,
            logicDeleteStrategy to false,
            optimisticStrategy to false
        ).forEach {
            it.first?.execute(it.second) { field, value ->
                paramMap[field.name] = value
            }
        }
        
        // Build column references
        val columns = toInsertFields.map { field ->
            ColumnReference(database = null, tableAlias = null, columnName = field.columnName)
        }
        
        // Build value expressions (parameters)
        val values = toInsertFields.map { field ->
            Parameter.NamedParameter(field.name) as Expression
        }
        
        // Build table reference
        val table = TableName(
            database = null,
            schema = null,
            table = tableName,
            alias = null
        )
        
        // Note: ConflictResolver is not currently supported in InsertClause
        // It's only available in UpsertClause. If needed in the future, 
        // add a conflictResolver field and onConflict() method similar to UpsertClause
        
        return InsertStatement(
            table = table,
            columns = columns,
            values = values,
            conflictResolver = null
        )
    }

    /**
     * Renders the InsertStatement to SQL with processed parameters.
     * This method handles parameter processing including field type conversion.
     *
     * @param wrapper Optional KronosDataSourceWrapper for rendering and parameter processing
     * @return Pair of SQL string and processed parameter map
     */
    private fun renderStatement(wrapper: KronosDataSourceWrapper?): Pair<String, Map<String, Any?>> {
        val dataSource = wrapper.orDefault()
        val support = getDBSupport(dataSource.dbType) ?: throw UnsupportedDatabaseTypeException(dataSource.dbType)

        // Get complete statement with all parameters and checks applied
        val finalStatement = toStatement(wrapper)

        // Render AST to SQL with parameters
        val renderedSql = support.getInsertSqlWithParams(dataSource, finalStatement)

        // Process parameters (field type conversion, etc.)
        val paramMapNew = mutableMapOf<String, Any?>()
        val fieldsMap = fieldsMapCache[kClass]!!
        
        // First, add parameters from renderedSql (if any)
        renderedSql.parameters.forEach { (key, value) ->
            val field = fieldsMap[key]
            if (field != null && value != null) {
                paramMapNew[key] = support.processParams(dataSource, field, value)
            } else {
                paramMapNew[key] = value
            }
        }
        
        // Then, add parameters from paramMap (INSERT values)
        // Check which parameters are actually used in the SQL
        paramMap.forEach { (key, value) ->
            if (!paramMapNew.containsKey(key) && renderedSql.sql.contains(":$key")) {
                val field = fieldsMap[key]
                if (field != null) {
                    paramMapNew[key] = support.processParams(dataSource, field, value)
                } else {
                    paramMapNew[key] = value
                }
            }
        }

        return Pair(renderedSql.sql, paramMapNew)
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