/** Copyright 2022-2025 kronos-orm */
package com.kotlinorm.ast

import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.enums.KColumnType.CUSTOM_CRITERIA_SQL

/** SELECT ... statement (mutable for easier DSL updates) */
class SelectStatement(
        var with: MutableList<CommonTableExpression> = mutableListOf(),
        var projections: MutableList<ProjectionItem> = mutableListOf(),
        var from: TableSource? = null,
        var where: Expression? = null,
        var groupBy: MutableList<Expression> = mutableListOf(),
        var having: Expression? = null,
        var orderBy: MutableList<OrderItem> = mutableListOf(),
        var distinct: Boolean = false,
        var limit: Int? = null,
        var offset: Int? = null,
        var lock: PessimisticLock? = null,
        var selectAll: Boolean = true,
        // Set operations: UNION/INTERSECT/EXCEPT with another query chain
        var setOps: MutableList<Pair<SetOperator, SelectStatement>> = mutableListOf()
) : Statement

{
    // ---- Convenience mutators to simplify DSL side updates ----

    fun setFrom(
        table: String,
        alias: String? = null,
        database: String? = null,
        schema: String? = null
    ): SelectStatement {
        this.from = TableName(database = database, schema = schema, table = table, alias = alias)
        return this
    }

    fun setWhereCriteria(criteria: Criteria?): SelectStatement {
        this.where = criteria?.let { CriteriaExpr(it) }
        return this
    }

    fun setHavingCriteria(criteria: Criteria?): SelectStatement {
        this.having = criteria?.let { CriteriaExpr(it) }
        return this
    }

    fun setProjectionsFromFields(fields: Collection<Field>): SelectStatement {
        this.projections =
            fields.map { f ->
                val alias = f.name.takeIf { it.isNotBlank() && it != f.columnName }
                val expr = when {
                    f is FunctionField -> {
                        // FunctionField需要特殊处理，不能直接用字符串存储
                        // 在AST阶段保持FunctionField对象，等待后续渲染时使用wrapper和数据库类型信息
                        FunctionFieldExpr(f)
                    }
                    f.type == CUSTOM_CRITERIA_SQL -> {
                        RawSqlExpr(f.toString())
                    }
                    else -> {
                        ColumnRef(tableAlias = null, column = f.columnName, sourceField = f)
                    }
                }
                ProjectionItem(expression = expr, alias = alias)
            }.toMutableList()
        return this
    }

    fun setGroupByFromFields(fields: Collection<Field>): SelectStatement {
        this.groupBy =
            fields.map { fieldItem ->
                if (fieldItem.type == CUSTOM_CRITERIA_SQL) RawSqlExpr(fieldItem.toString())
                else ColumnRef(tableAlias = null, column = fieldItem.columnName, sourceField = fieldItem)
            }.toMutableList()
        return this
    }

    fun setOrderByFromPairs(pairs: Collection<Pair<Field, com.kotlinorm.enums.SortType>>): SelectStatement {
        this.orderBy =
            pairs.map { (fieldItem, sortType) ->
                val expr = if (fieldItem.type == CUSTOM_CRITERIA_SQL) {
                    RawSqlExpr(fieldItem.toString())
                } else {
                    ColumnRef(tableAlias = null, column = fieldItem.columnName, sourceField = fieldItem)
                }
                OrderItem(
                    expression = expr,
                    direction = when (sortType) {
                        com.kotlinorm.enums.SortType.ASC -> OrderDirection.asc
                        com.kotlinorm.enums.SortType.DESC -> OrderDirection.desc
                    }
                )
            }.toMutableList()
        return this
    }

    /**
     * Set pagination using page index and size. It computes limit/offset and stores only those,
     * keeping the statement portable across dialects (renderer decides final SQL syntax).
     */
    fun setPage(index: Int, size: Int): SelectStatement {
        if (index > 0 && size > 0) {
            this.limit = size
            this.offset = ((index - 1).coerceAtLeast(0) * size)
        } else {
            this.limit = null
            this.offset = null
        }
        return this
    }
}

/** INSERT ... statement */
sealed interface InsertSource : SqlNode

data class ValuesSource(val rows: List<List<Expression>>) : InsertSource

data class SelectSource(val select: SelectStatement) : InsertSource

/** INSERT INTO target(columns...) source */
class InsertStatement(
        var target: TableName,
        var columns: MutableList<String> = mutableListOf(),
        var source: InsertSource
) : Statement {
    // Store fields that were previously in InsertClause
    var toInsertFields: MutableSet<Field> = mutableSetOf()
    
    fun addInsertField(field: Field): InsertStatement {
        this.toInsertFields.add(field)
        this.columns.add(field.columnName)
        return this
    }
}

/** UPDATE ... statement */
data class Assignment(val column: ColumnRef, val value: Expression) : SqlNode

/** UPDATE target [FROM ...] SET ... WHERE ... */
class UpdateStatement(
        var target: TableName,
        var set: MutableList<Assignment>,
        var from: TableSource? = null,
        var where: Expression? = null
) : Statement {
    // Store fields that were previously in UpdateClause
    var toUpdateFields: MutableSet<Field> = mutableSetOf()
    var plusAssigns: MutableList<Pair<Field, String>> = mutableListOf()
    var minusAssigns: MutableList<Pair<Field, String>> = mutableListOf()
    var condition: Criteria? = null
    
    fun setWhereCriteria(criteria: Criteria?): UpdateStatement {
        this.where = criteria?.let { CriteriaExpr(it) }
        this.condition = criteria
        return this
    }
    
    fun addUpdateField(field: Field): UpdateStatement {
        this.toUpdateFields.add(field)
        return this
    }
    
    fun addPlusAssign(field: Field, paramKey: String): UpdateStatement {
        this.plusAssigns.add(field to paramKey)
        return this
    }
    
    fun addMinusAssign(field: Field, paramKey: String): UpdateStatement {
        this.minusAssigns.add(field to paramKey)
        return this
    }
}

/** DELETE ... statement */
class DeleteStatement(
        var target: TableName,
        var using: TableSource? = null,
        var where: Expression? = null
) : Statement {
    // Store fields that were previously in DeleteClause
    var condition: Criteria? = null
    
    fun setWhereCriteria(criteria: Criteria?): DeleteStatement {
        this.where = criteria?.let { CriteriaExpr(it) }
        this.condition = criteria
        return this
    }
}

/** DDL minimal placeholder to represent table operations when needed by future generators. */
@Suppress("EnumEntryName")
enum class DdlType {
    createTable,
    dropTable,
    alterTable,
    createIndex,
    dropIndex,
    other
}

data class DdlStatement(val type: DdlType, val targetTable: TableName, val rawSql: String? = null) :
        Statement
