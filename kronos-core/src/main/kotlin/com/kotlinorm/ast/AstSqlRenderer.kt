package com.kotlinorm.ast

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.database.SqlManager.quoted
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper

/**
 * SQL renderer utility for the AST. Provides default implementations that can be used by database
 * support classes. Each database support class can override these methods to provide
 * database-specific SQL rendering.
 */
object AstSqlRenderer {

    data class RenderedSql(val sql: String, val params: MutableMap<String, Any?> = mutableMapOf())

    fun renderInsert(
            wrapper: KronosDataSourceWrapper,
            support: DatabasesSupport,
            stmt: InsertStatement
    ): String {
        return renderInsertWithParams(wrapper, support, stmt).sql
    }

    fun renderInsertWithParams(
            wrapper: KronosDataSourceWrapper,
            support: DatabasesSupport,
            stmt: InsertStatement
    ): RenderedSql {
        val cols =
                if (stmt.columns.isEmpty()) ""
                else stmt.columns.joinToString(", ") { support.quote(it) }
        val target =
                listOfNotNull(stmt.target.database, stmt.target.schema, stmt.target.table)
                        .joinToString(".") { support.quote(it) }
        val params = mutableMapOf<String, Any?>()
        val sql = buildString {
            append("INSERT INTO ")
            append(target)
            if (cols.isNotEmpty()) append(" (").append(cols).append(")")
            append(" ")
            when (val src = stmt.source) {
                is ValuesSource -> {
                    append("VALUES ")
                    append(
                            src.rows.joinToString(", ") { row ->
                                row.joinToString(prefix = "(", postfix = ")", separator = ", ") {
                                    val rendered = renderExpr(wrapper, support, it)
                                    rendered
                                }
                            }
                    )
                }
                is SelectSource -> {
                    val rendered = renderSelectWithParams(wrapper, support, src.select)
                    params.putAll(rendered.params)
                    append(rendered.sql)
                }
            }
        }
        return RenderedSql(sql, params)
    }

    fun renderUpdate(
            wrapper: KronosDataSourceWrapper,
            support: DatabasesSupport,
            stmt: UpdateStatement
    ): String {
        return renderUpdateWithParams(wrapper, support, stmt).sql
    }

    fun renderUpdateWithParams(
            wrapper: KronosDataSourceWrapper,
            support: DatabasesSupport,
            stmt: UpdateStatement
    ): RenderedSql {
        val target =
                listOfNotNull(stmt.target.database, stmt.target.schema, stmt.target.table)
                        .joinToString(".") { support.quote(it) }
        val params = mutableMapOf<String, Any?>()
        val setSql =
                stmt.set.joinToString(", ") { assign ->
                    val col = buildString {
                        if (!assign.column.tableAlias.isNullOrBlank()) {
                            append(support.quote(assign.column.tableAlias!!))
                            append('.')
                        }
                        append(support.quote(assign.column.column))
                    }
                    val v = renderExpr(wrapper, support, assign.value)
                    "$col = $v"
                }
        val whereSql =
                stmt.where?.let { w ->
                    when (w) {
                        is RawSqlExpr -> w.sql // may already contain WHERE
                        is CriteriaExpr -> {
                            val (sql, map) = renderCriteriaWithParams(wrapper, support, w.criteria)
                            params.putAll(map)
                            " WHERE $sql"
                        }
                        else -> " WHERE " + renderExpr(wrapper, support, w)
                    }
                }
                        ?: ""
        val sql = "UPDATE $target SET $setSql$whereSql"
        return RenderedSql(sql, params)
    }

    fun renderDelete(
            wrapper: KronosDataSourceWrapper,
            support: DatabasesSupport,
            stmt: DeleteStatement
    ): String {
        return renderDeleteWithParams(wrapper, support, stmt).sql
    }

