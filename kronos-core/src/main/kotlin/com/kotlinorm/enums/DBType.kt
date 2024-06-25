/**
 * Copyright 2022-2024 kronos-orm
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

/**
 * DBType
 *
 * The database type Kotlin ORM supports.
 *
 * @author OUSC
 */
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
    DM8;

    companion object {
        /**
         * Returns the enum constant of [DBType] that matches the given [name],
         * or null if no such constant exists.
         *
         * @param name the name of the enum constant to find
         * @return the enum constant that matches the given [name], or null if no such constant exists
         */
        fun fromName(name: String) = Mssql.takeIf { name == "Microsoft SQL Server" } ?: Postgres.takeIf { name == "PostgreSQL" } ?: entries.first { it.name.uppercase() == name.uppercase() }
    }
}