package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.beans.User
import com.kotlinorm.orm.database.table
import org.apache.commons.dbcp.BasicDataSource
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 此类演示了如何使用KotlinORM进行数据库表的操作，包括查询表是否存在、动态创建表、删除表以及结构同步。
 * 通过与数据库交互，实现了基于实体类的表管理功能。
 */
class TableOperation {

    // 初始化数据库连接池
    private val ds = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver" // MySQL驱动类名，需根据实际数据库类型调整
        url =
            "jdbc:mysql://localhost:3306/eqm?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowMultiQueries=true&allowPublicKeyRetrieval=true&useServerPrepStmts=false" // 数据库URL
        username = "root" // 数据库用户名
        password = "rootroot" // 数据库密码
        maxIdle = 10 // 最大空闲连接数
        maxActive = 10 // 最大活动连接数
    }

    init {
        // 配置Kronos ORM框架的基本设置
        Kronos.apply {
            // 设置字段命名策略为驼峰命名
            fieldNamingStrategy = LineHumpNamingStrategy
            // 设置表命名策略为驼峰命名
            tableNamingStrategy = LineHumpNamingStrategy
            // 设置数据源提供器
            dataSource = { KronosBasicWrapper(ds) }
        }
    }

    /**
     * 测试表是否存在功能。
     * 此方法应完成一个测试用例，验证某个表是否存在，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testExists() {
        // 不管有没有先删
        dataSource.table.deleteTable<User>()
        // 判断表是否存在
        val exists = dataSource.table.exists<User>()
        assertEquals(exists, false)
        // 创建表
        dataSource.table.createTable<User>()
        // 判断表是否存在
        val exists2 = dataSource.table.exists<User>()
        assertEquals(exists2, true)
    }

    /**
     * 测试动态创建表功能。
     * 此方法应完成一个测试用例，动态创建一个表，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testCreateTable() {
        // 不管有没有先删
        dataSource.table.deleteTable<User>()
        // 创建表
        dataSource.table.createTable<User>()
        // 判断表是否存在
        val exists = dataSource.table.exists<User>()
        assertEquals(exists, true)
    }

    /**
     * 测试删除表功能。
     * 此方法应完成一个测试用例，删除某个表，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testDeleteTable() {
        // 不管有没有先删
        dataSource.table.deleteTable<User>()
        // 创建表
        dataSource.table.createTable<User>()
        // 判断表是否存在
        val exists = dataSource.table.exists<User>()
        assertEquals(exists, true)
        // 删除表
        dataSource.table.deleteTable<User>()
        // 判断表是否存在
        val exists2 = dataSource.table.exists<User>()
        assertEquals(exists2, false)
    }

    /**
     * 测试结构同步功能。
     * 此方法应完成一个测试用例，同步某个表的结构，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testSyncTable() {
        // 初始化：删除表以确保从干净状态开始测试
        dataSource.table.deleteTable<User>()
        assertFalse(dataSource.table.exists<User>(), "表应不存在于初始状态")

        // 同步表结构
        dataSource.table.structureSync<User>()

        // 验证表是否被创建
        assertTrue(dataSource.table.exists<User>(), "表应在同步后存在")

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = User::class.java.fields.map { it.name }

        val actualColumns = dataSource.table.getTableColumns(dataSource(), "tb_user")

        // 确保所有期望的列都存在于实际的列列表中
        expectedColumns.forEach { column ->
            assertTrue(actualColumns.contains(column), "列 '$column' 应存在于表中")
        }

        // 可选：进一步验证列的属性，如类型、是否为主键等，这通常需要更复杂的数据库查询和比较逻辑

        println("表结构同步测试成功")
    }
}