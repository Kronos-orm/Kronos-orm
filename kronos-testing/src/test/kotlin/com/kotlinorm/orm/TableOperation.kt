package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.enums.DBType
import com.kotlinorm.orm.beans.*
import com.kotlinorm.orm.database.DBHelper.convertToSqlColumnType
import com.kotlinorm.orm.database.DBHelper.getDBNameFromUrl
import com.kotlinorm.orm.database.table
import com.kotlinorm.orm.insert.insert
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

//    // 初始化sqllite数据库连接池
//    private val ds = BasicDataSource().apply {
//        driverClassName = "org.sqlite.JDBC" // SQLite驱动类名
//        url = "jdbc:sqlite:D:/develop/sqllite/db/myDatabase.db" // SQLite数据库文件路径
//        maxIdle = 10 // 最大空闲连接数
//        maxActive = 10 // 最大活动连接数
//    }
//    val user = SqlliteUser()

//    // 初始化SQLserver数据库连接池
//    private val ds = BasicDataSource().apply {
//        driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver" // SQLServer驱动类名
//        url = "jdbc:sqlserver://localhost:1433;databaseName=myDatabase;encrypt=true;trustServerCertificate=true"
//        username = "sa" // SQLServer用户名
//        password = "root" // SQLServer密码
//        maxIdle = 10 // 最大空闲连接数
//        maxActive = 10 // 最大活动连接数
//    }
//    val user = SsqlUser()

