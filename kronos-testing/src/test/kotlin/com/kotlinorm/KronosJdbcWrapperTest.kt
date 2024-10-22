package com.kotlinorm

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.config.LineHumpNamingStrategy
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.beans.MysqlUser
import com.kotlinorm.orm.database.table
import org.apache.commons.dbcp2.BasicDataSource
import kotlin.test.Test
import kotlin.test.assertEquals

class BasicWrapperTest {
    private val ds = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/test"
        username = "root"
        password = "******"
        maxIdle = 10
    }

    private val wrapper by lazy {
        dataSource()
    }

    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
            dataSource = { KronosBasicWrapper(ds) }
        }
    }

    @Test
    fun testBatchUpdate() {
        dataSource.table.dropTable(MysqlUser())
        dataSource.table.createTable(MysqlUser())
        val result = wrapper.batchUpdate(
            KronosAtomicBatchTask(
                "insert into mysql_user (name, age) values (:name, :age)", arrayOf(
                    mapOf("name" to "test1", "age" to 1),
                    mapOf("name" to "test2", "age" to 2),
                    mapOf("name" to "test3", "age" to 3),
                    mapOf("name" to "test4", "age" to 4),
                    mapOf("name" to "test5", "age" to 5),
                )
            )
        )
        assertEquals(listOf(1, 1, 1, 1, 1), result.toList())
    }

    @Test
    fun testUpdate() {
        testBatchUpdate()
        val affectRows = wrapper.update(KronosAtomicActionTask("update mysql_user set name = 'test' where id = 1"))
        assertEquals(1, affectRows)
    }

    @Test
    fun testQueryList() {
        testUpdate()
        val result = wrapper.forList(KronosAtomicQueryTask("select * from mysql_user"))
        assertEquals(
            listOf(
                mapOf("id" to 1, "name" to "test", "age" to 1),
                mapOf("id" to 2, "name" to "test2", "age" to 2),
                mapOf("id" to 3, "name" to "test3", "age" to 3),
                mapOf("id" to 4, "name" to "test4", "age" to 4),
                mapOf("id" to 5, "name" to "test5", "age" to 5),
            ),
            result
        )
    }

    @Test
    fun testQueryListType() {
        testUpdate()
        val result = wrapper.forList(KronosAtomicQueryTask("select * from mysql_user"), MysqlUser::class)
        assertEquals(
            listOf(
                MysqlUser(1, "test", 1),
                MysqlUser(2, "test2", 2),
                MysqlUser(3, "test3", 3),
                MysqlUser(4, "test4", 4),
                MysqlUser(5, "test5", 5),
            ),
            result
        )
    }

    @Test
    fun testQueryListTypePrimitive() {
        testUpdate()
        val result = wrapper.forList(KronosAtomicQueryTask("select age from mysql_user"), Int::class)
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            result
        )
    }

    @Test
    fun testQueryObject() {
        testUpdate()
        val result = wrapper.forObject(KronosAtomicQueryTask("select * from mysql_user where id = 1"), MysqlUser::class)
        assertEquals(MysqlUser(1, "test", 1), result)
    }

    @Test
    fun testQueryObjectPrimitive() {
        testUpdate()
        val result = wrapper.forObject(KronosAtomicQueryTask("select age from mysql_user where id = 1"), Int::class)
        assertEquals(1, result)
    }

    @Test
    fun testQueryMap() {
        testUpdate()
        val result = wrapper.forMap(KronosAtomicQueryTask("select * from mysql_user where id = 1"))
        assertEquals(mapOf("id" to 1, "name" to "test", "age" to 1), result)
    }
}