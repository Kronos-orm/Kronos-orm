/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.pagination.OffsetPageQuery
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SamplePostgresJdbcWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimeTableNamePropagationTest : MysqlTestBase() {

    @Test
    fun `select page requalifies declared fields after runtime table override`() {
        val user = TestUser(1, "alice").apply { __tableName = "tb_user_001" }

        val task = user
            .select { it.id }
            .where { it.id == 1 }
            .page(pageIndex = 2, pageSize = 10)
            .build(SamplePostgresJdbcWrapper())
            .atomicTask

        assertEquals(
            "SELECT \"id\" FROM \"tb_user_001\" WHERE \"tb_user_001\".\"id\" = :id AND \"deleted\" = FALSE LIMIT 10 OFFSET 10",
            task.sql
        )
        assertEquals(mapOf("id" to 1), task.paramMap)
    }

    @Test
    fun `total page requalifies runtime table name in count and records`() {
        val user = TestUser(1, "alice").apply { __tableName = "tb_user_001" }

        val tasks = user
            .select { it.id }
            .where { it.id == 1 }
            .page(pageIndex = 2, pageSize = 10)
            .withTotal()
            .build(SamplePostgresJdbcWrapper())

        assertEquals(
            "SELECT COUNT(*) FROM (SELECT 1 AS count_value FROM \"tb_user_001\" " +
                "WHERE \"tb_user_001\".\"id\" = :id AND \"deleted\" = FALSE) AS \"total_count\"",
            tasks.countTask.atomicTask.sql
        )
        assertEquals(
            "SELECT \"id\" FROM \"tb_user_001\" WHERE \"tb_user_001\".\"id\" = :id " +
                "AND \"deleted\" = FALSE LIMIT 10 OFFSET 10",
            tasks.recordsTask.atomicTask.sql
        )
        assertEquals(mapOf("id" to 1), tasks.countTask.atomicTask.paramMap)
        assertEquals(mapOf("id" to 1), tasks.recordsTask.atomicTask.paramMap)
    }

    @Test
    fun `join requalifies conditions and projections for runtime table overrides`() {
        val user = TestUser(1, "alice").apply { __tableName = "tb_user_001" }
        val relation = UserRelation(2, "alice", id2 = 1).apply { __tableName = "tb_relation_001" }

        val joined = user.join(relation) { first, second ->
            innerJoin { first.id == second.id2 }
                .select { [first.id, second.id2] }
                .where { first.id == 1 }
        }
        val task = joined
            .page(pageIndex = 2, pageSize = 10)
            .build(SamplePostgresJdbcWrapper())
            .atomicTask

        assertEquals(
            "SELECT \"tb_user_001\".\"id\" AS \"id\", \"tb_relation_001\".\"id2\" AS \"id2\" " +
                "FROM \"tb_user_001\" INNER JOIN \"tb_relation_001\" " +
                "ON \"tb_user_001\".\"id\" = \"tb_relation_001\".\"id2\" " +
                "WHERE \"tb_user_001\".\"id\" = :id AND \"tb_user_001\".\"deleted\" = FALSE LIMIT 10 OFFSET 10",
            task.sql
        )
        assertEquals(mapOf("id" to 1), task.paramMap)
    }

    @Test
    fun `union materializes each runtime table name`() {
        val first = TestUser(1).apply { __tableName = "tb_user_001" }
            .select { it.id }
            .where { it.id == 1 }
        val second = TestUser(2).apply { __tableName = "tb_user_002" }
            .select { it.id }
            .where { it.id == 2 }

        val task = union(first, second).build(SamplePostgresJdbcWrapper()).atomicTask

        assertEquals(
            "(SELECT \"id\" FROM \"tb_user_001\" WHERE \"tb_user_001\".\"id\" = :id AND \"deleted\" = FALSE) " +
                "UNION (SELECT \"id\" FROM \"tb_user_002\" WHERE \"tb_user_002\".\"id\" = :id@1 AND \"deleted\" = FALSE)",
            task.sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2), task.paramMap)
    }

    @Test
    fun `union page preserves runtime table names in both branches`() {
        val first = TestUser(1).apply { __tableName = "tb_user_001" }
            .select { it.id }
            .where { it.id == 1 }
        val second = TestUser(2).apply { __tableName = "tb_user_002" }
            .select { it.id }
            .where { it.id == 2 }

        val task = OffsetPageQuery(
            union(first, second).limit(limit = 10, offset = 10),
            pageIndex = 2,
            pageSize = 10
        )
            .build(SamplePostgresJdbcWrapper())
            .atomicTask

        assertEquals(
            "(SELECT \"id\" FROM \"tb_user_001\" WHERE \"tb_user_001\".\"id\" = :id AND \"deleted\" = FALSE) " +
                "UNION (SELECT \"id\" FROM \"tb_user_002\" WHERE \"tb_user_002\".\"id\" = :id@1 AND \"deleted\" = FALSE) " +
                "LIMIT 10 OFFSET 10",
            task.sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2), task.paramMap)
    }

    @Test
    fun `ddl builder uses runtime table name`() {
        val user = TestUser().apply { __tableName = "tb_user_001" }

        val statement = Kronos.dataSource.table.buildCreateTableStatement(user)

        assertEquals("tb_user_001", statement.tableName.last)

        val source = TestUser().apply { __tableName = "tb_user_source_001" }
        val task = Kronos.dataSource.table.buildCreateTableAsSelectTask(
            TestUser().apply { __tableName = "tb_user_archive_001" },
            source.select { it.id }.where { it.id == 1 }
        ).atomicTasks.single()

        assertEquals(
            "CREATE TABLE IF NOT EXISTS `tb_user_archive_001` AS SELECT `id` FROM `tb_user_source_001` " +
                "WHERE `tb_user_source_001`.`id` = :id AND `deleted` = 0",
            task.sql
        )
    }
}
