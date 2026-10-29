package com.kotlinorm.database

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.database.postgres.PostgresqlStatements
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.render.SqlDialect
import com.kotlinorm.syntax.render.SqlRenderContext
import com.kotlinorm.syntax.render.toRenderedSql
import kotlin.test.Test
import kotlin.test.assertEquals

class PostgresTest {
    data class Account(
        @PrimaryKey(identity = true)
        val id: Long? = null
    ): KPojo

    @Test
    fun testLongColumnTypeInference() {
        val column = Account().__columns[0]
        val statement = PostgresqlStatements.createTable(
            DatabaseCreateTable("account", null, listOf(column), emptyList())
        ).first()
        val sql = statement.toRenderedSql(SqlRenderContext(SqlDialect.PostgreSql)).sql
        assertEquals(
            """CREATE TABLE IF NOT EXISTS "public"."account" ("id" BIGSERIAL NOT NULL PRIMARY KEY)""",
            sql
        )
    }
}
