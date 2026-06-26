/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *     http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kotlinorm.ast

/**
 * TableName
 *
 * Represents a table name reference with optional database/schema prefix. Supports formats like:
 * table, schema.table, database.schema.table
 *
 * @property database Optional database name
 * @property schema Optional schema name
 * @property table The table name (required)
 * @property alias Optional table alias
 *
 * @author OUSC
 */
data class TableName(
        val database: String? = null,
        val schema: String? = null,
        val table: String,
        val alias: String? = null
) : TableReference
