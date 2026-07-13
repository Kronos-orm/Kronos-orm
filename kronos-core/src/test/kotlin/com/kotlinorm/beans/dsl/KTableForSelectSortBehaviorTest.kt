package com.kotlinorm.beans.dsl

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.reflect.typeOf
import kotlin.test.assertNull
import kotlin.test.assertSame

class KTableForSelectSortBehaviorTest {

    @Test
    fun `select runtime stubs and aliases keep exact placeholder behavior`() {
        val table = KTableForSelect<KPojo>()
        val field = Field("user_name", "username", tableName = "tb_user")
        val function = KronosFunctionExpr(
            SqlExpr.Function(SqlIdentifier.of("COUNT"), args = listOf(SqlExpr.NumberLiteral("1"))),
            "count"
        )
        val expression = SqlExpr.Column("tb_user", "id")
        val pojo = SelectSortRuntimePojo(id = 1)

        with(table) {
            get(field, function)
            assertNull((1 as Any?) + 2)
            assertNull((1 as Any?) - 2)
            assertNull((1 as Any?) * 2)
            assertNull((1 as Any?) / 2)
            assertNull((1 as Any?) % 2)
            assertSame(pojo, pojo - field)
            assertSame(field, field.alias("nameAlias"))
            assertEquals(function.copy(alias = "total"), function.alias("total"))
            assertEquals(KronosFunctionExpr(expression, "expr", "idAlias"), expression.alias("idAlias"))
            assertEquals("value", "value".alias("ignored"))
            assertEquals(1, 1.over { partitionBy(field); orderBy(field.asc()) })
        }
    }

    @Test
    fun `select additions create exact projection and select item shapes`() {
        val table = KTableForSelect<KPojo>()
        val field = Field("id", "id", tableName = "tb_user")
        val fieldExpr = SqlExpr.Column("tb_user", "id")
        val functionExpr = SqlExpr.Function(SqlIdentifier.of("COUNT"), args = listOf(SqlExpr.NumberLiteral("1")))
        val selectable = SelectSortSelectable()

        table.addField(field)
        table.addFunction(KronosFunctionExpr(functionExpr, "count", alias = "total"))
        table.addRawSql("id + 1", alias = "nextId")
        table.addScalarSubquery(selectable, alias = "orderCount")

        assertEquals(listOf(field), table.fields)
        assertEquals(
            listOf<SqlSelectItem>(
                SqlSelectItem.Expr(
                    expr = functionExpr,
                    alias = "total",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "total",
                        expression = functionExpr,
                        scope = SqlSelectItemSourceScope.Aggregate
                    )
                ),
                SqlSelectItem.Expr(
                    expr = SqlExpr.UnsafeRaw("id + 1"),
                    alias = "nextId",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "nextId",
                        expression = SqlExpr.UnsafeRaw("id + 1"),
                        scope = SqlSelectItemSourceScope.Selected,
                        userReferenceable = true
                    )
                ),
                SqlSelectItem.Expr(
                    expr = SqlExpr.Subquery(selectable.query),
                    alias = "orderCount",
                    metadata = SqlSelectItemAliasMetadata(
                        outputName = "orderCount",
                        expression = SqlExpr.Subquery(selectable.query),
                        scope = SqlSelectItemSourceScope.Selected
                    )
                )
            ),
            table.selectItems
        )
        assertEquals(
            listOf<KTableForSelect.ProjectionItem>(
                KTableForSelect.ProjectionItem.FieldItem(field, fieldExpr),
                KTableForSelect.ProjectionItem.SelectItemValue(table.selectItems[0]),
                KTableForSelect.ProjectionItem.SelectItemValue(table.selectItems[1]),
                KTableForSelect.ProjectionItem.ScalarSubqueryValue(selectable, "orderCount", table.selectItems[2])
            ),
            table.projectionItems
        )
        assertEquals(Field("age", "ageAlias"), with(table) { Field("age", "age").setAlias("ageAlias") })
    }

    @Test
    fun `raw sql select item converts simple literals and unsafe sql exactly`() {
        assertEquals(SqlExpr.NumberLiteral("1"), "1".toRawSqlExpr())
        assertEquals(SqlExpr.NumberLiteral("1.5"), "1.5".toRawSqlExpr())
        assertEquals(SqlExpr.BooleanLiteral(true), "true".toRawSqlExpr())
        assertEquals(SqlExpr.BooleanLiteral(false), "FALSE".toRawSqlExpr())
        assertEquals(SqlExpr.NullLiteral, "null".toRawSqlExpr())
        assertEquals(SqlExpr.UnsafeRaw("id + 1"), "id + 1".toRawSqlExpr())
        assertEquals(
            SqlSelectItem.Expr(expr = SqlExpr.NumberLiteral("1"), alias = null, metadata = null),
            rawSqlSelectItem("1")
        )
    }

    @Test
    fun `sort additions create exact field expression and selectable items`() {
        val table = KTableForSort<KPojo>()
        val field = Field("id", "id", tableName = "tb_user")
        val fieldExpr = SqlExpr.Column("tb_user", "id")
        val expression = SqlExpr.Column("tb_user", "created_at")
        val selectable = SelectSortSelectable()

        table.addSortField(with(table) { field.desc() })
        table.addSortField("id + 1")
        table.addSortField(expression)
        table.addSortField(selectable)
        table.addSortExpression(SqlExpr.NumberLiteral("1"), SqlOrdering.Desc)
        table.addSortSubquery(selectable, SqlOrdering.Desc)

        assertEquals(
            listOf<KTableForSort.SortItem>(
                KTableForSort.SortItem.FieldItem(field, SqlOrdering.Desc, fieldExpr),
                KTableForSort.SortItem.ExpressionItem(SqlExpr.UnsafeRaw("id + 1"), SqlOrdering.Asc),
                KTableForSort.SortItem.ExpressionItem(expression, SqlOrdering.Asc),
                KTableForSort.SortItem.SelectableItem(selectable, SqlOrdering.Asc),
                KTableForSort.SortItem.ExpressionItem(SqlExpr.NumberLiteral("1"), SqlOrdering.Desc),
                KTableForSort.SortItem.SelectableItem(selectable, SqlOrdering.Desc)
            ),
            table.sortedItems
        )
        SelectSortRuntimePojo().afterSort { addSortField(field.asc()) }
        SelectSortRuntimePojo().afterSelect { addField(field) }
    }
}

@Table("select_sort_runtime_pojo")
data class SelectSortRuntimePojo(val id: Int? = null) : KPojo

private class SelectSortSelectable : KSelectable<SelectSortRuntimePojo>(
    SelectSortRuntimePojo()
) {
    override val selectedType = typeOf<SelectSortRuntimePojo>()

    val query = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.NumberLiteral("1"))),
        from = listOf(SqlTable.Ident("select_sort_runtime_pojo"))
    )

    override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask =
        error("SelectSortSelectable is only used for syntax materialization tests.")

    internal override fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper?): SqlQueryPlan =
        SqlQueryPlan(query, emptyMap())
}