    fun renderDeleteWithParams(
            wrapper: KronosDataSourceWrapper,
            support: DatabasesSupport,
            stmt: DeleteStatement
    ): RenderedSql {
        val target =
                listOfNotNull(stmt.target.database, stmt.target.schema, stmt.target.table)
                        .joinToString(".") { support.quote(it) }
        val params = mutableMapOf<String, Any?>()
        val whereSql =
                stmt.where?.let { w ->
                    when (w) {
                        is RawSqlExpr -> w.sql // may already contain WHERE
                        is CriteriaExpr -> {
                            val (sql, map) = renderCriteriaWithParams(wrapper, support, w.criteria)
                            params.putAll(map)
                            " WHERE $sql"
                        }
                        else -> " WHERE " + renderExpr(wrapper, support, w)
                    }
                }
                        ?: ""
        return RenderedSql("DELETE FROM $target$whereSql", params)
    }

    fun renderSelect(
            wrapper: KronosDataSourceWrapper,
            support: DatabasesSupport,
            stmt: SelectStatement
    ): String {
        return renderSelectWithParams(wrapper, support, stmt).sql
    }

    fun renderSelectWithParams(
            wrapper: KronosDataSourceWrapper,
            support: DatabasesSupport,
            stmt: SelectStatement
    ): RenderedSql {
        val sb = StringBuilder()
        val params = mutableMapOf<String, Any?>()
        // WITH (CTE)
        if (stmt.with.isNotEmpty()) {
            sb.append("WITH ")
            sb.append(
                    stmt.with.joinToString(", ") { cte ->
                        val cols =
                                if (cte.columns.isEmpty()) ""
                                else
                                        cte.columns.joinToString(prefix = "(", postfix = ")") {
                                            support.quote(it)
                                        }
                        val sub = renderSelect(wrapper, support, cte.query)
                        "${support.quote(cte.name)}$cols AS ($sub)"
                    }
            )
            sb.append(" ")
        }

        sb.append("SELECT ")
        if (stmt.distinct) sb.append("DISTINCT ")
        if (stmt.projections.isEmpty()) {
            sb.append("*")
        } else {
            sb.append(
                    stmt.projections.joinToString(", ") { p ->
                        val exprSql = renderExpr(wrapper, support, p.expression)
                        if (p.alias.isNullOrBlank()) exprSql
                        else "$exprSql AS ${support.quote(p.alias)}"
                    }
            )
        }

        // FROM
        stmt.from?.let {
            sb.append(" FROM ")
            sb.append(renderTableSource(wrapper, support, it))
        }

        // WHERE
        stmt.where?.let { whereExpr ->
            when (whereExpr) {
                is RawSqlExpr -> sb.append(whereExpr.sql)
                is CriteriaExpr -> {
                    val (sql, map) = renderCriteriaWithParams(wrapper, support, whereExpr.criteria)
                    if (sql.isNotBlank()) {
                        sb.append(" WHERE ").append(sql)
                        params.putAll(map)
                    } else {
                        // 空条件，不添加WHERE子句
                    }
                }
                else -> sb.append(" WHERE ").append(renderExpr(wrapper, support, whereExpr))
            }
        }

        // GROUP BY
        if (stmt.groupBy.isNotEmpty()) {
            // If groupBy contains a single RawSqlExpr that already starts with GROUP BY, pass
            // through
            if (stmt.groupBy.size == 1 && stmt.groupBy.first() is RawSqlExpr) {
                sb.append(" ").append((stmt.groupBy.first() as RawSqlExpr).sql)
            } else {
                sb.append(" GROUP BY ")
                sb.append(stmt.groupBy.joinToString(", ") { renderExpr(wrapper, support, it) })
            }
        }

        // HAVING
        stmt.having?.let { havingExpr ->
            when (havingExpr) {
                is RawSqlExpr -> sb.append(" ").append(havingExpr.sql)
                is CriteriaExpr -> {
                    val (sql, map) = renderCriteriaWithParams(wrapper, support, havingExpr.criteria)
                    if (sql.isNotBlank()) {
                        sb.append(" HAVING ").append(sql)
                        params.putAll(map)
                    } else {
                        // 空条件，不添加HAVING子句
                    }
                }
                else -> sb.append(" HAVING ").append(renderExpr(wrapper, support, havingExpr))
            }
        }

        // ORDER BY
        if (stmt.orderBy.isNotEmpty()) {
            // If orderBy contains a single RawSqlExpr that already starts with ORDER BY, pass
            // through
            if (stmt.orderBy.size == 1 && stmt.orderBy.first().expression is RawSqlExpr) {
                sb.append(" ").append((stmt.orderBy.first().expression as RawSqlExpr).sql)
            } else {
                sb.append(" ORDER BY ")
                sb.append(
                        stmt.orderBy.joinToString(", ") { oi ->
                            val e = renderExpr(wrapper, support, oi.expression)
                            val dir =
                                    when (oi.direction) {
                                        OrderDirection.asc -> "ASC"
                                        OrderDirection.desc -> "DESC"
                                    }
                            buildString {
                                append(e)
                                append(' ')
                                append(dir)
                                when (oi.nulls) {
                                    NullsOrder.first -> append(" NULLS FIRST")
                                    NullsOrder.last -> append(" NULLS LAST")
                                    NullsOrder.defaultOrder -> {
                                        /* noop */
                                    }
                                }
                            }
                        }
                )
            }
        }

        // LIMIT/OFFSET - 使用标准SQL语法，由DatabaseSupport处理方言差异
        val limit = stmt.limit
        val offset = stmt.offset
        if (limit != null) sb.append(" LIMIT ").append(limit)
        if (offset != null) sb.append(" OFFSET ").append(offset)

        // Locking (very minimal): treat any lock as FOR UPDATE if present
        stmt.lock?.let { sb.append(" FOR UPDATE") }

        // set operations
        if (stmt.setOps.isNotEmpty()) {
            stmt.setOps.forEach { (op, q) ->
                val opSql =
                        when (op) {
                            SetOperator.union -> " UNION "
                            SetOperator.unionAll -> " UNION ALL "
                            SetOperator.intersect -> " INTERSECT "
                            SetOperator.except -> " EXCEPT "
                        }
                sb.append(opSql)
                val sub = renderSelectWithParams(wrapper, support, q)
                params.putAll(sub.params)
                sb.append(sub.sql)
            }
        }

        return RenderedSql(sb.toString(), params)
    }

