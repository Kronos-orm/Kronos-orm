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

package com.kotlinorm.beans.task

import com.kotlinorm.enums.DBType

data class GeneratedKeyRequest(
    val tableName: String,
    val columnName: String
)

internal fun GeneratedKeyRequest.lastInsertIdFallbackSql(dbType: DBType): String =
    when (dbType) {
        DBType.Mysql, DBType.H2, DBType.OceanBase -> "SELECT LAST_INSERT_ID()"
        DBType.Oracle -> "SELECT MAX(${quoteOracleIdentifier(columnName)}) FROM ${quoteOracleIdentifier(tableName)}"
        DBType.Mssql -> "SELECT SCOPE_IDENTITY()"
        DBType.Postgres -> "SELECT LASTVAL()"
        DBType.DB2 -> "SELECT IDENTITY_VAL_LOCAL() FROM SYSIBM.SYSDUMMY1"
        DBType.Sybase -> "SELECT @@IDENTITY"
        DBType.SQLite -> "SELECT last_insert_rowid()"
        else -> throw UnsupportedOperationException("Unsupported database type: $dbType")
    }

private fun quoteOracleIdentifier(identifier: String): String =
    identifier.split('.').joinToString(".") { part ->
        "\"${part.trim('"').uppercase()}\""
    }
