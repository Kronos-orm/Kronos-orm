package com.kotlinorm.testutils

import com.kotlinorm.Kronos
import com.kotlinorm.enums.DBType
import com.kotlinorm.wrappers.*

/**
 * Base class for Kronos tests with common initialization
 * 
 * Provides pre-configured Kronos instances for different databases.
 * Subclasses can simply extend the appropriate base class instead of 
 * repeating the init block in every test.
 */
abstract class KronosTestBase(dbType: DBType) {
    companion object {
        private var initialized = false
        
        @Synchronized
        fun ensureInitialized(dbType: DBType) {
            if (!initialized) {
                Kronos.init {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
                    dataSource = {
                        when (dbType) {
                            DBType.Mysql -> SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
                            DBType.Postgres -> SamplePostgresJdbcWrapper()
                            DBType.Oracle -> SampleOracleJdbcWrapper
                            DBType.SQLite -> SampleSqliteJdbcWrapper
                            DBType.Mssql -> SampleSqlServerJdbcWrapper
                            else -> SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
                        }
                    }
                }
                initialized = true
            }
        }
    }
    
    init {
        ensureInitialized(dbType)
    }
}

/**
 * Base class for MySQL tests
 * Usage: class MyTest : MysqlTestBase()
 */
abstract class MysqlTestBase : KronosTestBase(DBType.Mysql)

/**
 * Base class for PostgreSQL tests
 * Usage: class MyTest : PostgresTestBase()
 */
abstract class PostgresTestBase : KronosTestBase(DBType.Postgres)

/**
 * Base class for Oracle tests
 * Usage: class MyTest : OracleTestBase()
 */
abstract class OracleTestBase : KronosTestBase(DBType.Oracle)

/**
 * Base class for SQLite tests
 * Usage: class MyTest : SqliteTestBase()
 */
abstract class SqliteTestBase : KronosTestBase(DBType.SQLite)

/**
 * Base class for SQL Server tests
 * Usage: class MyTest : MssqlTestBase()
 */
abstract class MssqlTestBase : KronosTestBase(DBType.Mssql)
