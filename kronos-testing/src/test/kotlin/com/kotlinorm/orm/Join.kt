package com.kotlinorm.orm//package tests

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.beans.Movie
import com.kotlinorm.orm.beans.UserRelation
import com.kotlinorm.orm.database.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.utils.GsonResolver
import com.kotlinorm.utils.queryList
import com.kotlinorm.utils.queryMap
import com.kotlinorm.utils.queryOne
import com.kotlinorm.tableOperation.beans.MysqlUser
import com.kotlinorm.tableOperation.beans.ProductLog
import org.apache.commons.dbcp.BasicDataSource
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Join {
    private val ds = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/test"
    }

    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
            dataSource = { KronosBasicWrapper(ds) }
            serializeResolver = GsonResolver
        }
    }

    @Test
    fun testJoinOneTable() {
        val (sql, paramMap) =
            MysqlUser(1).join(
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
            "SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender` " +
                    "FROM `tb_user` " +
                    "LEFT JOIN `user_relation` " +
                    "ON `tb_user`.`id` = `user_relation`.`id2` AND `tb_user`.`gender` = `user_relation`.`gender` " +
                    "WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 " +
                    "ORDER BY `tb_user`.`id` DESC",
            sql
        )
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testJoinMultipleTables() {
        val (sql, paramMap) =
            MysqlUser(1).join(
                UserRelation(1, "123", 1, 1),
                Movie(1),
                ProductLog(1)
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
            "SELECT `tb_user`.`id` AS `id`, `user_relation`.`gender` AS `gender`, `movie`.`id` AS `id@1` " +
                    "FROM `tb_user` " +
                    "LEFT JOIN `user_relation` " +
                    "ON `tb_user`.`id` = `user_relation`.`id2` AND `tb_user`.`gender` = `user_relation`.`gender` " +
                    "RIGHT JOIN `movie` " +
                    "ON `movie`.`year` = `tb_user`.`id` AND `movie`.`deleted` = 0 " +
                    "FULL JOIN `product_log` " +
                    "ON `product_log`.`id` = `tb_user`.`id` " +
                    "WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 " +
                    "ORDER BY `tb_user`.`id` DESC",
            sql
        )
        assertEquals(mapOf("id" to 1), paramMap)
    }

    @Test
    fun testWithTotal() {
        val (cnt, rcd) =
            MysqlUser(1).join(
                UserRelation(1, "123", 1, 1),
                Movie(1),
                ProductLog(1)
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

        assertEquals(
            "SELECT COUNT(1) " +
                    "FROM " +
                    "(" +
                    "SELECT 1 " +
                    "FROM `tb_user` " +
                    "LEFT JOIN `user_relation` " +
                    "ON `tb_user`.`id` = `user_relation`.`id2` AND `tb_user`.`gender` = `user_relation`.`gender` " +
                    "RIGHT JOIN `movie` " +
                    "ON `movie`.`year` = `tb_user`.`id` AND `movie`.`deleted` = 0 " +
                    "FULL JOIN `product_log` " +
                    "ON `product_log`.`id` = `tb_user`.`id` " +
                    "WHERE `tb_user`.`id` = :id AND `tb_user`.`deleted` = 0 " +
                    "ORDER BY `tb_user`.`id` DESC " +
                    "LIMIT 10 OFFSET 0" +
                    ") AS t",
            cnt.sql
        )
        assertEquals(mapOf("id" to 1), cnt.paramMap)
    }

    @Test
    fun testQuery() {

        dataSource.table.dropTable<MysqlUser>()
        dataSource.table.dropTable<UserRelation>()
        dataSource.table.dropTable<Movie>()
        dataSource.table.createTable<MysqlUser>()
        dataSource.table.createTable<UserRelation>()
        dataSource.table.createTable<Movie>()

        val user = MysqlUser(1)
        val relation = UserRelation(1, "123", 1, 1)
        val movie = Movie(1 , year = 1)

        user.insert().execute()
        relation.insert().execute()
        movie.insert().execute()

        val task =
            user.join(
                relation,
                movie
            ) { u, r, m ->
                leftJoin(r) { u.id == r.id }
                rightJoin(m) { m.year == u.id }
                select {
                    u.id + r.gender + m.id.`as`("username")
                }
                where { u.id == 1 }
                orderBy { u.id.desc() }
            }.build()

        val data = task.queryList<MysqlUser>()

        println()
    }

}