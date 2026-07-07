package com.kotlinorm.orm.sql

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlSelectItemSource
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import kotlin.test.Test
import kotlin.test.assertEquals

class OrmSyntaxFactoryTest {

    @Test
    fun `field conversions build complete syntax expressions and select metadata`() {
        val plain = Field(columnName = "id", name = "id", tableName = "tb_user")
        val aliased = Field(columnName = "user_name", name = "userName", tableName = "tb_user")

        assertEquals(
            SqlExpr.Column(columnName = "id"),
            plain.toSqlExpr()
        )
        assertEquals(
            SqlExpr.Column(tableName = "tb_user", columnName = "id"),
            plain.toSqlExpr(useTableAlias = true)
        )
        assertEquals(
            SqlExpr.Binary(
                SqlExpr.Column(tableName = "tb_user", columnName = "id"),
                SqlBinaryOperator.Equal,
                SqlExpr.Parameter(SqlParameter.Named("id"))
            ),
            plain.toSqlParameterEq("id", useTableAlias = true)
        )
        assertEquals(
            SqlSelectItem.Expr(
                expr = SqlExpr.Column(columnName = "id"),
                alias = null,
                metadata = SqlSelectItemAliasMetadata(
                    outputName = "id",
                    expression = SqlExpr.Column(columnName = "id"),
                    scope = SqlSelectItemSourceScope.Source,
                    source = SqlSelectItemSource(columnName = "id"),
                    userReferenceable = true
                )
            ),
            plain.toSqlSelectItem()
        )
        assertEquals(
            SqlSelectItem.Expr(
                expr = SqlExpr.Column(tableName = "tb_user", columnName = "user_name"),
                alias = "userName",
                metadata = SqlSelectItemAliasMetadata(
                    outputName = "userName",
                    expression = SqlExpr.Column(tableName = "tb_user", columnName = "user_name"),
                    scope = SqlSelectItemSourceScope.Selected,
                    source = SqlSelectItemSource(tableName = "tb_user", columnName = "user_name"),
                    userReferenceable = true
                )
            ),
            aliased.toSqlSelectItem(useTableAlias = true)
        )
    }

    @Test
    fun `literal conversion covers null expressions primitives chars and fallback objects`() {
        val expr = SqlExpr.Column(columnName = "id")

        assertEquals(
            listOf(
                SqlExpr.NullLiteral,
                expr,
                SqlExpr.StringLiteral("A"),
                SqlExpr.BooleanLiteral(true),
                SqlExpr.NumberLiteral("12"),
                SqlExpr.NumberLiteral("1.5"),
                SqlExpr.StringLiteral("x"),
                SqlExpr.StringLiteral("fallback")
            ),
            listOf(
                null.toSqlLiteralExpr(),
                expr.toSqlLiteralExpr(),
                "A".toSqlLiteralExpr(),
                true.toSqlLiteralExpr(),
                12.toSqlLiteralExpr(),
                1.5.toSqlLiteralExpr(),
                'x'.toSqlLiteralExpr(),
                object {
                    override fun toString(): String = "fallback"
                }.toSqlLiteralExpr()
            )
        )
    }
}
