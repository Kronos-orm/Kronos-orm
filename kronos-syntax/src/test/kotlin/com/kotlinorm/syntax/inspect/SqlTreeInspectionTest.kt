/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.inspect

import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlCaseBranch
import com.kotlinorm.syntax.expr.SqlJsonArrayItem
import com.kotlinorm.syntax.expr.SqlJsonObjectItem
import com.kotlinorm.syntax.expr.SqlJsonPassing
import com.kotlinorm.syntax.expr.SqlMatchPhase
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlNthValueFromMode
import com.kotlinorm.syntax.expr.SqlQuantifiedComparisonOperator
import com.kotlinorm.syntax.expr.SqlSubqueryQuantifier
import com.kotlinorm.syntax.expr.SqlTimeUnit
import com.kotlinorm.syntax.expr.SqlTrim
import com.kotlinorm.syntax.expr.SqlTrimMode
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.expr.SqlUnaryOperator
import com.kotlinorm.syntax.expr.SqlWindow
import com.kotlinorm.syntax.expr.SqlWindowFrame
import com.kotlinorm.syntax.expr.SqlWindowFrameBound
import com.kotlinorm.syntax.expr.SqlWindowFrameUnit
import com.kotlinorm.syntax.expr.SqlWindowItem
import com.kotlinorm.syntax.render.col
import com.kotlinorm.syntax.render.eq
import com.kotlinorm.syntax.render.id
import com.kotlinorm.syntax.render.named
import com.kotlinorm.syntax.render.num
import com.kotlinorm.syntax.render.set
import com.kotlinorm.syntax.render.table
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlConflictTarget
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSetOperator
import com.kotlinorm.syntax.statement.SqlTableConstraint
import com.kotlinorm.syntax.statement.SqlUpdateSetPair
import com.kotlinorm.syntax.statement.SqlUpsertAction
import com.kotlinorm.syntax.table.SqlGraphPattern
import com.kotlinorm.syntax.table.SqlGraphPatternTerm
import com.kotlinorm.syntax.table.SqlGraphQuantifier
import com.kotlinorm.syntax.table.SqlGraphSymbol
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlJsonColumn
import com.kotlinorm.syntax.table.SqlMatchRecognize
import com.kotlinorm.syntax.table.SqlRecognizeMeasureItem
import com.kotlinorm.syntax.table.SqlRowPattern
import com.kotlinorm.syntax.table.SqlRowPatternDefineItem
import com.kotlinorm.syntax.table.SqlRowPatternQuantifier
import com.kotlinorm.syntax.table.SqlRowPatternTerm
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.syntax.token.SqlUnsafeToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class SqlTreeInspectionTest {
    @Test
    fun collectsNamedParametersThroughSubqueriesAndIgnoresUnsafeRawText() {
        val subquery = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(col("order_id"))),
            from = listOf(table("orders")),
            where = col("tenant_id").eq(named("tenantId"))
        )
        val query = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(named("tenantId"))),
            from = listOf(table("user")),
            where = SqlExpr.Binary(
                SqlExpr.In(col("id"), SqlInRightOperand.Subquery(subquery)),
                SqlBinaryOperator.And,
                SqlExpr.UnsafeRaw(":rawParam")
            )
        )

        assertEquals(listOf("tenantId", "tenantId"), SqlParameterCollector.collectNamedParameters(query))
    }

    @Test
    fun inspectsStatementTableWhereOutputsAndAliases() {
        val where = col("id").eq(named("id"))
        val query = SqlQuery.Select(
            select = listOf(
                SqlSelectItem.Expr(col("id")),
                SqlSelectItem.Expr(SqlExpr.Function(name = id("COUNT")), alias = "total")
            ),
            from = listOf(table("user")),
            where = where
        )
        val update = SqlDmlStatement.Update(
            table = table("user"),
            setPairs = listOf(set("name", named("name"))),
            where = where
        )

        val outputs = SqlStatementInspector.queryOutputs(query)

        assertEquals(id("user"), SqlStatementInspector.tableNameOrNull(update))
        assertSame(where, SqlStatementInspector.whereOrNull(query))
        assertSame(where, SqlStatementInspector.whereOrNull(update))
        assertEquals(listOf("id", "total"), outputs.map { it.outputName })
        assertEquals(id("id"), outputs.first().sourceColumn)
        assertEquals(listOf("total"), SqlStatementInspector.selectAliasRegistry(query).keys.toList())
    }

    @Test
    fun inspectsUpsertConflictAndUpdateWhereExpressions() {
        val conflictWhere = col("tenant_id").eq(named("tenantId"))
        val updateWhere = col("deleted").eq(num("0"))
        val upsert = SqlDmlStatement.Upsert(
            table = table("user"),
            columns = listOf(id("id"), id("name")),
            values = listOf(named("id"), named("name")),
            primaryKeys = listOf(id("id")),
            conflictTarget = SqlConflictTarget(columns = listOf(id("id")), where = conflictWhere),
            action = SqlUpsertAction.Update(
                setPairs = listOf(SqlUpdateSetPair(com.kotlinorm.syntax.statement.SqlAssignmentTarget.Column(id("name")), named("name"))),
                where = updateWhere
            )
        )

        val where = assertIs<SqlExpr.Binary>(SqlStatementInspector.whereOrNull(upsert))
        assertSame(conflictWhere, where.left)
        assertEquals(SqlBinaryOperator.And, where.operator)
        assertSame(updateWhere, where.right)
    }

    @Test
    fun rewritesNestedDmlValuesAndWhereExpressions() {
        val update = SqlDmlStatement.Update(
            table = table("user"),
            setPairs = listOf(set("score", num("1"))),
            where = col("id").eq(num("1"))
        )
        val rewritten = SqlTreeRewriter.rewriteStatement(update, object : SqlNodeRewriter {
            override fun rewriteExpr(expr: SqlExpr): SqlExpr =
                if (expr == num("1")) num("2") else SqlTreeRewriter.rewriteExpr(expr, this)
        })

        val rewrittenUpdate = assertIs<SqlDmlStatement.Update>(rewritten)
        assertEquals(num("2"), rewrittenUpdate.setPairs.single().value)
        assertEquals(num("2"), (rewrittenUpdate.where as SqlExpr.Binary).right)
    }

    @Test
    fun inspectsAndRewritesSelectLevelWindowAndQualify() {
        val query = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(named("selected"))),
            window = listOf(
                SqlWindowItem(
                    id("w"),
                    SqlWindow(
                        partitionBy = listOf(named("partition")),
                        frame = SqlWindowFrame.Between(
                            SqlWindowFrameUnit.Rows,
                            SqlWindowFrameBound.Preceding(named("start")),
                            SqlWindowFrameBound.Following(named("end"))
                        )
                    )
                )
            ),
            qualify = named("qualify")
        )

        val rewritten = SqlTreeRewriter.rewriteQuery(query, object : SqlNodeRewriter {
            override fun rewriteExpr(expr: SqlExpr): SqlExpr =
                if (expr is SqlExpr.Parameter && expr.parameter is com.kotlinorm.syntax.expr.SqlParameter.Named) {
                    named("${expr.parameter.name}_rewritten")
                } else {
                    SqlTreeRewriter.rewriteExpr(expr, this)
                }
        })

        val rewrittenSelect = assertIs<SqlQuery.Select>(rewritten)
        val frame = assertIs<SqlWindowFrame.Between>(rewrittenSelect.window.single().window.frame)

        assertEquals(
            listOf("selected", "partition", "start", "end", "qualify"),
            SqlParameterCollector.collectNamedParameters(query)
        )
        assertEquals(
            "qualify_rewritten",
            ((rewrittenSelect.qualify as SqlExpr.Parameter).parameter as com.kotlinorm.syntax.expr.SqlParameter.Named).name
        )
        assertEquals(
            "start_rewritten",
            (((frame.start as SqlWindowFrameBound.Preceding).n as SqlExpr.Parameter).parameter as com.kotlinorm.syntax.expr.SqlParameter.Named).name
        )
        assertEquals(
            "end_rewritten",
            (((frame.end as SqlWindowFrameBound.Following).n as SqlExpr.Parameter).parameter as com.kotlinorm.syntax.expr.SqlParameter.Named).name
        )
    }

    @Test
    fun inspectsQueryOutputEdgeCases() {
        val values = SqlQuery.Values(listOf(listOf(named("value"))))
        val select = SqlQuery.Select(
            select = listOf(
                SqlSelectItem.Asterisk(),
                SqlSelectItem.Asterisk("u"),
                SqlSelectItem.Expr(SqlExpr.UnsafeRaw("1"))
            )
        )
        val with = SqlQuery.With(
            withItems = listOf(com.kotlinorm.syntax.statement.SqlWithItem("v", listOf("id"), values)),
            query = select
        )
        val set = SqlQuery.Set(select, SqlSetOperator.Union(), values)

        assertNull(SqlStatementInspector.whereOrNull(SqlDmlStatement.Insert(table("user"), mode = SqlInsertMode.Values(listOf(listOf(named("id")))))))
        assertEquals(listOf(null, "u", "expr2"), SqlStatementInspector.queryOutputs(select).map { it.outputName })
        assertEquals(listOf(null, "u", "expr2"), SqlStatementInspector.queryOutputs(with).map { it.outputName })
        assertEquals(listOf(null, "u", "expr2"), SqlStatementInspector.queryOutputs(set).map { it.outputName })
        assertEquals(emptyList(), SqlStatementInspector.queryOutputs(values))
        assertEquals(emptyMap(), SqlStatementInspector.selectAliasRegistry(values))
    }

    @Test
    fun walksBroadSqlTreeUsedByCoreInspection() {
        val visited = mutableListOf<String>()

        broadSqlNodes().forEach { node ->
            SqlNodeWalker.walk(node) { visited += it::class.simpleName.orEmpty() }
        }

        assert("Parameter" in visited)
        assert("Json" in visited)
        assert("Graph" in visited)
        assert("Window" in visited)
        assert("Check" in visited)
    }

    @Test
    fun rewritesBroadSqlTreeUsedByCorePlanning() {
        val rewriter = object : SqlNodeRewriter {}

        broadSqlExpressions().forEach { SqlTreeRewriter.rewriteExpr(it, rewriter) }
        broadSqlTables().forEach { SqlTreeRewriter.rewriteTable(it, rewriter) }
        broadSqlStatements().forEach { SqlTreeRewriter.rewriteStatement(it, rewriter) }

        assertEquals(44, broadSqlExpressions().size)
    }

    private fun broadSqlNodes(): List<com.kotlinorm.syntax.SqlNode> =
        broadSqlExpressions() + broadSqlTables() + broadSqlStatements()

    private fun broadSqlStatements(): List<com.kotlinorm.syntax.statement.SqlStatement> {
        val query = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(broadSqlExpressions().first(), alias = "x")),
            from = broadSqlTables(),
            where = broadSqlExpressions()[1],
            having = broadSqlExpressions()[2],
            window = listOf(SqlWindowItem(id("broad_window"), SqlWindow(partitionBy = listOf(named("selectWindowPartition"))))),
            qualify = named("selectQualify")
        )
        return listOf(
            query,
            SqlQuery.Set(query, SqlSetOperator.Union(), SqlQuery.Values(listOf(listOf(named("valuesParam"))))),
            SqlDmlStatement.Insert(table("user"), listOf(id("id")), SqlInsertMode.Values(listOf(listOf(named("insertParam"))))),
            SqlDmlStatement.Insert(table("user"), listOf(id("id")), SqlInsertMode.Subquery(query)),
            SqlDmlStatement.Delete(table("user"), broadSqlExpressions()[3]),
            SqlDmlStatement.Update(table("user"), listOf(set("name", broadSqlExpressions()[4])), broadSqlExpressions()[5]),
            SqlDmlStatement.Upsert(table("user"), listOf(id("id"), id("name")), listOf(named("id"), named("name")), listOf(id("id"))),
            SqlDmlStatement.Truncate(table("user")),
            SqlDdlStatement.CreateTable(
                tableName = id("user"),
                columns = listOf(SqlColumnDefinition(id("id"), SqlType.Int, defaultValue = named("defaultId"))),
                indexes = listOf(com.kotlinorm.syntax.statement.SqlIndexDefinition(id("idx_user_id"), listOf(id("id"))))
            ),
            SqlDdlStatement.CreateTableAsSelect(id("user_copy"), query),
            SqlDdlStatement.AlterTable.AddColumn(id("user"), SqlColumnDefinition(id("age"), SqlType.Int, defaultValue = named("ageDefault"))),
            SqlDdlStatement.AlterTable.ModifyColumn(id("user"), SqlColumnDefinition(id("age"), SqlType.Int, defaultValue = named("ageDefault2"))),
            SqlDdlStatement.AlterTable.AlterColumnDefault(id("user"), id("age"), named("ageDefault3")),
            SqlDdlStatement.AlterTable.AlterColumnDefault(id("user"), id("age"), null),
            SqlDdlStatement.AlterTable.DropColumn(id("user"), id("old_age")),
            SqlDdlStatement.AlterTable.RenameColumn(id("user"), id("old_name"), id("name")),
            SqlDdlStatement.AlterTable.RenameTable(id("user"), id("account")),
            SqlDdlStatement.AlterTable.AlterColumnNullable(id("user"), id("name"), nullable = false),
            SqlDdlStatement.DropTable(id("user")),
            SqlDdlStatement.CreateIndex(id("idx_user_name"), id("user"), listOf(id("name"))),
            SqlDdlStatement.DropIndex(id("idx_user_name"), id("user")),
            SqlDdlStatement.AddConstraint(id("user"), SqlTableConstraint.Check(id("ck_user_age"), col("age").eq(named("constraintAge")))),
            SqlDdlStatement.AddConstraint(id("user"), SqlTableConstraint.PrimaryKey(id("pk_user"), listOf(id("id")))),
            SqlDdlStatement.AddConstraint(id("user"), SqlTableConstraint.Unique(id("uk_user_name"), listOf(id("name")))),
            SqlDdlStatement.AddConstraint(id("user"), SqlTableConstraint.ForeignKey(id("fk_user_org"), listOf(id("org_id")), id("org"), listOf(id("id")))),
            SqlDdlStatement.DropConstraint(id("user"), id("ck_user_age"))
        )
    }

    private fun broadSqlTables(): List<SqlTable> {
        val rowPattern = SqlRowPattern(
            pattern = SqlRowPatternTerm.Then(
                SqlRowPatternTerm.Exclusion(SqlRowPatternTerm.Pattern("A", SqlRowPatternQuantifier.Between(named("rpStart"), named("rpEnd")))),
                SqlRowPatternTerm.Or(
                    SqlRowPatternTerm.Permute(listOf(SqlRowPatternTerm.Circumflex(), SqlRowPatternTerm.Dollar())),
                    SqlRowPatternTerm.Pattern("B", SqlRowPatternQuantifier.Quantity(named("rpQty")))
                )
            ),
            define = listOf(SqlRowPatternDefineItem("A", named("defineA")))
        )
        val matchRecognize = SqlMatchRecognize(
            partitionBy = listOf(named("partition")),
            measures = listOf(SqlRecognizeMeasureItem(named("measure"), "m")),
            rowPattern = rowPattern
        )
        val ident = table("user")
        val function = SqlTable.Func(name = "unnest", args = listOf(named("tableFuncArg")), matchRecognize = matchRecognize)
        val subquery = SqlTable.Subquery(query = SqlQuery.Values(listOf(listOf(named("subqueryTableParam")))), alias = SqlTableAlias("sq"))
        val json = SqlTable.Json(
            expr = named("jsonDoc"),
            path = named("jsonPath"),
            passingItems = listOf(SqlJsonPassing(named("jsonPassing"), "p")),
            columns = listOf(
                SqlJsonColumn.Column("name", SqlType.Varchar(), path = named("jsonColumnPath")),
                SqlJsonColumn.Exists("has_name", SqlType.Boolean, path = named("jsonExistsPath")),
                SqlJsonColumn.Nested(named("jsonNestedPath"), columns = listOf(SqlJsonColumn.Ordinality("ord")))
            ),
            matchRecognize = matchRecognize
        )
        val graphTerm = SqlGraphPatternTerm.And(
            SqlGraphPatternTerm.Vertex("a", where = named("vertexWhere")),
            SqlGraphPatternTerm.Quantified(
                SqlGraphPatternTerm.Or(
                    SqlGraphPatternTerm.Edge(SqlGraphSymbol.Dash, where = named("edgeWhere"), rightSymbol = SqlGraphSymbol.RightArrow),
                    SqlGraphPatternTerm.Alternation(
                        SqlGraphPatternTerm.Vertex("b"),
                        SqlGraphPatternTerm.Vertex("c")
                    )
                ),
                SqlGraphQuantifier.Between(named("graphStart"), named("graphEnd"))
            )
        )
        val graph = SqlTable.Graph(
            name = "g",
            patterns = listOf(SqlGraphPattern(term = graphTerm)),
            where = named("graphWhere"),
            columns = listOf(SqlSelectItem.Expr(named("graphColumn"))),
            matchRecognize = matchRecognize
        )
        return listOf(
            ident,
            function,
            subquery,
            SqlTable.Join(ident, SqlJoinType.Inner, function, SqlJoinCondition.On(named("joinOn"))),
            SqlTable.Join(ident, SqlJoinType.Inner, function, SqlJoinCondition.Using(listOf("id"))),
            json,
            graph
        )
    }

    private fun broadSqlExpressions(): List<SqlExpr> {
        val valuesQuery = SqlQuery.Values(listOf(listOf(named("queryValue"))))
        return listOf(
            SqlExpr.Tuple(listOf(named("tupleItem"))),
            SqlExpr.Array(listOf(named("arrayItem"))),
            SqlExpr.Unary(SqlUnaryOperator.Not, named("unary")),
            SqlExpr.Binary(named("left"), SqlBinaryOperator.And, named("right")),
            SqlExpr.JsonTest(named("jsonTest")),
            SqlExpr.In(named("inExpr"), SqlInRightOperand.Values(listOf(named("inValue")))),
            SqlExpr.In(named("inSubqueryExpr"), SqlInRightOperand.Subquery(valuesQuery)),
            SqlExpr.Between(named("betweenExpr"), named("betweenStart"), named("betweenEnd")),
            SqlExpr.Like(named("likeExpr"), named("likePattern"), named("likeEscape")),
            SqlExpr.SimilarTo(named("similarExpr"), named("similarPattern"), named("similarEscape")),
            SqlExpr.Case(listOf(SqlCaseBranch(named("caseWhen"), named("caseThen"))), named("caseDefault")),
            SqlExpr.SimpleCase(named("simpleCaseExpr"), listOf(SqlCaseBranch(named("simpleWhen"), named("simpleThen"))), named("simpleDefault")),
            SqlExpr.Coalesce(listOf(named("coalesceA"), named("coalesceB"))),
            SqlExpr.NullIf(named("nullIfExpr"), named("nullIfTest")),
            SqlExpr.Cast(named("castExpr"), SqlType.Int),
            SqlExpr.Window(
                named("windowExpr"),
                SqlWindow(
                    partitionBy = listOf(named("windowPartition")),
                    frame = SqlWindowFrame.Start(SqlWindowFrameUnit.Rows, SqlWindowFrameBound.Preceding(named("framePreceding")))
                )
            ),
            SqlExpr.Window(
                named("windowExpr2"),
                SqlWindow(frame = SqlWindowFrame.Between(SqlWindowFrameUnit.Rows, SqlWindowFrameBound.Following(named("frameFollowing")), SqlWindowFrameBound.CurrentRow))
            ),
            SqlExpr.Subquery(valuesQuery),
            SqlExpr.ExistsPredicate(valuesQuery),
            SqlExpr.QuantifiedComparisonPredicate(named("quantifiedExpr"), SqlQuantifiedComparisonOperator.Equal, SqlSubqueryQuantifier.Any, valuesQuery),
            SqlExpr.Grouping(listOf(named("groupingExpr"))),
            SqlExpr.Function(id("SUM"), args = listOf(named("functionArg")), filter = named("functionFilter")),
            SqlExpr.SubstringFunc(named("substringExpr"), named("substringFrom"), named("substringFor")),
            SqlExpr.TrimFunc(named("trimExpr"), SqlTrim(SqlTrimMode.Both, named("trimValue"))),
            SqlExpr.OverlayFunc(named("overlayExpr"), named("overlayPlacing"), named("overlayFrom"), named("overlayFor")),
            SqlExpr.PositionFunc(named("positionExpr"), named("positionIn")),
            SqlExpr.ExtractFunc(SqlTimeUnit.Day, named("extractExpr")),
            SqlExpr.JsonSerializeFunc(named("jsonSerialize")),
            SqlExpr.JsonParseFunc(named("jsonParse")),
            SqlExpr.JsonQueryFunc(named("jsonQuery"), named("jsonQueryPath"), passingItems = listOf(SqlJsonPassing(named("jsonQueryPassing"), "p"))),
            SqlExpr.JsonValueFunc(named("jsonValue"), named("jsonValuePath"), passingItems = listOf(SqlJsonPassing(named("jsonValuePassing"), "p"))),
            SqlExpr.JsonObjectFunc(listOf(SqlJsonObjectItem(named("jsonObjectKey"), named("jsonObjectValue")))),
            SqlExpr.JsonArrayFunc(listOf(SqlJsonArrayItem(named("jsonArrayValue")))),
            SqlExpr.JsonExistsFunc(named("jsonExists"), named("jsonExistsPath"), passingItems = listOf(SqlJsonPassing(named("jsonExistsPassing"), "p"))),
            SqlExpr.CountAsteriskFunc(filter = named("countFilter")),
            SqlExpr.JsonObjectAggFunc(SqlJsonObjectItem(named("jsonObjectAggKey"), named("jsonObjectAggValue")), filter = named("jsonObjectAggFilter")),
            SqlExpr.JsonArrayAggFunc(SqlJsonArrayItem(named("jsonArrayAggValue")), filter = named("jsonArrayAggFilter")),
            SqlExpr.ListAggFunc(expr = named("listAggExpr"), separator = named("listAggSeparator"), filter = named("listAggFilter")),
            SqlExpr.NullsTreatmentFunc("FIRST_VALUE", args = listOf(named("nullsTreatmentArg"))),
            SqlExpr.NthValueFunc(named("nthValueExpr"), named("nthValueRow"), SqlNthValueFromMode.First),
            SqlExpr.MatchPhase(SqlMatchPhase.Running, named("matchPhaseExpr")),
            SqlExpr.UnsafeCustom(listOf(SqlUnsafeToken.Text("COALESCE"), SqlUnsafeToken.Expr(named("unsafeCustomExpr")))),
            SqlExpr.ExcludedColumn(id("excludedColumn")),
            SqlExpr.SourceColumn(id("sourceColumn"))
        )
    }
}
