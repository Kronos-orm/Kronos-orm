/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.render

import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import kotlin.test.Test
import kotlin.test.assertEquals

class DmlRendererTest {
    @Test
    fun rendersInsertUpdateAndDeleteWithMysqlDialect() {
        val user = table("user")
        val insert = SqlDmlStatement.Insert(
            table = user,
            columns = cols("id", "name"),
            mode = SqlInsertMode.Values(listOf(listOf(num("1"), str("Ada"))))
        )
        val update = SqlDmlStatement.Update(
            table = user,
            setPairs = listOf(set("name", str("Grace"))),
            where = col("id").eq(num("1"))
        )
        val delete = SqlDmlStatement.Delete(
            table = user,
            where = col("id").eq(num("1"))
        )

        assertEquals("INSERT INTO `user` (`id`, `name`) VALUES (1, 'Ada')", insert.toSql(SqlDialect.MySql))
        assertEquals("UPDATE `user` SET `name` = 'Grace' WHERE `id` = 1", update.toSql(SqlDialect.MySql))
        assertEquals("DELETE FROM `user` WHERE `id` = 1", delete.toSql(SqlDialect.MySql))
    }

    @Test
    fun rendersTruncateAndMergeStyleUpsert() {
        val user = table("user")
        val upsert = SqlDmlStatement.Upsert(
            table = user,
            columns = cols("id", "name"),
            values = listOf(num("1"), str("Ada")),
            primaryKeys = cols("id")
        )

        assertEquals("TRUNCATE TABLE \"user\"", SqlDmlStatement.Truncate(user).toSql())
        assertEquals(
            """MERGE INTO "user" AS "t1" USING (SELECT 1 AS "id", 'Ada' AS "name") AS "t2" ON ("t1"."id" = "t2"."id") WHEN MATCHED THEN UPDATE SET "t1"."name" = "t2"."name" WHEN NOT MATCHED THEN INSERT ("id", "name") VALUES (1, 'Ada')""",
            upsert.toSql()
        )
    }
}