    private fun renderTableSource(
            wrapper: KronosDataSourceWrapper,
            support: DatabasesSupport,
            ts: TableSource
    ): String =
            when (ts) {
                is TableName -> {
                    val parts =
                            listOfNotNull(ts.database, ts.schema, ts.table).map {
                                support.quote(it)
                            }
                    val base = parts.joinToString(".")
                    if (ts.alias.isNullOrBlank()) base else "$base AS ${support.quote(ts.alias)}"
                }
                is SubquerySource -> {
                    val inner = renderSelect(wrapper, support, ts.query)
                    "($inner) AS ${support.quote(ts.alias)}"
                }
                is Join -> {
                    val left = renderTableSource(wrapper, support, ts.left)
                    val right = renderTableSource(wrapper, support, ts.right)
                    val typeSql =
                            when (ts.type) {
                                JoinType.inner -> "INNER JOIN"
                                JoinType.left -> "LEFT JOIN"
                                JoinType.right -> "RIGHT JOIN"
                                JoinType.full -> "FULL JOIN"
                                JoinType.cross -> "CROSS JOIN"
                                JoinType.lateral -> "LATERAL JOIN"
                            }
                    val onSql =
                            ts.on
                                    ?.let {
                                        when (it) {
                                            is RawSqlExpr -> it.sql
                                            is Expression -> renderExpr(wrapper, support, it)
                                        }
                                    }
                                    ?.let { " ON $it" }
                                    ?: ""
                    "$left $typeSql $right$onSql"
                }
                is RawTableSource -> ts.sql
            }

