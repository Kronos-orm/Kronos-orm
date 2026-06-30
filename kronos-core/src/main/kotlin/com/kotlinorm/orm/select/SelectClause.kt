/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm.orm.select

import com.kotlinorm.ast.BinaryExpression
import com.kotlinorm.ast.ColumnReference
import com.kotlinorm.ast.CriteriaToAstConverter
import com.kotlinorm.ast.FieldToExpressionConverter
import com.kotlinorm.ast.LimitClause
import com.kotlinorm.ast.Literal
import com.kotlinorm.ast.OrderByItem
import com.kotlinorm.ast.SelectItem
import com.kotlinorm.ast.SelectItemAliasMetadata
import com.kotlinorm.ast.SelectItemSourceScope
import com.kotlinorm.ast.SelectStatement
import com.kotlinorm.ast.SqlOperator
import com.kotlinorm.ast.QueryMaterializeContext
import com.kotlinorm.ast.SubqueryLowering
import com.kotlinorm.ast.TableName
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.cache.kPojoAllColumnsCache
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.cache.kPojoLogicDeleteCache
import com.kotlinorm.database.SqlManager.quoted
import com.kotlinorm.enums.KColumnType.CUSTOM_CRITERIA_SQL
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.enums.QueryType.QueryList
import com.kotlinorm.enums.QueryType.QueryOne
import com.kotlinorm.enums.QueryType.QueryOneOrNull
import com.kotlinorm.enums.SortType
import com.kotlinorm.database.RegisteredDBTypeManager.getDBSupport
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.functions.FunctionManager
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeSelectClause
import com.kotlinorm.orm.pagination.PagedClause
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSort
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.LinkedHashSet
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.getDefaultBoolean
import com.kotlinorm.utils.logAndReturn
import com.kotlinorm.utils.processParams
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KClass

