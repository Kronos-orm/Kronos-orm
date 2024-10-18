package com.kotlinorm.orm//package tests

import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.LineHumpNamingStrategy
import com.kotlinorm.orm.beans.Movie
import com.kotlinorm.orm.beans.UserRelation
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.utils.GsonResolver
import com.kotlinorm.orm.utils.TestWrapper
import com.kotlinorm.tableOperation.beans.MysqlUser
import com.kotlinorm.tableOperation.beans.ProductLog
import com.kotlinorm.utils.trimWhitespace
import kotlin.test.Test
import kotlin.test.assertEquals

class Join {

    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
            dataSource = { TestWrapper }
            serializeResolver = GsonResolver
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
                SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender`
                 FROM `tb_user`
                    LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2`
                    AND `tb_user`.`gender1` = `user_relation`.`gender` WHERE `tb_user`.`id` = :id
                    AND `tb_user`.`deleted` = 0 ORDER BY `tb_user`.`id` DESC
            """.trimWhitespace(), sql
        )
        assertEquals(mapOf("id" to 1, "username" to "123", "gender" to 1, "id2" to 1), paramMap)
    }

    @Test
    fun testJoinMultipleTables() {
        val (sql, paramMap) = MysqlUser(1).join(
            UserRelation(1, "123", 1, 1), Movie(1), ProductLog(1)
        ) { user, relation, movie, log ->
            leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
            rightJoin(movie) { movie.year == user.id }
            fullJoin(log) { log.id == user.id }
            select {
                user.id + relation.gender + movie.id
            }
            where { user.id == 1 }
            orderBy { user.id.desc() }
        }.build()

        assertEquals(
            """
                SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender`, `movie`.`id` AS `id@1` 
                FROM `tb_user` 
                    LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` AND `tb_user`.`gender1` = `user_relation`.`gender` 
                    RIGHT JOIN `movie` ON `movie`.`year` = `tb_user`.`id` AND `movie`.`deleted` = 0 
                    FULL JOIN `product_log` ON `product_log`.`id` = `tb_user`.`id` 
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
            UserRelation(1, "123", 1, 1), Movie(1), ProductLog(1)
        ) { user, relation, movie, log ->
            on {
                user.id == relation.id2 && user.gender == relation.gender && movie.year == user.id && log.id == user.id
            }
            select {
                user.id + relation.gender + movie.id
            }
            where { user.id == 1 }
            orderBy { user.id.desc() }
        }.build()

        assertEquals(
            """
                SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender`, `movie`.`id` AS `id@1` 
                FROM `tb_user` 
                    LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` AND `tb_user`.`gender1` = `user_relation`.`gender`
                    LEFT JOIN `movie` ON `movie`.`year` = `tb_user`.`id` AND `movie`.`deleted` = 0
                    LEFT JOIN `product_log` ON `product_log`.`id` = `tb_user`.`id` 
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
            UserRelation(1, "123", 1, 1), Movie(1), ProductLog(1)
        ) { user, relation, movie, log ->
            leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
            rightJoin(movie) { movie.year == user.id }
            fullJoin(log) { log.id == user.id }
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
                SELECT COUNT(1) FROM 
                    (SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender`, `movie`.`id` AS `id@1` 
                        FROM `tb_user` 
                            LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` AND `tb_user`.`gender1` = `user_relation`.`gender` 
                            RIGHT JOIN `movie` ON `movie`.`year` = `tb_user`.`id` AND `movie`.`deleted` = 0 FULL JOIN `product_log` ON `product_log`.`id` = `tb_user`.`id` 
                        WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 
                        ORDER BY `tb_user`.`id` DESC LIMIT 10 OFFSET 0) AS t
            """.trimWhitespace(),
            sql
        )
        assertEquals(mapOf("id" to 1, "username" to "123", "gender" to 1, "deleted" to 0.toByte(), "id2" to 1, "status" to -1, "remarkNum" to 0), paramMap)
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
                SELECT `tb_user`.`id` AS `id`, `test`.`user_relation`.`gender` AS `gender` 
                FROM `tb_user` 
                    LEFT JOIN `test`.`user_relation` ON `tb_user`.`id` = `test`.`user_relation`.`id2` AND `tb_user`.`gender1` = `test`.`user_relation`.`gender` 
                WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 
                ORDER BY `tb_user`.`id` DESC""".trimWhitespace(),
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
                "count(1)"
            }
            where { user.id == 1 }
        }.build()

        assertEquals(
            """
                SELECT count(1) 
                FROM `tb_user` 
                    LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` 
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