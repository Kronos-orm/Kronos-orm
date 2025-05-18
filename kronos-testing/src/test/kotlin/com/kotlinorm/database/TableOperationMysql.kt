package com.kotlinorm.database

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.sample.databases.MysqlUser
import com.kotlinorm.database.SqlHandler.execute
import com.kotlinorm.database.SqlHandler.queryOne
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getTableColumns
import com.kotlinorm.enums.DBType
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.plugins.LastInsertIdPlugin.lastInsertId
import com.kotlinorm.plugins.LastInsertIdPlugin.withId
import org.apache.commons.dbcp2.BasicDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 此类演示了如何使用KotlinORM进行数据库表的操作，包括查询表是否存在、动态创建表、删除表以及结构同步。
 * 通过与数据库交互，实现了基于实体类的表管理功能。
 */
class TableOperationMysql {

    // 初始化mysql数据库连接池
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
    val user = MysqlUser()

    init {
        // 配置Kronos ORM框架的基本设置
        Kronos.init {
            // 设置字段命名策略为驼峰命名
            fieldNamingStrategy = lineHumpNamingStrategy
            // 设置表命名策略为驼峰命名
            tableNamingStrategy = lineHumpNamingStrategy
            // 设置数据源提供器
            dataSource = { wrapper }
        }
    }


    /**
     * 测试表是否存在功能。
     * 此方法应完成一个测试用例，验证某个表是否存在，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testExists() {
        if (dataSource.table.exists(MysqlUser())) {
            dataSource.table.dropTable(MysqlUser())
        }
        // 判断表是否存在
        val exists = dataSource.table.exists(user)
        assertEquals(false, exists)
        // 创建表
        dataSource.table.createTable(user)
        // 判断表是否存在
        val exists2 = dataSource.table.exists(user)
        assertEquals(true, exists2)
    }

    /**
     * 测试mysql动态创建表功能。
     * 此方法应完成一个测试用例，动态创建一个表，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testCreateTable_mysql() {
        if (dataSource.table.exists(MysqlUser())) {
            dataSource.table.dropTable(MysqlUser())
        }
        // 创建表
        dataSource.table.createTable(user)
        // 判断表是否存在
        val exists = dataSource.table.exists(user)
        assertEquals(exists, true)

        val actualColumns = getTableColumns(dataSource(), "tb_user")

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = user.kronosColumns()
        println(actualColumns)

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(
                columnCreateDefSql(DBType.Mysql, column),
                columnCreateDefSql(DBType.Mysql, actualColumn),
                "列 '$column' 的类型应一致"
            )
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }
    }

    /**
     * 测试删除表功能。
     * 此方法应完成一个测试用例，删除某个表，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testDropTable() {
        if (dataSource.table.exists(MysqlUser())) {
            dataSource.table.dropTable(MysqlUser())
        }
        // 创建表
        dataSource.table.createTable(user)
        // 判断表是否存在
        val exists = dataSource.table.exists(user)
        assertEquals(exists, true)
        // 删除表
        dataSource.table.dropTable(user)
        // 判断表是否存在
        val exists2 = dataSource.table.exists(user)
        assertEquals(exists2, false)
    }

    /**
     * 测试mysql结构同步功能。
     * 此方法应完成一个测试用例，同步某个表的结构，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testSyncScheme_mysql() {
        // 同步user表结构
        val tableSync = dataSource.table.syncTable(user)
        if (!tableSync) {
            println("表结构相同无需同步")
        }

        // 索引
//        val list = user.kronosTableIndex()

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = user.kronosColumns()

        val actualColumns = getTableColumns(dataSource(), "tb_user")

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(
                columnCreateDefSql(DBType.Mysql, column),
                columnCreateDefSql(DBType.Mysql, actualColumn),
                "列 '$column' 的类型应一致"
            )
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }

        // 可选：进一步验证列的属性，如类型、是否为主键等，这通常需要更复杂的数据库查询和比较逻辑

        println("表结构同步测试成功")
    }


    /**
     * 测试获取自增主键
     */
    @Test
    fun testLastInsertId() {
        dataSource.table.syncTable(user)
        dataSource.table.truncateTable(user)
        wrapper.execute("ALTER TABLE tb_user AUTO_INCREMENT = 528")
        val newUser = MysqlUser(
            username = "yf",
            gender = 93
        )
        val lastInsertId = newUser.insert().withId().execute().lastInsertId
        assertEquals(528, lastInsertId, "自增主键值应为528")
    }

    @Test
    fun testPojoComment() {
        dataSource.table.syncTable(user)
        val user = MysqlUser()
        val comment = user.kronosTableComment()
        assertEquals("Kotlin Data Class for MysqlUser", comment, "表注释不正确")
        val realComment = wrapper.queryOne<String>(
            SqlManager.getTableCommentSql(wrapper),
            mapOf(
                "tableName" to user.kronosTableName(),
                "dbName" to "kronos_testing"
            )
        )
        assertEquals(comment, realComment, "表注释不正确")
    }

//    @Test
//    fun testMoveColumns() {
//        val expect = listOf(
//            Field(columnName = "0"),
//            Field(columnName = "1"),
//            Field(columnName = "2"),
//            Field(columnName = "3"),
//            Field(columnName = "4"),
//            Field(columnName = "5"),
//            Field(columnName = "6"),
//        )
//        val current = listOf(
//            Field(columnName = "0"),
//            Field(columnName = "1"),
//            Field(columnName = "3"),
//            Field(columnName = "4"),
//            Field(columnName = "2"),
//            Field(columnName = "5"),
//            Field(columnName = "6"),
//        )
//
////        val moved = moveColumn(expect, current)
////        println(moved.size)
//    }
}