class SelectClause<Source : KPojo, Selected : KPojo, Context : KPojo>(
    override val pojo: Source,
    setSelectFields: ToSelect<Source, Any?> = null,
    private val projectionClass: KClass<Selected>,
    internal val contextPojo: Context = pojo as Context
) : KSelectable<Selected>(pojo, projectionClass) {
    private var kClass = pojo.kClass()
    private var tableName = pojo.__tableName
    internal var paramMap = pojo.toDataMap()
    private val patchParamMap = mutableMapOf<String, Any?>()
    internal var logicDeleteStrategy = kPojoLogicDeleteCache[kClass]
    private var allFields = kPojoAllFieldsCache[kClass]!!
    private var allColumns = kPojoAllColumnsCache[kClass]!!

    /**
     * 级联查询允许的字段，若为空则表示所有字段均可级联查询，优先级高于[com.kotlinorm.annotations.Ignore[com.kotlinorm.enums.IgnoreAction.CASCADE_SELECT]]
     * */
    internal var cascadeAllowed: Set<Field>? = null
    internal var cascadeSelectedProps: Set<Field>? = null
    private var cascadeEnabled = true
    internal var operationType = KOperationType.SELECT // 级联操作类型，默认为SELECT

    // Cascade fields extracted from select { } lambda (non-column fields like KPojo/Collection<KPojo>)
    private var selectCascadeFields: LinkedHashSet<Field> = []

    // AST SelectStatement - all data stored here, directly modified without copy()
    internal var statement: SelectStatement = SelectStatement(
        selectList = mutableListOf(),
        from = TableName(table = tableName, database = null)
    )
    
    // Store parameter values extracted from Criteria (WHERE/BY/HAVING clauses)
    // Key: Parameter name, Value: parameter value
    internal val criteriaParams = mutableMapOf<String, Any?>()
    
    // Track whether logic delete condition has been applied to avoid duplication
    // when toStatement() is called multiple times (e.g., in PagedClause)
    private var logicDeleteApplied = false
    
    // Public API compatibility properties - computed from statement
    override var selectFields: LinkedHashSet<Field>
        get() {
            return statement.selectList.mapNotNull { item ->
                when (item) {
                    is SelectItem.ColumnSelectItem -> {
                        val column = item.column
                        allColumns.find {
                            it.columnName == column.columnName && it.tableName == (column.tableAlias ?: "")
                        }
                    }

                    else -> null
                }
            }.toLinkedSet()
        }
        set(value) {
            statement.selectList.clear()
            statement.selectList.addAll(fieldsToSelectItems(value))
            statement.distinct = false // Reset distinct when fields change
        }

    override var selectAll: Boolean = true

    override var limitCapacity: Int
        get() = statement.limit?.limit ?: 0
        set(value) {
            statement.limit = if (value > 0) LimitClause(limit = value, offset = statement.limit?.offset) else null
        }

    override var pageEnabled: Boolean = false

    /**
     * 初始化函数：用于在对象初始化时配置选择字段。
     * 该函数不接受参数，也不返回任何值。
     * 它首先检查setSelectFields是否为非空，如果是，则调用pojo.tableRun块，
     * 在该块内调用setSelectFields方法来设置选择的字段，并将当前字段集合转换为链接集合后赋值给selectFields属性。
     */
    init {
        if (setSelectFields != null) {
            pojo.afterSelect {
                setSelectFields(it) // 设置选择的字段
                if (fields.isEmpty() && selectItems.isEmpty()) {
                    throw EmptyFieldsException()
                }
                val fieldsSet = fields.toLinkedSet()
                if (fieldsSet.isNotEmpty() || selectItems.isNotEmpty()) {
                    // Split into DB columns and cascade fields
                    val columnFields = fieldsSet.filter { f -> f.isColumn }.toLinkedSet()
                    selectCascadeFields = fieldsSet.filter { f -> !f.isColumn }.toLinkedSet()
                    if (columnFields.isNotEmpty() || selectItems.isNotEmpty()) {
                        selectAll = false
                        statement.selectList.clear()
                        statement.selectList.addAll(projectionItemsToSelectItems(projectionItems, columnFields.toSet()))
                    }
                    // If only cascade fields, selectAll stays true (select all DB columns)
                }
            }
        }
    }

    /**
     * Converts a Field to a ColumnReference.
     * In single-table operations, we should not use table prefix.
     */
    private fun fieldToColumnReference(field: Field): ColumnReference {
        return FieldToExpressionConverter.fieldToColumnReference(field, useTableAlias = false)
    }

    /**
     * Converts a list of Fields to SelectItems.
     * Handles CUSTOM_CRITERIA_SQL fields as literal SQL expressions.
     * Handles FunctionField as function calls with aliases.
     * Adds aliases when field name differs from column name.
     */
    private fun fieldsToSelectItems(fields: Collection<Field>): List<SelectItem> {
        return fields.map { field ->
            if (field is com.kotlinorm.beans.dsl.FunctionField) {
                // For FunctionField, convert to function call expression with alias
                val expression = FieldToExpressionConverter.fieldToExpression(field, useTableAlias = false)
                val alias = functionAlias(field)
                SelectItem.ExpressionSelectItem(
                    expression = expression,
                    alias = alias,
                    metadata = SelectItemAliasMetadata(
                        outputName = alias,
                        expression = expression,
                        scope = FunctionManager.getSelectItemScope(field.functionName),
                        sourceField = field,
                        userReferenceable = true
                    )
                )
            } else if (field.type == CUSTOM_CRITERIA_SQL) {
                // For CUSTOM_CRITERIA_SQL fields, convert to expression
                val expression = FieldToExpressionConverter.fieldToExpression(field, useTableAlias = false)
                SelectItem.ExpressionSelectItem(
                    expression = expression,
                    alias = null,
                    metadata = SelectItemAliasMetadata(
                        outputName = "__kronos_expr_${field.columnName.hashCode().toUInt()}",
                        expression = expression,
                        scope = SelectItemSourceScope.UNKNOWN,
                        sourceField = field,
                        userReferenceable = false
                    )
                )
            } else {
                // Add alias if field name differs from column name
                val alias = if (field.name != field.columnName) field.name else null
                val column = fieldToColumnReference(field)
                SelectItem.ColumnSelectItem(
                    column = column,
                    alias = alias,
                    metadata = SelectItemAliasMetadata(
                        outputName = alias ?: field.columnName,
                        expression = column,
                        scope = if (alias == null) SelectItemSourceScope.SOURCE else SelectItemSourceScope.SELECTED,
                        sourceField = field,
                        userReferenceable = true
                    )
                )
            }
        }
    }

    private fun projectionItemsToSelectItems(
        projections: Collection<KTableForSelect.ProjectionItem>,
        columnFields: Set<Field>
    ): List<SelectItem> {
        if (projections.isEmpty()) {
            return fieldsToSelectItems(columnFields)
        }

        return projections.mapNotNull { projection ->
            when (projection) {
                is KTableForSelect.ProjectionItem.FieldItem ->
                    projection.field.takeIf { it in columnFields }
                        ?.let { fieldsToSelectItems(listOf(it)).single() }
                is KTableForSelect.ProjectionItem.SelectItemValue -> projection.item
            }
        }
    }

    private fun functionAlias(field: com.kotlinorm.beans.dsl.FunctionField): String {
        return field.name.takeIf { it != field.functionName } ?: field.functionName.lowercase()
    }

    /**
     * Returns the AST SelectStatement with all parameters and checks applied.
     * This method ensures the statement is complete and ready for rendering.
     *
     * @param wrapper Optional KronosDataSourceWrapper for logic delete strategy and other database-specific logic
     * @return The complete SelectStatement ready for SQL rendering
     */
    override fun toStatement(wrapper: KronosDataSourceWrapper?): SelectStatement {
        return toStatement(wrapper, mutableMapOf())
    }
    
    override fun toStatement(wrapper: KronosDataSourceWrapper?, parameterValues: MutableMap<String, Any?>): SelectStatement {
        val dataSource = wrapper.orDefault()

        // Copy criteriaParams (from where/by/having methods) to parameterValues
        parameterValues.putAll(criteriaParams)

        // Update statement with selectAll if needed
        if (selectAll) {
            val currentFields = selectFields
            val allFieldsSet = (currentFields + allColumns).toLinkedSet()
            statement.selectList.clear()
            statement.selectList.addAll(fieldsToSelectItems(allFieldsSet))
        } else if (statement.selectList.isEmpty()) {
            val currentFields = selectFields
            if (currentFields.isNotEmpty()) {
                statement.selectList.addAll(fieldsToSelectItems(currentFields))
            }
        }

        // Apply logic delete strategy - only apply once to avoid duplication
        // when toStatement() is called multiple times (e.g., in PagedClause)
        if (!logicDeleteApplied) {
            logicDeleteStrategy?.execute(defaultValue = getDefaultBoolean(dataSource, false)) { field, value ->
                // Build logic delete condition with literal value (not parameter)
                // Logic delete values are fixed (0 or 1), so we use literals
                val logicDeleteExpression = BinaryExpression(
                    ColumnReference(database = null, tableAlias = null, columnName = field.columnName),
                    SqlOperator.EQUAL,
                    Literal.NumberLiteral(value.toString())
                )

                if (statement.where != null) {
                    // Merge with existing where condition
                    statement.where = BinaryExpression(
                        statement.where!!,
                        SqlOperator.AND,
                        logicDeleteExpression
                    )
                } else {
                    // Only logic delete condition
                    statement.where = logicDeleteExpression
                }
            }
            logicDeleteApplied = true
        }

        return statement
    }

    /**
     * Renders the SelectStatement to SQL with processed parameters.
     * This method handles parameter processing including field type conversion.
     *
     * @param wrapper Optional KronosDataSourceWrapper for rendering and parameter processing
     * @return Pair of SQL string and processed parameter map
     */
    private fun renderStatement(wrapper: KronosDataSourceWrapper?): Pair<String, Map<String, Any?>> {
        val dataSource = wrapper.orDefault()
        val support = getDBSupport(dataSource.dbType) ?: throw UnsupportedDatabaseTypeException(dataSource.dbType)

        // Collect parameter values from Criteria during AST conversion in toStatement()
        val criteriaParameterValues = mutableMapOf<String, Any?>()
        
        // Get complete statement with all parameters and checks applied
        val finalStatement = toStatement(wrapper, criteriaParameterValues)

        // Lower deferred subqueries here so nested selectable parameters are collected into
        // the same parameter map used by the outer query build.
        val loweredStatement = SubqueryLowering.lower(
            finalStatement,
            QueryMaterializeContext(wrapper = wrapper, parameterValues = criteriaParameterValues)
        )

        // Render AST to SQL with parameters
        val renderedSql = support.getSelectSqlWithParams(dataSource, loweredStatement)

        // Build final parameter map from:
        // 1. Parameters extracted from Criteria in where()/by()/having() methods
        // 2. Parameters extracted from Criteria during toStatement()
        // 3. Parameters from AST rendering (renderedSql.parameters)
        // 4. Patch parameters added by patch()
        val paramMapNew = mutableMapOf<String, Any?>()
        val fieldMap = fieldsMapCache[kClass]!!
        
        // First, add parameter values extracted from Criteria in where()/by()/having() methods
        // These parameters are guaranteed to be used in the WHERE/HAVING clauses
        criteriaParams.forEach { (key, value) ->
            val field = fieldMap[key]
            if (field != null) {
                paramMapNew[key] = support.processParams(dataSource, field, value)
            } else {
                paramMapNew[key] = value
            }
        }
        
        // Second, add parameter values extracted from Criteria during toStatement()
        criteriaParameterValues.forEach { (key, value) ->
            if (!paramMapNew.containsKey(key)) {
                val field = fieldMap[key]
                if (field != null) {
                    paramMapNew[key] = support.processParams(dataSource, field, value)
                } else {
                    paramMapNew[key] = value
                }
            }
        }
        
        // Third, add any additional parameters from the rendered SQL
        renderedSql.parameters.forEach { (key, value) ->
            if (!paramMapNew.containsKey(key) && value != null) {
                val field = fieldMap[key]
                if (field != null) {
                    paramMapNew[key] = support.processParams(dataSource, field, value)
                } else {
                    paramMapNew[key] = value
                }
            }
        }

        return Pair(renderedSql.sql, paramMapNew)
    }

    fun single(): SelectClause<Source, Selected, Context> {
        statement.limit = LimitClause(limit = 1, offset = null)
        return this
    }

    fun limit(capacity: Int): SelectClause<Source, Selected, Context> {
        statement.limit = if (capacity > 0) LimitClause(limit = capacity, offset = statement.limit?.offset) else null
        return this
    }

    fun db(databaseName: String): SelectClause<Source, Selected, Context> {
        if (databaseName.isNotBlank()) {
            // Update table name in statement
            val currentFrom = statement.from as? TableName
            if (currentFrom != null) {
                statement.from = currentFrom.copy(database = databaseName)
            }
        }
        return this
    }

    /**
     * 根据指定的字段对当前对象进行排序。
     *
     * @param someFields 可排序字段的集合，这里的字段类型为 [ToSort]，单位为 [Unit]。
     *                   该参数指定了排序时所依据的字段。
     * @return 返回 [SelectClause] 对象，允许链式调用。
     */
    fun orderBy(someFields: ToSort<Context, Any?>): SelectClause<Source, Selected, Context> {
        if (someFields == null) throw EmptyFieldsException()

        contextPojo.afterSort {
            someFields(it)// 在这里对排序操作进行封装，为后续的链式调用提供支持。
            // Update statement with order by items directly
            if (statement.orderBy == null) {
                statement.orderBy = mutableListOf()
            }
            statement.orderBy!!.clear()
            val orderItems = if (sortedItems.isNotEmpty()) {
                sortedItems.map { item ->
                    when (item) {
                        is KTableForSort.SortItem.FieldItem -> OrderByItem(
                            expression = fieldToColumnReference(item.field),
                            direction = item.sortType
                        )
                        is KTableForSort.SortItem.ExpressionItem -> OrderByItem(
                            expression = item.expression,
                            direction = item.sortType
                        )
                    }
                }
            } else {
                sortedFields.map { (field, sortType) ->
                    OrderByItem(
                        expression = fieldToColumnReference(field),
                        direction = sortType
                    )
                }
            }
            statement.orderBy!!.addAll(orderItems)
        }
        return this // 返回当前对象，允许继续进行其他查询操作。
    }


    /**
     * 根据指定的字段对数据进行分组。
     *
     * @param someFields 要用于分组的字段，类型为 KTableField<T, Unit>。该字段不能为空。
     * @return 返回 SelectClause<T> 实例，允许链式调用。
     * @throws EmptyFieldsException 如果 someFields 为空，则抛出此异常。
     */
    fun groupBy(someFields: ToSelect<Source, Any?>): SelectClause<Source, Selected, Context> {
        // 检查 someFields 参数是否为空，如果为空则抛出异常
        someFields ?: throw EmptyFieldsException()
        pojo.afterSelect {
            someFields(it)
            if (fields.isEmpty()) {
                throw EmptyFieldsException()
            }
            // Update statement with group by expressions directly
            if (statement.groupBy == null) {
                statement.groupBy = mutableListOf()
            }
            statement.groupBy!!.clear()
            statement.groupBy!!.addAll(fields.map { fieldToColumnReference(it) })
        }
        return this
    }


    /**
     * 将当前选择语句设置为Distinct模式，即去除结果中的重复项。
     *
     * @return [SelectClause<T>] 返回当前选择语句实例，允许链式调用。
     */
    fun distinct(): SelectClause<Source, Selected, Context> {
        statement.distinct = true
        return this
    }


    /**
     * 设置分页信息，用于查询语句的分页操作。
     *
     * @param pi 当前页码，表示需要获取哪一页的数据。
     * @param ps 每页的记录数，指定每页显示的数据量。
     * @return 返回 SelectClause<T> 实例，支持链式调用。
     */
    fun page(pi: Int, ps: Int): SelectClause<Source, Selected, Context> {
        pageEnabled = true
        // Calculate offset: (pageIndex - 1) * pageSize
        val offset = if (pi > 0) (pi - 1) * ps else 0
        statement.limit = LimitClause(limit = ps, offset = offset)
        return this
    }

    fun cascade(enabled: Boolean): SelectClause<Source, Selected, Context> {
        cascadeEnabled = enabled
        return this
    }

    fun cascade(someFields: ToReference<Source, Any?>): SelectClause<Source, Selected, Context> {
        someFields ?: throw EmptyFieldsException()
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

    /**
     * 根据指定的字段构建查询条件，并返回SelectClause实例。
     *
     * @param someFields KTableField类型，表示要用来构建查询条件的字段。
     *                   不能为空，否则会抛出EmptyFieldsException异常。
     * @return 返回当前SelectClause实例，允许链式调用。
     */
    fun by(someFields: ToSelect<Source, Any?>): SelectClause<Source, Selected, Context> {
        // 检查someFields是否为空，为空则抛出异常
        someFields ?: throw EmptyFieldsException()
        pojo.afterSelect { t ->
            // 执行someFields中定义的查询逻辑
            someFields(t)
            if (fields.isEmpty()) {
                throw EmptyFieldsException()
            }
            // 构建查询条件，将字段名映射到参数值，并转换为查询条件对象
            val newCondition = fields.map { it.eq(paramMap[it.name]) }.toCriteria()
            // Convert to Expression and merge with existing where condition
            val localCriteriaParams = mutableMapOf<String, Any?>()
            val newExpression = CriteriaToAstConverter.convert(newCondition, localCriteriaParams, KOperationType.SELECT)
            
            // Store criteria parameters for later use in renderStatement
            criteriaParams.putAll(localCriteriaParams)
            
            // Only update where if newExpression is not null
            if (newExpression != null) {
                statement.where = if (statement.where == null) {
                    newExpression
                } else {
                    // Merge with AND
                    BinaryExpression(
                        statement.where!!,
                        SqlOperator.AND,
                        newExpression
                    )
                }
            }
        }
        return this // 返回当前SelectClause实例，允许链式调用
    }


    /**
     * 根据提供的选择条件构建查询条件。
     *
     * @param selectCondition 一个函数，用于定义条件查询。该函数接收一个 [ToFilter] 类型的参数，
     *                        并返回一个 [Boolean]? 类型的值，用于指定条件是否成立。如果为 null，则表示选择所有字段。
     * @return [SelectClause] 的实例，代表了一个查询的选择子句。
     */
    fun where(selectCondition: ToFilter<Context, Boolean?> = null): SelectClause<Source, Selected, Context> {
        selectCondition ?: return this
        contextPojo.afterFilter {
            criteriaParamMap = paramMap
            selectCondition(it) // 执行用户提供的条件函数
            if (criteria == null) return@afterFilter
            // Convert Criteria to Expression and merge with existing where condition
            val localCriteriaParams = mutableMapOf<String, Any?>()
            val newExpression = CriteriaToAstConverter.convert(criteria!!, localCriteriaParams, KOperationType.SELECT)
            
            // Store criteria parameters for later use in renderStatement
            criteriaParams.putAll(localCriteriaParams)
            
            // Only update where if newExpression is not null
            if (newExpression != null) {
                statement.where = if (statement.where == null) {
                    newExpression
                } else {
                    // Merge with AND
                    BinaryExpression(
                        statement.where!!,
                        SqlOperator.AND,
                        newExpression
                    )
                }
            }
        }
        return this
    }

    /**
     * 设置HAVING条件的函数，用于在查询中添加基于聚合结果的条件限制。
     *
     * @param selectCondition 一个KTableConditionalField类型的函数参数，表示筛选的条件。该条件是一个函数，
     *                        它接收当前的参数映射表和执行条件，并设置HAVING子句的条件。
     * @return 返回SelectClause类型的实例，允许链式调用。
     * @throws EmptyFieldsException 如果selectCondition为null，则抛出此异常，表示需要提供条件字段。
     */
    fun having(selectCondition: ToFilter<Context, Boolean?> = null): SelectClause<Source, Selected, Context> {
        // 检查是否提供了条件，未提供则抛出异常
        selectCondition ?: throw EmptyFieldsException()
        contextPojo.afterFilter {
            criteriaParamMap = paramMap // 设置属性参数映射
            selectCondition(it) // 执行传入的条件函数
            if (criteria == null) return@afterFilter
            // Convert Criteria to Expression and merge with existing having condition
            val localCriteriaParams = mutableMapOf<String, Any?>()
            val newExpression = CriteriaToAstConverter.convert(criteria!!, localCriteriaParams, KOperationType.SELECT)
            
            // Store criteria parameters for later use in renderStatement
            criteriaParams.putAll(localCriteriaParams)
            
            // Only update having if newExpression is not null
            if (newExpression != null) {
                statement.having = if (statement.having == null) {
                    newExpression
                } else {
                    // Merge with AND
                    BinaryExpression(
                        statement.having!!,
                        SqlOperator.AND,
                        newExpression
                    )
                }
            }
        }
        return this // 允许链式调用
    }

    fun withTotal(): PagedClause<Source, Selected, SelectClause<Source, Selected, Context>> {
        return PagedClause(this)
    }

    fun patch(vararg pairs: Pair<String, Any?>): SelectClause<Source, Selected, Context> {
        paramMap.putAll(pairs)
        patchParamMap.putAll(pairs)
        return this
    }

    fun lock(lock: PessimisticLock? = PessimisticLock.X): SelectClause<Source, Selected, Context> {
        statement.lock = lock
        return this
    }

    /**
     * 构建一个KronosAtomicTask对象。
     *
     * 该方法主要用于根据提供的KronosDataSourceWrapper（如果存在）和其他参数构建一个用于执行数据库操作的KronosAtomicTask对象。
     * 这包括构建SQL查询语句及其参数映射，配置逻辑删除策略，并根据不同的标志（如分页、去重、分组等）调整查询语句的构造。
     *
     * @param wrapper 可选的KronosDataSourceWrapper对象，用于提供数据库表信息等。
     * @return 构建好的KronosAtomicTask对象，包含了完整的SQL查询语句和对应的参数映射。
     */
    override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        // Render statement to SQL with processed parameters
        val (sql, paramMap) = renderStatement(wrapper)

        // 返回构建好的KronosAtomicTask对象
        val finalSelectFields = if (selectAll) {
            (allFields + selectCascadeFields).toLinkedSet()
        } else {
            (selectFields + selectCascadeFields).toLinkedSet()
        }
        return CascadeSelectClause.build(
            cascadeEnabled, cascadeAllowed, pojo, kClass, KronosAtomicQueryTask(
                sql, paramMap + patchParamMap, operationType = KOperationType.SELECT
            ), finalSelectFields,
            operationType, cascadeSelectedProps ?: mutableSetOf()
        )
    }

    /**
     * 执行Kronos操作的函数。
     *
     * @param wrapper 可选参数，KronosDataSourceWrapper的实例，用于提供数据源配置和上下文。
     *                如果为null，函数将使用默认配置执行操作。
     * @return 返回KronosOperationResult对象，包含操作的结果信息。
     */
    fun query(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any>> {
        return this.build().query(wrapper)
    }

    inline fun <reified T> queryList(
        wrapper: KronosDataSourceWrapper? = null,
        isKPojo: Boolean = false,
        superTypes: List<String> = []
    ): List<T> {
        return this.build().queryList(wrapper, isKPojo, superTypes)
    }

    @JvmName("queryForList")
    @Suppress("UNCHECKED_CAST")
    fun queryList(wrapper: KronosDataSourceWrapper? = null): List<Selected> {
        with(this.build()) {
            beforeQuery?.invoke(this)
            val result = atomicTask.logAndReturn(
                wrapper.orDefault().forList(atomicTask, projectionClass, true, []) as List<Selected>, QueryList
            )
            afterQuery?.invoke(result, QueryList, wrapper.orDefault())
            return result
        }
    }


    fun queryMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any> {
        limit(1)
        return this.build().queryMap(wrapper)
    }

    fun queryMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any>? {
        limit(1)
        return this.build().queryMapOrNull(wrapper)
    }

    inline fun <reified T> queryOne(
        wrapper: KronosDataSourceWrapper? = null,
        isKPojo: Boolean = false,
        superTypes: List<String> = []
    ): T {
        limit(1)
        return this.build().queryOne(wrapper, isKPojo, superTypes)
    }

    @JvmName("queryForObject")
    @Suppress("UNCHECKED_CAST")
    fun queryOne(wrapper: KronosDataSourceWrapper? = null): Selected {
        limit(1)
        with(build()) {
            beforeQuery?.invoke(this)
            val result = atomicTask.logAndReturn(
                (wrapper.orDefault().forObject(atomicTask, projectionClass, true, [])
                    ?: throw NullPointerException("No such record")) as Selected, QueryOne
            )
            afterQuery?.invoke(result, QueryOne, wrapper.orDefault())
            return result
        }
    }

    inline fun <reified T> queryOneOrNull(
        wrapper: KronosDataSourceWrapper? = null,
        isKPojo: Boolean = false,
        superTypes: List<String> = []
    ): T? {
        limit(1)
        return this.build().queryOneOrNull(wrapper, isKPojo, superTypes)
    }

    @JvmName("queryForObjectOrNull")
    @Suppress("UNCHECKED_CAST")
    fun queryOneOrNull(wrapper: KronosDataSourceWrapper? = null): Selected? {
        limit(1)
        with(build()) {
            beforeQuery?.invoke(this)
            val result = atomicTask.logAndReturn(
                wrapper.orDefault().forObject(atomicTask, projectionClass, true, []) as Selected?, QueryOneOrNull
            )
            afterQuery?.invoke(result, QueryOneOrNull, wrapper.orDefault())
            return result
        }
    }

    companion object {

        fun <Source : KPojo, Selected : KPojo, Context : KPojo> Iterable<SelectClause<Source, Selected, Context>>.by(
            someFields: ToSelect<Source, Any?>
        ): List<SelectClause<Source, Selected, Context>> {
            return map { it.by(someFields) }
        }

        fun <Source : KPojo, Selected : KPojo, Context : KPojo> Iterable<SelectClause<Source, Selected, Context>>.cascade(
            enabled: Boolean
        ): List<SelectClause<Source, Selected, Context>> {
            return map { it.cascade(enabled) }
        }

        fun <Source : KPojo, Selected : KPojo, Context : KPojo> Iterable<SelectClause<Source, Selected, Context>>.cascade(
            someFields: ToReference<Source, Any?>
        ): List<SelectClause<Source, Selected, Context>> {
            return map { it.cascade(someFields) }
        }

        /**
         * Applies the `where` operation to each update clause in the list based on the provided update condition.
         *
         * @param selectCondition the condition for the update clause. Defaults to null.
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <Source : KPojo, Selected : KPojo, Context : KPojo> Iterable<SelectClause<Source, Selected, Context>>.where(
            selectCondition: ToFilter<Context, Boolean?> = null
        ): List<SelectClause<Source, Selected, Context>> {
            return map { it.where(selectCondition) }
        }

        /**
         * Builds a KronosAtomicBatchTask from a list of UpdateClause objects.
         *
         * @param T The type of KPojo objects in the list.
         * @return A KronosAtomicBatchTask object with the SQL and parameter map array from the UpdateClause objects.
         */
        fun <Source : KPojo, Selected : KPojo, Context : KPojo> Iterable<SelectClause<Source, Selected, Context>>.build(): KronosAtomicBatchTask {
            val tasks = this.map { it.build() }
            return KronosAtomicBatchTask(
                sql = tasks.first().atomicTask.sql,
                paramMapArr = tasks.map { it.atomicTask.paramMap }.toTypedArray(),
                operationType = KOperationType.SELECT
            )
        }

        fun <Source : KPojo, Selected : KPojo, Context : KPojo> Iterable<SelectClause<Source, Selected, Context>>.query(
            wrapper: KronosDataSourceWrapper? = null
        ): List<List<Map<String, Any>>> {
            return map { it.query(wrapper) }
        }

        fun <Source : KPojo, Selected : KPojo, Context : KPojo> Iterable<SelectClause<Source, Selected, Context>>.queryList(
            wrapper: KronosDataSourceWrapper? = null
        ): List<List<Selected>> {
            return map { it.queryList(wrapper) }
        }

        fun <Source : KPojo, Selected : KPojo, Context : KPojo> Iterable<SelectClause<Source, Selected, Context>>.queryMap(
            wrapper: KronosDataSourceWrapper? = null
        ): List<Map<String, Any>> {
            return map { it.queryMap(wrapper) }
        }

        fun <Source : KPojo, Selected : KPojo, Context : KPojo> Iterable<SelectClause<Source, Selected, Context>>.queryMapOrNull(
            wrapper: KronosDataSourceWrapper? = null
        ): List<Map<String, Any>?> {
            return map { it.queryMapOrNull(wrapper) }
        }

        fun <Source : KPojo, Selected : KPojo, Context : KPojo> Iterable<SelectClause<Source, Selected, Context>>.queryOne(
            wrapper: KronosDataSourceWrapper? = null
        ): List<Selected> {
            return map { it.queryOne(wrapper) }
        }

        fun <Source : KPojo, Selected : KPojo, Context : KPojo> Iterable<SelectClause<Source, Selected, Context>>.queryOneOrNull(
            wrapper: KronosDataSourceWrapper? = null
        ): List<Selected?> {
            return map { it.queryOneOrNull(wrapper) }
        }
    }

}
