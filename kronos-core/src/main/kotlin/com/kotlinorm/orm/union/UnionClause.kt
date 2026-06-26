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

package com.kotlinorm.orm.union

import com.kotlinorm.ast.ColumnReference
import com.kotlinorm.ast.CriteriaToAstConverter
import com.kotlinorm.ast.LimitClause
import com.kotlinorm.ast.OrderByItem
import com.kotlinorm.ast.RenderContext
import com.kotlinorm.ast.UnionStatement
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.database.RegisteredDBTypeManager.getDBSupport
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.SortType
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.join.SelectFrom
import com.kotlinorm.orm.select.SelectClause
import com.kotlinorm.utils.DataSourceUtil.orDefault

/**
 * UnionClause
 *
 * Represents a UNION operation that combines multiple SELECT queries using AST.
 * Generates proper SQL UNION statements executed by the database.
 *
 * Example usage:
 * ```kotlin
 * // Using union() function
 * val result = union(
 *     User().select().where { it.id == 1 },
 *     User().select().where { it.id == 2 }
 * ).query()
 * 
 * // Using infix notation
 * val result = User().select().where { it.id == 1 }
 *     union User().select().where { it.id == 2 }
 *     union Customer().select().limit(1)
 *     query()
 * 
 * // Using UNION ALL
 * val result = User().select().where { it.id == 1 }
 *     unionAll User().select().where { it.id == 2 }
 *     query()
 * ```
 *
 * @author OUSC
 */
