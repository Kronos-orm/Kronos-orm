package com.kotlinorm.orm.select

import com.kotlinorm.Kronos
import com.kotlinorm.beans.sample.database.MysqlUser
import com.kotlinorm.enums.NoValueStrategyType
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.functions.bundled.exts.MathFunctions.add
import com.kotlinorm.functions.bundled.exts.MathFunctions.sub
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.avg
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
import com.kotlinorm.functions.bundled.exts.StringFunctions.concat
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper.Companion.sampleMysqlJdbcWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

class MysqlSelectTest {
    init {
        // 配置Kronos ORM框架的基本设置
        Kronos.init {
            // 设置字段命名策略为驼峰命名
            fieldNamingStrategy = lineHumpNamingStrategy
            // 设置表命名策略为驼峰命名
            tableNamingStrategy = lineHumpNamingStrategy
            // 设置数据源提供器
            dataSource = { sampleMysqlJdbcWrapper }
        }
    }

    val user = MysqlUser(2)

    @Test
    fun testSelectAllParams() {
        val (sql, paramMap) = user.select().build()

        assertEquals(mapOf("id" to 2), paramMap)
        assertEquals(
            "SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0",
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
        val (sql, paramMap) = user.select().single().build()

        assertEquals(mapOf("id" to 2), paramMap)
        assertEquals(
            "SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0 LIMIT 1",
            sql
        )
    }

    @Test
    fun testLimit() {
        val (sql, paramMap) = user.select().limit(10).build()

        assertEquals(mapOf("id" to 2), paramMap)
        assertEquals(
            "SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0 LIMIT 10",
            sql
        )
    }

    @Test
    fun testPage() {

        val (total, task) = user.select().page(1, 10).withTotal().build()
        val (sql, paramMap) = task
        val (sql2, paramMap2) = total

        assertEquals(
            "SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0 LIMIT 10 OFFSET 0",
            sql
        )
        assertEquals(mapOf("id" to 2), paramMap)

        assertEquals(
            "SELECT COUNT(1) FROM (SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = 0) AS total_count",
            sql2
        )
        assertEquals(mapOf("id" to 2), paramMap2)

    }

    @Test
    fun testSelectComplex() {
        val (sql, paramMap) = user
            .select { it.username }
            .where { it.id < 10 }
            .distinct()
            .groupBy { it.id }
            .orderBy { it.username.desc() }
            .having { it.id.eq }
            .build()

        assertEquals(mapOf("idMax" to 10, "id" to 2), paramMap)
        assertEquals(
            "SELECT DISTINCT `username` FROM `tb_user` WHERE `id` < :idMax AND `deleted` = 0 GROUP BY `id` HAVING `id` = :id ORDER BY `username` DESC",
            sql
        )
    }

    @Test
    fun testAsSql() {

        val (sql, paramMap) = user.select { it.id + it.username.as_("name") + it.gender + "COUNT(1) as `count`" }
            .lock(PessimisticLock.X)
            .build()

        assertEquals(
            "SELECT `id`, `username` AS `name`, `gender`, COUNT(1) as `count` FROM `tb_user` WHERE `id` = :id AND `deleted` = 0 FOR UPDATE",
            sql
        )
        assertEquals(mapOf("id" to 2), paramMap)
    }

    @Test
    fun testAlias() {

        val (sql, paramMap) = user.select { it.id + it.username.as_("name") }
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
            "SELECT `id`, `username` FROM `tb_user` WHERE `id` = :id OR `id` = :id@1 OR `id` = :id@2 AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("id" to 0, "id@1" to 2, "id@2" to 3), paramMap)
    }

    @Test
    fun testSelectIn() {
        val (sql, paramMap) = user.select { it.id + it.username }
            .where { it.id in listOf(0, 2, 3) }
            .build()

        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE `id` IN (:idList) AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("idList" to listOf(0, 2, 3)), paramMap)

        val (sql2, paramMap2) = user.select { it.id + it.username }
            .where { it.id in listOf<Int>() }
            .build()

        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE false AND `deleted` = 0",
            sql2
        )
        assertEquals(mapOf<String, Any>(), paramMap2)
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

        task.query()
    }

    @Test
    fun testSetDbName() {

        val (sql, paramMap) = user.select { it.id + it.username.as_("name") }
            .where { it.gender == 0 }.db("test")
            .build()

        assertEquals(
            "SELECT `id`, `username` AS `name` FROM `test`.`tb_user` WHERE `gender` = :gender AND `deleted` = 0",
            sql
        )
    }

    @Test
    fun testSelectCount() {

        val (sql, paramMap) = user.select { "count(1)" }
            .where { it.gender == 0 }.db("test")
            .build()

        assertEquals(
            "SELECT count(1) FROM `test`.`tb_user` WHERE `gender` = :gender AND `deleted` = 0",
            sql
        )
    }

    @Test
    fun testIfNoValue() {
        val (sql, paramMap) = user.select { "count(1)" }.where { it.gender.gt.ifNoValue(NoValueStrategyType.Ignore) }
            .build()

        assertEquals(
            "SELECT count(1) FROM `tb_user` WHERE `deleted` = 0",
            sql
        )
    }

    @Test
    fun testSelectUseEqMinus() {
        val (sql, paramMap) = user.select { "1" }
            .where { (it - it.gender).eq }
            .build()

        assertEquals(
            "SELECT 1 FROM `tb_user` WHERE `id` = :id AND `deleted` = 0",
            sql
        )
    }

    @Test
    fun testSelectUserConst() {
        val (sql, paramMap) = user.select { "1" }.where { "true".asSql() }.build()

        assertEquals(
            "SELECT 1 FROM `tb_user` WHERE true AND `deleted` = 0",
            sql
        )
    }

    @Test
    fun testSelectUseConstEqualGetValue() {
        val another = MysqlUser(id = 1)
        val (sql, paramMap) = user.select { "1" }.where { 1 == another.id.value }.build()

        assertEquals(
            "SELECT 1 FROM `tb_user` WHERE true AND `deleted` = 0",
            sql
        )
    }

    @Test
    fun testSelectBuiltInFunctionCount() {
        val (sql, paramMap) = user.select {
            f.count(1) + it.id + f.avg(it.id) + it.username + f.sum(it.id)
        }.where {
            f.add(it.id, 1) > f.sub(it.id, 1) && f.length(it.username) > 5 && it.username like f.concat(
                "%",
                it.username,
                "%"
            )
        }.build()

        assertEquals(
            "SELECT COUNT(1) AS count, `id`, AVG(`id`) AS avg, `username`, SUM(`id`) AS sum FROM `tb_user` WHERE (`id` - 1) < (`id` + 1) AND LENGTH(`username`) > :lengthMin AND `username` LIKE CONCAT('%', `username`, '%') AND `deleted` = 0",
            sql
        )

        assertEquals(mapOf("lengthMin" to 5), paramMap)
    }
}