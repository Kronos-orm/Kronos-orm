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

class KronosJdbcConfig(
    val dbType: DBType,
    val databaseProductName: String,
    val url: String,
    val driverName: String,
    val arguments: KronosArgumentRegistry = KronosArgumentRegistry.defaults(),
    val columnMappers: KronosColumnMapperRegistry = KronosColumnMapperRegistry.defaults()
) {
    var exceptionTranslator: KronosSQLExceptionTranslator = SqlStateSQLExceptionTranslator()
    var warningPolicy: KronosSqlWarningPolicy = KronosSqlWarningPolicy.IGNORE
    var statement: KronosStatementSettings = KronosStatementSettings()
    var resultSet: KronosResultSetSettings = KronosResultSetSettings()
    var oracleLongColumnStrategy: KronosOracleLongColumnStrategy = KronosOracleLongColumnStrategy.DISABLED
    val loadedPlugins: MutableList<String> = mutableListOf()

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

data class KronosStatementSettings(
    var fetchSize: Int? = null,
    var maxRows: Int? = null,
    var queryTimeoutSeconds: Int? = null,
    var poolable: Boolean? = null,
    var escapeProcessing: Boolean? = null,
    var closeOnCompletion: Boolean = false
)

data class KronosResultSetSettings(
    var type: Int = ResultSet.TYPE_FORWARD_ONLY,
    var concurrency: Int = ResultSet.CONCUR_READ_ONLY,
    var holdability: Int? = null
)

enum class KronosSqlWarningPolicy {
    IGNORE,
    THROW
}

enum class KronosOracleLongColumnStrategy {
    DISABLED,

    /**
     * Read Oracle LONG/LONG RAW columns before all other columns in the same row.
     *
     * This is a vendor workaround, not a generic JDBC behavior from Spring/Jdbi.
     */
    READ_FIRST
}
