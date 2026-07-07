/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.wrappers

import com.kotlinorm.enums.DBType

interface KronosJdbcPlugin {
    val name: String

    fun customize(config: KronosJdbcConfig)
}

data class KronosDatabaseIdentity(
    val dbType: DBType,
    val databaseProductName: String,
    val url: String,
    val userName: String,
    val driverName: String
)

object KronosJdbcPlugins {
    fun detectDbType(productName: String, url: String): DBType {
        runCatching { DBType.fromName(productName) }.getOrNull()?.let { return it }
        val marker = "$productName $url".lowercase()
        return when {
            "postgres" in marker || "pgsql" in marker -> DBType.Postgres
            "mysql" in marker || "mariadb" in marker -> DBType.Mysql
            "oracle" in marker -> DBType.Oracle
            "sql server" in marker || "mssql" in marker -> DBType.Mssql
            "sqlite" in marker -> DBType.SQLite
            "db2" in marker -> DBType.DB2
            "sybase" in marker -> DBType.Sybase
            "h2" in marker -> DBType.H2
            "oceanbase" in marker -> DBType.OceanBase
            "dameng" in marker || "dm dbms" in marker -> DBType.DM8
            "gauss" in marker -> DBType.GaussDB
            else -> DBType.Unknown
        }
    }

    fun autoPlugin(dbType: DBType): KronosJdbcPlugin? =
        when (dbType) {
            DBType.Mysql, DBType.OceanBase -> MysqlJdbcPlugin
            DBType.Postgres, DBType.GaussDB -> PostgresJdbcPlugin
            DBType.Oracle, DBType.DM8 -> OracleJdbcPlugin
            DBType.Mssql -> MssqlJdbcPlugin
            DBType.SQLite -> SqliteJdbcPlugin
            DBType.DB2 -> Db2JdbcPlugin
            DBType.H2 -> H2JdbcPlugin
            DBType.Sybase -> SybaseJdbcPlugin
            DBType.Unknown -> null
        }
}

object MysqlJdbcPlugin : KronosJdbcPlugin {
    override val name: String = "mysql"

    override fun customize(config: KronosJdbcConfig) {
        config.statement.fetchSize = config.statement.fetchSize ?: 1000
    }
}

object PostgresJdbcPlugin : KronosJdbcPlugin {
    override val name: String = "postgres"

    override fun customize(config: KronosJdbcConfig) {
        config.statement.fetchSize = config.statement.fetchSize ?: 1000
        config.columnMappers.registerVendorReader({ _, _, value, _ ->
            if (value.javaClass.name == "org.postgresql.util.PGobject") {
                value.javaClass.methods.firstOrNull { it.name == "getValue" && it.parameterCount == 0 }
                    ?.invoke(value)
            } else null
        })
    }
}

object OracleJdbcPlugin : KronosJdbcPlugin {
    override val name: String = "oracle"

    override fun customize(config: KronosJdbcConfig) {
        config.oracleLongColumnStrategy = KronosOracleLongColumnStrategy.READ_FIRST
        config.columnMappers.registerVendorReader(KronosVendorValueReader { resultSet, position, value, _ ->
            when (value.javaClass.name) {
                "oracle.sql.TIMESTAMP",
                "oracle.sql.TIMESTAMPTZ",
                "oracle.sql.TIMESTAMPLTZ" -> resultSet.getTimestamp(position)

                else -> oracleDateValue(resultSet, position, value)
            }
        })
    }

    private fun oracleDateValue(resultSet: java.sql.ResultSet, position: Int, value: Any): Any? {
        val className = value.javaClass.name
        val columnClassName = runCatching { resultSet.metaData.getColumnClassName(position) }.getOrNull()
        return when {
            className.startsWith("oracle.sql.DATE") ->
                if (columnClassName == "java.sql.Timestamp" || columnClassName == "oracle.sql.TIMESTAMP") {
                    resultSet.getTimestamp(position)
                } else {
                    resultSet.getDate(position)
                }

            value is java.sql.Date && columnClassName == "java.sql.Timestamp" ->
                resultSet.getTimestamp(position)

            else -> null
        }
    }
}

object MssqlJdbcPlugin : KronosJdbcPlugin {
    override val name: String = "mssql"

    override fun customize(config: KronosJdbcConfig) {
        config.statement.fetchSize = config.statement.fetchSize ?: 500
    }
}

object SqliteJdbcPlugin : KronosJdbcPlugin {
    override val name: String = "sqlite"

    override fun customize(config: KronosJdbcConfig) {
        config.statement.fetchSize = config.statement.fetchSize ?: 500
    }
}

object Db2JdbcPlugin : KronosJdbcPlugin {
    override val name: String = "db2"

    override fun customize(config: KronosJdbcConfig) {
        config.statement.fetchSize = config.statement.fetchSize ?: 500
    }
}

object H2JdbcPlugin : KronosJdbcPlugin {
    override val name: String = "h2"

    override fun customize(config: KronosJdbcConfig) = Unit
}

object SybaseJdbcPlugin : KronosJdbcPlugin {
    override val name: String = "sybase"

    override fun customize(config: KronosJdbcConfig) {
        config.statement.fetchSize = config.statement.fetchSize ?: 500
    }
}
