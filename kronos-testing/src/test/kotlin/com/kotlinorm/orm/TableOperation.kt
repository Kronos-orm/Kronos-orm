package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.enums.DBType
import com.kotlinorm.orm.beans.SqlliteUser
import com.kotlinorm.orm.beans.User
import com.kotlinorm.orm.beans.UserToBeSync
import com.kotlinorm.orm.database.DBHelper.convertToSqlColumnType
import com.kotlinorm.orm.database.table
import com.kotlinorm.utils.DataSourceUtil.orDefault
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

//    // 初始化mysql数据库连接池
//    private val ds = BasicDataSource().apply {
//        driverClassName = "com.mysql.cj.jdbc.Driver" // MySQL驱动类名，需根据实际数据库类型调整
//        url =
//            "jdbc:mysql://localhost:3306/eqm?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowMultiQueries=true&allowPublicKeyRetrieval=true&useServerPrepStmts=false" // 数据库URL
//        username = "root" // 数据库用户名
//        password = "rootroot" // 数据库密码
//        maxIdle = 10 // 最大空闲连接数
//        maxActive = 10 // 最大活动连接数
//    }
//    val user = User()

    // 初始化sqllite数据库连接池
    private val ds = BasicDataSource().apply {
        driverClassName = "org.sqlite.JDBC" // SQLite驱动类名
        url = "jdbc:sqlite:D:/develop/sqllite/db/myDatabase.db" // SQLite数据库文件路径
        maxIdle = 10 // 最大空闲连接数
        maxActive = 10 // 最大活动连接数
    }
    val user = SqlliteUser()


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
//        // 不管有没有先删
//        dataSource.table.dropTable(user)
        // 判断表是否存在
        val exists = dataSource.table.exists(user)
        assertEquals(true, exists)
//        // 创建表
//        dataSource.table.createTable(user)
//        // 判断表是否存在
//        val exists2 = dataSource.table.exists(user)
//        assertEquals(exists2, true)
    }

    /**
     * 测试mysql动态创建表功能。
     * 此方法应完成一个测试用例，动态创建一个表，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testCreateTable_mysql() {
        // 不管有没有先删
        dataSource.table.dropTable(user)
        // 创建表
        dataSource.table.createTable(user)
        // 判断表是否存在
        val exists = dataSource.table.exists(user)
        assertEquals(exists, true)

        val actualColumns = dataSource.table.getTableColumns("tb_user")

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = user.kronosColumns()

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(convertToSqlColumnType(DBType.Mysql,actualColumn.type,column.length,true,false), convertToSqlColumnType(DBType.Mysql,column.type,column.length,true,false) , "列 '$column' 的类型应一致")
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }
    }

    /**
     * 测试sqlite动态创建表功能。
     * 此方法应完成一个测试用例，动态创建一个表，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testCreateTable_sqlite() {
        // 不管有没有先删
        dataSource.table.dropTable(user)
        // 创建表
        dataSource.table.createTable(user)
        // 判断表是否存在
        val exists = dataSource.table.exists(user)
        assertEquals(exists, true)

        val actualColumns = dataSource.table.getTableColumns("tb_user")

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = user.kronosColumns()

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(actualColumn.type, convertToSqlColumnType(DBType.SQLite,column.type,0,true,false) , "列 '$column' 的类型应一致")
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
     * 测试mysql结构同步功能。
     * 此方法应完成一个测试用例，同步某个表的结构，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testSyncTable_mysql() {
        // 同步user表结构
        val structureSync = dataSource.table.structureSync(user)
        if (!structureSync) {
            println("表结构相同无需同步")
        }

        // 索引
//        val list = user.kronosTableIndex()

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = user.kronosColumns()

        val actualColumns = dataSource.table.getTableColumns("tb_user")

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(convertToSqlColumnType(DBType.Mysql,actualColumn.type,column.length,true,false), convertToSqlColumnType(DBType.Mysql,column.type,0,true,false) , "列 '$column' 的类型应一致")
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }

        // 可选：进一步验证列的属性，如类型、是否为主键等，这通常需要更复杂的数据库查询和比较逻辑

        println("表结构同步测试成功")
    }

    /**
     * 测试sqlite结构同步功能。
     * 此方法应完成一个测试用例，同步某个表的结构，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testSyncTable_sqlite() {
        // 同步user表结构
        val structureSync = dataSource.table.structureSync(user)
        if (!structureSync) {
            println("表结构相同无需同步")
        }

        // 索引
//        val list = user.kronosTableIndex()

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = user.kronosColumns()

        val actualColumns = dataSource.table.getTableColumns("tb_user")

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(actualColumn.type, convertToSqlColumnType(DBType.SQLite,column.type,0,true,false) , "列 '$column' 的类型应一致")
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }

        // 可选：进一步验证列的属性，如类型、是否为主键等，这通常需要更复杂的数据库查询和比较逻辑

        println("表结构同步测试成功")
    }
}