    private fun renderExpr(
            wrapper: KronosDataSourceWrapper,
            support: DatabasesSupport,
            e: Expression
    ): String =
            when (e) {
                is ColumnRef ->
                        buildString {
                            if (!e.tableAlias.isNullOrBlank()) {
                                append(support.quote(e.tableAlias))
                                append('.')
                            }
                            append(support.quote(e.column))
                        }
                is Literal<*> ->
                        when (val v = e.value) {
                            null -> "NULL"
                            is Number, is Boolean -> v.toString()
                            is String -> {
                                // Check if the string is a numeric literal
                                if (v.toIntOrNull() != null ||
                                                v.toLongOrNull() != null ||
                                                v.toDoubleOrNull() != null
                                ) {
                                    v
                                } else {
                                    "'" + v.replace("'", "''") + "'"
                                }
                            }
                            else -> "'" + v.toString().replace("'", "''") + "'"
                        }
                is NamedParam -> ":" + e.name
                is PositionalParam -> "?" // index ignored here
                is UnaryOp -> "${e.op} ${paren(renderExpr(wrapper, support, e.expr))}"
                is BinaryOp -> {
                    val left = renderExpr(wrapper, support, e.left)
                    val right = renderExpr(wrapper, support, e.right)
                    // 对于简单的列引用和参数，不需要添加括号
                    val leftStr = if (e.left is ColumnRef || e.left is NamedParam) left else paren(left)
                    val rightStr = if (e.right is ColumnRef || e.right is NamedParam) right else paren(right)
                    "$leftStr ${e.op} $rightStr"
                }
                is FuncCall ->
                        buildString {
                            append(e.name)
                            append('(')
                            if (e.distinct) append("DISTINCT ")
                            append(e.args.joinToString(", ") { renderExpr(wrapper, support, it) })
                            append(')')
                        }
                is CaseWhen ->
                        buildString {
                            append("CASE ")
                            e.whens.forEach { (c, v) ->
                                append("WHEN ")
                                append(renderExpr(wrapper, support, c))
                                append(" THEN ")
                                append(renderExpr(wrapper, support, v))
                                append(' ')
                            }
                            e.elseExpr?.let {
                                append("ELSE ")
                                append(renderExpr(wrapper, support, it))
                                append(' ')
                            }
                            append("END")
                        }
                is Between ->
                        buildString {
                            if (e.not) append("NOT ")
                            append(renderExpr(wrapper, support, e.expr))
                            append(" BETWEEN ")
                            append(renderExpr(wrapper, support, e.from))
                            append(" AND ")
                            append(renderExpr(wrapper, support, e.to))
                        }
                is InOp ->
                        buildString {
                            append(renderExpr(wrapper, support, e.expr))
                            append(' ')
                            if (e.not) append("NOT ")
                            append("IN ")
                            when (val list = e.list) {
                                is InValues ->
                                        append(
                                                list.values.joinToString(
                                                        prefix = "(",
                                                        postfix = ")"
                                                ) { renderExpr(wrapper, support, it) }
                                        )
                                is InSubquery ->
                                        append("(")
                                                .append(renderSelect(wrapper, support, list.query))
                                                .append(")")
                            }
                        }
                is Exists ->
                        buildString {
                            if (e.not) append("NOT ")
                            append("EXISTS (")
                            append(renderSelect(wrapper, support, e.subquery))
                            append(')')
                        }
                is Cast -> "CAST(${renderExpr(wrapper, support, e.expr)} AS ${e.typeName})"
                is RawSqlExpr -> e.sql
                is FunctionFieldExpr -> {
                    // 使用FunctionManager来渲染FunctionField，需要wrapper和数据库类型信息
                    com.kotlinorm.functions.FunctionManager.getBuiltFunctionField(
                        e.functionField, 
                        wrapper
                    )
                }
                is CriteriaExpr -> {
                    // 兼容：仅渲染SQL文本（无参数收集）
                    val (sql, _) = renderCriteriaWithParams(wrapper, support, e.criteria)
                    sql
                }
            }

    private fun paren(s: String): String =
            if (s.firstOrNull() == '(' && s.lastOrNull() == ')') s else "($s)"

    // ---- Criteria rendering with params collection (minimal parity with previous builder) ----
    private data class KeyCounterState(
            var initialized: Boolean = false,
            var metaOfMap: MutableMap<String, MutableMap<Int, Any?>> = mutableMapOf()
    )

