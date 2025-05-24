package com.kotlinorm.wrapper

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.sample.databases.MysqlUser
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.orm.ddl.table
import org.apache.commons.dbcp2.BasicDataSource
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class BasicWrapperTest {
    private val wrapper = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver" // MySQL驱动类名，需根据实际数据库类型调整
        // 数据库URL
        url =
            "jdbc:mysql://localhost:3306/kronos_testing?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowMultiQueries=true&allowPublicKeyRetrieval=true&useServerPrepStmts=false&rewriteBatchedStatements=true"
        username = System.getenv("db.username") // 数据库用户名
        password = System.getenv("db.password") // 数据库密码
        maxIdle = 10 // 最大空闲连接数
    }.let {
        KronosBasicWrapper(it)
    }

    init {
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
            dataSource = { wrapper }
            strictSetValue = true
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

        assertEquals(List(dataSet.size) { -2 }, result.toList())
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
        Kronos.init {
            strictSetValue = true
        }
        val result =
            wrapper.forList(KronosAtomicQueryTask("select username, score, gender, create_time createTime, update_time updateTime from tb_user"))
        assertEquals(
            dataSet.toList(),
            result
        )
        Kronos.init {
            strictSetValue = false
        }
        val result2 =
            wrapper.forList(KronosAtomicQueryTask("select username, score, gender, create_time createTime, update_time updateTime from tb_user"))
        assertEquals(
            dataSet.toList(),
            result2
        )
    }

    @Test
    fun testQueryListType() {
        testUpdate()
        Kronos.init {
            strictSetValue = true
        }
        val result = wrapper.forList(
            KronosAtomicQueryTask("select username, score, gender, create_time createTime, update_time updateTime from tb_user"),
            MysqlUser::class,
            true,
            listOf()
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
        Kronos.init {
            strictSetValue = false
        }
        val result2 = wrapper.forList(
            KronosAtomicQueryTask("select username, score, gender, create_time createTime, update_time updateTime from tb_user"),
            MysqlUser::class,
            true,
            listOf()
        )
        assertEquals(
            listOf(
                MysqlUser(null, "test", 1, 1, "2022-01-01T00:00", LocalDateTime.parse("2022-01-01T00:00")),
                MysqlUser(null, "test2", 2, 1, "2022-01-01T00:00", LocalDateTime.parse("2022-01-01T00:00")),
                MysqlUser(null, "test3", 3, 1, "2022-01-01T00:00", LocalDateTime.parse("2022-01-01T00:00")),
                MysqlUser(null, "test4", 4, 1, "2022-01-01T00:00", LocalDateTime.parse("2022-01-01T00:00")),
                MysqlUser(null, "test5", 5, 1, "2022-01-01T00:00", LocalDateTime.parse("2022-01-01T00:00")),
            ),
            result2
        )
    }

    @Test
    fun testQueryListTypePrimitive() {
        testUpdate()
        Kronos.init {
            strictSetValue = true
        }
        val result = wrapper.forList(KronosAtomicQueryTask("select score from tb_user"), Int::class, false, listOf("Kotlin.Int"))
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            result
        )
        Kronos.init {
            strictSetValue = false
        }
        val result2 =
            wrapper.forList(KronosAtomicQueryTask("select score from tb_user"), Int::class, false, listOf("Kotlin.Int"))
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            result2
        )
    }

    @Test
    fun testQueryObject() {
        testUpdate()
        Kronos.init {
            strictSetValue = true
        }
        val result1 =
            wrapper.forObject(
                KronosAtomicQueryTask("select * from tb_user where id = 1"),
                MysqlUser::class,
                true,
                listOf()
            )
        assertEquals(MysqlUser(1, "test", 1, 1, deleted = false), result1)

        Kronos.init {
            strictSetValue = false
        }
        val result2 =
            wrapper.forObject(
                KronosAtomicQueryTask("select * from tb_user where id = 1"),
                MysqlUser::class,
                true,
                listOf()
            )
        assertEquals(MysqlUser(1, "test", 1, 1, deleted = false), result2)
    }

    @Test
    fun testQueryObjectPrimitive() {
        testUpdate()
        Kronos.init {
            strictSetValue = true
        }
        val result = wrapper.forObject(KronosAtomicQueryTask("select score from tb_user where id = 1"), Int::class, false, listOf("Kotlin.Int"))
        assertEquals(1, result)
        Kronos.init {
            strictSetValue = false
        }
        val result2 = wrapper.forObject(
            KronosAtomicQueryTask("select score from tb_user where id = 1"),
            Int::class,
            false,
            listOf("Kotlin.Int")
        )
        assertEquals(1, result2)
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
                "deleted" to false
            ), result
        )
    }
}