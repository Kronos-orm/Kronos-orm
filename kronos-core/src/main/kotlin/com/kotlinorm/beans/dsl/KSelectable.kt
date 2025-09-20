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

package com.kotlinorm.beans.dsl

import com.kotlinorm.ast.SelectStatement
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper

/**
 * Base class for selectable clauses that use AST SelectStatement
 */
abstract class KSelectable<T : KPojo>(
    val pojo: T
) {
    abstract fun build(wrapper: KronosDataSourceWrapper? = null): KronosQueryTask
    
    // The AST statement - the single source of truth
    var selectStatement: SelectStatement = SelectStatement()
    
    // Delegate properties to statement
    var selectAll: Boolean
        get() = selectStatement.selectAll
        set(value) { selectStatement.selectAll = value }
    
    var limitCapacity: Int
        get() = selectStatement.limit ?: 0
        set(value) { 
            if (value > 0) {
                selectStatement.limit = value
            } else {
                selectStatement.limit = null
                selectStatement.offset = null
            }
        }
    
    // Delegate selectFields to statement
    var selectFields: LinkedHashSet<Field> = linkedSetOf()
        set(value) {
            field = value
            selectStatement.setProjectionsFromFields(value)
        }
    
    init {
        // Initialize the AST statement
        selectStatement = SelectStatement()
    }
    
    /**
     * Set pagination parameters
     */
    fun setPage(pageIndex: Int, pageSize: Int) {
        selectStatement.setPage(pageIndex, pageSize)
    }
    
    /**
     * Set limit
     */
    fun setLimit(limit: Int) {
        selectStatement.limit = limit
    }
    
    /**
     * Set offset
     */
    fun setOffset(offset: Int) {
        selectStatement.offset = offset
    }
    
    /**
     * Enable distinct
     */
    fun setDistinct(distinct: Boolean) {
        selectStatement.distinct = distinct
    }
}
