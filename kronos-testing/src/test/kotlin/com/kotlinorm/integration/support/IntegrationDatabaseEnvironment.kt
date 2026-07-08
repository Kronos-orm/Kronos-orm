package com.kotlinorm.integration.support

import com.kotlinorm.database.SqlHandler
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource
import java.nio.file.Files
import javax.sql.DataSource

data class IntegrationDatabaseEnvironment(
    val displayName: String,
    val driverClassName: String,
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val enabled: Boolean = true,
    val probeSql: String = "SELECT 1",
    val supportsConflictUpsert: Boolean = true,
    val wrapperFactory: (DataSource) -> KronosDataSourceWrapper = { KronosJdbcWrapper(it) },
) {
    fun createWrapper(): KronosDataSourceWrapper {
        val source = BasicDataSource().apply {
            driverClassName = this@IntegrationDatabaseEnvironment.driverClassName
            url = this@IntegrationDatabaseEnvironment.url
            username = this@IntegrationDatabaseEnvironment.username
            password = this@IntegrationDatabaseEnvironment.password
            maxTotal = 3
            maxIdle = 2
            minIdle = 0
            validationQuery = probeSql
        }
        return wrapperFactory(source)
    }
}

object IntegrationDatabaseEnvironments {
    val mysql = IntegrationDatabaseEnvironment(
        displayName = "MySQL",
        driverClassName = "com.mysql.cj.jdbc.Driver",
        url = env("MYSQL_URL")
            ?: "jdbc:mysql://localhost:3306/kronos_testing?useUnicode=true&characterEncoding=utf-8&useSSL=false" +
            "&serverTimezone=Asia/Shanghai&allowMultiQueries=true&allowPublicKeyRetrieval=true" +
            "&useServerPrepStmts=false&rewriteBatchedStatements=true",
        username = env("MYSQL_USERNAME") ?: "kronos",
        password = env("MYSQL_PASSWORD") ?: "",
    )

    val postgres = IntegrationDatabaseEnvironment(
        displayName = "PostgreSQL",
        driverClassName = "org.postgresql.Driver",
        url = env("POSTGRES_URL") ?: "jdbc:postgresql://localhost:5432/kronos_testing",
        username = env("POSTGRES_USERNAME") ?: "postgres",
        password = env("POSTGRES_PASSWORD") ?: "",
    )

    val sqlServer = IntegrationDatabaseEnvironment(
        displayName = "SQL Server",
        driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver",
        url = env("SQLSERVER_JDBC_URL") ?: env("SQLSERVER_URL") ?: env("MSSQL_URL")
            ?: "jdbc:sqlserver://localhost:1433;databaseName=kronos_testing;encrypt=true;trustServerCertificate=true",
        username = env("SQLSERVER_USERNAME") ?: env("MSSQL_USERNAME") ?: "SA",
        password = env("SQLSERVER_PASSWORD") ?: env("MSSQL_PASSWORD") ?: "YourStrong!Passw0rd",
        supportsConflictUpsert = false,
    )

    val oracle = IntegrationDatabaseEnvironment(
        displayName = "Oracle",
        driverClassName = "oracle.jdbc.OracleDriver",
        url = env("ORACLE_JDBC_URL") ?: env("ORACLE_URL") ?: "jdbc:oracle:thin:@localhost:1521/FREEPDB1",
        username = env("ORACLE_USERNAME") ?: "kronos",
        password = env("ORACLE_PASSWORD") ?: "KronosPassw0rd1",
        probeSql = "SELECT 1 FROM DUAL",
        supportsConflictUpsert = false,
    )

    val sqlite = IntegrationDatabaseEnvironment(
        displayName = "SQLite",
        driverClassName = "org.sqlite.JDBC",
        url = env("SQLITE_URL") ?: sqliteTempUrl(),
    )

    private fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

    private fun sqliteTempUrl(): String {
        val file = Files.createTempFile("kronos-testing-integration-", ".sqlite").toFile()
        file.deleteOnExit()
        return "jdbc:sqlite:${file.absolutePath}"
    }
}

fun KronosDataSourceWrapper.verifyConnection(probeSql: String) {
    with(SqlHandler) {
        query(probeSql)
    }
}
