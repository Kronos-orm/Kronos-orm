package com.kotlinorm.ast

import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.database.mysql.MysqlSupport
import com.kotlinorm.database.postgres.PostgresqlSupport
import com.kotlinorm.database.sqlite.SqliteSupport
import kotlin.test.Test
import kotlin.test.assertTrue

private class TestWrapper(override val dbType: DBType) : KronosDataSourceWrapper {
    override val url: String = ""
    override val userName: String = ""
    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()
    override fun forList(task: KAtomicQueryTask, kClass: kotlin.reflect.KClass<*>, isKPojo: Boolean, superTypes: List<String>): List<Any> = emptyList()
    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? = null
    override fun forObject(task: KAtomicQueryTask, kClass: kotlin.reflect.KClass<*>, isKPojo: Boolean, superTypes: List<String>): Any? = null
    override fun update(task: KAtomicActionTask): Int = 0
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
    override fun transact(block: () -> Any?): Any? = block()
}

class AstAdvancedRendererTest {

    @Test
    fun `render select with CTE, union all, order nulls, distinct and lock`() {
        val base = select {
            project(col("id", "u"))
            from = table("users", alias = "u")
            where = col("active", "u") eq lit(true)
            distinct = true
            orderBy(col("name", "u"), direction = OrderDirection.asc, nulls = NullsOrder.last)
            limit = 10
            offset = 5
        }
        val q2 = select {
            project(col("id", "u"))
            from = table("users", alias = "u")
            where = col("active", "u") eq lit(false)
        }
        val withQ = select {
            project(col("id"))
            from = table("t1")
        }
        val composed = base.apply {
            with = listOf(CommonTableExpression("cte_ids", withQ)).toMutableList()
            setOps = listOf(SetOperator.unionAll to q2).toMutableList()
            lock = PessimisticLock.X
        }

        val mysqlSql = MysqlSupport.getSelectSql(TestWrapper(DBType.Mysql), composed)
        val pgSql = PostgresqlSupport.getSelectSql(TestWrapper(DBType.Postgres), composed)
        val sqliteSql = SqliteSupport.getSelectSql(TestWrapper(DBType.SQLite), composed)

        // WITH clause
        assertTrue(mysqlSql.startsWith("WITH "))
        assertTrue(pgSql.startsWith("WITH "))
        assertTrue(sqliteSql.startsWith("WITH "))
        // UNION ALL present
        assertTrue(mysqlSql.contains(" UNION ALL "))
        assertTrue(pgSql.contains(" UNION ALL "))
        assertTrue(sqliteSql.contains(" UNION ALL "))
        // DISTINCT
        assertTrue(mysqlSql.contains("SELECT DISTINCT"))
        // ORDER BY nulls (dialects will keep NULLS LAST token in generic form)
        assertTrue(mysqlSql.contains("ORDER BY"))
        assertTrue(pgSql.contains("ORDER BY"))
        // pagination
        assertTrue(mysqlSql.contains(" LIMIT "))
        assertTrue(pgSql.contains(" LIMIT "))
        assertTrue(sqliteSql.contains(" LIMIT "))
        // lock
        assertTrue(mysqlSql.contains("FOR UPDATE"))
    }

    @Test
    fun `render insert with multiple values rows`() {
        val insert = InsertStatement(
            target = table("tb_demo"),
            columns = listOf("a", "b").toMutableList(),
            source = ValuesSource(
                listOf(
                    listOf(lit(1), lit("x")),
                    listOf(lit(2), lit("y"))
                )
            )
        )
        val sql = MysqlSupport.getInsertSql(TestWrapper(DBType.Mysql), insert)
        assertTrue(sql.contains("INSERT INTO"))
        assertTrue(sql.contains("VALUES (1, 'x'), (2, 'y')"))
    }
}
