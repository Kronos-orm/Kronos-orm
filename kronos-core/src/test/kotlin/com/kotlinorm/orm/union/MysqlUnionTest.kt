package com.kotlinorm.orm.union

import com.kotlinorm.beans.sample.UserRelation
import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.utils.trimWhitespace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MysqlUnionTest : MysqlTestBase() {

    @Test
    fun testSimpleUnion() {
        val task1 = MysqlUser().select().where { it.id == 1 }.build()
        val task2 = MysqlUser().select().where { it.id == 2 }.build()
        
        println("Task 1 SQL: ${task1.atomicTask.sql}")
        println("Task 1 params: ${task1.atomicTask.paramMap}")
        println("Task 2 SQL: ${task2.atomicTask.sql}")
        println("Task 2 params: ${task2.atomicTask.paramMap}")
        
        val (sql, paramMap) = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 }
        ).build()

        println("Union SQL: $sql")
        println("Union params: $paramMap")

        assertEquals(
            """
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id AND `deleted` = 0) 
                UNION 
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id@1 AND `deleted` = 0)
            """.trimWhitespace(),
            sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2), paramMap)
    }

    @Test
    fun testUnionAll() {
        val (sql, paramMap) = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 }
        ).all().build()

        assertEquals(
            """
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id AND `deleted` = 0) 
                UNION ALL 
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id@1 AND `deleted` = 0)
            """.trimWhitespace(),
            sql
        )
    }

    @Test
    fun testInfixUnion() {
        val (sql, paramMap) = (MysqlUser().select().where { it.id == 1 }
            union MysqlUser().select().where { it.id == 2 }).build()

        assertEquals(
            """
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id AND `deleted` = 0) 
                UNION 
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id@1 AND `deleted` = 0)
            """.trimWhitespace(),
            sql
        )
    }

    @Test
    fun testInfixUnionAll() {
        val (sql, paramMap) = (MysqlUser().select().where { it.id == 1 }
            unionAll MysqlUser().select().where { it.id == 2 }).build()

        assertEquals(
            """
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id AND `deleted` = 0) 
                UNION ALL 
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id@1 AND `deleted` = 0)
            """.trimWhitespace(),
            sql
        )
    }

    @Test
    fun testThreeTableUnion() {
        val (sql, paramMap) = (MysqlUser().select().where { it.id == 1 }
            union MysqlUser().select().where { it.id == 2 }
            union MysqlUser().select().where { it.id == 3 }).build()

        assertEquals(
            """
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id AND `deleted` = 0) 
                UNION 
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id@1 AND `deleted` = 0) 
                UNION 
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id@2 AND `deleted` = 0)
            """.trimWhitespace(),
            sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2, "id@2" to 3), paramMap)
    }

    @Test
    fun testUnionWithSelectFields() {
        val (sql, paramMap) = union(
            MysqlUser().select { it.id + it.username }.where { it.id == 1 },
            MysqlUser().select { it.id + it.username }.where { it.id == 2 }
        ).build()

        assertEquals(
            """
                (SELECT `id`, `username` 
                FROM `tb_user` 
                WHERE `id` = :id AND `deleted` = 0) 
                UNION 
                (SELECT `id`, `username` 
                FROM `tb_user` 
                WHERE `id` = :id@1 AND `deleted` = 0)
            """.trimWhitespace(),
            sql
        )
    }

    @Test
    fun testUnionWithJoin() {
        val query1 = MysqlUser().select().where { it.id == 1 }
        val query2 = MysqlUser(2).join(UserRelation(2, "test", 1, 1)) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select { user.id + user.username + relation.gender }
        }
        
        println("Query 1 SQL: ${query1.build().atomicTask.sql}")
        println("Query 1 params: ${query1.build().atomicTask.paramMap}")
        println("Query 2 SQL: ${query2.build().atomicTask.sql}")
        println("Query 2 params: ${query2.build().atomicTask.paramMap}")
        
        val (sql, paramMap) = union(query1, query2).build()

        println("Union SQL: $sql")
        println("Union params: $paramMap")

        assertEquals(
            """
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id AND `deleted` = 0) 
                UNION 
                (SELECT `tb_user`.`id` AS `id`, `tb_user`.`username` AS `username`, `user_relation`.`gender` AS `gender` 
                FROM `tb_user` 
                LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` 
                WHERE `tb_user`.`id` = :id@1 AND `tb_user`.`deleted` = 0)
            """.trimWhitespace(),
            sql
        )
    }

    @Test
    fun testUnionWithMultipleJoins() {
        val (sql, paramMap) = (
            MysqlUser(1).join(UserRelation(1, "test1", 1, 1)) { user, relation ->
                leftJoin(relation) { user.id == relation.id2 }
                select { user.id + relation.gender }
            }
            union MysqlUser(2).join(UserRelation(2, "test2", 1, 1)) { user, relation ->
                leftJoin(relation) { user.id == relation.id2 }
                select { user.id + relation.gender }
            }
        ).build()

        assertEquals(
            """
                (SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender` 
                FROM `tb_user` 
                LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` 
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0) 
                UNION 
                (SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender` 
                FROM `tb_user` 
                LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` 
                WHERE `tb_user`.`id` = :id@1 AND `tb_user`.`deleted` = 0)
            """.trimWhitespace(),
            sql
        )
    }

    @Test
    fun testUnionWithLimit() {
        val (sql, paramMap) = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 }
        ).limit(10).build()

        assertEquals(
            """
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id AND `deleted` = 0) 
                UNION 
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id@1 AND `deleted` = 0) 
                LIMIT 10
            """.trimWhitespace(),
            sql
        )
    }

    @Test
    fun testUnionWithLimitAndOffset() {
        val (sql, paramMap) = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 }
        ).limit(10, 5).build()

        assertEquals(
            """
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id AND `deleted` = 0) 
                UNION 
                (SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` 
                FROM `tb_user` 
                WHERE `id` = :id@1 AND `deleted` = 0) 
                LIMIT 10 OFFSET 5
            """.trimWhitespace(),
            sql
        )
    }

    @Test
    fun testComplexUnionWithJoinAndLimit() {
        val (sql, paramMap) = union(
            MysqlUser().select { it.id + it.username }.where { it.id == 1 },
            MysqlUser(2).join(UserRelation(2, "test", 1, 1)) { user, relation ->
                leftJoin(relation) { user.id == relation.id2 }
                select { user.id + user.username }
            },
            MysqlUser().select { it.id + it.username }.where { it.id == 3 }
        ).limit(20).build()

        assertEquals(
            """
                (SELECT `id`, `username`
                FROM `tb_user`
                WHERE `id` = :id AND `deleted` = 0)
                UNION
                (SELECT `tb_user`.`id` AS `id`, `tb_user`.`username` AS `username`
                FROM `tb_user`
                LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2`
                WHERE `tb_user`.`id` = :id@1 AND `tb_user`.`deleted` = 0)
                UNION
                (SELECT `id`, `username`
                FROM `tb_user`
                WHERE `id` = :id@2 AND `deleted` = 0)
                LIMIT 20
            """.trimWhitespace(),
            sql
        )
    }

    @Test
    fun testUnionWithDifferentWhereConditions() {
        val (sql, paramMap) = union(
            MysqlUser().select { it.id + it.username }.where { it.username == "Alice" },
            MysqlUser().select { it.id + it.username }.where { it.gender == 1 }
        ).build()

        assertEquals(
            """
                (SELECT `id`, `username`
                FROM `tb_user`
                WHERE `username` = :username AND `deleted` = 0)
                UNION
                (SELECT `id`, `username`
                FROM `tb_user`
                WHERE `gender` = :gender AND `deleted` = 0)
            """.trimWhitespace(),
            sql
        )
        assertEquals(mapOf("username" to "Alice", "gender" to 1), paramMap)
    }

    @Test
    fun testUnionParamMapContainsAllValues() {
        val (sql, paramMap) = union(
            MysqlUser().select().where { it.id == 10 },
            MysqlUser().select().where { it.id == 20 },
            MysqlUser().select().where { it.id == 30 }
        ).build()

        // Verify all three id params are present with disambiguation suffixes
        assertTrue(paramMap.containsKey("id"), "paramMap should contain 'id'")
        assertTrue(paramMap.containsKey("id@1"), "paramMap should contain 'id@1'")
        assertTrue(paramMap.containsKey("id@2"), "paramMap should contain 'id@2'")
        assertEquals(10, paramMap["id"])
        assertEquals(20, paramMap["id@1"])
        assertEquals(30, paramMap["id@2"])
    }

    @Test
    fun testUnionAllWithThreeTables() {
        val (sql, paramMap) = (MysqlUser().select { it.id }.where { it.id == 1 }
            unionAll MysqlUser().select { it.id }.where { it.id == 2 }
            unionAll MysqlUser().select { it.id }.where { it.id == 3 }).build()

        assertEquals(
            """
                (SELECT `id`
                FROM `tb_user`
                WHERE `id` = :id AND `deleted` = 0)
                UNION ALL
                (SELECT `id`
                FROM `tb_user`
                WHERE `id` = :id@1 AND `deleted` = 0)
                UNION ALL
                (SELECT `id`
                FROM `tb_user`
                WHERE `id` = :id@2 AND `deleted` = 0)
            """.trimWhitespace(),
            sql
        )
        assertEquals(mapOf("id" to 1, "id@1" to 2, "id@2" to 3), paramMap)
    }

    @Test
    fun testUnionWithRangeWhereConditions() {
        val (sql, paramMap) = union(
            MysqlUser().select { it.id + it.username }.where { it.id > 0 && it.id < 10 },
            MysqlUser().select { it.id + it.username }.where { it.id > 100 && it.id < 200 }
        ).build()

        assertEquals(
            """
                (SELECT `id`, `username`
                FROM `tb_user`
                WHERE `id` > :idMin AND `id` < :idMax AND `deleted` = 0)
                UNION
                (SELECT `id`, `username`
                FROM `tb_user`
                WHERE `id` > :idMin@1 AND `id` < :idMax@1 AND `deleted` = 0)
            """.trimWhitespace(),
            sql
        )
        assertEquals(0, paramMap["idMin"])
        assertEquals(10, paramMap["idMax"])
        assertEquals(100, paramMap["idMin@1"])
        assertEquals(200, paramMap["idMax@1"])
    }

    @Test
    fun testUnionSqlContainsBacktickQuoting() {
        val (sql, _) = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 }
        ).build()

        // MySQL dialect uses backtick quoting
        assertTrue(sql.contains("`tb_user`"), "SQL should use backtick quoting for table name")
        assertTrue(sql.contains("`id`"), "SQL should use backtick quoting for column name")
        assertTrue(sql.contains("UNION"), "SQL should contain UNION keyword")
        assertTrue(!sql.contains("UNION ALL"), "SQL should not contain UNION ALL for plain union")
    }

    @Test
    fun testUnionAllSqlContainsUnionAllKeyword() {
        val (sql, _) = union(
            MysqlUser().select().where { it.id == 1 },
            MysqlUser().select().where { it.id == 2 }
        ).all().build()

        assertTrue(sql.contains("UNION ALL"), "SQL should contain UNION ALL keyword")
    }
}

