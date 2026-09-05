@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.orm.sql.mysql

import com.kotlinorm.annotations.UnsafeProjectionOverride
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.select.where
import com.kotlinorm.orm.union.union
import com.kotlinorm.orm.union.unionAll
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert

import com.kotlinorm.testfixtures.entities.Product
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.functions.bundled.exts.MathFunctions.add
import com.kotlinorm.functions.bundled.exts.MathFunctions.sub
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.avg
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
import com.kotlinorm.functions.bundled.exts.StringFunctions.concat
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.syntax.statement.SqlLock
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.reflect.typeOf

class MysqlSelectSqlTest : MysqlTestBase() {

    private val user by lazy { TestUser(2) }

    @Test
    fun testSelectAllParams() {
        val (sql, paramMap) = user.select().build()

        assertEquals(emptyMap(), paramMap)
        assertEquals(
            "SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `deleted` = 0",
            sql
        )
    }

    @Test
    fun testSelectOneParam() {
        val (sql, paramMap) = user.select { it.id }.build()

        assertEquals(emptyMap(), paramMap)
        assertEquals("SELECT `id` FROM `tb_user` WHERE `deleted` = 0", sql)
    }

    @Test
    fun testSelectParams() {

        val (sql, paramMap) = user.select { [it.id, it.username, it.gender, "123".alias("literalValue")] }.build()

        assertEquals("SELECT `id`, `username`, `gender`, 123 AS `literalValue` FROM `tb_user` WHERE `deleted` = 0", sql)
        assertEquals(emptyMap(), paramMap)
    }

    @OptIn(UnsafeProjectionOverride::class)
    @Test
    fun testDuplicateProjectionNamesReserveExplicitSuffixes() {
        val task = user
            .select { [it.id, it.id, it.username.alias("id_1")] }
            .build()
            .atomicTask

        assertEquals(
            "SELECT `id`, `id` AS `id_2`, `username` AS `id_1` FROM `tb_user` WHERE `deleted` = 0",
            task.sql
        )
        assertEquals(typeOf<Int?>(), task.resultColumns["id"]?.type)
        assertEquals(typeOf<Int?>(), task.resultColumns["id_2"]?.type)
        assertEquals(typeOf<String?>(), task.resultColumns["id_1"]?.type)
    }

    @Test
    fun testWhereSugarMatchesSelectWhere() {
        val (sugarSql, sugarParams) = TestUser().where { it.id == 10 }.build()
        val (explicitSql, explicitParams) = TestUser().select().where { it.id == 10 }.build()

        assertEquals(explicitSql, sugarSql)
        assertEquals(explicitParams, sugarParams)
    }

    @Test
    fun testSingle() {
        val (sql, paramMap) = user.select().single().build()

        assertEquals(emptyMap(), paramMap)
        assertEquals(
            "SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `deleted` = 0 LIMIT 1",
            sql
        )
    }

    @Test
    fun testLimit() {
        val (sql, paramMap) = user.select().limit(10).build()

        assertEquals(emptyMap(), paramMap)
        assertEquals(
            "SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `deleted` = 0 LIMIT 10",
            sql
        )
    }