    fun renderCriteriaDirect(
            wrapper: KronosDataSourceWrapper,
            support: DatabasesSupport,
            criteria: Criteria
    ): String {
        // 处理空值策略
        if (criteria.noValueStrategyType == com.kotlinorm.enums.NoValueStrategyType.Ignore && criteria.value == null) {
            return ""
        }
        
        val baseSql = when (criteria.type) {
            com.kotlinorm.enums.ConditionType.Equal ->
                    "${criteria.field.quoted(wrapper)} = :${criteria.field.name}"
            com.kotlinorm.enums.ConditionType.Like ->
                    "${criteria.field.quoted(wrapper)} LIKE :${criteria.field.name}"
            com.kotlinorm.enums.ConditionType.In -> {
                val value = criteria.value
                if (value is List<*> && value.isEmpty()) {
                    "false"
                } else {
                    "${criteria.field.quoted(wrapper)} IN (:${criteria.field.name}List)"
                }
            }
            com.kotlinorm.enums.ConditionType.Gt ->
                    "${criteria.field.quoted(wrapper)} > :${criteria.field.name}Min"
            com.kotlinorm.enums.ConditionType.Ge ->
                    "${criteria.field.quoted(wrapper)} >= :${criteria.field.name}Min"
            com.kotlinorm.enums.ConditionType.Lt ->
                    "${criteria.field.quoted(wrapper)} < :${criteria.field.name}Max"
            com.kotlinorm.enums.ConditionType.Le ->
                    "${criteria.field.quoted(wrapper)} <= :${criteria.field.name}Max"
            com.kotlinorm.enums.ConditionType.Between ->
                    "${criteria.field.quoted(wrapper)} BETWEEN :${criteria.field.name}Min AND :${criteria.field.name}Max"
            com.kotlinorm.enums.ConditionType.IsNull -> "${criteria.field.quoted(wrapper)} IS NULL"
            com.kotlinorm.enums.ConditionType.And -> {
                val childSqls =
                        criteria.children.filterNotNull().map {
                            renderCriteriaDirect(wrapper, support, it)
                        }.filter { it.isNotEmpty() }
                childSqls.joinToString(" AND ")
            }
            com.kotlinorm.enums.ConditionType.Or -> {
                val childSqls =
                        criteria.children.filterNotNull().map {
                            renderCriteriaDirect(wrapper, support, it)
                        }.filter { it.isNotEmpty() }
                childSqls.joinToString(" OR ")
            }
            com.kotlinorm.enums.ConditionType.Root -> {
                val childSqls =
                        criteria.children.filterNotNull().map {
                            renderCriteriaDirect(wrapper, support, it)
                        }.filter { it.isNotEmpty() }
                childSqls.joinToString(" AND ")
            }
            com.kotlinorm.enums.ConditionType.Regexp ->
                    "${criteria.field.quoted(wrapper)} REGEXP :${criteria.field.name}Pattern"
            com.kotlinorm.enums.ConditionType.Sql -> criteria.value as? String
                            ?: criteria.field.quoted(wrapper)
            else ->
                    "${criteria.field.quoted(wrapper)} ${criteria.type.value} :${criteria.field.name}"
        }
        
        // 处理 not 属性
        return if (criteria.not && baseSql.isNotEmpty()) {
            when (criteria.type) {
                com.kotlinorm.enums.ConditionType.IsNull -> baseSql.replace("IS NULL", "IS NOT NULL")
                com.kotlinorm.enums.ConditionType.Like -> baseSql.replace("LIKE", "NOT LIKE")
                com.kotlinorm.enums.ConditionType.In -> baseSql.replace("IN", "NOT IN")
                com.kotlinorm.enums.ConditionType.Between -> baseSql.replace("BETWEEN", "NOT BETWEEN")
                com.kotlinorm.enums.ConditionType.Regexp -> baseSql.replace("REGEXP", "NOT REGEXP")
                com.kotlinorm.enums.ConditionType.Equal -> baseSql.replace("=", "!=")
                else -> "NOT ($baseSql)"
            }
        } else {
            baseSql
        }
    }

