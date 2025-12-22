package com.kotlinorm.orm.upsert

import com.kotlinorm.GsonProcessor
import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.sample.databases.MysqlUser
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.plugins.LastInsertIdPlugin.lastInsertId
import org.apache.commons.dbcp2.BasicDataSource
import kotlin.test.Test

class UpsertMySql {
    private val wrapper = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver" // MySQL驱动类名，需根据实际数据库类型调整
        // 数据库URL
        url =
            "jdbc:mysql://localhost:3306/kronos_testing?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowMultiQueries" +
                    "=true&allowPublicKeyRetrieval=true&useServerPrepStmts=false&rewriteBatchedStatements=true"
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
    fun testUpsertDeletedRow() {
        dataSource.table.syncTable<MysqlUser>()
        val user = MysqlUser(username = "张三")
        val lastInsertId = user.insert().execute().lastInsertId?.toInt()
        MysqlUser(id = lastInsertId).delete().logic().by { it.id }.execute()
        MysqlUser(id = lastInsertId, username = "李四").upsert { it.username }.on { it.id }.execute()
    }

    @Test
    fun testUpsertOnDeleted() {
        dataSource.table.syncTable<MysqlUser>()
        val user = MysqlUser(username = "张三")
        val lastInsertId = user.insert().execute().lastInsertId?.toInt()
        MysqlUser(id = lastInsertId).delete().logic().by { it.id }.execute()
        MysqlUser(username = "李四", deleted = true).upsert { it.username }.on { it.deleted }.execute()
    }
}