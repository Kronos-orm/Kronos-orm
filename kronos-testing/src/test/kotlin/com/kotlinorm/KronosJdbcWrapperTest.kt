package com.kotlinorm

import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.beans.MysqlUser
import com.kotlinorm.orm.database.table
import org.apache.commons.dbcp2.BasicDataSource
import java.time.LocalDateTime
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
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
            dataSource = { KronosBasicWrapper(ds) }
        }
    }

    private val dataSet: Array<Map<String, Any?>> = arrayOf(
        mapOf(
            "username" to "test1",
            "score" to 1,
            "gender" to 1,
            "createTime" to "2022-01-01T00:00",
            "updateTime" to LocalDateTime.parse("2022-01-01T00:00")
        ),
        mapOf(
            "username" to "test2",
            "score" to 2,
            "gender" to 1,
            "createTime" to "2022-01-01T00:00",
            "updateTime" to LocalDateTime.parse("2022-01-01T00:00")
        ),
        mapOf(
            "username" to "test3",
            "score" to 3,
            "gender" to 1,
            "createTime" to "2022-01-01T00:00",
            "updateTime" to LocalDateTime.parse("2022-01-01T00:00")
        ),
        mapOf(
            "username" to "test4",
            "score" to 4,
            "gender" to 1,
            "createTime" to "2022-01-01T00:00",
            "updateTime" to LocalDateTime.parse("2022-01-01T00:00")
        ),
        mapOf(
            "username" to "test5",
            "score" to 5,
            "gender" to 1,
            "createTime" to "2022-01-01T00:00",
            "updateTime" to LocalDateTime.parse("2022-01-01T00:00")
        )
    )

    @Test
    fun testBatchInsert() {
        if (dataSource.table.exists(MysqlUser())) {
            dataSource.table.dropTable(MysqlUser())
        }
        dataSource.table.syncTable(MysqlUser())
        val result = wrapper.batchUpdate(
            KronosAtomicBatchTask(
                "insert into tb_user (username, score, gender, create_time, update_time) values (:username, :score, :gender, :createTime, :updateTime)",
                dataSet
            )
        )
        assertEquals(listOf(1, 1, 1, 1, 1), result.toList())
    }

    @Test
    fun testUpdate() {
        testBatchInsert()
        val affectRows = wrapper.update(KronosAtomicActionTask("update tb_user set username = 'test' where id = 1"))
        assertEquals(1, affectRows)
    }

    @Test
    fun testQueryList() {
        testBatchInsert()
        val result =
            wrapper.forList(KronosAtomicQueryTask("select username, score, gender, create_time createTime, update_time updateTime from tb_user"))
        assertEquals(
            dataSet.toList(),
            result
        )
    }

    @Test
    fun testQueryListType() {
        testUpdate()
        val result = wrapper.forList(
            KronosAtomicQueryTask("select username, score, gender, create_time createTime, update_time updateTime from tb_user"),
            MysqlUser::class
        )
        assertEquals(
            listOf(
                MysqlUser(null, "test", 1, 1, "2022-01-01T00:00", LocalDateTime.parse("2022-01-01T00:00")),
                MysqlUser(null, "test2", 2, 1, "2022-01-01T00:00", LocalDateTime.parse("2022-01-01T00:00")),
                MysqlUser(null, "test3", 3, 1, "2022-01-01T00:00", LocalDateTime.parse("2022-01-01T00:00")),
                MysqlUser(null, "test4", 4, 1, "2022-01-01T00:00", LocalDateTime.parse("2022-01-01T00:00")),
                MysqlUser(null, "test5", 5, 1, "2022-01-01T00:00", LocalDateTime.parse("2022-01-01T00:00")),
            ),
            result
        )
    }

    @Test
    fun testQueryListTypePrimitive() {
        testUpdate()
        val result = wrapper.forList(KronosAtomicQueryTask("select score from tb_user"), Int::class)
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            result
        )
    }

    @Test
    fun testQueryObject() {
        testUpdate()
        val result =
            wrapper.forObject(KronosAtomicQueryTask("select * from tb_user where id = 1"), MysqlUser::class)
        assertEquals(MysqlUser(1, "test", 1, 1, deleted = false), result)
    }

    @Test
    fun testQueryObjectPrimitive() {
        testUpdate()
        val result = wrapper.forObject(KronosAtomicQueryTask("select score from tb_user where id = 1"), Int::class)
        assertEquals(1, result)
    }

    @Test
    fun testQueryMap() {
        testUpdate()
        val result = wrapper.forMap(KronosAtomicQueryTask("select * from tb_user where id = 1"))
        assertEquals(
            mapOf(
                "id" to 1,
                "username" to "test",
                "score" to 1,
                "gender" to 1,
                "create_time" to "2022-01-01T00:00",
                "update_time" to LocalDateTime.parse("2022-01-01T00:00"),
                "deleted" to 0
            ), result
        )
    }
}