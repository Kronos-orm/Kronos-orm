package com.kotlinorm.orm.select

import com.kotlinorm.GsonProcessor
import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.sample.databases.MysqlUser
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import org.apache.commons.dbcp2.BasicDataSource
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectMysql {
    private val wrapper = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver" // MySQL驱动类名，需根据实际数据库类型调整
        // 数据库URL
        url =
            "jdbc:mysql://localhost:3306/kronos_testing?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowMultiQueries=true&allowPublicKeyRetrieval=true&useServerPrepStmts=false&rewriteBatchedStatements=true"
        username = System.getenv("MYSQL_USERNAME") // 数据库用户名
        password = System.getenv("MYSQL_PASSWORD") // 数据库密码
    }.let {
        KronosBasicWrapper(it)
    }

    init {
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
            createTimeStrategy.enabled = false
            updateTimeStrategy.enabled = false
            logicDeleteStrategy.enabled = false
            optimisticLockStrategy.enabled = false
            dataSource = { wrapper }
            serializeProcessor = GsonProcessor
        }
    }

    @Test
    fun testSelectWithCollectionWhere() {
        dataSource.table.syncTable<MysqlUser>()
        dataSource.table.truncateTable<MysqlUser>()
        MysqlUser(id = 1).insert().execute()
        MysqlUser(id = 2).insert().execute()
        MysqlUser(id = 3).insert().execute()
        val users = MysqlUser()
            .select()
            .where { it.id in listOf(1, 2, 3, 4) }
            .queryList()
        assertEquals(3, users.size)
    }
}