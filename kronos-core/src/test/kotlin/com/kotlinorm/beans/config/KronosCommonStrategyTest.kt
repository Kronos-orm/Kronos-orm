package com.kotlinorm.beans.config

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.enums.KColumnType.BIGINT
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import java.time.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KronosCommonStrategyTest {
    @BeforeTest
    fun setUp() {
        with(Kronos) {
            dataSource = { SampleMysqlJdbcWrapper() }
            tableNamingStrategy = lineHumpNamingStrategy
            fieldNamingStrategy = lineHumpNamingStrategy
            createTimeStrategy.enabled = true
            updateTimeStrategy.enabled = true
            primaryKeyStrategy.enabled = true
        }
    }

    @AfterTest
    fun tearDown() {
        with(Kronos) {
            createTimeStrategy.enabled = false
            updateTimeStrategy.enabled = false
            primaryKeyStrategy.enabled = false
        }
    }

    data class TestPojo(
        var id: Int? = null,
        var name: String? = null,
        var createTime: LocalDateTime? = null,
        var updateTime: LocalDateTime? = null,
    ) : KPojo

    @CreateTime(enable = false)
    @UpdateTime(enable = false)
    data class TestPojo2(
        var id: Int? = null,
        var name: String? = null,
        var createTime: LocalDateTime? = null,
        var updateTime: LocalDateTime? = null,
    ) : KPojo

    data class LongStrategyPojo(
        @PrimaryKey(custom = true)
        var id: Int? = null,
        var name: String? = null,
        @ColumnType(BIGINT)
        @DateTimeFormat("[invalid")
        var createTime: Long? = null,
        @ColumnType(BIGINT)
        @DateTimeFormat("[invalid")
        var updateTime: Long? = null,
        @LogicDelete
        var deleted: Boolean? = null,
    ) : KPojo

    @Test
    fun testUpdateTimeStrategy() {
        val (sql, paramMap) = TestPojo(1, "kronos").update { [it.name, it.createTime] }.by { it.id }.build()
        assertEquals("UPDATE `test_pojo` SET `name` = :nameNew, `update_time` = :updateTimeNew WHERE `id` = :id", sql)
        assertEquals(
            mapOf(
                "id" to 1,
                "nameNew" to "kronos",
                "updateTimeNew" to paramMap["updateTimeNew"],
            ),
            paramMap
        )
    }

    @Test
    fun testCreateTimeStrategy() {
        val (sql, paramMap) = TestPojo(1).insert().build()
        assertEquals("INSERT INTO `test_pojo` (`id`, `name`, `create_time`, `update_time`) VALUES (:id, :name, :createTime, :updateTime)", sql)
        assertEquals(
            mapOf(
                "id" to 1,
                "name" to null,
                "createTime" to paramMap["createTime"],
                "updateTime" to paramMap["updateTime"],
            ),
            paramMap
        )
    }

    @Test
    fun testCreateTimeStrategy2() {
        val (sql, paramMap) = TestPojo2(1).insert().build()
        assertEquals("INSERT INTO `test_pojo2` (`id`, `name`, `create_time`, `update_time`) VALUES (:id, :name, :createTime, :updateTime)", sql)
        assertEquals(
            mapOf(
                "id" to 1,
                "name" to null,
                "createTime" to null,
                "updateTime" to null,
            ),
            paramMap
        )
    }

    @Test
    fun testUpdateTimeStrategy2() {
        val (sql, paramMap) = TestPojo2(1, "kronos").update { it.name }.by { it.id }.build()
        assertEquals("UPDATE `test_pojo2` SET `name` = :nameNew WHERE `id` = :id", sql)
        assertEquals(
            mapOf(
                "id" to 1,
                "nameNew" to "kronos",
            ),
            paramMap
        )
    }

    @Test
    fun testPrimaryKeyStrategy() {
        val (sql, paramMap) = TestPojo(1).insert().build()
        assertEquals("INSERT INTO `test_pojo` (`id`, `name`, `create_time`, `update_time`) VALUES (:id, :name, :createTime, :updateTime)", sql)
        assertEquals(
            mapOf(
                "id" to 1,
                "name" to null,
                "createTime" to paramMap["createTime"],
                "updateTime" to paramMap["updateTime"],
            ),
            paramMap
        )
    }

    @Test
    fun automaticLongTemporalStrategiesMaterializeEpochMillisAcrossDml() {
        val wrapper = SampleMysqlJdbcWrapper()
        val lowerBound = System.currentTimeMillis() - 1_000

        val insertParams = LongStrategyPojo(1, "insert").insert().build(wrapper).component2()
        assertEpochMillis(insertParams, "createTime", lowerBound)
        assertEpochMillis(insertParams, "updateTime", lowerBound)

        val updateParams = LongStrategyPojo(1, "update")
            .update { it.name }
            .by { it.id }
            .build(wrapper)
            .component2()
        assertEpochMillis(updateParams, "updateTimeNew", lowerBound)

        val deleteParams = LongStrategyPojo(1)
            .delete()
            .by { it.id }
            .build(wrapper)
            .component2()
        assertEpochMillis(deleteParams, "updateTimeNew", lowerBound)

        val upsertParams = LongStrategyPojo(1, "upsert")
            .upsert()
            .onConflict()
            .build(wrapper)
            .component2()
        assertEpochMillis(upsertParams, "createTime", lowerBound)
        assertEpochMillis(upsertParams, "updateTime", lowerBound)
    }

    private fun assertEpochMillis(
        parameters: Map<String, Any?>,
        name: String,
        lowerBound: Long
    ) {
        val value = assertIs<Long>(parameters[name])
        assertTrue(value in lowerBound..(System.currentTimeMillis() + 1_000))
    }
}
