/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm.enums

/** Database dialects supported by Kronos. */
enum class DBType {
    Mysql,
    Oracle,
    Postgres,
    Mssql,
    SQLite,
    DB2,
    Sybase,
    H2,
    OceanBase,
    DM8,
    GaussDB,
    Unknown;

    companion object {
        /** Resolves a product name without relying on JVM APIs. */
        fun fromName(name: String): DBType = when {
            name.equals("Microsoft SQL Server", ignoreCase = true) -> Mssql
            name.equals("PostgreSQL", ignoreCase = true) -> Postgres
            else -> entries.first { it.name.equals(name, ignoreCase = true) }
        }
    }
}
