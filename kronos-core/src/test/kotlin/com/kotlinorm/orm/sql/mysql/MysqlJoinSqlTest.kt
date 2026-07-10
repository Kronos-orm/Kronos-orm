package com.kotlinorm.orm.sql.mysql

import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.where
import com.kotlinorm.orm.union.union
import com.kotlinorm.orm.union.unionAll
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert

import com.kotlinorm.GsonProcessor
import com.kotlinorm.Kronos
import com.kotlinorm.testfixtures.entities.Address
import com.kotlinorm.testfixtures.entities.Movie
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.utils.trimWhitespace
import kotlin.test.Test
import kotlin.test.assertEquals

class MysqlJoinSqlTest : MysqlTestBase() {
    init {
        Kronos.serializeProcessor = GsonProcessor
    }

    @Test
    fun testJoinOneTable() {
        val (sql, paramMap) = TestUser(1).join(
            UserRelation(1, "123", 1, 1),
        ) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
            select {
                [user.id, relation.gender]
            }
            where { user.id == 1 }
            orderBy { user.id.desc() }
        }.build()

        assertEquals(
            """
                SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender` 
                FROM `tb_user` 
                LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` AND `tb_user`.`gender` = `user_relation`.`gender` 
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 
                ORDER BY `tb_user`.`id` DESC
            """.trimWhitespace(), sql
        )
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testJoinMultipleTables() {
        val (sql, paramMap) = TestUser(1).join(
            UserRelation(1, "123", 1, 1), Movie(1), Address(1)
        ) { user, relation, movie, address ->
            leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
            rightJoin(movie) { movie.year == user.id }
            fullJoin(address) { address.userId == user.id }
            select {
                [user.id, relation.gender, movie.id]
            }
            where { user.id == 1 }
            orderBy { user.id.desc() }
        }.build()

        assertEquals(
            """
                SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender`, `movie`.`id` AS `id@1` 
                FROM `tb_user` 
                LEFT JOIN `user_relation` 
                ON `tb_user`.`id` = `user_relation`.`id2` AND `tb_user`.`gender` = `user_relation`.`gender` 
                RIGHT JOIN `movie` 
                ON `movie`.`year` = `tb_user`.`id` AND `movie`.`deleted` = 0 
                FULL JOIN `tb_address` 
                ON `tb_address`.`user_id` = `tb_user`.`id` AND `tb_address`.`deleted` = 0 
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 
                ORDER BY `tb_user`.`id` DESC
            """.trimWhitespace(),
            sql
        )
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testOnMultipleTables() {
        val (sql, paramMap) = TestUser(1).join(
            UserRelation(1, "123", 1, 1), Movie(1), Address(1)
        ) { user, relation, movie, address ->
            leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
            leftJoin(movie) { movie.year == user.id }
            leftJoin(address) { address.userId == user.id }
            select {
                [user.id, relation.gender, movie.id]
            }
            where { user.id == 1 }
            orderBy { user.id.desc() }
        }.build()

        assertEquals(
            """
                SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender`, `movie`.`id` AS `id@1` 
                FROM `tb_user` 
                LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` AND `tb_user`.`gender` = `user_relation`.`gender`
                LEFT JOIN `movie` ON `movie`.`year` = `tb_user`.`id` AND `movie`.`deleted` = 0 
                LEFT JOIN `tb_address` ON `tb_address`.`user_id` = `tb_user`.`id` AND `tb_address`.`deleted` = 0
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 
                ORDER BY `tb_user`.`id` DESC
            """.trimWhitespace(), sql
        )
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testWithTotal() {
        val (cnt, rcd) = TestUser(1).join(
            UserRelation(1, "123", 1, 1), Movie(1), Address(1)
        ) { user, relation, movie, address ->
            leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
            rightJoin(movie) { movie.year == user.id }
            fullJoin(address) { address.userId == user.id }
            select {
                [user.id, relation.gender, movie.id]
            }
            where { user.id == 1 }
            orderBy { user.id.desc() }
            page(1, 10)
        }.withTotal().build()
        val (sql, paramMap) = cnt
        val (sql2, paramMap2) = rcd

        assertEquals(
            """
               SELECT COUNT(*) FROM (SELECT 1 AS count_value
                    FROM `tb_user`
                    LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` AND `tb_user`.`gender` = `user_relation`.`gender`
                    RIGHT JOIN `movie` ON `movie`.`year` = `tb_user`.`id` AND `movie`.`deleted` = 0
                    FULL JOIN `tb_address` ON `tb_address`.`user_id` = `tb_user`.`id` AND `tb_address`.`deleted` = 0
                    WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0) AS `total_count`
            """.trimWhitespace(),
            sql
        )
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testSetDbName() {
        val (sql, paramMap) = TestUser(1).join(
            UserRelation(1, "123", 1, 1),
        ) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
            select {
                [user.id, relation.gender]
            }
            db(relation to "test")
            where { user.id == 1 }
            orderBy { user.id.desc() }
        }.build()

        assertEquals(
            """
                SELECT `tb_user`.`id` AS `id`, `test`.`user_relation`.`gender` AS `gender` 
                FROM `tb_user` 
                LEFT JOIN `test`.`user_relation` 
                ON `tb_user`.`id` = `test`.`user_relation`.`id2` AND `tb_user`.`gender` = `test`.`user_relation`.`gender` 
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 
                ORDER BY `tb_user`.`id` DESC
            """.trimWhitespace(),
            sql
        )
    }

    @Test
    fun testSelectCount() {
        val (sql, paramMap) = TestUser(1).join(
            UserRelation(1, "123", 1, 1),
        ) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select {
                f.count(1)
            }
            where { user.id == 1 }
        }.build()

        assertEquals(
            """
                SELECT COUNT(1) AS count FROM `tb_user`
                LEFT JOIN `user_relation` 
                ON `tb_user`.`id` = `user_relation`.`id2` 
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0
            """.trimWhitespace(), sql
        )

        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun `join aggregate function argument keeps source table qualifier`() {
        val (sql, paramMap) = TestUser(1).join(
            UserRelation(1, "123", 1, 1),
        ) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 }
            select {
                [
                    user.id,
                    f.count(relation.id).alias("relationCount"),
                ]
            }
            groupBy { user.id }
            having { f.count(relation.id) > 0 }
            where { user.id == 1 }
        }.build()

        assertEquals(
            """
                SELECT COUNT(`user_relation`.`id`) AS relationCount, `tb_user`.`id` AS `id` FROM `tb_user`
                LEFT JOIN `user_relation`
                ON `tb_user`.`id` = `user_relation`.`id2`
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0
                GROUP BY `tb_user`.`id`
                HAVING COUNT(`user_relation`.`id`) > :countMin
            """.trimWhitespace(),
            sql
        )
        assertEquals(mapOf("countMin" to 0, "id" to 1), paramMap)
    }
}
