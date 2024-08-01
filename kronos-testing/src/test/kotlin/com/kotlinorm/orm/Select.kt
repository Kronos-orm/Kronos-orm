package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.orm.beans.Movie
import com.kotlinorm.orm.beans.User
import com.kotlinorm.orm.database.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.relationQuery.oneToMany.School
import com.kotlinorm.orm.relationQuery.oneToMany.Student
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.utils.GsonResolver
import com.kotlinorm.tableOperation.beans.MysqlUser
import org.apache.commons.dbcp2.BasicDataSource
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Select {

    private val ds = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/test"
        username = "root"
        password = ""
    }

    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
            dataSource = { KronosBasicWrapper(ds) }
            serializeResolver = GsonResolver
        }
    }

    val user = MysqlUser(2)

    @Test
    fun testSelectAllParams() {
        val (sql, paramMap) = user.select { }.build()

        assertEquals(mapOf("id" to 2), paramMap)
        assertEquals(
            "SELECT `id`, `username`, `gender`, `habbits`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0",
            sql
        )
    }

    @Test
    fun testSelectOneParam() {
        val (sql, paramMap) = user.select { it.id }.build()

        assertEquals(mapOf("id" to 2), paramMap)
        assertEquals("SELECT `id` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0", sql)
    }

    @Test
    fun testSelectParams() {

        val (sql, paramMap) = user.select { it.id + it.username + it.gender + "123" }.build()

        assertEquals("SELECT `id`, `username`, `gender`, 123 FROM `tb_user` WHERE `id` = :id AND `deleted` = 0", sql)
        assertEquals(mapOf("id" to 2), paramMap)
    }

    @Test
    fun testSingle() {
        val (sql, paramMap) = user.select { }.single().build()

        assertEquals(mapOf("id" to 2), paramMap)
        assertEquals(
            "SELECT `id`, `username`, `gender`, `habbits`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0 LIMIT 1",
            sql
        )
    }

    @Test
    fun testLimit() {
        val (sql, paramMap) = user.select { }.limit(10).build()

        assertEquals(mapOf("id" to 2), paramMap)
        assertEquals(
            "SELECT `id`, `username`, `gender`, `habbits`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0 LIMIT 10",
            sql
        )
    }

    @Test
    fun testPage() {

        val (total, task) = user.select { }.page(1, 10).withTotal().build()
        val (sql, paramMap) = task
        val (sql2, paramMap2) = total

        assertEquals(
            "SELECT `id`, `username`, `gender`, `habbits`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0 LIMIT 10 OFFSET 0",
            sql
        )
        assertEquals(mapOf("id" to 2), paramMap)

        assertEquals(
            "SELECT COUNT(1) FROM (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = 0 LIMIT 10 OFFSET 0) AS t",
            sql2
        )
        assertEquals(mapOf("id" to 2), paramMap2)

    }

    @Test
    fun testSelectComplex() {
        val (sql, paramMap) = MysqlUser()
            .select { it.username }
            .where { it.id < 10 }
            .distinct()
            .groupBy { it.id }
            .orderBy { it.username.desc() }
            .having { it.id.eq }
            .build()

        assertEquals(mapOf("idMax" to 10), paramMap)
        assertEquals(
            "SELECT DISTINCT `username` FROM `tb_user` WHERE `id` < :idMax AND `deleted` = 0 GROUP BY `id` HAVING `id` = :id ORDER BY `username` DESC",
            sql
        )
    }

    @Test
    fun testAsSql() {

        val (sql, paramMap) = user.select { it.id + it.username.`as`("name") + it.gender + "COUNT(1) as `count`" }.lock(PessimisticLock.X)
            .build()

        assertEquals(
            "SELECT `id`, `username` AS `name`, `gender`, COUNT(1) as `count` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("id" to 2), paramMap)
    }

    @Test
    fun testAlias() {

        val (sql, paramMap) = user.select { it.id + it.username.`as`("name") }
            .where { it.gender == 0 }
            .build()

        assertEquals(
            "SELECT `id`, `username` AS `name` FROM `tb_user` WHERE `gender` = :gender AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("gender" to 0), paramMap)
    }

    @Test
    fun testGetKey() {
        val (sql, paramMap) = user.select { it.id + it.username }
            .where { it.id == 0 || it.id == 2 || it.id == 3 }
            .build()

        assertEquals(
            "SELECT `id`, `username` AS `name` FROM `tb_user` WHERE `gender` = :gender AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("gender" to 0), paramMap)
    }

    @Test
    fun testRegexp() {
        val (sql, paramMap, task) = user.select { it.id + it.username }
            .where { (it.id == 0 || it.id == 2 || it.id == 3) && it.username.regexp("\\d+") }
            .build()

        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE (`id` = :id OR `id` = :id@1 OR `id` = :id@2) AND `username` REGEXP :usernamePattern AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 0,
                "id@1" to 2,
                "id@2" to 3,
                "usernamePattern" to "\\d+"
            ), paramMap
        )

        val data = task.query()

        println(data)
    }

    @Test
    fun testDatebase() {
        dataSource.table.dropTable<User>()
        dataSource.table.createTable<User>()

        val testU = User(1 , "a")
        testU.insert().execute()

        val res = testU.select {it.id}.queryOne<Int>()
        println(res)
    }

    @Test
    fun testDatebase1() {
        val res = user.select {
            it.id + Movie().select { m -> m.id }.where { m -> m.director == it.username }.single()
        }.queryOne()

        println(res)
    }

    @Test
    fun testSetDbName() {

        val (sql, paramMap) = user.select { it.id + it.username.`as`("name") }
            .where { it.gender == 0 }.db("test")
            .build()

        assertEquals(
            "SELECT `id`, `username` AS `username` FROM `test`.`tb_user` WHERE `gender` = :gender AND `deleted` = 0",
            sql
        )
    }
}