package com.kotlinorm.orm.sql.dialects

import com.kotlinorm.database.SqlManager
import com.kotlinorm.enums.DBType
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.exceptions.InvalidDataAccessApiUsageException
import com.kotlinorm.functions.bundled.exts.MathFunctions.bin
import com.kotlinorm.functions.bundled.exts.StringFunctions.repeat
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.pagination.toCursor
import com.kotlinorm.orm.select.filter
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.testfixtures.entities.DialectUser
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testutils.ExpectedTask
import com.kotlinorm.testutils.assertTaskEquals
import com.kotlinorm.testutils.coreSqlDialects
import com.kotlinorm.testutils.initializeCoreSqlTestDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CoreOrmDialectSqlTest {

    @Test
    fun `bin renders for MySQL and fails during build for every other built-in database`() {
        initializeCoreSqlTestDefaults()

        val mysqlTask = DialectUser()
            .select { f.bin(10).alias("bin") }
            .build(CapturingDialectWrapper(DBType.Mysql))
            .atomicTask

        assertTaskEquals(
            ExpectedTask("SELECT BIN(10) AS bin FROM `tb_user` WHERE `deleted` = 0"),
            mysqlTask,
            "mysql bin"
        )

        listOf(DBType.H2, DBType.Postgres, DBType.SQLite, DBType.Mssql, DBType.Oracle, DBType.DM8)
            .forEach { dbType ->
                val error = assertFailsWith<UnsupportedOperationException> {
                    DialectUser()
                        .select { f.bin(10).alias("bin") }
                        .build(CapturingDialectWrapper(dbType))
                }
                assertEquals(
                    "Kronos built-in function 'bin' is not supported by ${SqlManager.dialectOf(dbType).family}.",
                    error.message,
                    "$dbType bin rejection"
                )
            }
    }

    @Test
    fun `Oracle compatible databases preserve native repeat zero rendering`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            0 to ExpectedTask("SELECT RPAD('x', 0 * LENGTH('x'), 'x') AS REPEAT FROM \"TB_USER\" WHERE \"DELETED\" = 0"),
            3 to ExpectedTask("SELECT RPAD('x', 3 * LENGTH('x'), 'x') AS REPEAT FROM \"TB_USER\" WHERE \"DELETED\" = 0"),
        )

        listOf(DBType.Oracle, DBType.DM8).forEach { dbType ->
            expected.forEach { (times, expectedTask) ->
                val task = DialectUser()
                    .select { f.repeat("x", times).alias("repeat") }
                    .build(CapturingDialectWrapper(dbType))
                    .atomicTask

                assertTaskEquals(expectedTask, task, "$dbType repeat($times)")
            }
        }
    }

    @Test
    fun `select renders complete sql for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask(
                "SELECT `id`, `username` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `tb_user`.`username` = :username AND `deleted` = 0 LIMIT 10",
                mapOf("id" to 1, "username" to "neo")
            ),
            DBType.Postgres to ExpectedTask(
                """SELECT "id", "username" FROM "tb_user" WHERE "tb_user"."id" = :id AND "tb_user"."username" = :username AND "deleted" = FALSE LIMIT 10""",
                mapOf("id" to 1, "username" to "neo")
            ),
            DBType.SQLite to ExpectedTask(
                """SELECT "id", "username" FROM "tb_user" WHERE "tb_user"."id" = :id AND "tb_user"."username" = :username AND "deleted" = 0 LIMIT 10""",
                mapOf("id" to 1, "username" to "neo")
            ),
            DBType.Mssql to ExpectedTask(
                "SELECT TOP (10) [id], [username] FROM [tb_user] WHERE [tb_user].[id] = :id AND [tb_user].[username] = :username AND [deleted] = 0",
                mapOf("id" to 1, "username" to "neo")
            ),
            DBType.Oracle to ExpectedTask(
                """SELECT "ID", "USERNAME" FROM "TB_USER" WHERE "TB_USER"."ID" = :id AND "TB_USER"."USERNAME" = :username AND "DELETED" = 0 FETCH NEXT 10 ROWS ONLY""",
                mapOf("id" to 1, "username" to "neo")
            )
        )

        coreSqlDialects.forEach { dialect ->
            val task = DialectUser(id = 1, username = "neo")
                .select { [it.id, it.username] }
                .where { it.id == 1 && it.username == "neo" }
                .limit(10)
                .build(dialect.wrapper)
                .atomicTask

            assertTaskEquals(expected.getValue(dialect.dbType), task, dialect.label)
        }
    }

    @Test
    fun `result filter renders derived boundary and colliding parameters for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask(
                "SELECT `q`.`id`, `q`.`username` FROM (SELECT `id`, `username` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0) AS `q` WHERE `q`.`id` = :id",
                mapOf("id" to 20, "id@1" to 10)
            ),
            DBType.Postgres to ExpectedTask(
                """SELECT "q"."id", "q"."username" FROM (SELECT "id", "username" FROM "tb_user" WHERE "tb_user"."id" = :id@1 AND "deleted" = FALSE) AS "q" WHERE "q"."id" = :id""",
                mapOf("id" to 20, "id@1" to 10)
            ),
            DBType.SQLite to ExpectedTask(
                """SELECT "q"."id", "q"."username" FROM (SELECT "id", "username" FROM "tb_user" WHERE "tb_user"."id" = :id@1 AND "deleted" = 0) AS "q" WHERE "q"."id" = :id""",
                mapOf("id" to 20, "id@1" to 10)
            ),
            DBType.Mssql to ExpectedTask(
                "SELECT [q].[id], [q].[username] FROM (SELECT [id], [username] FROM [tb_user] WHERE [tb_user].[id] = :id@1 AND [deleted] = 0) AS [q] WHERE [q].[id] = :id",
                mapOf("id" to 20, "id@1" to 10)
            ),
            DBType.Oracle to ExpectedTask(
                """SELECT "Q"."ID", "Q"."USERNAME" FROM (SELECT "ID", "USERNAME" FROM "TB_USER" WHERE "TB_USER"."ID" = :id@1 AND "DELETED" = 0) "Q" WHERE "Q"."ID" = :id""",
                mapOf("id" to 20, "id@1" to 10)
            )
        )

        coreSqlDialects.forEach { dialect ->
            val task = DialectUser()
                .select { [it.id, it.username] }
                .where { it.id == 10 }
                .filter { it.id == 20 }
                .build(dialect.wrapper)
                .atomicTask

            assertTaskEquals(expected.getValue(dialect.dbType), task, dialect.label)
        }
    }

    @Test
    fun `select limit zero renders explicit empty limit for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask(
                "SELECT `id` FROM `tb_user` WHERE `deleted` = 0 LIMIT 0"
            ),
            DBType.Postgres to ExpectedTask(
                """SELECT "id" FROM "tb_user" WHERE "deleted" = FALSE LIMIT 0"""
            ),
            DBType.SQLite to ExpectedTask(
                """SELECT "id" FROM "tb_user" WHERE "deleted" = 0 LIMIT 0"""
            ),
            DBType.Mssql to ExpectedTask(
                "SELECT TOP (0) [id] FROM [tb_user] WHERE [deleted] = 0"
            ),
            DBType.Oracle to ExpectedTask(
                """SELECT "ID" FROM "TB_USER" WHERE "DELETED" = 0 FETCH NEXT 0 ROWS ONLY"""
            )
        )

        coreSqlDialects.forEach { dialect ->
            val task = DialectUser()
                .select { it.id }
                .limit(0)
                .build(dialect.wrapper)
                .atomicTask

            assertTaskEquals(expected.getValue(dialect.dbType), task, dialect.label)
        }
    }

    @Test
    fun `sql server page without orderBy uses primary key order`() {
        initializeCoreSqlTestDefaults()

        val task = DialectUser()
            .select { [it.id, it.username] }
            .page(3, 5)
            .build(CapturingDialectWrapper(DBType.Mssql))
            .atomicTask

        assertTaskEquals(
            ExpectedTask(
                "SELECT [id], [username] FROM [tb_user] WHERE [deleted] = 0 ORDER BY [id] ASC OFFSET 10 ROWS FETCH NEXT 5 ROWS ONLY"
            ),
            task,
            "mssql"
        )
    }

    @Test
    fun `paged count renders legal sql for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask(
                "SELECT COUNT(*) FROM (SELECT 1 AS count_value FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) AS `total_count`",
                mapOf("id" to 1)
            ),
            DBType.Postgres to ExpectedTask(
                """SELECT COUNT(*) FROM (SELECT 1 AS count_value FROM "tb_user" WHERE "tb_user"."id" = :id AND "deleted" = FALSE) AS "total_count"""",
                mapOf("id" to 1)
            ),
            DBType.SQLite to ExpectedTask(
                """SELECT COUNT(*) FROM (SELECT 1 AS count_value FROM "tb_user" WHERE "tb_user"."id" = :id AND "deleted" = 0) AS "total_count"""",
                mapOf("id" to 1)
            ),
            DBType.Mssql to ExpectedTask(
                "SELECT COUNT(*) FROM (SELECT 1 AS count_value FROM [tb_user] WHERE [tb_user].[id] = :id AND [deleted] = 0) AS [total_count]",
                mapOf("id" to 1)
            ),
            DBType.Oracle to ExpectedTask(
                """SELECT COUNT(*) FROM (SELECT 1 AS COUNT_VALUE FROM "TB_USER" WHERE "TB_USER"."ID" = :id AND "DELETED" = 0) "TOTAL_COUNT"""",
                mapOf("id" to 1)
            )
        )

        coreSqlDialects.forEach { dialect ->
            val countTask = DialectUser(id = 1)
                .select { it.id }
                .where { it.id == 1 }
                .orderBy { it.id.desc() }
                .page(1, 10)
                .withTotal()
                .build(dialect.wrapper)
                .countTask
                .atomicTask

            assertTaskEquals(expected.getValue(dialect.dbType), countTask, dialect.label)
        }
    }

    @Test
    fun `cursor pagination renders complete sql for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask(
                "SELECT `id`, `username` FROM `tb_user` WHERE `tb_user`.`username` = :username AND `deleted` = 0 AND `id` < :cursor_id ORDER BY `id` DESC LIMIT 3",
                mapOf("username" to "neo", "cursor_id" to 10)
            ),
            DBType.Postgres to ExpectedTask(
                """SELECT "id", "username" FROM "tb_user" WHERE "tb_user"."username" = :username AND "deleted" = FALSE AND "id" < :cursor_id ORDER BY "id" DESC LIMIT 3""",
                mapOf("username" to "neo", "cursor_id" to 10)
            ),
            DBType.SQLite to ExpectedTask(
                """SELECT "id", "username" FROM "tb_user" WHERE "tb_user"."username" = :username AND "deleted" = 0 AND "id" < :cursor_id ORDER BY "id" DESC LIMIT 3""",
                mapOf("username" to "neo", "cursor_id" to 10)
            ),
            DBType.Mssql to ExpectedTask(
                "SELECT TOP (3) [id], [username] FROM [tb_user] WHERE [tb_user].[username] = :username AND [deleted] = 0 AND [id] < :cursor_id ORDER BY [id] DESC",
                mapOf("username" to "neo", "cursor_id" to 10)
            ),
            DBType.Oracle to ExpectedTask(
                """SELECT "ID", "USERNAME" FROM "TB_USER" WHERE "TB_USER"."USERNAME" = :username AND "DELETED" = 0 AND "ID" < :cursor_id ORDER BY "ID" DESC FETCH NEXT 3 ROWS ONLY""",
                mapOf("username" to "neo", "cursor_id" to 10)
            )
        )

        coreSqlDialects.forEach { dialect ->
            val wrapper = CapturingDialectWrapper(dialect.dbType)

            DialectUser()
                .select { [it.id, it.username] }
                .where { it.username == "neo" }
                .orderBy { it.id.desc() }
                .cursor(pageSize = 2, after = mapOf<String, Any?>("id" to 10).toCursor())
                .toMapList(wrapper)

            assertTaskEquals(expected.getValue(dialect.dbType), wrapper.queryTasks.single(), dialect.label)
        }
    }

    @Test
    fun `insert renders complete sql for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask(
                "INSERT INTO `tb_user` (`id`, `username`, `score`, `gender`, `deleted`) VALUES (:id, :username, :score, :gender, :deleted)",
                mapOf("id" to 1, "username" to "neo", "score" to 7, "gender" to 1, "deleted" to 0)
            ),
            DBType.Postgres to ExpectedTask(
                """INSERT INTO "tb_user" ("id", "username", "score", "gender", "deleted") VALUES (:id, :username, :score, :gender, :deleted)""",
                mapOf("id" to 1, "username" to "neo", "score" to 7, "gender" to 1, "deleted" to false)
            ),
            DBType.SQLite to ExpectedTask(
                """INSERT INTO "tb_user" ("id", "username", "score", "gender", "deleted") VALUES (:id, :username, :score, :gender, :deleted)""",
                mapOf("id" to 1, "username" to "neo", "score" to 7, "gender" to 1, "deleted" to 0)
            ),
            DBType.Mssql to ExpectedTask(
                "INSERT INTO [tb_user] ([id], [username], [score], [gender], [deleted]) VALUES (:id, :username, :score, :gender, :deleted)",
                mapOf("id" to 1, "username" to "neo", "score" to 7, "gender" to 1, "deleted" to 0)
            ),
            DBType.Oracle to ExpectedTask(
                """INSERT INTO "TB_USER" ("ID", "USERNAME", "SCORE", "GENDER", "DELETED") VALUES (:id, :username, :score, :gender, :deleted)""",
                mapOf("id" to 1, "username" to "neo", "score" to 7, "gender" to 1, "deleted" to 0)
            )
        )

        coreSqlDialects.forEach { dialect ->
            val task = DialectUser(id = 1, username = "neo", score = 7, gender = 1)
                .insert()
                .build(dialect.wrapper)
                .atomicTasks
                .single()

            assertTaskEquals(expected.getValue(dialect.dbType), task, dialect.label)
        }
    }

    @Test
    fun `update renders complete sql for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask(
                "UPDATE `tb_user` SET `username` = :usernameNew WHERE `id` = :id AND `deleted` = 0",
                mapOf("usernameNew" to "trinity", "id" to 1)
            ),
            DBType.Postgres to ExpectedTask(
                """UPDATE "tb_user" SET "username" = :usernameNew WHERE "id" = :id AND "deleted" = FALSE""",
                mapOf("usernameNew" to "trinity", "id" to 1)
            ),
            DBType.SQLite to ExpectedTask(
                """UPDATE "tb_user" SET "username" = :usernameNew WHERE "id" = :id AND "deleted" = 0""",
                mapOf("usernameNew" to "trinity", "id" to 1)
            ),
            DBType.Mssql to ExpectedTask(
                "UPDATE [tb_user] SET [username] = :usernameNew WHERE [id] = :id AND [deleted] = 0",
                mapOf("usernameNew" to "trinity", "id" to 1)
            ),
            DBType.Oracle to ExpectedTask(
                """UPDATE "TB_USER" SET "USERNAME" = :usernameNew WHERE "ID" = :id AND "DELETED" = 0""",
                mapOf("usernameNew" to "trinity", "id" to 1)
            )
        )

        coreSqlDialects.forEach { dialect ->
            val task = DialectUser(id = 1, username = "neo")
                .update()
                .set { it.username = "trinity" }
                .by { it.id }
                .build(dialect.wrapper)
                .atomicTasks
                .single()

            assertTaskEquals(expected.getValue(dialect.dbType), task, dialect.label)
        }
    }

    @Test
    fun `delete renders complete sql for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask("DELETE FROM `tb_user` WHERE `id` = :id", mapOf("id" to 1)),
            DBType.Postgres to ExpectedTask("""DELETE FROM "tb_user" WHERE "id" = :id""", mapOf("id" to 1)),
            DBType.SQLite to ExpectedTask("""DELETE FROM "tb_user" WHERE "id" = :id""", mapOf("id" to 1)),
            DBType.Mssql to ExpectedTask("DELETE FROM [tb_user] WHERE [id] = :id", mapOf("id" to 1)),
            DBType.Oracle to ExpectedTask("""DELETE FROM "TB_USER" WHERE "ID" = :id""", mapOf("id" to 1))
        )

        coreSqlDialects.forEach { dialect ->
            val task = DialectUser(id = 1)
                .delete()
                .logic(false)
                .by { it.id }
                .build(dialect.wrapper)
                .atomicTasks
                .single()

            assertTaskEquals(expected.getValue(dialect.dbType), task, dialect.label)
        }
    }

    @Test
    fun `predicate boolean constants render dialect safe sql for sql server and oracle`() {
        initializeCoreSqlTestDefaults()

        val expectedFalse = mapOf(
            DBType.Mssql to ExpectedTask(
                "SELECT [id] FROM [tb_user] WHERE 1 = 0 AND [deleted] = 0",
                emptyMap()
            ),
            DBType.Oracle to ExpectedTask(
                """SELECT "ID" FROM "TB_USER" WHERE 1 = 0 AND "DELETED" = 0""",
                emptyMap()
            )
        )
        val expectedTrue = mapOf(
            DBType.Mssql to ExpectedTask(
                "SELECT [id] FROM [tb_user] WHERE 1 = 1 AND [deleted] = 0",
                emptyMap()
            ),
            DBType.Oracle to ExpectedTask(
                """SELECT "ID" FROM "TB_USER" WHERE 1 = 1 AND "DELETED" = 0""",
                emptyMap()
            )
        )
        val expectedEmptyIn = mapOf(
            DBType.Mssql to ExpectedTask(
                "SELECT [id] FROM [tb_user] WHERE 1 = 0 AND [deleted] = 0",
                emptyMap()
            ),
            DBType.Oracle to ExpectedTask(
                """SELECT "ID" FROM "TB_USER" WHERE 1 = 0 AND "DELETED" = 0""",
                emptyMap()
            )
        )
        val expectedEmptyNotIn = mapOf(
            DBType.Mssql to ExpectedTask(
                "SELECT [id] FROM [tb_user] WHERE 1 = 1 AND [deleted] = 0",
                emptyMap()
            ),
            DBType.Oracle to ExpectedTask(
                """SELECT "ID" FROM "TB_USER" WHERE 1 = 1 AND "DELETED" = 0""",
                emptyMap()
            )
        )
        val expectedUpdateFalse = mapOf(
            DBType.Mssql to ExpectedTask(
                "UPDATE [tb_user] SET [username] = :usernameNew WHERE 1 = 0 AND [deleted] = 0",
                mapOf("usernameNew" to "trinity")
            ),
            DBType.Oracle to ExpectedTask(
                """UPDATE "TB_USER" SET "USERNAME" = :usernameNew WHERE 1 = 0 AND "DELETED" = 0""",
                mapOf("usernameNew" to "trinity")
            )
        )
        val expectedDeleteTrue = mapOf(
            DBType.Mssql to ExpectedTask("DELETE FROM [tb_user] WHERE 1 = 1", emptyMap()),
            DBType.Oracle to ExpectedTask("""DELETE FROM "TB_USER" WHERE 1 = 1""", emptyMap())
        )

        coreSqlDialects
            .filter { it.dbType == DBType.Mssql || it.dbType == DBType.Oracle }
            .forEach { dialect ->
                val falseTask = DialectUser()
                    .select { it.id }
                    .where { false.asSql() }
                    .build(dialect.wrapper)
                    .atomicTask
                val trueTask = DialectUser()
                    .select { it.id }
                    .where { true.asSql() }
                    .build(dialect.wrapper)
                    .atomicTask
                val emptyInTask = DialectUser()
                    .select { it.id }
                    .where { it.id in emptyList<Int>() }
                    .build(dialect.wrapper)
                    .atomicTask
                val emptyNotInTask = DialectUser()
                    .select { it.id }
                    .where { it.id !in emptyList<Int>() }
                    .build(dialect.wrapper)
                    .atomicTask
                val updateFalseTask = DialectUser(username = "neo")
                    .update()
                    .set { it.username = "trinity" }
                    .where { false.asSql() }
                    .build(dialect.wrapper)
                    .atomicTasks
                    .single()
                val deleteTrueTask = DialectUser()
                    .delete()
                    .logic(false)
                    .where { true.asSql() }
                    .build(dialect.wrapper)
                    .atomicTasks
                    .single()

                assertTaskEquals(expectedFalse.getValue(dialect.dbType), falseTask, "${dialect.label} false.asSql")
                assertTaskEquals(expectedTrue.getValue(dialect.dbType), trueTask, "${dialect.label} true.asSql")
                assertTaskEquals(expectedEmptyIn.getValue(dialect.dbType), emptyInTask, "${dialect.label} empty IN")
                assertTaskEquals(expectedEmptyNotIn.getValue(dialect.dbType), emptyNotInTask, "${dialect.label} empty NOT IN")
                assertTaskEquals(expectedUpdateFalse.getValue(dialect.dbType), updateFalseTask, "${dialect.label} update false.asSql")
                assertTaskEquals(expectedDeleteTrue.getValue(dialect.dbType), deleteTrueTask, "${dialect.label} delete true.asSql")
            }
    }

    @Test
    fun `on conflict upsert renders complete sql for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask(
                "INSERT INTO `tb_user` (`id`, `username`, `score`, `deleted`) VALUES (:id, :username, :score, :deleted) ON DUPLICATE KEY UPDATE `username` = :username, `deleted` = :deleted",
                mapOf("id" to 1, "username" to "neo", "score" to null, "deleted" to 0)
            ),
            DBType.Postgres to ExpectedTask(
                """INSERT INTO "tb_user" ("id", "username", "score", "deleted") VALUES (:id, :username, :score, :deleted) ON CONFLICT ("id") DO UPDATE SET "username" = :username, "deleted" = :deleted""",
                mapOf("id" to 1, "username" to "neo", "score" to null, "deleted" to false)
            ),
            DBType.SQLite to ExpectedTask(
                """INSERT INTO "tb_user" ("id", "username", "score", "deleted") VALUES (:id, :username, :score, :deleted) ON CONFLICT ("id") DO UPDATE SET "username" = :username, "deleted" = :deleted""",
                mapOf("id" to 1, "username" to "neo", "score" to null, "deleted" to 0)
            ),
            DBType.Mssql to ExpectedTask(
                "MERGE INTO [tb_user] AS [t1] USING (SELECT :id AS [id], :username AS [username], :score AS [score], :deleted AS [deleted]) AS [t2] ON ([t1].[id] = [t2].[id]) WHEN MATCHED THEN UPDATE SET [t1].[username] = :username, [t1].[deleted] = :deleted WHEN NOT MATCHED THEN INSERT ([id], [username], [score], [deleted]) VALUES (:id, :username, :score, :deleted);",
                mapOf("id" to 1, "username" to "neo", "score" to null, "deleted" to 0)
            ),
            DBType.Oracle to ExpectedTask(
                """MERGE INTO "TB_USER" "T1" USING (SELECT :id AS "ID", :username AS "USERNAME", :score AS "SCORE", :deleted AS "DELETED") "T2" ON ("T1"."ID" = "T2"."ID") WHEN MATCHED THEN UPDATE SET "T1"."USERNAME" = :username, "T1"."DELETED" = :deleted WHEN NOT MATCHED THEN INSERT ("ID", "USERNAME", "SCORE", "DELETED") VALUES (:id, :username, :score, :deleted)""",
                mapOf("id" to 1, "username" to "neo", "score" to null, "deleted" to 0)
            )
        )

        coreSqlDialects.forEach { dialect ->
            val task = DialectUser(id = 1, username = "neo")
                .upsert { it.username }
                .onConflict()
                .build(dialect.wrapper)
                .atomicTasks
                .single()

            assertTaskEquals(expected.getValue(dialect.dbType), task, dialect.label)
        }
    }

    @Test
    fun `join renders complete sql for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask(
                "SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender` FROM `tb_user` LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` AND `tb_user`.`gender` = `user_relation`.`gender` WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 ORDER BY `tb_user`.`id` DESC",
                mapOf("id" to 1)
            ),
            DBType.Postgres to ExpectedTask(
                """SELECT "tb_user"."id" AS "id", "user_relation"."gender" AS "gender" FROM "tb_user" LEFT JOIN "user_relation" ON "tb_user"."id" = "user_relation"."id2" AND "tb_user"."gender" = "user_relation"."gender" WHERE "tb_user"."id" = :id AND "tb_user"."deleted" = FALSE ORDER BY "tb_user"."id" DESC""",
                mapOf("id" to 1)
            ),
            DBType.SQLite to ExpectedTask(
                """SELECT "tb_user"."id" AS "id", "user_relation"."gender" AS "gender" FROM "tb_user" LEFT JOIN "user_relation" ON "tb_user"."id" = "user_relation"."id2" AND "tb_user"."gender" = "user_relation"."gender" WHERE "tb_user"."id" = :id AND "tb_user"."deleted" = 0 ORDER BY "tb_user"."id" DESC""",
                mapOf("id" to 1)
            ),
            DBType.Mssql to ExpectedTask(
                "SELECT [tb_user].[id] AS [id], [user_relation].[gender] AS [gender] FROM [tb_user] LEFT JOIN [user_relation] ON [tb_user].[id] = [user_relation].[id2] AND [tb_user].[gender] = [user_relation].[gender] WHERE [tb_user].[id] = :id AND [tb_user].[deleted] = 0 ORDER BY [tb_user].[id] DESC",
                mapOf("id" to 1)
            ),
            DBType.Oracle to ExpectedTask(
                """SELECT "TB_USER"."ID" AS "ID", "USER_RELATION"."GENDER" AS "GENDER" FROM "TB_USER" LEFT JOIN "USER_RELATION" ON "TB_USER"."ID" = "USER_RELATION"."ID2" AND "TB_USER"."GENDER" = "USER_RELATION"."GENDER" WHERE "TB_USER"."ID" = :id AND "TB_USER"."DELETED" = 0 ORDER BY "TB_USER"."ID" DESC""",
                mapOf("id" to 1)
            )
        )

        coreSqlDialects.forEach { dialect ->
            val task = DialectUser(id = 1)
                .join(UserRelation(id = 10, gender = 1, id2 = 1)) { user, relation ->
                    leftJoin { user.id == relation.id2 && user.gender == relation.gender }
                        .select { [user.id, relation.gender] }
                        .where { user.id == 1 }
                        .orderBy { user.id.desc() }
                }
                .build(dialect.wrapper)
                .atomicTask

            assertTaskEquals(expected.getValue(dialect.dbType), task, dialect.label)
        }
    }

    @Test
    fun `join limit zero renders explicit empty limit for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask(
                "SELECT `tb_user`.`id` AS `id` FROM `tb_user` LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` WHERE `tb_user`.`deleted` = 0 LIMIT 0"
            ),
            DBType.Postgres to ExpectedTask(
                """SELECT "tb_user"."id" AS "id" FROM "tb_user" LEFT JOIN "user_relation" ON "tb_user"."id" = "user_relation"."id2" WHERE "tb_user"."deleted" = FALSE LIMIT 0"""
            ),
            DBType.SQLite to ExpectedTask(
                """SELECT "tb_user"."id" AS "id" FROM "tb_user" LEFT JOIN "user_relation" ON "tb_user"."id" = "user_relation"."id2" WHERE "tb_user"."deleted" = 0 LIMIT 0"""
            ),
            DBType.Mssql to ExpectedTask(
                "SELECT TOP (0) [tb_user].[id] AS [id] FROM [tb_user] LEFT JOIN [user_relation] ON [tb_user].[id] = [user_relation].[id2] WHERE [tb_user].[deleted] = 0"
            ),
            DBType.Oracle to ExpectedTask(
                """SELECT "TB_USER"."ID" AS "ID" FROM "TB_USER" LEFT JOIN "USER_RELATION" ON "TB_USER"."ID" = "USER_RELATION"."ID2" WHERE "TB_USER"."DELETED" = 0 FETCH NEXT 0 ROWS ONLY"""
            )
        )

        coreSqlDialects.forEach { dialect ->
            val task = DialectUser()
                .join(UserRelation()) { user, relation ->
                    leftJoin { user.id == relation.id2 }
                        .select { user.id }
                        .limit(0)
                }
                .build(dialect.wrapper)
                .atomicTask

            assertTaskEquals(expected.getValue(dialect.dbType), task, dialect.label)
        }
    }

    @Test
    fun `union with limit offset renders complete sql for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask(
                "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0) ORDER BY `id` ASC LIMIT 5 OFFSET 2",
                mapOf("id" to 1, "id@1" to 2)
            ),
            DBType.Postgres to ExpectedTask(
                """(SELECT "id" FROM "tb_user" WHERE "tb_user"."id" = :id AND "deleted" = FALSE) UNION (SELECT "id" FROM "tb_user" WHERE "tb_user"."id" = :id@1 AND "deleted" = FALSE) ORDER BY "id" ASC LIMIT 5 OFFSET 2""",
                mapOf("id" to 1, "id@1" to 2)
            ),
            DBType.SQLite to ExpectedTask(
                """SELECT "id" FROM "tb_user" WHERE "tb_user"."id" = :id AND "deleted" = 0 UNION SELECT "id" FROM "tb_user" WHERE "tb_user"."id" = :id@1 AND "deleted" = 0 ORDER BY "id" ASC LIMIT 5 OFFSET 2""",
                mapOf("id" to 1, "id@1" to 2)
            ),
            DBType.Mssql to ExpectedTask(
                "(SELECT [id] FROM [tb_user] WHERE [tb_user].[id] = :id AND [deleted] = 0) UNION (SELECT [id] FROM [tb_user] WHERE [tb_user].[id] = :id@1 AND [deleted] = 0) ORDER BY [id] ASC OFFSET 2 ROWS FETCH NEXT 5 ROWS ONLY",
                mapOf("id" to 1, "id@1" to 2)
            ),
            DBType.Oracle to ExpectedTask(
                """(SELECT "ID" FROM "TB_USER" WHERE "TB_USER"."ID" = :id AND "DELETED" = 0) UNION (SELECT "ID" FROM "TB_USER" WHERE "TB_USER"."ID" = :id@1 AND "DELETED" = 0) ORDER BY "ID" ASC OFFSET 2 ROWS FETCH NEXT 5 ROWS ONLY""",
                mapOf("id" to 1, "id@1" to 2)
            )
        )

        coreSqlDialects.forEach { dialect ->
            val task = union(
                DialectUser().select { it.id }.where { it.id == 1 },
                DialectUser().select { it.id }.where { it.id == 2 }
            )
                .orderBy("id" to SqlOrdering.Asc)
                .limit(5, 2)
                .build(dialect.wrapper)
                .atomicTask

            assertTaskEquals(expected.getValue(dialect.dbType), task, dialect.label)
        }
    }

    @Test
    fun `union limit zero renders explicit empty limit when ordered`() {
        initializeCoreSqlTestDefaults()

        val task = union(
            DialectUser().select { it.id }.where { it.id == 1 },
            DialectUser().select { it.id }.where { it.id == 2 }
        )
            .orderBy("id" to SqlOrdering.Asc)
            .limit(0)
            .build(CapturingDialectWrapper(DBType.Mssql))
            .atomicTask

        assertTaskEquals(
            ExpectedTask(
                "(SELECT [id] FROM [tb_user] WHERE [tb_user].[id] = :id AND [deleted] = 0) UNION (SELECT [id] FROM [tb_user] WHERE [tb_user].[id] = :id@1 AND [deleted] = 0) ORDER BY [id] ASC OFFSET 0 ROWS FETCH NEXT 0 ROWS ONLY",
                mapOf("id" to 1, "id@1" to 2)
            ),
            task,
            "mssql"
        )
    }

    @Test
    fun `sql server union limit without orderBy fails before rendering invalid offset fetch`() {
        initializeCoreSqlTestDefaults()

        assertFailsWith<InvalidDataAccessApiUsageException> {
            union(
                DialectUser().select { it.id },
                DialectUser().select { it.id }
            )
                .limit(0)
                .build(CapturingDialectWrapper(DBType.Mssql))
        }
    }

    @Test
    fun `drop table renders complete ddl sql for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask("DROP TABLE IF EXISTS `tb_archive`"),
            DBType.Postgres to ExpectedTask("DROP TABLE IF EXISTS \"public\".\"tb_archive\""),
            DBType.SQLite to ExpectedTask("DROP TABLE IF EXISTS \"tb_archive\""),
            DBType.Mssql to ExpectedTask("DROP TABLE IF EXISTS [dbo].[tb_archive]"),
            DBType.Oracle to ExpectedTask("DROP TABLE \"TB_ARCHIVE\"")
        )

        coreSqlDialects.forEach { dialect ->
            val task = dialect.wrapper.table
                .buildDropTableTask("tb_archive")
                .atomicTasks
                .single()

            assertTaskEquals(expected.getValue(dialect.dbType), task, dialect.label)
        }
    }
}

private class CapturingDialectWrapper(
    override val dbType: DBType
) : KronosDataSourceWrapper {
    val queryTasks = mutableListOf<KAtomicQueryTask>()
    override val url: String = "jdbc:test://localhost/kronos"
    override val userName: String = "kronos"

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        queryTasks += task
        return emptyList()
    }

    override fun first(task: KAtomicQueryTask): Any? {
        queryTasks += task
        return null
    }

    override fun update(task: KAtomicActionTask): Int = 1
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf(1)
    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?
    ): Any? = TransactionScope().block()
}
