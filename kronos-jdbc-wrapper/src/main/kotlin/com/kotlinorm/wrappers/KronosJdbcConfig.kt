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
import java.sql.ResultSet

/**
 * Mutable JDBC behavior configured on a wrapper and snapshotted for each statement handle.
 *
 * Argument factories operate only at physical binding time, and column mappers operate only
 * at physical result-reading time. Neither registry performs logical target conversion.
 *
 * @param dbType Kronos database dialect associated with this configuration
 * @param databaseProductName product name reported by JDBC metadata
 * @param url JDBC URL used for diagnostics and plugin matching
 * @param driverName JDBC driver name used for diagnostics and plugin matching
 * @param arguments ordered physical parameter binders
 * @param columnMappers ordered physical result readers and vendor normalizers
 */
class KronosJdbcConfig(
    val dbType: DBType,
    val databaseProductName: String,
    val url: String,
    val driverName: String,
    val arguments: KronosArgumentRegistry = KronosArgumentRegistry.defaults(),
    val columnMappers: KronosColumnMapperRegistry = KronosColumnMapperRegistry.defaults()
) {
    /** Converts driver SQLExceptions after the operation has failed. */
    var exceptionTranslator: KronosSQLExceptionTranslator = SqlStateSQLExceptionTranslator()

    /** Controls whether collected JDBC warnings are ignored or raised. */
    var warningPolicy: KronosSqlWarningPolicy = KronosSqlWarningPolicy.IGNORE

    /** Statement-level fetch, row-limit, timeout, and lifecycle settings. */
    var statement: KronosStatementSettings = KronosStatementSettings()

    /** Result-set type, concurrency, and holdability requested at prepare time. */
    var resultSet: KronosResultSetSettings = KronosResultSetSettings()

    /** Optional Oracle `LONG`/`LONG RAW` read-order workaround. */
    var oracleLongColumnStrategy: KronosOracleLongColumnStrategy = KronosOracleLongColumnStrategy.DISABLED

    /** Names of automatically applied database plugins, in installation order. */
    val loadedPlugins: MutableList<String> = mutableListOf()

    /**
     * Captures current settings and registry order for one statement handle.
     *
     * Mutable settings and registration lists are copied so later wrapper configuration
     * changes do not alter an operation already in progress.
     *
     * @return independently mutable configuration with equivalent current values
     */
    fun snapshot(): KronosJdbcConfig =
        KronosJdbcConfig(
            dbType = dbType,
            databaseProductName = databaseProductName,
            url = url,
            driverName = driverName,
            arguments = arguments.copy(),
            columnMappers = columnMappers.copy()
        ).also { copy ->
            copy.exceptionTranslator = exceptionTranslator
            copy.warningPolicy = warningPolicy
            copy.statement = statement.copy()
            copy.resultSet = resultSet.copy()
            copy.oracleLongColumnStrategy = oracleLongColumnStrategy
            copy.loadedPlugins.addAll(loadedPlugins)
        }
}

/** Settings applied to every prepared statement created from a configuration snapshot. */
data class KronosStatementSettings(
    var fetchSize: Int? = null,
    var maxRows: Int? = null,
    var queryTimeoutSeconds: Int? = null,
    var poolable: Boolean? = null,
    var escapeProcessing: Boolean? = null,
    var closeOnCompletion: Boolean = false
)

/** JDBC result-set characteristics requested when preparing non-generated-key statements. */
data class KronosResultSetSettings(
    var type: Int = ResultSet.TYPE_FORWARD_ONLY,
    var concurrency: Int = ResultSet.CONCUR_READ_ONLY,
    var holdability: Int? = null
)

/** Determines whether collected JDBC warnings are retained only or raised as exceptions. */
enum class KronosSqlWarningPolicy {
    IGNORE,
    THROW
}

/** Selects handling for Oracle's order-sensitive `LONG` and `LONG RAW` columns. */
enum class KronosOracleLongColumnStrategy {
    DISABLED,

    /**
     * Read Oracle LONG/LONG RAW columns before all other columns in the same row.
     *
     * This is a vendor workaround, not a generic JDBC behavior from Spring/Jdbi.
     */
    READ_FIRST
}
