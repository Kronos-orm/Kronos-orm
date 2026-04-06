/**
 * Copyright 2022-2025 kronos-orm
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
package com.kotlinorm.database

import com.kotlinorm.ast.*
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.database.mysql.MysqlSqlRenderer
import com.kotlinorm.database.postgres.PostgresqlSqlRenderer
import com.kotlinorm.database.sqlite.SqliteSqlRenderer
import com.kotlinorm.database.mssql.MssqlSqlRenderer
import com.kotlinorm.database.oracle.OracleSqlRenderer
import com.kotlinorm.enums.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class SqlRendererTest {
    private val mysql = MysqlSqlRenderer()
    private val postgres = PostgresqlSqlRenderer()
    private val sqlite = SqliteSqlRenderer()
    private val mssql = MssqlSqlRenderer()
    private val oracle = OracleSqlRenderer()

    private val table = TableName(table = "users")
    private val col = { name: String -> ColumnReference(null, null, name) }
    private val tCol = { t: String, name: String -> ColumnReference(null, t, name) }
    private val param = { name: String -> Parameter.NamedParameter(name) as Expression }
    private val num = { v: String -> Literal.NumberLiteral(v) as Expression }
    private val str = { v: String -> Literal.StringLiteral(v) as Expression }

    // === SELECT ===

    @Test fun testMysqlSimpleSelect() {
        val stmt = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("id"), null),
                SelectItem.ColumnSelectItem(col("name"), null)
            ),
            from = table
        )
        val r = mysql.render(stmt)
        assertEquals("SELECT `id`, `name` FROM `users`", r.sql)
    }
    @Test fun testPostgresSimpleSelect() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id"), null)),
            from = table
        )
        val r = postgres.render(stmt)
        assertEquals("SELECT \"id\" FROM \"users\"", r.sql)
    }

    @Test fun testMssqlSimpleSelect() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id"), null)),
            from = table
        )
        val r = mssql.render(stmt)
        assertEquals("SELECT [id] FROM [users]", r.sql)
    }

    @Test fun testOracleSimpleSelect() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id"), null)),
            from = table
        )
        val r = oracle.render(stmt)
        assertEquals("SELECT \"id\" FROM \"users\"", r.sql)
    }

    @Test fun testSqliteSimpleSelect() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id"), null)),
            from = table
        )
        val r = sqlite.render(stmt)
        assertEquals("SELECT \"id\" FROM \"users\"", r.sql)
    }

    @Test fun testSelectWithAlias() {
        val stmt = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("user_name"), "name")
            ),
            from = table
        )
        val r = mysql.render(stmt)
        assertEquals("SELECT `user_name` AS `name` FROM `users`", r.sql)
    }

    @Test fun testSelectAllColumns() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table
        )
        assertEquals("SELECT * FROM `users`", mysql.render(stmt).sql)
    }

    @Test fun testSelectAllColumnsWithTable() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem("u")),
            from = table
        )
        assertEquals("SELECT `u`.* FROM `users`", mysql.render(stmt).sql)
    }

    @Test fun testSelectDistinct() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("name"), null)),
            from = table,
            distinct = true
        )
        assertEquals("SELECT DISTINCT `name` FROM `users`", mysql.render(stmt).sql)
    }
    // === WHERE ===

    @Test fun testSelectWithWhere() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id"), null)),
            from = table,
            where = BinaryExpression(col("id"), SqlOperator.EQUAL, param("id"))
        )
        val r = mysql.render(stmt)
        assertEquals("SELECT `id` FROM `users` WHERE `id` = :id", r.sql)
    }

    @Test fun testSelectWithAndCondition() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = BinaryExpression(
                BinaryExpression(col("age"), SqlOperator.GREATER_THAN, num("18")),
                SqlOperator.AND,
                BinaryExpression(col("name"), SqlOperator.EQUAL, param("name"))
            )
        )
        val r = mysql.render(stmt)
        assertEquals("SELECT * FROM `users` WHERE `age` > 18 AND `name` = :name", r.sql)
    }

    @Test fun testSelectWithOrCondition() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = BinaryExpression(
                BinaryExpression(col("status"), SqlOperator.EQUAL, num("1")),
                SqlOperator.OR,
                BinaryExpression(col("role"), SqlOperator.EQUAL, str("admin"))
            )
        )
        val r = mysql.render(stmt)
        assertEquals("SELECT * FROM `users` WHERE `status` = 1 OR `role` = 'admin'", r.sql)
    }

    @Test fun testSelectWithBetween() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = SpecialExpression.BetweenExpression(col("age"), num("18"), num("65"))
        )
        assertEquals("SELECT * FROM `users` WHERE `age` BETWEEN 18 AND 65", mysql.render(stmt).sql)
    }

    @Test fun testSelectWithNotBetween() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = SpecialExpression.BetweenExpression(col("age"), num("18"), num("65"), not = true)
        )
        assertEquals("SELECT * FROM `users` WHERE `age` NOT BETWEEN 18 AND 65", mysql.render(stmt).sql)
    }

    @Test fun testSelectWithIn() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = SpecialExpression.InExpression(col("id"), listOf(num("1"), num("2"), num("3")))
        )
        assertEquals("SELECT * FROM `users` WHERE `id` IN (1, 2, 3)", mysql.render(stmt).sql)
    }

    @Test fun testSelectWithNotIn() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = SpecialExpression.InExpression(col("id"), listOf(num("1"), num("2")), not = true)
        )
        assertEquals("SELECT * FROM `users` WHERE `id` NOT IN (1, 2)", mysql.render(stmt).sql)
    }

    @Test fun testSelectWithIsNull() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = SpecialExpression.IsNullExpression(col("email"))
        )
        assertEquals("SELECT * FROM `users` WHERE `email` IS NULL", mysql.render(stmt).sql)
    }

    @Test fun testSelectWithIsNotNull() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = SpecialExpression.IsNullExpression(col("email"), not = true)
        )
        assertEquals("SELECT * FROM `users` WHERE `email` IS NOT NULL", mysql.render(stmt).sql)
    }

    @Test fun testSelectWithLike() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = SpecialExpression.LikeExpression(col("name"), str("%test%"))
        )
        assertEquals("SELECT * FROM `users` WHERE `name` LIKE '%test%'", mysql.render(stmt).sql)
    }

    @Test fun testSelectWithNotLike() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = SpecialExpression.LikeExpression(col("name"), str("%test%"), not = true)
        )
        assertEquals("SELECT * FROM `users` WHERE `name` NOT LIKE '%test%'", mysql.render(stmt).sql)
    }

    @Test fun testSelectWithRawSql() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = SpecialExpression.RawSqlExpression("status = 1 AND role = 'admin'")
        )
        assertEquals("SELECT * FROM `users` WHERE status = 1 AND role = 'admin'", mysql.render(stmt).sql)
    }
    // === ORDER BY / GROUP BY / HAVING ===

    @Test fun testSelectWithOrderBy() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            orderBy = mutableListOf(
                OrderByItem(col("name"), SortType.ASC),
                OrderByItem(col("id"), SortType.DESC)
            )
        )
        assertEquals("SELECT * FROM `users` ORDER BY `name` ASC, `id` DESC", mysql.render(stmt).sql)
    }

    @Test fun testSelectWithGroupBy() {
        val stmt = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("status"), null),
                SelectItem.ExpressionSelectItem(col("id"), "cnt")
            ),
            from = table,
            groupBy = mutableListOf(col("status"))
        )
        val r = mysql.render(stmt)
        assertTrue(r.sql.contains("GROUP BY `status`"))
    }

    @Test fun testSelectWithHaving() {
        val stmt = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(col("status"), null)
            ),
            from = table,
            groupBy = mutableListOf(col("status")),
            having = BinaryExpression(col("cnt"), SqlOperator.GREATER_THAN, num("5"))
        )
        val r = mysql.render(stmt)
        assertTrue(r.sql.contains("HAVING `cnt` > 5"))
    }

    // === PAGINATION ===

    @Test fun testMysqlLimitOnly() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            limit = LimitClause(10, null)
        )
        assertEquals("SELECT * FROM `users` LIMIT 10", mysql.render(stmt).sql)
    }

    @Test fun testMysqlLimitOffset() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            limit = LimitClause(10, 20)
        )
        assertEquals("SELECT * FROM `users` LIMIT 10 OFFSET 20", mysql.render(stmt).sql)
    }

    @Test fun testPostgresLimitOffset() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            limit = LimitClause(10, 20)
        )
        assertEquals("SELECT * FROM \"users\" LIMIT 10 OFFSET 20", postgres.render(stmt).sql)
    }

    @Test fun testSqliteLimitOffset() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            limit = LimitClause(10, 20)
        )
        assertEquals("SELECT * FROM \"users\" LIMIT 10 OFFSET 20", sqlite.render(stmt).sql)
    }

    @Test fun testMssqlFetchOnly() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            limit = LimitClause(10, null)
        )
        assertEquals("SELECT * FROM [users] FETCH NEXT 10 ROWS ONLY", mssql.render(stmt).sql)
    }

    @Test fun testMssqlOffsetFetch() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            limit = LimitClause(10, 20)
        )
        assertEquals("SELECT * FROM [users] OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", mssql.render(stmt).sql)
    }

    @Test fun testOracleFetchFirst() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            limit = LimitClause(10, null)
        )
        assertEquals("SELECT * FROM \"users\" FETCH FIRST 10 ROWS ONLY", oracle.render(stmt).sql)
    }

    @Test fun testOracleOffsetFetch() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            limit = LimitClause(10, 20)
        )
        assertEquals("SELECT * FROM \"users\" OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", oracle.render(stmt).sql)
    }
    // === LOCKING ===

    @Test fun testMysqlForUpdate() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            lock = PessimisticLock.X
        )
        assertTrue(mysql.render(stmt).sql.contains("FOR UPDATE"))
    }

    @Test fun testMysqlShareLock() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            lock = PessimisticLock.S
        )
        assertTrue(mysql.render(stmt).sql.contains("LOCK IN SHARE MODE"))
    }

    @Test fun testPostgresForUpdate() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            lock = PessimisticLock.X
        )
        assertTrue(postgres.render(stmt).sql.contains("FOR UPDATE"))
    }

    @Test fun testPostgresForShare() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            lock = PessimisticLock.S
        )
        assertTrue(postgres.render(stmt).sql.contains("FOR SHARE"))
    }

    @Test fun testMssqlRowLockX() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            lock = PessimisticLock.X
        )
        assertTrue(mssql.render(stmt).sql.contains("WITH (ROWLOCK, UPDLOCK)"))
    }

    @Test fun testMssqlRowLockS() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            lock = PessimisticLock.S
        )
        assertTrue(mssql.render(stmt).sql.contains("WITH (ROWLOCK, HOLDLOCK)"))
    }

    @Test fun testOracleForUpdate() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            lock = PessimisticLock.X
        )
        assertTrue(oracle.render(stmt).sql.contains("FOR UPDATE(NOWAIT)"))
    }

    @Test fun testOracleShareLock() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            lock = PessimisticLock.S
        )
        assertTrue(oracle.render(stmt).sql.contains("LOCK IN SHARE MODE"))
    }

    @Test fun testSqliteLockThrows() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            lock = PessimisticLock.X
        )
        assertFailsWith<UnsupportedOperationException> { sqlite.render(stmt) }
    }
    // === INSERT ===

    @Test fun testMysqlInsert() {
        val stmt = InsertStatement(
            table = table,
            columns = listOf(col("id"), col("name")),
            values = listOf(param("id"), param("name"))
        )
        assertEquals("INSERT INTO `users` (`id`, `name`) VALUES (:id, :name)", mysql.render(stmt).sql)
    }

    @Test fun testPostgresInsert() {
        val stmt = InsertStatement(
            table = table,
            columns = listOf(col("id"), col("name")),
            values = listOf(param("id"), param("name"))
        )
        assertEquals("INSERT INTO \"users\" (\"id\", \"name\") VALUES (:id, :name)", postgres.render(stmt).sql)
    }

    @Test fun testMssqlInsert() {
        val stmt = InsertStatement(
            table = table,
            columns = listOf(col("id"), col("name")),
            values = listOf(param("id"), param("name"))
        )
        assertEquals("INSERT INTO [users] ([id], [name]) VALUES (:id, :name)", mssql.render(stmt).sql)
    }

    @Test fun testOracleInsert() {
        val stmt = InsertStatement(
            table = table,
            columns = listOf(col("id"), col("name")),
            values = listOf(param("id"), param("name"))
        )
        assertEquals("INSERT INTO \"users\" (\"id\", \"name\") VALUES (:id, :name)", oracle.render(stmt).sql)
    }

    @Test fun testSqliteInsert() {
        val stmt = InsertStatement(
            table = table,
            columns = listOf(col("id"), col("name")),
            values = listOf(param("id"), param("name"))
        )
        assertEquals("INSERT INTO \"users\" (\"id\", \"name\") VALUES (:id, :name)", sqlite.render(stmt).sql)
    }

    // === UPDATE ===

    @Test fun testMysqlUpdate() {
        val stmt = UpdateStatement(
            table = table,
            assignments = mutableListOf(Assignment(col("name"), param("name"))),
            where = BinaryExpression(col("id"), SqlOperator.EQUAL, param("id"))
        )
        assertEquals("UPDATE `users` SET `name` = :name WHERE `id` = :id", mysql.render(stmt).sql)
    }

    @Test fun testPostgresUpdate() {
        val stmt = UpdateStatement(
            table = table,
            assignments = mutableListOf(Assignment(col("name"), param("name"))),
            where = BinaryExpression(col("id"), SqlOperator.EQUAL, param("id"))
        )
        assertEquals("UPDATE \"users\" SET \"name\" = :name WHERE \"id\" = :id", postgres.render(stmt).sql)
    }

    @Test fun testMssqlUpdate() {
        val stmt = UpdateStatement(
            table = table,
            assignments = mutableListOf(Assignment(col("name"), param("name"))),
            where = BinaryExpression(col("id"), SqlOperator.EQUAL, param("id"))
        )
        assertEquals("UPDATE [users] SET [name] = :name WHERE [id] = :id", mssql.render(stmt).sql)
    }

    @Test fun testUpdateMultipleAssignments() {
        val stmt = UpdateStatement(
            table = table,
            assignments = mutableListOf(
                Assignment(col("name"), param("name")),
                Assignment(col("age"), param("age"))
            ),
            where = BinaryExpression(col("id"), SqlOperator.EQUAL, param("id"))
        )
        assertEquals("UPDATE `users` SET `name` = :name, `age` = :age WHERE `id` = :id", mysql.render(stmt).sql)
    }

    @Test fun testUpdateWithoutWhere() {
        val stmt = UpdateStatement(
            table = table,
            assignments = mutableListOf(Assignment(col("status"), num("0")))
        )
        assertEquals("UPDATE `users` SET `status` = 0", mysql.render(stmt).sql)
    }

    // === DELETE ===

    @Test fun testMysqlDelete() {
        val stmt = DeleteStatement(table, BinaryExpression(col("id"), SqlOperator.EQUAL, param("id")))
        assertEquals("DELETE FROM `users` WHERE `id` = :id", mysql.render(stmt).sql)
    }

    @Test fun testPostgresDelete() {
        val stmt = DeleteStatement(table, BinaryExpression(col("id"), SqlOperator.EQUAL, param("id")))
        assertEquals("DELETE FROM \"users\" WHERE \"id\" = :id", postgres.render(stmt).sql)
    }

    @Test fun testMssqlDelete() {
        val stmt = DeleteStatement(table, BinaryExpression(col("id"), SqlOperator.EQUAL, param("id")))
        assertEquals("DELETE FROM [users] WHERE [id] = :id", mssql.render(stmt).sql)
    }

    @Test fun testDeleteWithoutWhere() {
        val stmt = DeleteStatement(table)
        assertEquals("DELETE FROM `users`", mysql.render(stmt).sql)
    }
    // === UPSERT / CONFLICT RESOLUTION ===

    private fun makeConflictResolver(): ConflictResolver {
        val idField = Field("id", "id")
        val nameField = Field("name", "name")
        val ageField = Field("age", "age")
        return ConflictResolver(
            tableName = "users",
            onFields = linkedSetOf(idField),
            toUpdateFields = linkedSetOf(nameField, ageField),
            toInsertFields = linkedSetOf(idField, nameField, ageField)
        )
    }

    @Test fun testMysqlUpsert() {
        val resolver = makeConflictResolver()
        val stmt = InsertStatement(
            table = table,
            columns = listOf(col("id"), col("name"), col("age")),
            values = listOf(param("id"), param("name"), param("age")),
            conflictResolver = resolver
        )
        val r = mysql.render(stmt)
        assertTrue(r.sql.contains("ON DUPLICATE KEY UPDATE"))
        assertTrue(r.sql.contains("`id` = VALUES(`id`)"))
    }

    @Test fun testPostgresUpsert() {
        val resolver = makeConflictResolver()
        val stmt = InsertStatement(
            table = table,
            columns = listOf(col("id"), col("name"), col("age")),
            values = listOf(param("id"), param("name"), param("age")),
            conflictResolver = resolver
        )
        val r = postgres.render(stmt)
        assertTrue(r.sql.contains("ON CONFLICT"))
        assertTrue(r.sql.contains("DO UPDATE SET"))
        assertTrue(r.sql.contains("EXCLUDED"))
    }

    @Test fun testSqliteUpsert() {
        val resolver = makeConflictResolver()
        val stmt = InsertStatement(
            table = table,
            columns = listOf(col("id"), col("name"), col("age")),
            values = listOf(param("id"), param("name"), param("age")),
            conflictResolver = resolver
        )
        val r = sqlite.render(stmt)
        assertTrue(r.sql.contains("ON CONFLICT"))
        assertTrue(r.sql.contains("DO UPDATE SET"))
    }

    @Test fun testMssqlUpsert() {
        val resolver = makeConflictResolver()
        val stmt = InsertStatement(
            table = table,
            columns = listOf(col("id"), col("name"), col("age")),
            values = listOf(param("id"), param("name"), param("age")),
            conflictResolver = resolver
        )
        val r = mssql.render(stmt)
        assertTrue(r.sql.contains("IF EXISTS"))
        assertTrue(r.sql.contains("UPDATE"))
        assertTrue(r.sql.contains("INSERT INTO"))
    }

    @Test fun testOracleUpsert() {
        val resolver = makeConflictResolver()
        val stmt = InsertStatement(
            table = table,
            columns = listOf(col("id"), col("name"), col("age")),
            values = listOf(param("id"), param("name"), param("age")),
            conflictResolver = resolver
        )
        val r = oracle.render(stmt)
        assertTrue(r.sql.contains("BEGIN"))
        assertTrue(r.sql.contains("EXCEPTION"))
        assertTrue(r.sql.contains("DUP_VAL_ON_INDEX"))
    }
    // === DDL: CREATE TABLE ===

    private fun makeCreateTable(comment: String? = null): DdlStatement.CreateTableStatement {
        return DdlStatement.CreateTableStatement(
            tableName = "users",
            columns = listOf(
                ColumnDefinition("id", KColumnType.INT, nullable = false, primaryKey = PrimaryKeyType.IDENTITY),
                ColumnDefinition("name", KColumnType.VARCHAR, length = 100, nullable = false),
                ColumnDefinition("age", KColumnType.INT, nullable = true),
                ColumnDefinition("bio", KColumnType.TEXT, nullable = true),
                ColumnDefinition("created_at", KColumnType.DATETIME, nullable = true)
            ),
            indexes = listOf(
                KTableIndex("idx_name", arrayOf("name"), "UNIQUE", "BTREE")
            ),
            comment = comment
        )
    }

    @Test fun testMysqlCreateTable() {
        val r = mysql.render(makeCreateTable())
        assertTrue(r.sql.contains("CREATE TABLE IF NOT EXISTS"))
        assertTrue(r.sql.contains("`users`"))
        assertTrue(r.sql.contains("INT"))
        assertTrue(r.sql.contains("VARCHAR(100)"))
        assertTrue(r.sql.contains("AUTO_INCREMENT"))
        assertTrue(r.sql.contains("UNIQUE INDEX"))
        assertTrue(r.sql.contains("USING BTREE"))
    }

    @Test fun testMysqlCreateTableWithComment() {
        val r = mysql.render(makeCreateTable("User table"))
        assertTrue(r.sql.contains("COMMENT = 'User table'"))
    }

    @Test fun testPostgresCreateTable() {
        val r = postgres.render(makeCreateTable())
        assertTrue(r.sql.contains("CREATE TABLE IF NOT EXISTS"))
        assertTrue(r.sql.contains("\"users\""))
        assertTrue(r.sql.contains("SERIAL"))
        assertTrue(r.sql.contains("VARCHAR(100)"))
        assertTrue(r.sql.contains("UNIQUE INDEX"))
    }

    @Test fun testPostgresCreateTableWithComment() {
        val r = postgres.render(makeCreateTable("User table"))
        assertTrue(r.sql.contains("COMMENT ON TABLE"))
    }

    @Test fun testSqliteCreateTable() {
        val r = sqlite.render(makeCreateTable())
        assertTrue(r.sql.contains("CREATE TABLE IF NOT EXISTS"))
        assertTrue(r.sql.contains("\"users\""))
        assertTrue(r.sql.contains("INTEGER"))
        assertTrue(r.sql.contains("AUTOINCREMENT"))
    }

    @Test fun testMssqlCreateTable() {
        val r = mssql.render(makeCreateTable())
        assertTrue(r.sql.contains("[dbo]"))
        assertTrue(r.sql.contains("[users]"))
        assertTrue(r.sql.contains("INT"))
        assertTrue(r.sql.contains("IDENTITY(1,1)"))
        assertTrue(r.sql.contains("VARCHAR(100)"))
    }

    @Test fun testOracleCreateTable() {
        val r = oracle.render(makeCreateTable())
        assertTrue(r.sql.contains("CREATE TABLE"), "Missing CREATE TABLE in: ${r.sql}")
        assertTrue(r.sql.contains("\"USERS\""), "Missing \"USERS\" in: ${r.sql}")
        assertTrue(r.sql.contains("NUMBER"), "Missing NUMBER in: ${r.sql}")
        assertTrue(r.sql.contains("VARCHAR2(100)"), "Missing VARCHAR2(100) in: ${r.sql}")
        assertTrue(r.sql.contains("CLOB"), "Missing CLOB in: ${r.sql}")
        assertTrue(r.sql.contains("TIMESTAMP"), "Missing TIMESTAMP in: ${r.sql}")
    }

    // === DDL: ALTER TABLE ===

    @Test fun testMysqlAddColumn() {
        val stmt = DdlStatement.AlterTableStatement.AddColumnStatement(
            "users", ColumnDefinition("email", KColumnType.VARCHAR, length = 255)
        )
        val r = mysql.render(stmt)
        assertTrue(r.sql.contains("ALTER TABLE"))
        assertTrue(r.sql.contains("ADD COLUMN"))
        assertTrue(r.sql.contains("VARCHAR(255)"))
    }

    @Test fun testMysqlDropColumn() {
        val stmt = DdlStatement.AlterTableStatement.DropColumnStatement("users", "email")
        val r = mysql.render(stmt)
        assertTrue(r.sql.contains("ALTER TABLE"))
        assertTrue(r.sql.contains("DROP COLUMN"))
        assertTrue(r.sql.contains("`email`"))
    }

    @Test fun testMysqlModifyColumn() {
        val stmt = DdlStatement.AlterTableStatement.ModifyColumnStatement(
            "users", ColumnDefinition("name", KColumnType.VARCHAR, length = 500)
        )
        val r = mysql.render(stmt)
        assertTrue(r.sql.contains("ALTER TABLE"))
        assertTrue(r.sql.contains("MODIFY COLUMN") || r.sql.contains("ALTER COLUMN"))
    }

    @Test fun testPostgresAddColumn() {
        val stmt = DdlStatement.AlterTableStatement.AddColumnStatement(
            "users", ColumnDefinition("email", KColumnType.VARCHAR, length = 255)
        )
        val r = postgres.render(stmt)
        assertTrue(r.sql.contains("ALTER TABLE"))
        assertTrue(r.sql.contains("ADD COLUMN"))
    }

    @Test fun testPostgresDropColumn() {
        val stmt = DdlStatement.AlterTableStatement.DropColumnStatement("users", "email")
        val r = postgres.render(stmt)
        assertTrue(r.sql.contains("DROP COLUMN"))
    }

    @Test fun testPostgresModifyColumn() {
        val stmt = DdlStatement.AlterTableStatement.ModifyColumnStatement(
            "users", ColumnDefinition("name", KColumnType.VARCHAR, length = 500)
        )
        val r = postgres.render(stmt)
        assertTrue(r.sql.contains("ALTER TABLE"))
    }

    @Test fun testMssqlAddColumn() {
        val stmt = DdlStatement.AlterTableStatement.AddColumnStatement(
            "users", ColumnDefinition("email", KColumnType.VARCHAR, length = 255)
        )
        val r = mssql.render(stmt)
        assertTrue(r.sql.contains("ALTER TABLE"))
        assertTrue(r.sql.contains("ADD"))
    }

    @Test fun testMssqlDropColumn() {
        val stmt = DdlStatement.AlterTableStatement.DropColumnStatement("users", "email")
        val r = mssql.render(stmt)
        assertTrue(r.sql.contains("DROP COLUMN"))
    }

    @Test fun testMssqlModifyColumn() {
        val stmt = DdlStatement.AlterTableStatement.ModifyColumnStatement(
            "users", ColumnDefinition("name", KColumnType.VARCHAR, length = 500)
        )
        val r = mssql.render(stmt)
        assertTrue(r.sql.contains("ALTER TABLE"))
        assertTrue(r.sql.contains("ALTER COLUMN"))
    }

    @Test fun testOracleAddColumn() {
        val stmt = DdlStatement.AlterTableStatement.AddColumnStatement(
            "users", ColumnDefinition("email", KColumnType.VARCHAR, length = 255)
        )
        val r = oracle.render(stmt)
        assertTrue(r.sql.contains("ALTER TABLE"))
        assertTrue(r.sql.contains("ADD"))
    }

    @Test fun testOracleModifyColumn() {
        val stmt = DdlStatement.AlterTableStatement.ModifyColumnStatement(
            "users", ColumnDefinition("name", KColumnType.VARCHAR, length = 500)
        )
        val r = oracle.render(stmt)
        assertTrue(r.sql.contains("ALTER TABLE"))
        assertTrue(r.sql.contains("MODIFY"))
    }

    @Test fun testSqliteAddColumn() {
        val stmt = DdlStatement.AlterTableStatement.AddColumnStatement(
            "users", ColumnDefinition("email", KColumnType.VARCHAR, length = 255)
        )
        val r = sqlite.render(stmt)
        assertTrue(r.sql.contains("ALTER TABLE"))
        assertTrue(r.sql.contains("ADD COLUMN"))
    }
    // === DDL: DROP TABLE / TRUNCATE / INDEX ===

    @Test fun testMysqlDropTable() {
        val stmt = DdlStatement.DropTableStatement("users", ifExists = true)
        val r = mysql.render(stmt)
        assertTrue(r.sql.contains("DROP TABLE"))
        assertTrue(r.sql.contains("IF EXISTS"))
    }

    @Test fun testPostgresDropTable() {
        val r = postgres.render(DdlStatement.DropTableStatement("users", ifExists = true))
        assertTrue(r.sql.contains("DROP TABLE"))
    }

    @Test fun testMssqlDropTable() {
        val r = mssql.render(DdlStatement.DropTableStatement("users", ifExists = true))
        assertTrue(r.sql.contains("DROP TABLE"))
    }

    @Test fun testOracleDropTable() {
        val r = oracle.render(DdlStatement.DropTableStatement("users", ifExists = true))
        assertTrue(r.sql.contains("DROP TABLE"))
    }

    @Test fun testSqliteDropTable() {
        val r = sqlite.render(DdlStatement.DropTableStatement("users", ifExists = true))
        assertTrue(r.sql.contains("DROP TABLE"))
    }

    @Test fun testMysqlTruncate() {
        val r = mysql.render(DdlStatement.TruncateTableStatement("users"))
        assertTrue(r.sql.contains("TRUNCATE TABLE"))
    }

    @Test fun testPostgresTruncate() {
        val r = postgres.render(DdlStatement.TruncateTableStatement("users", restartIdentity = true))
        assertTrue(r.sql.contains("TRUNCATE TABLE"))
    }

    @Test fun testMysqlCreateIndex() {
        val r = mysql.render(DdlStatement.CreateIndexStatement("idx_email", "users", listOf("email"), unique = true))
        assertTrue(r.sql.contains("CREATE") && r.sql.contains("UNIQUE") && r.sql.contains("INDEX"))
    }

    @Test fun testPostgresCreateIndex() {
        val r = postgres.render(DdlStatement.CreateIndexStatement("idx_email", "users", listOf("email")))
        assertTrue(r.sql.contains("CREATE") && r.sql.contains("INDEX"))
    }

    @Test fun testMysqlDropIndex() {
        val r = mysql.render(DdlStatement.DropIndexStatement("idx_email", "users"))
        assertTrue(r.sql.contains("DROP INDEX"))
    }

    @Test fun testPostgresDropIndex() {
        val r = postgres.render(DdlStatement.DropIndexStatement("idx_email", "users"))
        assertTrue(r.sql.contains("DROP INDEX"))
    }
    // === LITERALS ===

    @Test fun testMysqlStringLiteral() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ExpressionSelectItem(str("hello"), null)),
            from = table
        )
        assertTrue(mysql.render(stmt).sql.contains("'hello'"))
    }

    @Test fun testMysqlStringEscape() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ExpressionSelectItem(str("it's"), null)),
            from = table
        )
        assertTrue(mysql.render(stmt).sql.contains("'it''s'"))
    }

    @Test fun testBooleanLiteral() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = BinaryExpression(col("active"), SqlOperator.EQUAL, Literal.BooleanLiteral(true))
        )
        assertTrue(mysql.render(stmt).sql.contains("true"))
    }

    @Test fun testNullLiteral() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = BinaryExpression(col("name"), SqlOperator.EQUAL, Literal.NullLiteral)
        )
        assertTrue(mysql.render(stmt).sql.contains("NULL"))
    }

    @Test fun testMysqlDateLiteral() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ExpressionSelectItem(Literal.DateLiteral("2024-01-01"), null)),
            from = table
        )
        assertTrue(mysql.render(stmt).sql.contains("'2024-01-01'"))
    }

    @Test fun testOracleDateLiteral() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ExpressionSelectItem(Literal.DateLiteral("2024-01-01"), null)),
            from = table
        )
        assertTrue(oracle.render(stmt).sql.contains("DATE '2024-01-01'"))
    }

    @Test fun testMysqlTimeLiteral() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ExpressionSelectItem(Literal.TimeLiteral("12:30:00"), null)),
            from = table
        )
        assertTrue(mysql.render(stmt).sql.contains("'12:30:00'"))
    }

    @Test fun testOracleTimeLiteral() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ExpressionSelectItem(Literal.TimeLiteral("12:30:00"), null)),
            from = table
        )
        assertTrue(oracle.render(stmt).sql.contains("TIMESTAMP '12:30:00'"))
    }

    @Test fun testMysqlTimestampLiteral() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ExpressionSelectItem(Literal.TimestampLiteral("2024-01-01 12:30:00"), null)),
            from = table
        )
        assertTrue(mysql.render(stmt).sql.contains("'2024-01-01 12:30:00'"))
    }

    @Test fun testOracleTimestampLiteral() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.ExpressionSelectItem(Literal.TimestampLiteral("2024-01-01 12:30:00"), null)),
            from = table
        )
        assertTrue(oracle.render(stmt).sql.contains("TIMESTAMP '2024-01-01 12:30:00'"))
    }

    // === JOIN ===

    @Test fun testMysqlInnerJoin() {
        val stmt = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(tCol("u", "id"), null),
                SelectItem.ColumnSelectItem(tCol("o", "amount"), null)
            ),
            from = JoinTable(
                left = TableName(table = "users", alias = "u"),
                joinType = JoinType.INNER_JOIN,
                right = TableName(table = "orders", alias = "o"),
                condition = BinaryExpression(tCol("u", "id"), SqlOperator.EQUAL, tCol("o", "user_id"))
            )
        )
        val r = mysql.render(stmt)
        assertTrue(r.sql.contains("INNER JOIN"))
        assertTrue(r.sql.contains("`u`.`id`"))
        assertTrue(r.sql.contains("`o`.`amount`"))
    }

    @Test fun testMysqlLeftJoin() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = JoinTable(
                left = TableName(table = "users"),
                joinType = JoinType.LEFT_JOIN,
                right = TableName(table = "orders"),
                condition = BinaryExpression(col("id"), SqlOperator.EQUAL, col("user_id"))
            )
        )
        assertTrue(mysql.render(stmt).sql.contains("LEFT JOIN"))
    }

    @Test fun testMysqlRightJoin() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = JoinTable(
                left = TableName(table = "users"),
                joinType = JoinType.RIGHT_JOIN,
                right = TableName(table = "orders"),
                condition = BinaryExpression(col("id"), SqlOperator.EQUAL, col("user_id"))
            )
        )
        assertTrue(mysql.render(stmt).sql.contains("RIGHT JOIN"))
    }

    @Test fun testMysqlCrossJoin() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = JoinTable(
                left = TableName(table = "users"),
                joinType = JoinType.CROSS_JOIN,
                right = TableName(table = "roles"),
                condition = null
            )
        )
        assertTrue(mysql.render(stmt).sql.contains("CROSS JOIN"))
    }

    @Test fun testMysqlFullJoin() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = JoinTable(
                left = TableName(table = "users"),
                joinType = JoinType.FULL_JOIN,
                right = TableName(table = "orders"),
                condition = BinaryExpression(col("id"), SqlOperator.EQUAL, col("user_id"))
            )
        )
        assertTrue(mysql.render(stmt).sql.contains("FULL JOIN"))
    }

    @Test fun testPostgresJoin() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = JoinTable(
                left = TableName(table = "users"),
                joinType = JoinType.LEFT_JOIN,
                right = TableName(table = "orders"),
                condition = BinaryExpression(col("id"), SqlOperator.EQUAL, col("user_id"))
            )
        )
        val r = postgres.render(stmt)
        assertTrue(r.sql.contains("LEFT JOIN") && r.sql.contains("\"users\"") && r.sql.contains("\"orders\""))
    }

    @Test fun testMssqlJoin() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = JoinTable(
                left = TableName(table = "users"),
                joinType = JoinType.INNER_JOIN,
                right = TableName(table = "orders"),
                condition = BinaryExpression(col("id"), SqlOperator.EQUAL, col("user_id"))
            )
        )
        val r = mssql.render(stmt)
        assertTrue(r.sql.contains("INNER JOIN") && r.sql.contains("[users]") && r.sql.contains("[orders]"))
    }

    @Test fun testNestedJoin() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = JoinTable(
                left = JoinTable(
                    left = TableName(table = "users"),
                    joinType = JoinType.INNER_JOIN,
                    right = TableName(table = "orders"),
                    condition = BinaryExpression(col("id"), SqlOperator.EQUAL, col("user_id"))
                ),
                joinType = JoinType.LEFT_JOIN,
                right = TableName(table = "products"),
                condition = BinaryExpression(col("product_id"), SqlOperator.EQUAL, col("pid"))
            )
        )
        val r = mysql.render(stmt)
        assertTrue(r.sql.contains("INNER JOIN") && r.sql.contains("LEFT JOIN") && r.sql.contains("`products`"))
    }

    // === TABLE WITH ALIAS / DATABASE PREFIX ===

    @Test fun testTableWithAlias() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(table = "users", alias = "u")
        )
        assertTrue(mysql.render(stmt).sql.contains("`users` AS `u`"))
    }

    @Test fun testTableWithDatabasePrefix() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = TableName(database = "mydb", table = "users")
        )
        assertTrue(mysql.render(stmt).sql.contains("`mydb`.`users`"))
    }

    @Test fun testColumnWithDatabasePrefix() {
        val stmt = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ColumnSelectItem(ColumnReference("mydb", "u", "id"), null)
            ),
            from = table
        )
        assertTrue(mysql.render(stmt).sql.contains("`mydb`.`u`.`id`"))
    }

    // === UNION ===

    @Test fun testMysqlUnion() {
        val q1 = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id"), null)),
            from = TableName(table = "users")
        )
        val q2 = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id"), null)),
            from = TableName(table = "admins")
        )
        val r = mysql.render(UnionStatement(queries = listOf(q1, q2), unionAll = false))
        assertTrue(r.sql.contains("UNION") && !r.sql.contains("UNION ALL"))
    }

    @Test fun testMysqlUnionAll() {
        val q1 = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id"), null)),
            from = TableName(table = "users")
        )
        val q2 = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id"), null)),
            from = TableName(table = "admins")
        )
        assertTrue(mysql.render(UnionStatement(queries = listOf(q1, q2), unionAll = true)).sql.contains("UNION ALL"))
    }

    @Test fun testUnionWithOrderByAndLimit() {
        val q1 = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id"), null)),
            from = TableName(table = "users")
        )
        val q2 = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id"), null)),
            from = TableName(table = "admins")
        )
        val r = mysql.render(UnionStatement(
            queries = listOf(q1, q2), unionAll = false,
            orderBy = listOf(OrderByItem(col("id"), SortType.ASC)),
            limit = LimitClause(10, null)
        ))
        assertTrue(r.sql.contains("ORDER BY") && r.sql.contains("LIMIT 10"))
    }

    // === UNARY / EXPRESSIONS ===

    @Test fun testNotExpression() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = UnaryExpression(UnaryOperator.NOT, BinaryExpression(col("active"), SqlOperator.EQUAL, Literal.BooleanLiteral(true)))
        )
        assertTrue(mysql.render(stmt).sql.contains("NOT"))
    }

    @Test fun testNegateExpression() {
        val stmt = UpdateStatement(
            table = table,
            assignments = mutableListOf(Assignment(col("balance"), UnaryExpression(UnaryOperator.NEGATE, col("amount"))))
        )
        assertTrue(mysql.render(stmt).sql.contains("-`amount`"))
    }

    @Test fun testExpressionSelectItem() {
        val stmt = SelectStatement(
            selectList = mutableListOf(
                SelectItem.ExpressionSelectItem(
                    BinaryExpression(col("price"), SqlOperator.MULTIPLY, col("qty")), "total"
                )
            ),
            from = table
        )
        val r = mysql.render(stmt)
        assertTrue(r.sql.contains("`price` * `qty`") && r.sql.contains("AS `total`"))
    }

    // === SUBQUERY ===

    @Test fun testInSubquery() {
        val subquery = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("user_id"), null)),
            from = TableName(table = "orders")
        )
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = SpecialExpression.InSubqueryExpression(col("id"), subquery)
        )
        assertTrue(mysql.render(stmt).sql.contains("IN (SELECT"))
    }

    @Test fun testSubqueryTable() {
        val subquery = SelectStatement(
            selectList = mutableListOf(SelectItem.ColumnSelectItem(col("id"), null)),
            from = TableName(table = "users"),
            where = BinaryExpression(col("active"), SqlOperator.EQUAL, Literal.BooleanLiteral(true))
        )
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = SubqueryTable(subquery, "active_users")
        )
        val r = mysql.render(stmt)
        assertTrue(r.sql.contains("(SELECT") && r.sql.contains("active_users"))
    }

    // === COLUMN TYPES (DDL coverage for each dialect) ===

    @Test fun testMysqlColumnTypes() {
        val r = mysql.render(DdlStatement.CreateTableStatement("t", listOf(
            ColumnDefinition("c1", KColumnType.INT), ColumnDefinition("c2", KColumnType.BIGINT),
            ColumnDefinition("c3", KColumnType.VARCHAR, length = 200), ColumnDefinition("c4", KColumnType.TEXT),
            ColumnDefinition("c5", KColumnType.DATETIME), ColumnDefinition("c6", KColumnType.TIMESTAMP)
        )))
        assertTrue(r.sql.contains("INT") && r.sql.contains("BIGINT") && r.sql.contains("VARCHAR(200)"))
        assertTrue(r.sql.contains("TEXT") && r.sql.contains("DATETIME") && r.sql.contains("TIMESTAMP"))
    }

    @Test fun testPostgresColumnTypes() {
        val r = postgres.render(DdlStatement.CreateTableStatement("t", listOf(
            ColumnDefinition("c1", KColumnType.INT), ColumnDefinition("c2", KColumnType.BIGINT),
            ColumnDefinition("c3", KColumnType.VARCHAR, length = 200), ColumnDefinition("c4", KColumnType.TEXT),
            ColumnDefinition("c5", KColumnType.BIT), ColumnDefinition("c6", KColumnType.UUID),
            ColumnDefinition("c7", KColumnType.JSON)
        )))
        assertTrue(r.sql.contains("INTEGER") && r.sql.contains("BIGINT") && r.sql.contains("VARCHAR(200)"))
        assertTrue(r.sql.contains("BOOLEAN") && r.sql.contains("UUID") && r.sql.contains("JSONB"))
    }

    @Test fun testMssqlColumnTypes() {
        val r = mssql.render(DdlStatement.CreateTableStatement("t", listOf(
            ColumnDefinition("c1", KColumnType.INT), ColumnDefinition("c2", KColumnType.BIGINT),
            ColumnDefinition("c3", KColumnType.VARCHAR, length = 200), ColumnDefinition("c4", KColumnType.NVARCHAR, length = 100),
            ColumnDefinition("c5", KColumnType.TEXT), ColumnDefinition("c6", KColumnType.BIT),
            ColumnDefinition("c7", KColumnType.BLOB), ColumnDefinition("c8", KColumnType.DATE)
        )))
        assertTrue(r.sql.contains("INT") && r.sql.contains("VARCHAR(200)") && r.sql.contains("NVARCHAR(100)"))
        assertTrue(r.sql.contains("BIT") && r.sql.contains("VARBINARY(MAX)") && r.sql.contains("DATE"))
    }

    @Test fun testOracleColumnTypes() {
        val r = oracle.render(DdlStatement.CreateTableStatement("t", listOf(
            ColumnDefinition("c1", KColumnType.INT), ColumnDefinition("c2", KColumnType.BIGINT),
            ColumnDefinition("c3", KColumnType.VARCHAR, length = 200), ColumnDefinition("c4", KColumnType.TEXT),
            ColumnDefinition("c5", KColumnType.DATE), ColumnDefinition("c6", KColumnType.BLOB),
            ColumnDefinition("c7", KColumnType.JSON), ColumnDefinition("c8", KColumnType.TIMESTAMP, scale = 3)
        )))
        assertTrue(r.sql.contains("NUMBER") && r.sql.contains("VARCHAR2(200)") && r.sql.contains("CLOB"))
        assertTrue(r.sql.contains("DATE") && r.sql.contains("BLOB") && r.sql.contains("JSON") && r.sql.contains("TIMESTAMP(3)"))
    }

    @Test fun testSqliteColumnTypes() {
        val r = sqlite.render(DdlStatement.CreateTableStatement("t", listOf(
            ColumnDefinition("c1", KColumnType.INT), ColumnDefinition("c2", KColumnType.BIGINT),
            ColumnDefinition("c3", KColumnType.VARCHAR, length = 200), ColumnDefinition("c4", KColumnType.TEXT),
            ColumnDefinition("c5", KColumnType.FLOAT), ColumnDefinition("c6", KColumnType.BLOB)
        )))
        assertTrue(r.sql.contains("INTEGER") && r.sql.contains("TEXT") && r.sql.contains("REAL") && r.sql.contains("BLOB"))
    }

    // === DEFAULT VALUES / SPECIAL DDL ===

    @Test fun testColumnWithDefaultValue() {
        val r = mysql.render(DdlStatement.CreateTableStatement("t", listOf(
            ColumnDefinition("status", KColumnType.INT, defaultValue = Literal.NumberLiteral("0")),
            ColumnDefinition("name", KColumnType.VARCHAR, length = 50, defaultValue = Literal.StringLiteral("unknown"))
        )))
        assertTrue(r.sql.contains("DEFAULT 0") && r.sql.contains("DEFAULT 'unknown'"))
    }

    @Test fun testPositionalParameter() {
        val stmt = SelectStatement(
            selectList = mutableListOf(SelectItem.AllColumnsSelectItem(null)),
            from = table,
            where = BinaryExpression(col("id"), SqlOperator.EQUAL, Parameter.PositionalParameter(0))
        )
        assertTrue(mysql.render(stmt).sql.contains("?"))
    }

    @Test fun testMssqlLargeVarchar() {
        val r = mssql.render(DdlStatement.CreateTableStatement("t", listOf(
            ColumnDefinition("c1", KColumnType.VARCHAR, length = 9000),
            ColumnDefinition("c2", KColumnType.NVARCHAR, length = 5000)
        )))
        assertTrue(r.sql.contains("VARCHAR(MAX)") && r.sql.contains("NVARCHAR(MAX)"))
    }

    @Test fun testPostgresSerialIdentity() {
        val r = postgres.render(DdlStatement.CreateTableStatement("t", listOf(
            ColumnDefinition("id", KColumnType.INT, primaryKey = PrimaryKeyType.IDENTITY),
            ColumnDefinition("big_id", KColumnType.BIGINT, primaryKey = PrimaryKeyType.IDENTITY)
        )))
        assertTrue(r.sql.contains("SERIAL") && r.sql.contains("BIGSERIAL"))
    }

    @Test fun testSqliteIndexWithCollate() {
        val r = sqlite.render(DdlStatement.CreateTableStatement("t",
            listOf(ColumnDefinition("name", KColumnType.VARCHAR, length = 100)),
            indexes = listOf(KTableIndex("idx_name", arrayOf("name"), "", "NOCASE"))
        ))
        assertTrue(r.sql.contains("COLLATE NOCASE"))
    }

    @Test fun testOracleUppercaseTable() {
        val r = oracle.render(DdlStatement.CreateTableStatement("users", listOf(
            ColumnDefinition("id", KColumnType.INT, primaryKey = PrimaryKeyType.IDENTITY)
        )))
        assertTrue(r.sql.contains("\"USERS\""))
    }

    @Test fun testMssqlDboSchema() {
        val r = mssql.render(DdlStatement.CreateTableStatement("users", listOf(
            ColumnDefinition("id", KColumnType.INT)
        )))
        assertTrue(r.sql.contains("[dbo]"))
    }
}
