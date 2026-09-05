package com.kotlinorm.orm.insert

import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class InsertClauseSyntaxTest : MysqlTestBase() {

    @Test
    fun testToSqlStatementGeneratesInsertSyntaxStatement() {
        val statement = TestUser(1)
            .insert()
            .toSqlStatement()

        assertEquals(
            SqlDmlStatement.Insert(
                table = SqlTable.Ident("tb_user"),
                columns = expectedColumnIdentifiers(),
                mode = SqlInsertMode.Values(
                    listOf(expectedParameterNames().map { SqlExpr.Parameter(SqlParameter.Named(it)) })
                )
            ),
            statement
        )
    }

    @Test
    fun testToSqlStatementIncludesExpectedColumns() {
        val statement = TestUser(1)
            .insert()
            .toSqlStatement()

        assertEquals(
            listOf("id", "username", "score", "create_time", "update_time", "deleted"),
            statement.columns.map { it.last }
        )
    }

    private fun expectedColumnIdentifiers(): List<SqlIdentifier> =
        listOf("id", "username", "score", "create_time", "update_time", "deleted")
            .map(SqlIdentifier::of)

    private fun expectedParameterNames(): List<String> =
        listOf("id", "username", "score", "createTime", "updateTime", "deleted")
}
