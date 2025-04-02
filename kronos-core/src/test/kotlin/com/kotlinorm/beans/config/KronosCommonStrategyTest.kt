package com.kotlinorm.beans.config

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.update.update
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class KronosCommonStrategyTest {
    init {
        Kronos.init {
            dataSource = { SampleMysqlJdbcWrapper() }
            tableNamingStrategy = lineHumpNamingStrategy
            fieldNamingStrategy = lineHumpNamingStrategy
            createTimeStrategy.enabled = true
            updateTimeStrategy.enabled = true
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

    @Test
    fun testUpdateTimeStrategy() {
        val (sql, paramMap) = TestPojo(1, "kronos").update { it.name + it.createTime }.by { it.id }.build()
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
        assertEquals("INSERT INTO `test_pojo` (`id`, `create_time`) VALUES (:id, :createTime)", sql)
        assertEquals(
            mapOf(
                "id" to 1,
                "createTime" to paramMap["createTime"],
            ),
            paramMap
        )
    }

    @Test
    fun testCreateTimeStrategy2() {
        val (sql, paramMap) = TestPojo2(1).insert().build()
        assertEquals("INSERT INTO `test_pojo2` (`id`) VALUES (:id)", sql)
        assertEquals(
            mapOf(
                "id" to 1,
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
}