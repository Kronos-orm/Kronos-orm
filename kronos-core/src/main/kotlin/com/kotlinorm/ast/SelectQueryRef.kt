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

package com.kotlinorm.ast

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.interfaces.KronosDataSourceWrapper

/**
 * Context used when deferred query expressions are materialized into concrete AST nodes.
 */
data class QueryMaterializeContext(
    val wrapper: KronosDataSourceWrapper? = null,
    val parameterValues: MutableMap<String, Any?> = mutableMapOf()
)

/**
 * Delayed reference to a selectable query. It keeps subqueries from freezing their statement too
 * early, so outer query build steps can coordinate aliases and parameters first.
 */
fun interface SelectQueryRef {
    fun materialize(context: QueryMaterializeContext): SelectStatement
}

data class KSelectableQueryRef(
    val query: KSelectable<*>
) : SelectQueryRef {
    override fun materialize(context: QueryMaterializeContext): SelectStatement {
        return query.toStatement(context.wrapper, context.parameterValues)
    }
}
