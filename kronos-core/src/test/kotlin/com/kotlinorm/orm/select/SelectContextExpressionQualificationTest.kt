package com.kotlinorm.orm.select

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlCaseBranch
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.expr.SqlQuantifiedComparisonOperator
import com.kotlinorm.syntax.expr.SqlSubqueryQuantifier
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.expr.SqlUnaryOperator
import com.kotlinorm.syntax.expr.SqlWindow
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectContextExpressionQualificationTest {

    @Test
    fun `qualifySourceExpr qualifies recursive expression shapes`() {
        val subquery = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(SqlExpr.Column(columnName = "sub_id")))
        )
        val cases = listOf(
            c("id") to q("id"),
            q("id") to q("id"),
            SqlExpr.Unary(SqlUnaryOperator.Not, c("active")) to
                SqlExpr.Unary(SqlUnaryOperator.Not, q("active")),
            SqlExpr.Binary(c("score"), SqlBinaryOperator.Plus, SqlExpr.NumberLiteral("1")) to
                SqlExpr.Binary(q("score"), SqlBinaryOperator.Plus, SqlExpr.NumberLiteral("1")),
            SqlExpr.Tuple(listOf(c("id"), c("score"))) to
                SqlExpr.Tuple(listOf(q("id"), q("score"))),
            SqlExpr.Array(listOf(c("id"), SqlExpr.StringLiteral("x"))) to
                SqlExpr.Array(listOf(q("id"), SqlExpr.StringLiteral("x"))),
            SqlExpr.In(c("id"), SqlInRightOperand.Values(listOf(c("manager_id"), SqlExpr.Parameter(SqlParameter.Named("id"))))) to
                SqlExpr.In(q("id"), SqlInRightOperand.Values(listOf(q("manager_id"), SqlExpr.Parameter(SqlParameter.Named("id"))))),
            SqlExpr.In(c("id"), SqlInRightOperand.Subquery(subquery)) to
                SqlExpr.In(q("id"), SqlInRightOperand.Subquery(subquery)),
            SqlExpr.Between(c("score"), c("min_score"), c("max_score")) to
                SqlExpr.Between(q("score"), q("min_score"), q("max_score")),
            SqlExpr.Like(c("name"), c("pattern"), c("escape")) to
                SqlExpr.Like(q("name"), q("pattern"), q("escape")),
            SqlExpr.Like(c("name"), SqlExpr.StringLiteral("A%")) to
                SqlExpr.Like(q("name"), SqlExpr.StringLiteral("A%")),
            functionExpr(c("score"), c("id"), c("group_id"), c("deleted")) to
                functionExpr(q("score"), q("id"), q("group_id"), q("deleted")),
            SqlExpr.Window(
                expr = SqlExpr.Function(SqlIdentifier.of("sum"), args = listOf(c("score"))),
                window = SqlWindow(
                    partitionBy = listOf(c("group_id")),
                    orderBy = listOf(SqlOrderingItem(c("id"), SqlOrdering.Desc))
                )
            ) to SqlExpr.Window(
                expr = SqlExpr.Function(SqlIdentifier.of("sum"), args = listOf(q("score"))),
                window = SqlWindow(
                    partitionBy = listOf(q("group_id")),
                    orderBy = listOf(SqlOrderingItem(q("id"), SqlOrdering.Desc))
                )
            ),
            SqlExpr.Case(
                branches = listOf(SqlCaseBranch(SqlExpr.Binary(c("score"), SqlBinaryOperator.GreaterThan, SqlExpr.NumberLiteral("100")), c("name"))),
                default = c("fallback")
            ) to SqlExpr.Case(
                branches = listOf(SqlCaseBranch(SqlExpr.Binary(q("score"), SqlBinaryOperator.GreaterThan, SqlExpr.NumberLiteral("100")), q("name"))),
                default = q("fallback")
            ),
            SqlExpr.SimpleCase(
                expr = c("status"),
                branches = listOf(SqlCaseBranch(SqlExpr.StringLiteral("A"), c("name"))),
                default = c("fallback")
            ) to SqlExpr.SimpleCase(
                expr = q("status"),
                branches = listOf(SqlCaseBranch(SqlExpr.StringLiteral("A"), q("name"))),
                default = q("fallback")
            ),
            SqlExpr.Coalesce(listOf(c("nickname"), c("name"), SqlExpr.StringLiteral("unknown"))) to
                SqlExpr.Coalesce(listOf(q("nickname"), q("name"), SqlExpr.StringLiteral("unknown"))),
            SqlExpr.NullIf(c("name"), c("nickname")) to
                SqlExpr.NullIf(q("name"), q("nickname")),
            SqlExpr.Cast(c("score"), SqlType.Int) to
                SqlExpr.Cast(q("score"), SqlType.Int),
            SqlExpr.QuantifiedComparisonPredicate(
                expr = c("score"),
                operator = SqlQuantifiedComparisonOperator.GreaterThan,
                quantifier = SqlSubqueryQuantifier.Any,
                query = subquery
            ) to SqlExpr.QuantifiedComparisonPredicate(
                expr = q("score"),
                operator = SqlQuantifiedComparisonOperator.GreaterThan,
                quantifier = SqlSubqueryQuantifier.Any,
                query = subquery
            ),
            SqlExpr.StringLiteral("unchanged") to SqlExpr.StringLiteral("unchanged")
        )

        cases.forEach { (input, expected) ->
            assertEquals(expected, input.qualifySourceExpr(SOURCE_ALIAS))
        }
    }

    @Test
    fun `qualify helpers keep full projection order and literal behavior`() {
        val expr = SqlExpr.Binary(c("score"), SqlBinaryOperator.Plus, SqlExpr.NumberLiteral("1"))
        val qualifiedExpr = SqlExpr.Binary(q("score"), SqlBinaryOperator.Plus, SqlExpr.NumberLiteral("1"))
        val metadata = SqlSelectItemAliasMetadata(
            outputName = "nextScore",
            expression = expr,
            scope = SqlSelectItemSourceScope.Selected
        )
        val qualifiedMetadata = metadata.copy(expression = qualifiedExpr)
        val field = Field(columnName = "id", name = "id", tableName = "tb_user")

        assertEquals(q("score"), c("score").qualifySourceAliasIfPresent(SOURCE_ALIAS))
        assertEquals(c("score"), c("score").qualifySourceAliasIfPresent(null))
        assertEquals(
            listOf(
                KTableForSelect.ProjectionItem.FieldItem(field),
                KTableForSelect.ProjectionItem.SelectItemValue(
                    SqlSelectItem.Expr(qualifiedExpr, "nextScore", qualifiedMetadata)
                )
            ),
            listOf(
                KTableForSelect.ProjectionItem.FieldItem(field),
                KTableForSelect.ProjectionItem.SelectItemValue(
                    SqlSelectItem.Expr(expr, "nextScore", metadata)
                )
            ).qualifyProjectionItems(SOURCE_ALIAS)
        )
        assertEquals(
            SqlSelectItem.Expr(qualifiedExpr, "nextScore", qualifiedMetadata),
            SqlSelectItem.Expr(expr, "nextScore", metadata).qualifySourceSelectItem(SOURCE_ALIAS)
        )
        assertEquals(
            SqlSelectItem.Asterisk(),
            SqlSelectItem.Asterisk().qualifySourceSelectItem(SOURCE_ALIAS)
        )
        assertEquals(
            SelectOrderItem.ExprItem(qualifiedExpr, SqlOrdering.Desc),
            SelectOrderItem.ExprItem(expr, SqlOrdering.Desc).qualifySource(SOURCE_ALIAS)
        )
        assertEquals(
            SelectOrderItem.FieldItem(field, SqlOrdering.Asc),
            SelectOrderItem.FieldItem(field, SqlOrdering.Asc).qualifySource(SOURCE_ALIAS)
        )
        assertEquals(42, SqlExpr.NumberLiteral("42").numberLiteralInt())
        assertEquals(null, SqlExpr.NumberLiteral("x").numberLiteralInt())
        assertEquals(null, SqlExpr.StringLiteral("42").numberLiteralInt())
    }

    private fun functionExpr(arg: SqlExpr, order: SqlExpr, within: SqlExpr, filter: SqlExpr): SqlExpr =
        SqlExpr.Function(
            name = SqlIdentifier.of("sum"),
            args = listOf(arg),
            orderBy = listOf(SqlOrderingItem(order, SqlOrdering.Desc)),
            withinGroup = listOf(SqlOrderingItem(within, SqlOrdering.Asc)),
            filter = SqlExpr.Binary(filter, SqlBinaryOperator.Equal, SqlExpr.NumberLiteral("0"))
        )

    private fun c(name: String): SqlExpr.Column =
        SqlExpr.Column(columnName = name)

    private fun q(name: String): SqlExpr.Column =
        SqlExpr.Column(tableName = SOURCE_ALIAS, columnName = name)

    private companion object {
        const val SOURCE_ALIAS = "src"
    }
}
