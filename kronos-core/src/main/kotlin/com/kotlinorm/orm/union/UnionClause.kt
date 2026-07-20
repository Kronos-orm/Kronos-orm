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

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.parser.NoneDataSourceWrapper
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.database.SqlManager.renderStatement
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.InvalidDataAccessApiUsageException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.orm.sql.materializeSqlQuery
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.quantifier.SqlQuantifier
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSetOperator
import com.kotlinorm.utils.DataSourceUtil.orDefault
import kotlin.reflect.KType

class UnionClause<Selected : KPojo> internal constructor(
    selectables: List<KSelectable<out KPojo>>,
    selectedType: KType,
    nullableSelectedType: KType,
    initialUnionAll: Boolean = false
) : KSelectable<Selected>(selectables.first().pojo) {
    override val selectedType: KType = selectedType
    override val nullableSelectedType: KType = nullableSelectedType
    internal val selectables: MutableList<KSelectable<out KPojo>> = selectables.toMutableList()
    internal var unionAll: Boolean = initialUnionAll
    private var orderByItems: List<SqlOrderingItem> = emptyList()
    private var limitClause: SqlLimit? = null

    fun all(): UnionClause<Selected> {
        unionAll = true
        return this
    }

    fun orderBy(vararg items: Pair<String, SqlOrdering>): UnionClause<Selected> {
        orderByItems = items.map { (columnName, ordering) ->
            SqlOrderingItem(
                expr = SqlExpr.Column(columnName = columnName),
                ordering = ordering
            )
        }
        return this
    }

    fun limit(limit: Int, offset: Int? = null): UnionClause<Selected> {
        limitClause = if (limit >= 0) SqlLimit.limit(limit, offset) else null
        return this
    }

    @PublishedApi
    internal override fun prepareFirstResult() {
        limit(1)
    }

    fun toMapList(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any?>> {
        return build(wrapper).toMapList(wrapper)
    }

    fun toMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?> {
        limit(1)
        return build(wrapper).toMap(wrapper)
    }

    fun toMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?>? {
        limit(1)
        return build(wrapper).toMapOrNull(wrapper)
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.LowPriorityInOverloadResolution
    inline fun <reified T> toList(
        wrapper: KronosDataSourceWrapper? = null
    ): List<T> {
        return build(wrapper).toList(wrapper)
    }

    @JvmName("toProjectionList")
    @Suppress("UNCHECKED_CAST")
    fun toList(wrapper: KronosDataSourceWrapper? = null): List<Selected> {
        return build(wrapper).toList(wrapper, selectedType) as List<Selected>
    }

    internal override fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper?): SqlQueryPlan {
        val dataSource = wrapper.orDefault()
        validateSqlServerLimit(dataSource)
        val parameters = linkedMapOf<String, Any?>()
        val parameterCounter = mutableMapOf<String, Int>()
        val queries = selectables.map { selectable ->
            selectable.materializeSqlQuery(parameters, parameterCounter, dataSource)
        }
        val query = queries.reduce { left, right ->
            SqlQuery.Set(
                left = left,
                operator = SqlSetOperator.Union(if (unionAll) SqlQuantifier.All else null),
                right = right
            )
        }.withUnionTail()
        return SqlQueryPlan(query, parameters)
    }

    private fun validateSqlServerLimit(dataSource: KronosDataSourceWrapper) {
        if (dataSource === NoneDataSourceWrapper) return
        if (dataSource.dbType == DBType.Mssql && limitClause != null && orderByItems.isEmpty()) {
            throw InvalidDataAccessApiUsageException(
                "SQL Server union limit() requires orderBy() because OFFSET/FETCH cannot be rendered without ORDER BY."
            )
        }
    }

    private fun SqlQuery.withUnionTail(): SqlQuery =
        if (this is SqlQuery.Set) {
            copy(orderBy = orderByItems, limit = limitClause)
        } else {
            this
        }

    override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        val dataSource = wrapper.orDefault()
        val plan = toSqlQueryPlan(dataSource)
        val renderedSql = renderStatement(dataSource, plan.query, plan.parameters)
        return KronosQueryTask(
            KronosAtomicQueryTask(
                sql = renderedSql.sql,
                paramMap = renderedSql.parameters,
                operationType = KOperationType.SELECT,
                statement = plan.query,
                targetType = selectedType,
                resultColumnTypes = resultColumnTypes(),
                listParameterOccurrences = renderedSql.listParameterOccurrences
            )
        )
    }
}
