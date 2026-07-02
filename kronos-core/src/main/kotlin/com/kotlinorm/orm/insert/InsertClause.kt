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
import com.kotlinorm.ast.FieldToExpressionConverter
import com.kotlinorm.ast.InsertStatement
import com.kotlinorm.ast.Literal
import com.kotlinorm.ast.Parameter
import com.kotlinorm.ast.RenderContext
import com.kotlinorm.ast.SelectItem
import com.kotlinorm.ast.SelectStatement
import com.kotlinorm.ast.Statement
import com.kotlinorm.ast.SubqueryLowering
import com.kotlinorm.ast.TableName
import com.kotlinorm.ast.UnionStatement
import com.kotlinorm.ast.CriteriaToAstConverter
import com.kotlinorm.ast.QueryMaterializeContext
import com.kotlinorm.ast.toScalarSubqueryExpression
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForInsertSelect.Companion.afterInsertSelect
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.generator.SnowflakeIdGenerator
import com.kotlinorm.beans.generator.UUIDGenerator
import com.kotlinorm.beans.generator.customIdGenerator
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.cache.fieldsMapCache
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
import com.kotlinorm.orm.union.UnionClause
import com.kotlinorm.types.ToInsertSelect
import com.kotlinorm.types.ToReference
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.createInstance
import com.kotlinorm.utils.execute

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
    private var sourceQuery: KSelectable<*>? = null
    private var sourceUnion: UnionClause? = null
    private var sourceValueProvider: ((List<Field>) -> List<Any?>)? = null

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
        val sourceParameterValues = mutableMapOf<String, Any?>()
        val finalStatement = toStatement(wrapper, sourceParameterValues)
        val (sql, paramMapNew) = renderStatement(wrapper, finalStatement, sourceParameterValues)

        return CascadeInsertClause.build(
            cascadeEnabled,
            cascadeAllowed,
            pojo,
            KronosAtomicActionTask(
                sql,
                paramMapNew,
                operationType = KOperationType.INSERT,
                statement = finalStatement,
                stash = stash
            )
        )

    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build().execute(wrapper)
    }

    @PublishedApi
    internal fun <S : KPojo> fromSource(
        query: KSelectable<S>,
        values: ToInsertSelect<S, Any?> = null
    ): InsertClause<T> {
        sourceQuery = query
        sourceUnion = null
        sourceValueProvider = values?.let { insertValues ->
            {
                val source = query.selectedKClass.createInstance()
                source.afterInsertSelect { insertValues(it) }
            }
        }
        cascadeEnabled = false
        return this
    }

    @PublishedApi
    internal fun fromSource(
        query: UnionClause,
        values: ((List<Field>) -> List<Any?>)? = null
    ): InsertClause<T> {
        sourceQuery = null
        sourceUnion = query
        sourceValueProvider = values
        cascadeEnabled = false
        return this
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
    fun toStatement(
        wrapper: KronosDataSourceWrapper? = null,
        parameterValues: MutableMap<String, Any?> = mutableMapOf()
    ): InsertStatement {
        var useIdentity = false
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
        
        val sourceStatement: Statement? = when {
            sourceQuery != null -> sourceQuery?.toStatement(wrapper, parameterValues)
            sourceUnion != null -> sourceUnion?.toStatement(wrapper, parameterValues)
            else -> null
        }
        val values = if (sourceStatement == null) {
            toInsertFields.map { field ->
                Parameter.NamedParameter(field.name) as Expression
            }
        } else {
            sourceValueProvider?.invoke(toInsertFields)
                ?.also { provided ->
                    require(provided.size == toInsertFields.size) {
                        "Insert-select value count (${provided.size}) must match target insertable field count (${toInsertFields.size})."
                    }
                }
                ?.mapIndexed { index, value ->
                    value.toInsertSelectExpression(toInsertFields[index], parameterValues)
                }
                ?: emptyList()
        }
        val finalSourceStatement = sourceStatement?.let { statement ->
            if (values.isEmpty()) {
                validateDefaultInsertSelectArity(statement, toInsertFields.size)
                statement
            } else {
                rewriteInsertSelectProjection(statement, values)
            }
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
            conflictResolver = null,
            source = finalSourceStatement
        )
    }

    private fun validateDefaultInsertSelectArity(statement: Statement, targetFieldCount: Int) {
        val arities = queryOutputArities(statement)
        arities.forEach { sourceFieldCount ->
            require(sourceFieldCount == targetFieldCount) {
                "Insert-select source column count ($sourceFieldCount) must match target insertable field count ($targetFieldCount)."
            }
        }
    }

    private fun queryOutputArities(statement: Statement): List<Int> {
        return when (statement) {
            is SelectStatement -> listOf(statement.selectList.size)
            is UnionStatement -> statement.queries.map { it.selectList.size }
            else -> error("Insert-select source must be a SELECT or UNION statement, but was ${statement::class.simpleName}.")
        }
    }

    private fun rewriteInsertSelectProjection(statement: Statement, values: List<Expression>): Statement {
        return when (statement) {
            is SelectStatement -> {
                statement.selectList.clear()
                statement.selectList.addAll(values.map { SelectItem.ExpressionSelectItem(it, null) })
                statement
            }
            is UnionStatement -> {
                val rewrittenQueries = statement.queries.map { query ->
                    val copy = query.copyForProjectionRewrite()
                    copy.selectList.clear()
                    copy.selectList.addAll(values.map { SelectItem.ExpressionSelectItem(it, null) })
                    copy
                }
                statement.copy(queries = rewrittenQueries)
            }
            else -> error("Insert-select source must be a SELECT or UNION statement, but was ${statement::class.simpleName}.")
        }
    }

    private fun SelectStatement.copyForProjectionRewrite(): SelectStatement {
        return SelectStatement(
            selectList = selectList.toMutableList(),
            from = from,
            where = where,
            groupBy = groupBy?.toMutableList(),
            having = having,
            orderBy = orderBy?.toMutableList(),
            limit = limit,
            distinct = distinct,
            lock = lock
        )
    }

    private fun Any?.toInsertSelectExpression(
        targetField: Field,
        parameterValues: MutableMap<String, Any?>
    ): Expression {
        return when (this) {
            null -> Literal.NullLiteral
            is Expression -> this
            is Field -> FieldToExpressionConverter.fieldToExpression(this)
            is com.kotlinorm.ast.SelectQueryRef -> toScalarSubqueryExpression()
            is KSelectable<*> -> toScalarSubqueryExpression()
            else -> {
                val paramName = CriteriaToAstConverter.getUniqueParamName(
                    targetField.name,
                    parameterValues,
                    mutableMapOf()
                )
                parameterValues[paramName] = this
                Parameter.NamedParameter(paramName)
            }
        }
    }

    /**
     * Renders the InsertStatement to SQL with processed parameters.
     * This method handles parameter processing including field type conversion.
     *
     * @param wrapper Optional KronosDataSourceWrapper for rendering and parameter processing
     * @return Pair of SQL string and processed parameter map
     */
    private fun renderStatement(
        wrapper: KronosDataSourceWrapper?,
        finalStatement: InsertStatement = toStatement(wrapper),
        sourceParameterValues: MutableMap<String, Any?> = mutableMapOf()
    ): Pair<String, Map<String, Any?>> {
        val dataSource = wrapper.orDefault()
        val support = getDBSupport(dataSource.dbType) ?: throw UnsupportedDatabaseTypeException(dataSource.dbType)

        val loweredStatement = SubqueryLowering.lower(
            finalStatement,
            QueryMaterializeContext(wrapper = dataSource, parameterValues = sourceParameterValues)
        ) as InsertStatement
        val renderContext = RenderContext(quotes = support.quotes, dbType = dataSource.dbType)
        renderContext.boundParameters.putAll(sourceParameterValues)
        val renderedSql = support.renderer.render(loweredStatement, renderContext)

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

        // Include parameters materialized by the source SELECT in INSERT ... SELECT.
        sourceParameterValues.forEach { (key, value) ->
            if (!paramMapNew.containsKey(key) && renderedSql.sql.contains(":$key")) {
                val field = fieldsMap[key]
                if (field != null && value != null) {
                    paramMapNew[key] = support.processParams(dataSource, field, value)
                } else {
                    paramMapNew[key] = value
                }
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
