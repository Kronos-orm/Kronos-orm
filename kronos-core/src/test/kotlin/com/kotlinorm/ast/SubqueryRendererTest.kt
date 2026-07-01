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

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.database.mysql.MysqlSqlRenderer
import com.kotlinorm.database.mssql.MssqlSqlRenderer
import com.kotlinorm.database.oracle.OracleSqlRenderer
import com.kotlinorm.database.postgres.PostgresqlSqlRenderer
import com.kotlinorm.database.sqlite.SqliteSqlRenderer
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.enums.SortType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SubqueryRendererTest {
    private val renderer = MysqlSqlRenderer()

    @Test
    fun `render scalar subquery in select list`() {
        val orders = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("amount", "o"), null)),
            from = TableName(table = "orders", alias = "o"),
            where = BinaryExpression(col("user_id", "o"), SqlOperator.EQUAL, col("id", "u")),
            limit = LimitClause(limit = 1, offset = null)
        )
        val users = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("id", "u"), null),
                SelectItem.ExpressionSelectItem(SubqueryExpression.ScalarSubquery(orders), "lastOrderAmount")
            ),
            from = TableName(table = "users", alias = "u")
        )

        val sql = renderer.render(users).sql

        assertTrue(sql.contains("(SELECT"))
        assertTrue(sql.contains("AS `lastOrderAmount`"))
        assertTrue(sql.contains("LIMIT 1"))
    }

    @Test
    fun `render exists and not exists predicates`() {
        val orders = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "o"), null)),
            from = TableName(table = "orders", alias = "o"),
            where = BinaryExpression(col("user_id", "o"), SqlOperator.EQUAL, col("id", "u"))
        )
        val users = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "u"), null)),
            from = TableName(table = "users", alias = "u"),
            where = BinaryExpression(
                SubqueryExpression.ExistsExpression(orders),
                SqlOperator.AND,
                SubqueryExpression.ExistsExpression(orders, not = true)
            )
        )

        val sql = renderer.render(users).sql

        assertTrue(sql.contains("EXISTS (SELECT"))
        assertTrue(sql.contains("NOT EXISTS (SELECT"))
    }

    @Test
    fun `render row value in subquery`() {
        val latestOrders = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("user_id", "o"), null),
                SelectItem.ExpressionSelectItem(FunctionCall("MAX", listOf(col("created_at", "o"))), "created_at")
            ),
            from = TableName(table = "orders", alias = "o"),
            groupBy = mutableListOf(col("user_id", "o"))
        )
        val users = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "u"), null)),
            from = TableName(table = "users", alias = "u"),
            where = SpecialExpression.InSubqueryExpression(
                RowValueExpression(listOf(col("id", "u"), col("created_at", "u"))),
                latestOrders
            )
        )

        val sql = renderer.render(users).sql

        assertTrue(sql.contains("(`u`.`id`, `u`.`created_at`) IN (SELECT"))
    }

    @Test
    fun `row value rejects single expression`() {
        assertFailsWith<IllegalArgumentException> {
            RowValueExpression(listOf(col("id")))
        }
    }

    @Test
    fun `in subquery validates column count`() {
        val oneColumn = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("user_id", "o"), null)),
            from = TableName(table = "orders", alias = "o")
        )
        val twoColumns = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("user_id", "o"), null),
                SelectItem.ColumnSelectItem(col("created_at", "o"), null)
            ),
            from = TableName(table = "orders", alias = "o")
        )

        val tupleMismatch = assertFailsWith<IllegalArgumentException> {
            renderer.render(
                SelectStatement(
                    selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "u"), null)),
                    from = TableName(table = "users", alias = "u"),
                    where = SpecialExpression.InSubqueryExpression(
                        RowValueExpression(listOf(col("id", "u"), col("created_at", "u"))),
                        oneColumn
                    )
                )
            )
        }
        assertEquals(
            "IN subquery column count mismatch: left side has 2 column(s), but subquery selects 1 column(s).",
            tupleMismatch.message
        )

        val scalarMismatch = assertFailsWith<IllegalArgumentException> {
            renderer.render(
                SelectStatement(
                    selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "u"), null)),
                    from = TableName(table = "users", alias = "u"),
                    where = SpecialExpression.InSubqueryExpression(col("id", "u"), twoColumns)
                )
            )
        }
        assertEquals(
            "IN subquery column count mismatch: left side has 1 column(s), but subquery selects 2 column(s).",
            scalarMismatch.message
        )
    }

    @Test
    fun `wrap select statement with outer filter`() {
        val inner = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("id", "u"), null),
                SelectItem.ExpressionSelectItem(col("amount", "o"), "lastOrderAmount")
            ),
            from = TableName(table = "users", alias = "u")
        )
        val outer = inner.wrapWithOuterFilter(
            alias = "q",
            outerWhere = BinaryExpression(col("lastOrderAmount", "q"), SqlOperator.GREATER_THAN, Literal.NumberLiteral("100")),
            outerOrderBy = mutableListOf(OrderByItem(col("lastOrderAmount", "q"), SortType.DESC))
        )

        val sql = renderer.render(outer).sql

        assertTrue(sql.contains("FROM (SELECT"))
        assertTrue(sql.contains("AS `q`"))
        assertTrue(sql.contains("WHERE `q`.`lastOrderAmount` > 100"))
        assertTrue(sql.contains("ORDER BY `q`.`lastOrderAmount` DESC"))
        assertEquals(listOf("id", "lastOrderAmount"), inner.projectFromAlias("q").map {
            (it as SelectItem.ColumnSelectItem).column.columnName
        })
    }

    @Test
    fun `lower deferred scalar subquery before rendering`() {
        val queryRef = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(SelectItem.ColumnSelectItem(col("amount", "o"), null)),
                from = TableName(table = "orders", alias = "o"),
                limit = LimitClause(limit = 1, offset = null)
            )
        }
        val statement = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ExpressionSelectItem(DeferredSubqueryExpression.Scalar(queryRef), "amount")
            ),
            from = TableName(table = "users", alias = "u")
        )

        val lowered = SubqueryLowering.lower(statement)
        val sql = renderer.render(lowered).sql

        assertTrue(sql.contains("(SELECT"))
        assertTrue(sql.contains("AS `amount`"))
    }

    @Test
    fun `internal deferred subquery builders create lowerable expressions`() {
        val queryRef = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(SelectItem.ColumnSelectItem(col("user_id", "o"), null)),
                from = TableName(table = "orders", alias = "o"),
                limit = LimitClause(limit = 1, offset = null)
            )
        }

        val scalar = queryRef.toScalarSubqueryExpression()
        val exists = queryRef.toExistsSubqueryExpression()
        val notExists = queryRef.toNotExistsSubqueryExpression()
        val inSubquery = col("id", "u").toInSubqueryExpression(queryRef)
        val notInSubquery = col("id", "u").toNotInSubqueryExpression(queryRef)

        assertTrue(SubqueryLowering.lowerExpression(scalar) is SubqueryExpression.ScalarSubquery)
        assertTrue(SubqueryLowering.lowerExpression(exists) is SubqueryExpression.ExistsExpression)
        assertTrue((SubqueryLowering.lowerExpression(notExists) as SubqueryExpression.ExistsExpression).not)
        assertTrue(SubqueryLowering.lowerExpression(inSubquery) is SpecialExpression.InSubqueryExpression)
        assertTrue((SubqueryLowering.lowerExpression(notInSubquery) as SpecialExpression.InSubqueryExpression).not)
    }

    @Test
    fun `selectable query ref passes materialize parameter map`() {
        val selectable = ParameterCollectingSelectable()
        val params = mutableMapOf<String, Any?>()

        KSelectableQueryRef(selectable).materialize(QueryMaterializeContext(parameterValues = params))

        assertEquals(1, params["fromSelectable"])
    }

    @Test
    fun `lower deferred subqueries inside derived table source`() {
        val existsRef = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "o"), null)),
                from = TableName(table = "orders", alias = "o")
            )
        }
        val derived = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "u"), null)),
            from = TableName(table = "users", alias = "u"),
            where = DeferredSubqueryExpression.Exists(existsRef)
        )
        val statement = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "q"), null)),
            from = SubqueryTable(derived, "q")
        )

        val lowered = SubqueryLowering.lower(statement)
        val loweredSource = lowered.from as SubqueryTable
        val loweredWhere = loweredSource.subquery.where

        assertTrue(loweredWhere is SubqueryExpression.ExistsExpression)
        assertTrue(renderer.render(lowered).sql.contains("FROM (SELECT"))
    }

    @Test
    fun `lower deferred subqueries inside joined table source and condition`() {
        val scalarRef = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "o"), null)),
                from = TableName(table = "orders", alias = "o"),
                limit = LimitClause(limit = 1, offset = null)
            )
        }
        val right = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("user_id", "s"), null)),
            from = TableName(table = "scores", alias = "s"),
            where = BinaryExpression(
                col("user_id", "s"),
                SqlOperator.EQUAL,
                DeferredSubqueryExpression.Scalar(scalarRef)
            )
        )
        val statement = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "u"), null)),
            from = JoinTable(
                left = TableName(table = "users", alias = "u"),
                joinType = com.kotlinorm.enums.JoinType.LEFT_JOIN,
                right = SubqueryTable(right, "sq"),
                condition = BinaryExpression(
                    col("id", "u"),
                    SqlOperator.EQUAL,
                    DeferredSubqueryExpression.Scalar(scalarRef)
                )
            )
        )

        val lowered = SubqueryLowering.lower(statement)
        val join = lowered.from as JoinTable
        val rightSource = join.right as SubqueryTable
        val rightWhere = (rightSource.subquery.where as BinaryExpression).right
        val joinRight = (join.condition as BinaryExpression).right

        assertTrue(rightWhere is SubqueryExpression.ScalarSubquery)
        assertTrue(joinRight is SubqueryExpression.ScalarSubquery)
    }

    @Test
    fun `render nested scalar subquery`() {
        val inner = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("amount", "p"), null)),
            from = TableName(table = "payments", alias = "p"),
            limit = LimitClause(limit = 1, offset = null)
        )
        val middle = SelectStatement(
            selectList = mutableListOf(SelectItem.ExpressionSelectItem(SubqueryExpression.ScalarSubquery(inner), "amount")),
            from = TableName(table = "orders", alias = "o"),
            limit = LimitClause(limit = 1, offset = null)
        )
        val outer = SelectStatement(
            selectList = mutableListOf(SelectItem.ExpressionSelectItem(SubqueryExpression.ScalarSubquery(middle), "lastPayment")),
            from = TableName(table = "users", alias = "u")
        )

        val sql = renderer.render(outer).sql

        assertTrue(sql.contains("(SELECT (SELECT"))
        assertTrue(sql.contains("AS `lastPayment`"))
    }

    @Test
    fun `render quantified comparisons`() {
        val orderTotals = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("amount", "o"), null)),
            from = TableName(table = "orders", alias = "o")
        )
        val users = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "u"), null)),
            from = TableName(table = "users", alias = "u"),
            where = BinaryExpression(
                SubqueryExpression.QuantifiedComparison(
                    col("credit", "u"),
                    SqlOperator.GREATER_THAN,
                    SubqueryExpression.Quantifier.ANY,
                    orderTotals
                ),
                SqlOperator.AND,
                SubqueryExpression.QuantifiedComparison(
                    col("quota", "u"),
                    SqlOperator.GREATER_THAN_OR_EQUAL,
                    SubqueryExpression.Quantifier.ALL,
                    orderTotals
                )
            )
        )

        val sql = renderer.render(users).sql

        assertTrue(sql.contains("`u`.`credit` > ANY (SELECT"))
        assertTrue(sql.contains("`u`.`quota` >= ALL (SELECT"))
    }

    @Test
    fun `render order by scalar subquery and selected alias`() {
        val orders = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("amount", "o"), null)),
            from = TableName(table = "orders", alias = "o"),
            limit = LimitClause(limit = 1, offset = null)
        )
        val users = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("id", "u"), null),
                SelectItem.ExpressionSelectItem(col("name", "u"), "displayName")
            ),
            from = TableName(table = "users", alias = "u"),
            orderBy = mutableListOf(
                OrderByItem(SubqueryExpression.ScalarSubquery(orders), SortType.DESC),
                OrderByItem(col("displayName"), SortType.ASC)
            )
        )

        val sql = renderer.render(users).sql

        assertTrue(sql.contains("ORDER BY (SELECT"))
        assertTrue(sql.contains("`displayName` ASC"))
    }

    @Test
    fun `criteria converter accepts structured subquery predicates`() {
        val oneColumn = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(SelectItem.ColumnSelectItem(col("user_id", "o"), null)),
                from = TableName(table = "orders", alias = "o"),
                limit = LimitClause(limit = 1, offset = null)
            )
        }

        val scalar = Criteria(Field("amount"), ConditionType.GT, value = CriteriaSubqueryValue.Scalar(oneColumn))
        val scalarExpression = CriteriaToAstConverter.convert(scalar)
        assertTrue(scalarExpression is BinaryExpression)
        assertTrue((scalarExpression as BinaryExpression).right is DeferredSubqueryExpression.Scalar)

        val exists = Criteria(type = ConditionType.SQL, value = CriteriaSubqueryValue.Exists(oneColumn))
        assertTrue(CriteriaToAstConverter.convert(exists) is DeferredSubqueryExpression.Exists)

        val notIn = Criteria(Field("id"), ConditionType.IN, not = true, value = CriteriaSubqueryValue.In(oneColumn))
        val notInExpression = CriteriaToAstConverter.convert(notIn)
        assertTrue(notInExpression is DeferredSubqueryExpression.In)
        assertTrue((notInExpression as DeferredSubqueryExpression.In).not)

        val tupleIn = Criteria(
            Field("ignored"),
            ConditionType.IN,
            value = CriteriaSubqueryValue.In(
                query = oneColumn,
                value = RowValueExpression(listOf(col("id", "u"), col("created_at", "u")))
            )
        )
        val tupleExpression = CriteriaToAstConverter.convert(tupleIn)
        assertTrue((tupleExpression as DeferredSubqueryExpression.In).value is RowValueExpression)

        val quantified = Criteria(
            Field("amount"),
            ConditionType.LE,
            value = CriteriaSubqueryValue.QuantifiedComparison(oneColumn, SubqueryExpression.Quantifier.SOME)
        )
        assertTrue(CriteriaToAstConverter.convert(quantified) is DeferredSubqueryExpression.QuantifiedComparison)
    }

    @Test
    fun `automatic layering moves selected alias predicates to outer query`() {
        val statement = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("id", "u"), null),
                SelectItem.ExpressionSelectItem(
                    FunctionCall("MAX", listOf(col("amount", "o"))),
                    "maxAmount"
                )
            ),
            from = TableName(table = "users", alias = "u"),
            where = BinaryExpression(
                BinaryExpression(col("id"), SqlOperator.GREATER_THAN, Literal.NumberLiteral("10")),
                SqlOperator.AND,
                BinaryExpression(col("maxAmount"), SqlOperator.GREATER_THAN, Literal.NumberLiteral("100"))
            ),
            orderBy = mutableListOf(OrderByItem(col("maxAmount"), SortType.DESC))
        )

        val layered = statement.applyAutomaticLayering("q")
        val sql = renderer.render(layered).sql

        assertTrue(sql.contains("FROM (SELECT"))
        assertTrue(sql.contains("WHERE `id` > 10"))
        assertTrue(sql.contains("WHERE `q`.`maxAmount` > 100"))
        assertTrue(sql.contains("ORDER BY `q`.`maxAmount` DESC"))
    }

    @Test
    fun `render update set scalar subquery and where subqueries`() {
        val latestOrderAmount = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(SelectItem.ColumnSelectItem(col("amount", "o"), null)),
                from = TableName(table = "orders", alias = "o"),
                where = BinaryExpression(col("user_id", "o"), SqlOperator.EQUAL, col("id", "u")),
                limit = LimitClause(limit = 1, offset = null)
            )
        }
        val activeOrders = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "o"), null)),
                from = TableName(table = "orders", alias = "o"),
                where = BinaryExpression(col("user_id", "o"), SqlOperator.EQUAL, col("id", "u"))
            )
        }
        val statement = UpdateStatement(
            table = TableName(table = "users", alias = "u"),
            assignments = mutableListOf(
                Assignment(col("last_order_amount"), DeferredSubqueryExpression.Scalar(latestOrderAmount))
            ),
            where = BinaryExpression(
                DeferredSubqueryExpression.Exists(activeOrders),
                SqlOperator.AND,
                DeferredSubqueryExpression.In(col("id", "u"), activeOrders)
            )
        )

        val sql = renderer.render(statement).sql

        assertTrue(sql.contains("UPDATE `users` AS `u` SET `last_order_amount` = (SELECT"))
        assertTrue(sql.contains("WHERE EXISTS (SELECT"))
        assertTrue(sql.contains("`u`.`id` IN (SELECT"))
    }

    @Test
    fun `render delete where exists in and scalar subqueries`() {
        val orders = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(SelectItem.ColumnSelectItem(col("user_id", "o"), null)),
                from = TableName(table = "orders", alias = "o"),
                where = BinaryExpression(col("user_id", "o"), SqlOperator.EQUAL, col("id", "u"))
            )
        }
        val latestAmount = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(SelectItem.ColumnSelectItem(col("amount", "o"), null)),
                from = TableName(table = "orders", alias = "o"),
                where = BinaryExpression(col("user_id", "o"), SqlOperator.EQUAL, col("id", "u")),
                limit = LimitClause(limit = 1, offset = null)
            )
        }
        val statement = DeleteStatement(
            table = TableName(table = "users", alias = "u"),
            where = BinaryExpression(
                BinaryExpression(
                    DeferredSubqueryExpression.Exists(orders),
                    SqlOperator.AND,
                    DeferredSubqueryExpression.In(col("id", "u"), orders, not = true)
                ),
                SqlOperator.AND,
                BinaryExpression(
                    DeferredSubqueryExpression.Scalar(latestAmount),
                    SqlOperator.GREATER_THAN,
                    Literal.NumberLiteral("100")
                )
            )
        )

        val sql = renderer.render(statement).sql

        assertTrue(sql.contains("DELETE FROM `users` AS `u` WHERE EXISTS (SELECT"))
        assertTrue(sql.contains("`u`.`id` NOT IN (SELECT"))
        assertTrue(sql.contains("(SELECT `o`.`amount` FROM `orders` AS `o`"))
    }

    @Test
    fun `render insert select source`() {
        val source = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("id", "u"), null),
                SelectItem.ColumnSelectItem(col("name", "u"), null)
            ),
            from = TableName(table = "users", alias = "u"),
            where = BinaryExpression(col("active", "u"), SqlOperator.EQUAL, Literal.BooleanLiteral(true))
        )
        val statement = InsertStatement(
            table = TableName(table = "archived_users"),
            columns = listOf(col("id"), col("name")),
            values = emptyList(),
            source = source
        )

        val sql = renderer.render(statement).sql

        assertEquals(
            "INSERT INTO `archived_users` (`id`, `name`) SELECT `u`.`id`, `u`.`name` FROM `users` AS `u` WHERE `u`.`active` = true",
            sql
        )
    }

    @Test
    fun `render mysql upsert scalar subquery assignment`() {
        val score = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(SelectItem.ColumnSelectItem(col("score", "s"), null)),
                from = TableName(table = "scores", alias = "s"),
                where = BinaryExpression(col("user_id", "s"), SqlOperator.EQUAL, col("id")),
                limit = LimitClause(limit = 1, offset = null)
            )
        }
        val statement = InsertStatement(
            table = TableName(table = "users"),
            columns = listOf(col("id"), col("score")),
            values = listOf(Parameter.NamedParameter("id"), Parameter.NamedParameter("score")),
            conflictAssignments = listOf(
                Assignment(col("score"), DeferredSubqueryExpression.Scalar(score))
            )
        )

        val sql = renderer.render(statement).sql

        assertTrue(sql.contains("ON DUPLICATE KEY UPDATE `score` = (SELECT"))
        assertTrue(sql.contains("LIMIT 1"))
    }

    @Test
    fun `render create table as select`() {
        val source = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("id", "u"), null),
                SelectItem.ColumnSelectItem(col("name", "u"), null)
            ),
            from = TableName(table = "users", alias = "u")
        )
        val statement = DdlStatement.CreateTableAsSelectStatement(
            tableName = "active_users",
            query = source
        )

        val sql = renderer.render(statement).sql

        assertEquals(
            "CREATE TABLE IF NOT EXISTS `active_users` AS SELECT `u`.`id`, `u`.`name` FROM `users` AS `u`",
            sql
        )
    }

    @Test
    fun `render subquery order update delete and insert select across dialects`() {
        val statements = listOf(
            orderByScalarSubqueryStatement(),
            updateSetAndWhereSubqueryStatement(),
            deleteWhereSubqueryStatement(),
            insertSelectStatement()
        )

        dialectRenderers().forEach { (name, dialectRenderer) ->
            statements.forEach { statement ->
                val sql = dialectRenderer.render(statement).sql

                assertTrue(sql.contains("SELECT"), "$name should render nested SELECT: $sql")
                assertTrue(!sql.contains("DeferredSubqueryExpression"), "$name should lower deferred nodes: $sql")
            }
        }
    }

    @Test
    fun `render expression upsert matrix`() {
        val statement = upsertScalarSubqueryStatement()

        val mysqlSql = MysqlSqlRenderer().render(statement).sql
        assertTrue(mysqlSql.contains("ON DUPLICATE KEY UPDATE `score` = (SELECT"))

        val postgresSql = PostgresqlSqlRenderer().render(statement).sql
        assertTrue(postgresSql.contains("ON CONFLICT (\"id\") DO UPDATE SET \"score\" = (SELECT"))

        val sqliteSql = SqliteSqlRenderer().render(statement).sql
        assertTrue(sqliteSql.contains("ON CONFLICT (\"id\") DO UPDATE SET \"score\" = (SELECT"))

        assertFailsWith<IllegalStateException> {
            MssqlSqlRenderer().render(statement)
        }
        assertFailsWith<IllegalStateException> {
            OracleSqlRenderer().render(statement)
        }
    }

    @Test
    fun `render create table as select matrix`() {
        val source = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("id", "u"), null),
                SelectItem.ColumnSelectItem(col("name", "u"), null)
            ),
            from = TableName(table = "users", alias = "u")
        )

        val mysqlSql = MysqlSqlRenderer().render(
            DdlStatement.CreateTableAsSelectStatement("active_users", source)
        ).sql
        assertEquals(
            "CREATE TABLE IF NOT EXISTS `active_users` AS SELECT `u`.`id`, `u`.`name` FROM `users` AS `u`",
            mysqlSql
        )

        val postgresSql = PostgresqlSqlRenderer().render(
            DdlStatement.CreateTableAsSelectStatement("active_users", source)
        ).sql
        assertEquals(
            "CREATE TABLE IF NOT EXISTS \"active_users\" AS SELECT \"u\".\"id\", \"u\".\"name\" FROM \"users\" AS \"u\"",
            postgresSql
        )

        val sqliteSql = SqliteSqlRenderer().render(
            DdlStatement.CreateTableAsSelectStatement("active_users", source)
        ).sql
        assertEquals(
            "CREATE TABLE IF NOT EXISTS \"active_users\" AS SELECT \"u\".\"id\", \"u\".\"name\" FROM \"users\" AS \"u\"",
            sqliteSql
        )

        val mssqlSql = MssqlSqlRenderer().render(
            DdlStatement.CreateTableAsSelectStatement("active_users", source)
        ).sql
        assertEquals(
            "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[active_users]') AND type in (N'U')) BEGIN SELECT [u].[id], [u].[name] INTO [dbo].[active_users] FROM [users] AS [u]; END;",
            mssqlSql
        )

        val oracleSql = OracleSqlRenderer().render(
            DdlStatement.CreateTableAsSelectStatement("active_users", source, ifNotExists = false)
        ).sql
        assertEquals(
            "CREATE TABLE \"ACTIVE_USERS\" AS SELECT \"u\".\"id\", \"u\".\"name\" FROM \"users\" AS \"u\"",
            oracleSql
        )

        assertFailsWith<IllegalArgumentException> {
            OracleSqlRenderer().render(DdlStatement.CreateTableAsSelectStatement("active_users", source))
        }
    }

    private fun dialectRenderers(): List<Pair<String, AbstractSqlRenderer>> {
        return listOf(
            "MySQL" to MysqlSqlRenderer(),
            "PostgreSQL" to PostgresqlSqlRenderer(),
            "SQLite" to SqliteSqlRenderer(),
            "MSSQL" to MssqlSqlRenderer(),
            "Oracle" to OracleSqlRenderer()
        )
    }

    private fun orderByScalarSubqueryStatement(): SelectStatement {
        val maxScore = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ExpressionSelectItem(FunctionCall("MAX", listOf(col("score", "s"))), "score")
            ),
            from = TableName(table = "scores", alias = "s"),
            where = BinaryExpression(col("user_id", "s"), SqlOperator.EQUAL, col("id", "u"))
        )
        return SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id", "u"), null)),
            from = TableName(table = "users", alias = "u"),
            orderBy = mutableListOf(OrderByItem(SubqueryExpression.ScalarSubquery(maxScore), SortType.DESC))
        )
    }

    private fun updateSetAndWhereSubqueryStatement(): UpdateStatement {
        val maxScore = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(
                    SelectItem.ExpressionSelectItem(FunctionCall("MAX", listOf(col("score", "s"))), "score")
                ),
                from = TableName(table = "scores", alias = "s"),
                where = BinaryExpression(col("user_id", "s"), SqlOperator.EQUAL, col("id", "u"))
            )
        }
        val activeScores = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(SelectItem.ColumnSelectItem(col("user_id", "s"), null)),
                from = TableName(table = "scores", alias = "s")
            )
        }
        return UpdateStatement(
            table = TableName(table = "users", alias = "u"),
            assignments = mutableListOf(Assignment(col("score"), DeferredSubqueryExpression.Scalar(maxScore))),
            where = DeferredSubqueryExpression.In(col("id", "u"), activeScores)
        )
    }

    private fun deleteWhereSubqueryStatement(): DeleteStatement {
        val inactiveUsers = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(SelectItem.ColumnSelectItem(col("user_id", "s"), null)),
                from = TableName(table = "scores", alias = "s"),
                where = BinaryExpression(col("active", "s"), SqlOperator.EQUAL, Literal.BooleanLiteral(false))
            )
        }
        return DeleteStatement(
            table = TableName(table = "users", alias = "u"),
            where = DeferredSubqueryExpression.In(col("id", "u"), inactiveUsers, not = true)
        )
    }

    private fun insertSelectStatement(): InsertStatement {
        val source = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("id", "u"), null),
                SelectItem.ColumnSelectItem(col("name", "u"), null)
            ),
            from = TableName(table = "users", alias = "u")
        )
        return InsertStatement(
            table = TableName(table = "archived_users"),
            columns = listOf(col("id"), col("name")),
            values = emptyList(),
            source = source
        )
    }

    private fun upsertScalarSubqueryStatement(): InsertStatement {
        val score = SelectQueryRef {
            SelectStatement(
                selectList = mutableListOf(
                    SelectItem.ExpressionSelectItem(FunctionCall("MAX", listOf(col("score", "s"))), "score")
                ),
                from = TableName(table = "scores", alias = "s"),
                where = BinaryExpression(col("user_id", "s"), SqlOperator.EQUAL, col("id"))
            )
        }
        return InsertStatement(
            table = TableName(table = "users"),
            columns = listOf(col("id"), col("score")),
            values = listOf(Parameter.NamedParameter("id"), Parameter.NamedParameter("score")),
            conflictResolver = com.kotlinorm.database.ConflictResolver(
                tableName = "users",
                onFields = [Field("id", "id")],
                toUpdateFields = [Field("score", "score")],
                toInsertFields = [Field("id", "id"), Field("score", "score")]
            ),
            conflictAssignments = listOf(
                Assignment(col("score"), DeferredSubqueryExpression.Scalar(score))
            )
        )
    }

    private fun col(name: String, table: String? = null): ColumnReference {
        return ColumnReference(database = null, tableAlias = table, columnName = name)
    }

}

internal class ParameterCollectingPojo : KPojo {
    override fun toDataMap(): MutableMap<String, Any?> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    override fun <T : KPojo> fromMapData(map: Map<String, Any?>): T = this as T

    override var __tableName: String = "parameter_collecting"
}

internal class ParameterCollectingSelectable :
    KSelectable<ParameterCollectingPojo>(ParameterCollectingPojo(), ParameterCollectingPojo::class) {
    override var selectAll: Boolean = false
    override var pageEnabled: Boolean = false
    override var limitCapacity: Int = 0

    override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        error("not used")
    }

    override fun toStatement(wrapper: KronosDataSourceWrapper?): SelectStatement {
        return toStatement(wrapper, mutableMapOf())
    }

    override fun toStatement(
        wrapper: KronosDataSourceWrapper?,
        parameterValues: MutableMap<String, Any?>
    ): SelectStatement {
        parameterValues["fromSelectable"] = 1
        return SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(ColumnReference(tableAlias = null, columnName = "id"), null)
            ),
            from = TableName(table = "parameter_collecting")
        )
    }
}