//    // 初始化Postgres数据库连接池
//    private val ds = BasicDataSource().apply {
//        driverClassName = "org.postgresql.Driver" // Postgres驱动类名
//        url = "jdbc:postgresql://localhost:5432/postgres" // Postgres数据库URL
//        username = "postgres" // Postgres用户名
//        password = "root" // Postgres密码
//        maxIdle = 10 // 最大空闲连接数
//        maxActive = 10 // 最大活动连接数
//    }
//    val user = PgUser()

    // 初始化oracle数据库连接池
    private val ds = BasicDataSource().apply {
        driverClassName = "oracle.jdbc.OracleDriver" // Oracle驱动类名
        url = "jdbc:oracle:thin:@localhost:1521/orclpdb" // Oracle数据库URL
        username = "YF" // Oracle用户名
        password = "root" // Oracle密码
        maxIdle = 10 // 最大空闲连接数
        maxActive = 10 // 最大活动连接数
    }
    val user = OracleUser()


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
        assertEquals(false, exists)
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
        println(actualColumns)

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(
                convertToSqlColumnType(DBType.Mysql, actualColumn.type, column.length, true, false),
                convertToSqlColumnType(DBType.Mysql, column.type, column.length, true, false),
                "列 '$column' 的类型应一致"
            )
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }
    }

    /**
     * 测试oracle动态创建表功能。
     * 此方法应完成一个测试用例，动态创建一个表，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testCreateTable_oracle() {
        println(getDBNameFromUrl(dataSource()))
        // 不管有没有先删
        dataSource.table.dropTable(user)
        // 创建表
        dataSource.table.createTable(user)
        // 判断表是否存在
        val exists = dataSource.table.exists(user)
        assertEquals(exists, true)

        val actualColumns = dataSource.table.getTableColumns("TB_USER")

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = user.kronosColumns().map {
            it.columnName = it.columnName.uppercase()
            it
        }
        println("actualColumns_columnName" + actualColumns.map { it.columnName })
        println("expectedColumns_columnName" + expectedColumns.map { it.columnName })

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(
                convertToSqlColumnType(DBType.Oracle, actualColumn.type, column.length, true, false),
                convertToSqlColumnType(DBType.Oracle, column.type, column.length, true, false),
                "列 '$column' 的类型应一致"
            )
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
            assertEquals(
                convertToSqlColumnType(
                    DBType.SQLite,
                    actualColumn.type,
                    actualColumn.length,
                    actualColumn.nullable,
                    false
                ),
                convertToSqlColumnType(DBType.SQLite, column.type, column.length, column.nullable, false),
                "列 '$column' 的类型应一致"
            )
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }
    }

    /**
     * 测试sqlite动态创建表功能。
     * 此方法应完成一个测试用例，动态创建一个表，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testCreateTable_myssql() {
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
            assertEquals(
                convertToSqlColumnType(
                    DBType.Mysql,
                    actualColumn.type,
                    actualColumn.length,
                    actualColumn.nullable,
                    false
                ),
                convertToSqlColumnType(DBType.Mssql, column.type, 0, column.nullable, false),
                "列 '$column' 的类型应一致"
            )
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }
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

        val actualColumns = dataSource.table.getTableColumns("tb_user")

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = user.kronosColumns()

        println("actualColumns" + actualColumns.map { it.nullable })
        println("expectedColumns" + expectedColumns.map { it.nullable })

        println("actualColumns" + actualColumns.map { it.primaryKey })
        println("expectedColumns" + expectedColumns.map { it.primaryKey })

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(
                convertToSqlColumnType(
                    DBType.Postgres,
                    actualColumn.type,
                    actualColumn.length,
                    actualColumn.nullable,
                    actualColumn.primaryKey
                ),
                convertToSqlColumnType(DBType.Postgres, column.type, column.length, column.nullable, column.primaryKey),
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
            assertEquals(
                convertToSqlColumnType(DBType.Mysql, actualColumn.type, column.length, true, false),
                convertToSqlColumnType(DBType.Mysql, column.type, 0, true, false),
                "列 '$column' 的类型应一致"
            )
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }

        // 可选：进一步验证列的属性，如类型、是否为主键等，这通常需要更复杂的数据库查询和比较逻辑

        println("表结构同步测试成功")
    }

    /**
     * 测试oracle结构同步功能。
     * 此方法应完成一个测试用例，同步某个表的结构，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testSyncTable_oracle() {
        // 同步user表结构
        val structureSync = dataSource.table.structureSync(user)
        if (!structureSync) {
            println("表结构相同无需同步")
        }

        // 索引
//        val list = user.kronosTableIndex()

        // 验证表结构：通过查询数据库的表结构信息并与实体类字段对比来实现
        val expectedColumns = user.kronosColumns().map {
            it.columnName = it.columnName.uppercase()
            it
        }

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        val actualColumns = dataSource.table.getTableColumns("TB_USER")
        println("actualColumns:" + actualColumns.map { it.columnName })
        println("expectedColumns:" + expectedColumns.map { it.columnName })

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(
                convertToSqlColumnType(DBType.Oracle, actualColumn.type, column.length, true, false),
                convertToSqlColumnType(DBType.Oracle, column.type, column.length, true, false),
                "列 '$column' 的类型应一致"
            )
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
            assertEquals(
                convertToSqlColumnType(DBType.SQLite, actualColumn.type, 0, true, false),
                convertToSqlColumnType(DBType.SQLite, column.type, 0, true, false),
                "列 '$column' 的类型应一致"
            )
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }

        // 可选：进一步验证列的属性，如类型、是否为主键等，这通常需要更复杂的数据库查询和比较逻辑

        println("表结构同步测试成功")
    }

    /**
     * 测试SQLServer结构同步功能。
     * 此方法应完成一个测试用例，同步某个表的结构，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testSyncTable_ssql() {
        println(user.kronosColumns().map { it.columnName })
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
            assertEquals(
                convertToSqlColumnType(DBType.Mssql, actualColumn.type, column.length, true, false),
                convertToSqlColumnType(DBType.Mssql, column.type, column.length, true, false),
                "列 '$column' 的类型应一致"
            )
            assertEquals(actualColumn.tableName, column.tableName, "列 '$column' 的表名应一致")
        }

        // 可选：进一步验证列的属性，如类型、是否为主键等，这通常需要更复杂的数据库查询和比较逻辑

        println("表结构同步测试成功")
    }

    /**
     * 测试postgresql结构同步功能。
     * 此方法应完成一个测试用例，同步某个表的结构，并使用assertEquals断言结果正确性。
     */
    @Test
    fun testSyncTable_postgresql() {
        println(user.kronosColumns().map { it.columnName })
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
        println("expectedColumns: " + expectedColumns.map { it.nullable })
        println("actualColumns: " + actualColumns.map { it.nullable })

        // 确保所有期望的列都存在于实际的列列表中，且类型一致
        expectedColumns.forEach { column ->
            val actualColumn = actualColumns.find { it.columnName == column.columnName }
            assertTrue(actualColumn != null, "列 '$column' 应存在于表中")
            assertEquals(
                convertToSqlColumnType(
                    DBType.Postgres,
                    actualColumn.type,
                    column.length,
                    actualColumn.nullable,
                    actualColumn.primaryKey
                ),
                convertToSqlColumnType(DBType.Postgres, column.type, column.length, column.nullable, column.primaryKey),
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
    fun testLastInsertId(){
        val newUser = OracleUser(
            username = "yf",
            gender = 93
        )
        val (_, lastInsertId) = newUser.insert().execute()
        if (lastInsertId != null) {
            println("插入成功，新记录的ID为：$lastInsertId")
        } else {
            println("插入失败")
        }
    }
}