class UnionClause internal constructor(
    selectables: List<KSelectable<out KPojo>>,
    initialUnionAll: Boolean = false
) {
    private val selectables: MutableList<KSelectable<out KPojo>> = selectables.toMutableList()
    private var unionAll: Boolean = initialUnionAll
    private var orderByItems: List<OrderByItem>? = null
    private var limitClause: LimitClause? = null
    
    /**
     * Use UNION ALL instead of UNION (includes duplicates)
     * Note: This applies to ALL union operations in this clause
     */
    fun all(): UnionClause {
        this.unionAll = true
        return this
    }
    
    /**
     * Add another query to this union with UNION (removes duplicates)
     * Internal method used by infix functions
     */
    internal fun addQueryWithUnion(selectable: KSelectable<out KPojo>): UnionClause {
        this.selectables.add(selectable)
        // Keep the current unionAll setting
        return this
    }
    
    /**
     * Add another query to this union with UNION ALL (includes duplicates)
     * Internal method used by infix functions
     */
    internal fun addQueryWithUnionAll(selectable: KSelectable<out KPojo>): UnionClause {
        this.selectables.add(selectable)
        this.unionAll = true
        return this
    }
    
    /**
     * Add ORDER BY clause to the entire union result
     */
    fun orderBy(vararg items: Pair<String, SortType>): UnionClause {
        this.orderByItems = items.map { (columnName, sortType) ->
            OrderByItem(
                expression = ColumnReference(
                    tableAlias = null,
                    columnName = columnName
                ),
                direction = sortType
            )
        }
        return this
    }
    
    /**
     * Add LIMIT clause to the entire union result
     */
    fun limit(limit: Int, offset: Int? = null): UnionClause {
        this.limitClause = LimitClause(limit = limit, offset = offset)
        return this
    }
    
    /**
     * Execute the union query and return results
     */
    fun query(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any>> {
        return build(wrapper).query(wrapper)
    }
    
    /**
     * Execute the union query and return a single result
     */
    fun queryMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any> {
        limit(1)
        return build(wrapper).queryMap(wrapper)
    }
    
    /**
     * Execute the union query and return a single result or null
     */
    fun queryMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any>? {
        limit(1)
        return build(wrapper).queryMapOrNull(wrapper)
    }
    
    /**
     * Execute the union query and return typed results
     */
    inline fun <reified T> queryList(
        wrapper: KronosDataSourceWrapper? = null,
        isKPojo: Boolean = false,
        superTypes: List<String> = listOf()
    ): List<T> {
        return build(wrapper).queryList(wrapper, isKPojo, superTypes)
    }
    
    /**
     * Execute the union query and return a single typed result
     */
    inline fun <reified T> queryOne(
        wrapper: KronosDataSourceWrapper? = null,
        isKPojo: Boolean = false,
        superTypes: List<String> = listOf()
    ): T {
        limit(1)
        return build(wrapper).queryOne(wrapper, isKPojo, superTypes)
    }
    
    /**
     * Execute the union query and return a single typed result or null
     */
    inline fun <reified T> queryOneOrNull(
        wrapper: KronosDataSourceWrapper? = null,
        isKPojo: Boolean = false,
        superTypes: List<String> = listOf()
    ): T? {
        limit(1)
        return build(wrapper).queryOneOrNull(wrapper, isKPojo, superTypes)
    }
    
    /**
     * Generate UnionStatement AST from the selectables
     * 
     * @param wrapper Optional data source wrapper
     * @param parameterValues Mutable map to collect all parameter values from all queries
     * @return UnionStatement AST
     */
    fun toStatement(wrapper: KronosDataSourceWrapper? = null, parameterValues: MutableMap<String, Any?> = mutableMapOf()): UnionStatement {
        // Track parameter name counters for unique naming
        val parameterCounter = mutableMapOf<String, Int>()
        
        // Convert each selectable to SelectStatement and collect parameters
        val selectStatements = selectables.mapIndexed { index, selectable ->
            // Each selectable needs its own parameter map to collect its parameters
            val queryParams = mutableMapOf<String, Any?>()
            
            // Call toStatement with parameter collection
            val statement = when (selectable) {
                is SelectClause -> {
                    selectable.toStatement(wrapper, queryParams)
                }
                is SelectFrom<*> -> {
                    selectable.toStatement(wrapper, queryParams)
                }
                else -> {
                    // For other types, just call the single-parameter version
                    selectable.toStatement(wrapper)
                }
            }
            
            // Rename parameters to ensure uniqueness and merge into main parameter map
            queryParams.forEach { (paramName, value) ->
                if (index == 0) {
                    // First query: use original parameter names
                    parameterValues[paramName] = value
                    parameterCounter[paramName] = 0
                } else {
                    // Subsequent queries: rename parameters to ensure uniqueness
                    val uniqueName = CriteriaToAstConverter.getUniqueParamName(
                        paramName,
                        parameterValues,
                        parameterCounter
                    )
                    parameterValues[uniqueName] = value
                }
            }
            
            statement
        }
        
        // Create UnionStatement with ORDER BY and LIMIT applied to the entire result
        return UnionStatement(
            queries = selectStatements,
            unionAll = unionAll,
            orderBy = orderByItems,
            limit = limitClause
        )
    }
    
    /**
     * Build the UnionStatement AST and render to SQL
     */
    fun build(wrapper: KronosDataSourceWrapper? = null): KronosQueryTask {
        val dataSource = wrapper.orDefault()
        
        // Collect all parameters from all selectables
        val allParameters = mutableMapOf<String, Any?>()
        val unionStatement = toStatement(wrapper, allParameters)
        
        // Get database support for rendering
        val support = getDBSupport(dataSource.dbType) 
            ?: throw UnsupportedDatabaseTypeException(dataSource.dbType)
        
        // Create render context and pre-bind all parameters
        val context = RenderContext(quotes = support.quotes)
        context.boundParameters.putAll(allParameters)
        
        // Render the UnionStatement to SQL
        val sql = support.renderer.renderUnionStatement(unionStatement, context)
        
        // Get the final parameters from context (may have been renamed during rendering)
        val finalParameters = context.boundParameters.toMap()
        
        // Create atomic task
        val atomicTask = KronosAtomicQueryTask(
            sql = sql,
            paramMap = finalParameters,
            operationType = KOperationType.SELECT
        )
        
        // Return query task
        return KronosQueryTask(atomicTask)
    }
}
