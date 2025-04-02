package com.kotlinorm.beans.config

import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

class KronosCommonStrategyTest {
    init {
        Kronos.init {
            dataSource = { SampleMysqlJdbcWrapper() }
            tableNamingStrategy = lineHumpNamingStrategy
            fieldNamingStrategy = lineHumpNamingStrategy
            createTimeStrategy.enabled = true
        }
    }

    data class TestPojo(
        var id: Int? = null,
        var name: String? = null,
        var createTime: String? = null
    ) : KPojo

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
}