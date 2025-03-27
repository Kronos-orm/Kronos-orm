package com.kotlinorm.orm.join

import com.kotlinorm.GsonProcessor
import com.kotlinorm.Kronos
import com.kotlinorm.beans.sample.Address
import com.kotlinorm.beans.sample.Movie
import com.kotlinorm.beans.sample.UserRelation
import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.utils.trimWhitespace
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper.Companion.sampleMysqlJdbcWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

class MysqlJoinTest {

    init {
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
            dataSource = { sampleMysqlJdbcWrapper }
            serializeProcessor = GsonProcessor
        }
    }

    @Test
    fun testJoinOneTable() {
        val (sql, paramMap) = MysqlUser(1).join(
            UserRelation(1, "123", 1, 1),
        ) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
            select {
                user.id + relation.gender
            }
            where { user.id == 1 }
            orderBy { user.id.desc() }
        }.build()

        assertEquals(
            """
                SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender` FROM `tb_user` 
                LEFT JOIN `user_relation` ON `user_relation`.`id2` = `tb_user`.`id` AND `user_relation`.`gender` = `tb_user`.`gender` 
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 
                ORDER BY `tb_user`.`id` DESC
            """.trimWhitespace(), sql
        )
        assertEquals(mapOf("id" to 1, "username" to "123", "gender" to 1, "id2" to 1), paramMap)
    }

    @Test
    fun testJoinMultipleTables() {
        val (sql, paramMap) = MysqlUser(1).join(
            UserRelation(1, "123", 1, 1), Movie(1), Address(1)
        ) { user, relation, movie, address ->
            leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
            rightJoin(movie) { movie.year == user.id }
            fullJoin(address) { address.userId == user.id }
            select {
                user.id + relation.gender + movie.id
            }
            where { user.id == 1 }
            orderBy { user.id.desc() }
        }.build()

        assertEquals(
            """
                SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender`, `movie`.`id` AS `id@1` FROM `tb_user` 
                LEFT JOIN `user_relation` 
                ON `user_relation`.`id2` = `tb_user`.`id` AND `user_relation`.`gender` = `tb_user`.`gender` 
                RIGHT JOIN `movie` ON `tb_user`.`id` = `movie`.`year` AND `movie`.`deleted` = 0 
                FULL JOIN `product_log` ON `tb_user`.`id` = `product_log`.`id` 
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 
                ORDER BY `tb_user`.`id` DESC
            """.trimWhitespace(),
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "username" to "123",
                "gender" to 1,
                "deleted" to 0.toByte(),
                "id2" to 1,
                "status" to -1,
                "remarkNum" to 0
            ), paramMap
        )
    }

    @Test
    fun testOnMultipleTables() {
        val (sql, paramMap) = MysqlUser(1).join(
            UserRelation(1, "123", 1, 1), Movie(1), Address(1)
        ) { user, relation, movie, address ->
            on {
                user.id == relation.id2 && user.gender == relation.gender && movie.year == user.id && address.userId == user.id
            }
            select {
                user.id + relation.gender + movie.id
            }
            where { user.id == 1 }
            orderBy { user.id.desc() }
        }.build()

        assertEquals(
            """
                SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender`, `movie`.`id` AS `id@1` FROM `tb_user` 
                LEFT JOIN `user_relation` ON `user_relation`.`id2` = `tb_user`.`id` AND `user_relation`.`gender` = `tb_user`.`gender` 
                LEFT JOIN `movie` ON `tb_user`.`id` = `movie`.`year` AND `movie`.`deleted` = 0 
                LEFT JOIN `product_log` ON `tb_user`.`id` = `product_log`.`id` 
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 
                ORDER BY `tb_user`.`id` DESC
            """.trimWhitespace(), sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "username" to "123",
                "gender" to 1,
                "deleted" to 0.toByte(),
                "id2" to 1,
                "status" to -1,
                "remarkNum" to 0
            ), paramMap
        )
    }

    @Test
    fun testWithTotal() {
        val (cnt, rcd) = MysqlUser(1).join(
            UserRelation(1, "123", 1, 1), Movie(1), Address(1)
        ) { user, relation, movie, address ->
            leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
            rightJoin(movie) { movie.year == user.id }
            fullJoin(address) { address.userId == user.id }
            select {
                user.id + relation.gender + movie.id
            }
            where { user.id == 1 }
            orderBy { user.id.desc() }
            page(1, 10)
        }.withTotal().build()
        val (sql, paramMap) = cnt
        val (sql2, paramMap2) = rcd

        assertEquals(
            """
                SELECT COUNT(1) FROM (SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender`, `movie`.`id` AS `id@1` FROM `tb_user` 
                    LEFT JOIN `user_relation` ON `user_relation`.`id2` = `tb_user`.`id` AND `user_relation`.`gender` = `tb_user`.`gender` 
                    RIGHT JOIN `movie` ON `tb_user`.`id` = `movie`.`year` AND `movie`.`deleted` = 0 
                    FULL JOIN `product_log` ON `tb_user`.`id` = `product_log`.`id` 
                    WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 
                    ORDER BY `tb_user`.`id` DESC LIMIT 10 OFFSET 0) AS t
            """.trimWhitespace(),
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "username" to "123",
                "gender" to 1,
                "deleted" to 0.toByte(),
                "id2" to 1,
                "status" to -1,
                "remarkNum" to 0
            ), paramMap
        )
    }

    @Test
    fun testSetDbName() {
        val (sql, paramMap) = MysqlUser(1).join(
            UserRelation(1, "123", 1, 1),
        ) { user, relation ->
            leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
            select {
                user.id + relation.gender
            }
            db(relation to "test")
            where { user.id == 1 }
            orderBy { user.id.desc() }
        }.build()

        assertEquals(
            """
                SELECT `tb_user`.`id` AS `id`, `test`.`user_relation`.`gender` AS `gender` FROM `tb_user` 
                LEFT JOIN `test`.`user_relation` ON `test`.`user_relation`.`id2` = `tb_user`.`id` AND `test`.`user_relation`.`gender` = `tb_user`.`gender` 
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 
                ORDER BY `tb_user`.`id` DESC
            """.trimWhitespace(),
            sql
        )
    }

    @Test
    fun testSelectCount() {
        val (sql, paramMap) = MysqlUser(1).join(
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
                ON `user_relation`.`id2` = `tb_user`.`id`
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0
            """.trimWhitespace(), sql
        )

        assertEquals(
            mapOf(
                "id" to 1, "username" to "123", "gender" to 1, "id2" to 1
            ), paramMap
        )
    }
}