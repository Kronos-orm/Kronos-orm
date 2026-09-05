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

/**
 * Database-specific JDBC customization applied once while a wrapper is initialized.
 *
 * A plugin may adjust statement settings or register physical readers/argument factories.
 * It must preserve the registry contracts: physical readers return raw values, and logical
 * target conversion remains the responsibility of `ValueCodec`.
 */
interface KronosJdbcPlugin {
    /** Stable diagnostic name recorded in [KronosJdbcConfig.loadedPlugins]. */
    val name: String

    /** Applies this plugin's defaults and extensions to [config]. */
    fun customize(config: KronosJdbcConfig)
}

/**
 * Connection metadata captured at wrapper initialization for diagnostics and plugin selection.
 */
data class KronosDatabaseIdentity(
    /** Detected or explicitly configured Kronos database dialect. */
    val dbType: DBType,
    /** JDBC product name reported by the driver. */
    val databaseProductName: String,
    /** JDBC URL reported by the driver. */
    val url: String,
    /** JDBC user name reported by the driver. */
    val userName: String,
    /** JDBC driver name reported by the driver. */
    val driverName: String
)

/** Built-in database detection and automatic plugin selection. */
object KronosJdbcPlugins {
    /**
     * Detects a dialect from the product name first, then from product name and URL markers.
     *
     * @return detected dialect, or [DBType.Unknown] when no marker is recognized
     */
    fun detectDbType(productName: String, url: String): DBType {
        runCatching { DBType.fromName(productName) }.getOrNull()?.let { return it }
        val marker = "$productName $url".lowercase()
        return when {
            "postgres" in marker || "pgsql" in marker -> DBType.Postgres
            "mysql" in marker || "mariadb" in marker -> DBType.Mysql
            "oracle" in marker -> DBType.Oracle
            "sql server" in marker || "mssql" in marker || "sqlserver" in marker -> DBType.Mssql
            "sqlite" in marker -> DBType.SQLite
            "db2" in marker -> DBType.DB2
            "sybase" in marker -> DBType.Sybase
            "h2" in marker -> DBType.H2
            "oceanbase" in marker -> DBType.OceanBase
            "dameng" in marker || "dm dbms" in marker || "jdbc:dm:" in marker -> DBType.DM8
            "gauss" in marker -> DBType.GaussDB
            else -> DBType.Unknown
        }
    }

    /** Returns the built-in plugin for [dbType], or `null` when no plugin is required. */
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

/** MySQL/OceanBase defaults: use a fetch size of 1000 when none is configured. */
object MysqlJdbcPlugin : KronosJdbcPlugin {
    override val name: String = "mysql"

    override fun customize(config: KronosJdbcConfig) {
        config.statement.fetchSize = config.statement.fetchSize ?: 1000
    }
}

/** PostgreSQL/GaussDB defaults plus `PGobject` vendor-value unwrapping. */
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

/** Oracle/DM8 defaults, Boolean-to-integer binding, and Oracle temporal normalization. */
object OracleJdbcPlugin : KronosJdbcPlugin {
    override val name: String = "oracle"

    override fun customize(config: KronosJdbcConfig) {
        config.oracleLongColumnStrategy = KronosOracleLongColumnStrategy.READ_FIRST
        config.arguments.register(KronosArgumentFactory { value, _ ->
            if (value is Boolean) {
                KronosArgument { position, statement, _ -> statement.setInt(position, if (value) 1 else 0) }
            } else {
                null
            }
        })
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

/** SQL Server defaults: use a fetch size of 500 when none is configured. */
object MssqlJdbcPlugin : KronosJdbcPlugin {
    override val name: String = "mssql"

    override fun customize(config: KronosJdbcConfig) {
        config.statement.fetchSize = config.statement.fetchSize ?: 500
    }
}

/** SQLite defaults: use a fetch size of 500 when none is configured. */
object SqliteJdbcPlugin : KronosJdbcPlugin {
    override val name: String = "sqlite"

    override fun customize(config: KronosJdbcConfig) {
        config.statement.fetchSize = config.statement.fetchSize ?: 500
    }
}

/** DB2 defaults: use a fetch size of 500 when none is configured. */
object Db2JdbcPlugin : KronosJdbcPlugin {
    override val name: String = "db2"

    override fun customize(config: KronosJdbcConfig) {
        config.statement.fetchSize = config.statement.fetchSize ?: 500
    }
}

/** H2 plugin hook; H2 requires no additional defaults today. */
object H2JdbcPlugin : KronosJdbcPlugin {
    override val name: String = "h2"

    override fun customize(config: KronosJdbcConfig) = Unit
}

/** Sybase defaults: use a fetch size of 500 when none is configured. */
object SybaseJdbcPlugin : KronosJdbcPlugin {
    override val name: String = "sybase"

    override fun customize(config: KronosJdbcConfig) {
        config.statement.fetchSize = config.statement.fetchSize ?: 500
    }
}
