package com.kotlinorm.database

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.sample.databases.PgUser
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getTableColumns
import com.kotlinorm.enums.DBType
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import org.apache.commons.dbcp2.BasicDataSource
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 此类演示了如何使用KotlinORM进行数据库表的操作，包括查询表是否存在、动态创建表、删除表以及结构同步。
 * 通过与数据库交互，实现了基于实体类的表管理功能。
 */
class TableOperationPostgres {

    // 初始化Postgres数据库连接池
    private val ds = BasicDataSource().apply {
        driverClassName = "org.postgresql.Driver" // Postgres驱动类名
        url = "jdbc:postgresql://localhost:5432/kronos_testing" // Postgres数据库URL
        username = "kronos" // Postgres用户名
        password = "kronos" // Postgres密码
        maxIdle = 10 // 最大空闲连接数
    }.let {
        KronosBasicWrapper(it)
    }
    val user = PgUser()

    init {
        // 配置Kronos ORM框架的基本设置
        Kronos.init {
            // 设置字段命名策略为驼峰命名
            fieldNamingStrategy = lineHumpNamingStrategy
            // 设置表命名策略为驼峰命名
            tableNamingStrategy = lineHumpNamingStrategy
            // 设置数据源提供器
            dataSource = { ds }
        }
    }


    /**
     * 测试表是否存在功能。
     * 此方法应完成一个测试用例，验证某个表是否存在，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testExists() {
        // 不管有没有先删
        dataSource.table.dropTable(user)
        // 判断表是否存在
        val exists = dataSource.table.exists(user)
        assertEquals(false, exists)
        // 创建表
        dataSource.table.createTable(user)
        // 判断表是否存在
        val exists2 = dataSource.table.exists(user)
        assertEquals(exists2, true)
    }

    /**
     * 测试postgresql动态创建表功能。
     * 此方法应完成一个测试用例，动态创建一个表，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testCreateTable_postgresql() {
        // 不管有没有先删
        dataSource.table.dropTable(user)
        // 创建表
        dataSource.table.createTable(user)
        // 判断表是否存在
        val exists = dataSource.table.exists(user)
        assertEquals(exists, true)

        val actualColumns = getTableColumns(dataSource(), "tb_user")

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = user.kronosColumns()

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(
                columnCreateDefSql(DBType.Postgres, column),
                columnCreateDefSql(DBType.Postgres, actualColumn),
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
        // 不管有没有先删
        dataSource.table.dropTable(user)
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
     * 测试postgresql结构同步功能。
     * 此方法应完成一个测试用例，同步某个表的结构，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testSyncScheme_postgresql() {
        // 同步user表结构
        dataSource.table.truncateTable(user)
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
                columnCreateDefSql(DBType.Postgres, actualColumn),
                columnCreateDefSql(DBType.Postgres, column),
                "列 '$column' 的类型应一致"
            )
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }

        // 可选：进一步验证列的属性，如类型、是否为主键等，这通常需要更复杂的数据库查询和比较逻辑

        println("表结构同步测试成功")
    }

    @Test
    fun testInsertTimeStamp() {
        dataSource.table.dropTable(user)
        dataSource.table.syncTable(user)
        val user = PgUser(id = 8, regTime = Instant.ofEpochMilli(160000000000L))
        user.insert().execute()
        val selected = PgUser().select { it.id + it.regTime }.where { it.regTime == Instant.ofEpochMilli(160000000000L) }.queryOne()
        val exist = PgUser().select().page(1,1).queryList()
        print(exist.size)
        assertEquals(
            8, selected.id, "插入的用户ID应该为8"
        )
        assertEquals(
            Instant.ofEpochMilli(160000000000L), selected.regTime,
            "插入的用户创建时间应该为160000000000L对应的时间戳"
        )
    }
}