    @Test
    fun testPage() {

        val tasks = user.select().page(1, 10).withTotal().build()
        val (sql, paramMap) = tasks.recordsTask
        val (sql2, paramMap2) = tasks.countTask

        assertEquals(
            "SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `deleted` = 0 LIMIT 10 OFFSET 0",
            sql
        )
        assertEquals(emptyMap(), paramMap)

        assertEquals(
            "SELECT COUNT(*) FROM (SELECT 1 AS count_value FROM `tb_user` WHERE `deleted` = 0) AS `total_count`",
            sql2
        )
        assertEquals(emptyMap(), paramMap2)

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
            "SELECT DISTINCT `username` FROM `tb_user` WHERE `tb_user`.`id` < :idMax AND `deleted` = 0 GROUP BY `id` HAVING `tb_user`.`id` = :id ORDER BY `username` DESC",
            sql
        )
    }

    @Test
    fun testAsSql() {

        val (sql, paramMap) = user.select { [it.id, it.username.alias("name"), it.gender, "COUNT(1)".alias("count")] }
            .lock(SqlLock.Update())
            .build()

        assertEquals(
            "SELECT `id`, `username` AS `name`, `gender`, COUNT(1) AS `count` FROM `tb_user` WHERE `deleted` = 0 FOR UPDATE",
            sql
        )
        assertEquals(emptyMap(), paramMap)
    }

    @Test
    fun testAlias() {

        val (sql, paramMap) = user.select { [it.id, it.username.alias("name")] }
            .where { it.gender == 0 }
            .build()

        assertEquals(
            "SELECT `id`, `username` AS `name` FROM `tb_user` WHERE `tb_user`.`gender` = :gender AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("gender" to 0), paramMap)
    }

    @Test
    fun testGetKey() {
        val (sql, paramMap) = user.select { [it.id, it.username] }
            .where { it.id == 0 || it.id == 2 || it.id == 3 }
            .build()

        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE (`tb_user`.`id` = :id OR `tb_user`.`id` = :id@1 OR `tb_user`.`id` = :id@2) AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("id" to 0, "id@1" to 2, "id@2" to 3), paramMap)
    }

    @Test
    fun testSelectIn() {
        val (sql, paramMap, task) = user.select { [it.id, it.username] }
            .where { it.id in [0, 2, 3] }
            .build()

        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE `tb_user`.`id` IN (:idList) AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("idList" to [0, 2, 3]), paramMap)
        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE `tb_user`.`id` IN (?, ?, ?) AND `deleted` = 0",
            task.atomicTask.parsed().jdbcSql
        )
        assertEquals([0, 2, 3], task.atomicTask.parsed().jdbcParamList.toList())

        val (sql2, paramMap2) = user.select { [it.id, it.username] }
            .where { it.id in emptyList<Int>() }
            .build()

        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE false AND `deleted` = 0",
            sql2
        )
        assertEquals(mapOf<String, Any>(), paramMap2)

        val (sql3, paramMap3) = user.select { [it.id, it.username] }
            .where { it.id in emptyList<Int>() }
            .build()

        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE false AND `deleted` = 0",
            sql3
        )
        assertEquals(mapOf<String, Any>(), paramMap3)

        val ids = ["1", "2", "3"]
        val (sql4, paramMap4) = user.select { [it.id, it.username] }
            .where { it.username in ids }
            .build()

        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE `tb_user`.`username` IN (:usernameList) AND `deleted` = 0",
            sql4
        )
        assertEquals(mapOf("usernameList" to ids), paramMap4)
    }

    @Test
    fun testRegexp() {
        val (sql, paramMap, task) = user.select { [it.id, it.username] }
            .where { (it.id == 0 || it.id == 2 || it.id == 3) && it.username.regexp("\\d+") }
            .build()

        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE (`tb_user`.`id` = :id OR `tb_user`.`id` = :id@1 OR `tb_user`.`id` = :id@2) AND `tb_user`.`username` REGEXP :usernamePattern AND `deleted` = 0",
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

        task.toMapList()
    }

    @Test
    fun testSetDbName() {

        val (sql, paramMap) = user.select { [it.id, it.username.alias("name")] }
            .where { it.gender == 0 }.db("test")
            .build()

        assertEquals(
            "SELECT `id`, `username` AS `name` FROM `test`.`tb_user` WHERE `tb_user`.`gender` = :gender AND `deleted` = 0",
            sql
        )
    }

    @Test
    fun testSelectCount() {

        val (sql, paramMap) = user.select { "count(1)".alias("count") }
            .where { it.gender == 0 }.db("test")
            .build()

        assertEquals(
            "SELECT count(1) AS `count` FROM `test`.`tb_user` WHERE `tb_user`.`gender` = :gender AND `deleted` = 0",
            sql
        )
    }

    @Test
    fun testTakeIfOmittedCondition() {
        val (sql, paramMap) = user.select { "count(1)".alias("count") }.where { it.gender.gt.takeIf(false) }
            .build()

        assertEquals(
            "SELECT count(1) AS `count` FROM `tb_user` WHERE `deleted` = 0",
            sql
        )
    }

    @Test
    fun testSelectUseEqMinus() {
        val (sql, paramMap) = user.select { "1".alias("one") }
            .where { (it - it.gender).eq }
            .build()

        assertEquals(
            "SELECT 1 AS `one` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0",
            sql
        )
    }

    @Test
    fun testSelectUserConst() {
        val (sql, paramMap) = user.select { "1".alias("one") }.where { "true".asSql() }.build()

        assertEquals(
            "SELECT 1 AS `one` FROM `tb_user` WHERE true AND `deleted` = 0",
            sql
        )
    }

    @Test
    fun testSelectUseConstEqualGetValue() {
        val another = TestUser(id = 1)
        val (sql, paramMap) = user.select { "1".alias("one") }.where { 1 == another.id.value }.build()

        assertEquals(
            "SELECT 1 AS `one` FROM `tb_user` WHERE true AND `deleted` = 0",
            sql
        )
    }

    @Test
    fun testSelectBuiltInFunctionCount() {
        val (sql, paramMap) = user.select {
            [f.count(1).alias("count"), it.id, f.avg(it.id).alias("avg"), it.username, f.sum(it.id).alias("sum")]
        }.where {
            it.id + 1 > it.id - 1 && f.length(it.username) > 5 && it.username like ("%" + it.username + "%")
        }.build()

        assertEquals(
            "SELECT COUNT(1) AS count, `id`, AVG(`id`) AS avg, `username`, SUM(`id`) AS sum FROM `tb_user` WHERE (`id` + 1) > (`id` - 1) AND LENGTH(`username`) > :lengthMin AND `tb_user`.`username` LIKE CONCAT('%', `username`, '%') AND `deleted` = 0",
            sql
        )

        assertEquals(mapOf("lengthMin" to 5), paramMap)
    }

    @Test
    fun testTakeIf() {
        val (sql, paramMap) = user
            .select{ it.id }
            .where {
                it.id == 1 &&
                        (it.username == "123").takeIf(user.id == 1)
            }
            .build()
        assertEquals(
            "SELECT `id` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("id" to 1), paramMap)

        val (sql2, paramMap2) = user
            .select { it.id }
            .where {
                it.username.contains("123") &&
                        ((it.gender == 1 && it.username == "123") || (it.gender == 2 && it.username == "123")).takeIf(
                            user.id == 2
                        )
            }
            .build()
        assertEquals(
            "SELECT `id` FROM `tb_user` WHERE `tb_user`.`username` LIKE :username ESCAPE '\\\\' AND " +
                    "((`tb_user`.`gender` = :gender AND `tb_user`.`username` = :username@1) OR (`tb_user`.`gender` = :gender@1 AND `tb_user`.`username` = :username@2)) AND " +
                    "`deleted` = 0",
            sql2
        )
        assertEquals(
            mapOf(
                "username" to "%123%",
                "gender" to 1,
                "username@1" to "123",
                "gender@1" to 2,
                "username@2" to "123",
            ), paramMap2
        )
    }

    @Test
    fun testSelectPatch() {
        val (sql, paramMap) = user.select {
            [it.id, it.username]
        }.where {
            it.id == 1 && "`username` = :username".asSql()
        }.patch("id" to 2, "username" to "123").build()
        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE `tb_user`.`id` = :id AND `username` = :username AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 2,
                "username" to "123"
            ), paramMap
        )
    }

    @Test
    fun testSelectBuildUsesOriginalTableName() {
        // A().select().queryList<B>() should query A's table, not B's
        // Verify that build() generates SQL with TestUser's table (tb_user), not Product's (tb_product)
        val (sql, paramMap) = TestUser(1).select().build()
        assertEquals(
            "SELECT `id`, `username`, `score`, `gender`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted` FROM `tb_user` WHERE `deleted` = 0",
            sql
        )
        assertEquals(emptyMap(), paramMap)
    }

    @Test
    fun testTableNameProperty() {
        // Verify __tableName returns correct values for each KPojo
        assertEquals("tb_user", TestUser().__tableName)
        assertEquals("tb_product", Product().__tableName)
    }
}
