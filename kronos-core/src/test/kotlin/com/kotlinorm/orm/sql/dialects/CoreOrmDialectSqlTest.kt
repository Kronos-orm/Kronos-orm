package com.kotlinorm.orm.sql.dialects

import com.kotlinorm.enums.DBType
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert
import com.kotlinorm.testfixtures.entities.DialectUser
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testutils.ExpectedTask
import com.kotlinorm.testutils.assertTaskEquals
import com.kotlinorm.testutils.coreSqlDialects
import com.kotlinorm.testutils.initializeCoreSqlTestDefaults
import kotlin.test.Test

class CoreOrmDialectSqlTest {

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
    fun `native upsert renders complete sql for every supported dialect`() {
        initializeCoreSqlTestDefaults()

        val expected = mapOf(
            DBType.Mysql to ExpectedTask(
                "INSERT INTO `tb_user` (`id`, `username`, `score`, `deleted`) VALUES (:id, :username, :score, :deleted) ON DUPLICATE KEY UPDATE `username` = :username",
                mapOf("id" to 1, "username" to "neo", "score" to null, "deleted" to 0)
            ),
            DBType.Postgres to ExpectedTask(
                """INSERT INTO "tb_user" ("id", "username", "score", "deleted") VALUES (:id, :username, :score, :deleted) ON CONFLICT ("id") DO UPDATE SET "username" = :username""",
                mapOf("id" to 1, "username" to "neo", "score" to null, "deleted" to false)
            ),
            DBType.SQLite to ExpectedTask(
                """INSERT INTO "tb_user" ("id", "username", "score", "deleted") VALUES (:id, :username, :score, :deleted) ON CONFLICT ("id") DO UPDATE SET "username" = :username""",
                mapOf("id" to 1, "username" to "neo", "score" to null, "deleted" to 0)
            ),
            DBType.Mssql to ExpectedTask(
                "MERGE INTO [tb_user] AS [t1] USING (SELECT :id AS [id], :username AS [username], :score AS [score], :deleted AS [deleted]) AS [t2] ON ([t1].[id] = [t2].[id]) WHEN MATCHED THEN UPDATE SET [t1].[username] = :username WHEN NOT MATCHED THEN INSERT ([id], [username], [score], [deleted]) VALUES (:id, :username, :score, :deleted)",
                mapOf("id" to 1, "username" to "neo", "score" to null, "deleted" to 0)
            ),
            DBType.Oracle to ExpectedTask(
                """MERGE INTO "TB_USER" "T1" USING (SELECT :id AS "ID", :username AS "USERNAME", :score AS "SCORE", :deleted AS "DELETED") "T2" ON ("T1"."ID" = "T2"."ID") WHEN MATCHED THEN UPDATE SET "T1"."USERNAME" = :username WHEN NOT MATCHED THEN INSERT ("ID", "USERNAME", "SCORE", "DELETED") VALUES (:id, :username, :score, :deleted)""",
                mapOf("id" to 1, "username" to "neo", "score" to null, "deleted" to 0)
            )
        )

        coreSqlDialects.forEach { dialect ->
            val task = DialectUser(id = 1, username = "neo")
                .upsert { it.username }
                .on { it.id }
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
                    leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
                    select { [user.id, relation.gender] }
                    where { user.id == 1 }
                    orderBy { user.id.desc() }
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
                "(SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0) UNION (SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id@1 AND `deleted` = 0) LIMIT 5 OFFSET 2",
                mapOf("id" to 1, "id@1" to 2)
            ),
            DBType.Postgres to ExpectedTask(
                """(SELECT "id" FROM "tb_user" WHERE "tb_user"."id" = :id AND "deleted" = FALSE) UNION (SELECT "id" FROM "tb_user" WHERE "tb_user"."id" = :id@1 AND "deleted" = FALSE) LIMIT 5 OFFSET 2""",
                mapOf("id" to 1, "id@1" to 2)
            ),
            DBType.SQLite to ExpectedTask(
                """SELECT "id" FROM "tb_user" WHERE "tb_user"."id" = :id AND "deleted" = 0 UNION SELECT "id" FROM "tb_user" WHERE "tb_user"."id" = :id@1 AND "deleted" = 0 LIMIT 5 OFFSET 2""",
                mapOf("id" to 1, "id@1" to 2)
            ),
            DBType.Mssql to ExpectedTask(
                "(SELECT [id] FROM [tb_user] WHERE [tb_user].[id] = :id AND [deleted] = 0) UNION (SELECT [id] FROM [tb_user] WHERE [tb_user].[id] = :id@1 AND [deleted] = 0) OFFSET 2 ROWS FETCH NEXT 5 ROWS ONLY",
                mapOf("id" to 1, "id@1" to 2)
            ),
            DBType.Oracle to ExpectedTask(
                """(SELECT "ID" FROM "TB_USER" WHERE "TB_USER"."ID" = :id AND "DELETED" = 0) UNION (SELECT "ID" FROM "TB_USER" WHERE "TB_USER"."ID" = :id@1 AND "DELETED" = 0) OFFSET 2 ROWS FETCH NEXT 5 ROWS ONLY""",
                mapOf("id" to 1, "id@1" to 2)
            )
        )

        coreSqlDialects.forEach { dialect ->
            val task = union(
                DialectUser().select { it.id }.where { it.id == 1 },
                DialectUser().select { it.id }.where { it.id == 2 }
            )
                .limit(5, 2)
                .build(dialect.wrapper)
                .atomicTask

            assertTaskEquals(expected.getValue(dialect.dbType), task, dialect.label)
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