    private fun renderCriteriaWithParams(
            wrapper: KronosDataSourceWrapper,
            support: DatabasesSupport,
            criteria: Criteria,
            params: MutableMap<String, Any?> = mutableMapOf(),
            keyCounter: KeyCounterState = KeyCounterState()
    ): Pair<String, MutableMap<String, Any?>> {
        val sql = renderCriteriaDirect(wrapper, support, criteria).trim()
        fun collect(c: Criteria?) {
            if (c == null) return
            val field = c.field
            val v = c.value
            when (c.type) {
                com.kotlinorm.enums.ConditionType.Equal -> {
                    if (v !is Field) {
                        val key = safeKey(field.name, keyCounter, params, c)
                        if (v != null && v !is FunctionField) params[key] = v
                    }
                }
                com.kotlinorm.enums.ConditionType.Like -> {
                    val key = safeKey(field.name, keyCounter, params, c)
                    if (v != null && v !is com.kotlinorm.beans.dsl.FunctionField) params[key] = v
                }
                com.kotlinorm.enums.ConditionType.In -> {
                    val key = safeKey(field.name + "List", keyCounter, params, c)
                    if (v != null && v !is List<*> || (v is List<*> && v.isNotEmpty())) {
                        params[key] = v
                    }
                }
                com.kotlinorm.enums.ConditionType.Gt, com.kotlinorm.enums.ConditionType.Ge -> {
                    val key = safeKey(field.name + "Min", keyCounter, params, c)
                    if (v != null && v !is com.kotlinorm.beans.dsl.FunctionField) params[key] = v
                }
                com.kotlinorm.enums.ConditionType.Lt, com.kotlinorm.enums.ConditionType.Le -> {
                    val key = safeKey(field.name + "Max", keyCounter, params, c)
                    if (v != null && v !is com.kotlinorm.beans.dsl.FunctionField) params[key] = v
                }
                com.kotlinorm.enums.ConditionType.Between -> {
                    val range = v as? ClosedRange<*>
                    if (range != null) {
                        val k1 = safeKey(field.name + "Min", keyCounter, params, c)
                        val k2 = safeKey(field.name + "Max", keyCounter, params, c)
                        params[k1] = range.start
                        params[k2] = range.endInclusive
                    }
                }
                com.kotlinorm.enums.ConditionType.Regexp -> {
                    val key = safeKey(field.name + "Pattern", keyCounter, params, c)
                    if (v != null) params[key] = v
                }
                com.kotlinorm.enums.ConditionType.And,
                com.kotlinorm.enums.ConditionType.Or,
                com.kotlinorm.enums.ConditionType.Root -> {
                    c.children.forEach { collect(it) }
                }
                else -> {}
            }
        }
        collect(criteria)
        return sql to params
    }

    private fun safeKey(
            keyName: String,
            keyCounters: KeyCounterState,
            dataMap: MutableMap<String, Any?>,
            data: Any
    ): String {
        if (!keyCounters.initialized) {
            keyCounters.initialized = true
            dataMap.keys.forEach { key ->
                val (k, c) =
                        if (key.contains("@")) {
                            val split = key.split("@")
                            split[0] to split[1].toInt()
                        } else {
                            key to 0
                        }
                keyCounters.metaOfMap.getOrPut(k) { mutableMapOf() }[c] = dataMap[key]
            }
        }
        val value =
                when (data) {
                    is Criteria -> data.value
                    else -> data
                }
        val keyCount = keyCounters.metaOfMap[keyName]?.toList()?.firstOrNull { it.second == value }
        return if (keyCount == null) {
            val counter = keyCounters.metaOfMap[keyName]?.keys?.maxOrNull() ?: -1
            keyCounters.metaOfMap.getOrPut(keyName) { mutableMapOf() }[counter + 1] = dataMap
            if (counter + 1 == 0) keyName else "$keyName@${counter + 1}"
        } else {
            if (keyCount.first == 0) keyName else "$keyName@${keyCount.first}"
        }
    }
}
