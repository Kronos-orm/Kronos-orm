package com.kotlinorm.ast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AstBuildersTest {

    @Test
    fun `select with exists subquery and join subquery as source`() {
        // Subquery: select id from orders where amount > :min
        val orderSub = select {
            project(col("id", tableAlias = "o"))
            from = table("orders", alias = "o")
            where = col("amount", "o") gt param(":min")
        }

        // Main query: select u.id, u.name from users u
        // join (select id from orders ...) s on s.id = u.id
        // where exists (select ...) and u.status in (1,2)
        val q = select {
            project(col("id", "u"))
            project(col("name", "u"))
            from = join(
                table("users", alias = "u"),
                subquery(orderSub, alias = "s"),
                type = JoinType.inner,
                on = col("id", "s") eq col("id", "u")
            )
            where = exists(orderSub) and inValues(col("status", "u"), lit(1), lit(2))
            orderBy(col("id", "u"))
        }

        assertIs<SelectStatement>(q)
        assertEquals(2, q.projections.size)
        assertIs<Join>(q.from!!)
        val j = q.from as Join
        assertIs<SubquerySource>(j.right)
        val where = q.where
        assertIs<BinaryOp>(where)
        val andExpr = where as BinaryOp
        assertEquals("AND", andExpr.op)
        assertIs<Exists>(andExpr.left)
        assertIs<InOp>(andExpr.right)
        assertTrue(q.orderBy.isNotEmpty())
    }

    @Test
    fun `insert from select`() {
        val src = select {
            project(col("id", "u"))
            project(col("name", "u"))
            from = table("users", alias = "u")
            where = col("active", "u") eq lit(true)
        }

        val stmt = InsertStatement(
            target = table("archived_users"),
            columns = listOf("id", "name").toMutableList(),
            source = SelectSource(src)
        )

        assertIs<InsertStatement>(stmt)
        assertIs<SelectSource>(stmt.source)
        val select = (stmt.source as SelectSource).select
        assertEquals(2, select.projections.size)
    }

    @Test
    fun `update with from join and subquery condition`() {
        val recentOrders = select {
            project(col("user_id", "o"))
            from = table("orders", alias = "o")
            where = col("created_at", "o") gt param(":since")
        }

        val upd = UpdateStatement(
            target = table("users", alias = "u"),
            set = listOf(Assignment(col("flag", "u"), lit(true))).toMutableList(),
            from = join(
                table("users", alias = "u"),
                subquery(recentOrders, alias = "r"),
                on = col("id", "u") eq col("user_id", "r")
            ),
            where = exists(recentOrders)
        )

        assertIs<UpdateStatement>(upd)
        assertNotNull(upd.from)
        assertIs<Join>(upd.from!!)
        assertIs<Exists>(upd.where!!)
    }

    @Test
    fun `delete with exists`() {
        val sub = select {
            project(col("1"))
            from = table("orders", alias = "o")
            where = col("o_user_id") eq col("id", "u")
        }

        val del = DeleteStatement(
            target = table("users", alias = "u"),
            where = exists(sub)
        )

        assertIs<DeleteStatement>(del)
        assertIs<Exists>(del.where!!)
    }
}
