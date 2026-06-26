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

import com.kotlinorm.beans.dsl.KTableIndex

/**
 * DdlStatement
 *
 * Sealed class representing DDL (Data Definition Language) SQL statements. Includes CREATE TABLE,
 * ALTER TABLE, DROP TABLE, CREATE INDEX, DROP INDEX, and TRUNCATE TABLE.
 *
 * @author OUSC
 */
sealed class DdlStatement : Statement {
    /**
     * CreateTableStatement
     *
     * Represents a CREATE TABLE statement.
     *
     * @property tableName The name of the table to create
     * @property columns List of column definitions
     * @property indexes Optional list of index definitions
     * @property comment Optional table comment
     */
    data class CreateTableStatement(
            val tableName: String,
            val columns: List<ColumnDefinition>,
            val indexes: List<KTableIndex> = emptyList(),
            val comment: String? = null
    ) : DdlStatement()

    /**
     * AlterTableStatement
     *
     * Represents an ALTER TABLE statement. This is a sealed class itself to represent different
     * types of ALTER TABLE operations.
     */
    sealed class AlterTableStatement : DdlStatement() {
        abstract val tableName: String
        
        /**
         * AddColumnStatement
         *
         * Represents ALTER TABLE ... ADD COLUMN.
         */
        data class AddColumnStatement(
                override val tableName: String,
                val column: ColumnDefinition
        ) : AlterTableStatement()

        /**
         * DropColumnStatement
         *
         * Represents ALTER TABLE ... DROP COLUMN.
         */
        data class DropColumnStatement(
                override val tableName: String,
                val columnName: String
        ) : AlterTableStatement()

        /**
         * ModifyColumnStatement
         *
         * Represents ALTER TABLE ... ALTER COLUMN / MODIFY COLUMN.
         */
        data class ModifyColumnStatement(
                override val tableName: String,
                val column: ColumnDefinition
        ) : AlterTableStatement()
    }

    /**
     * DropTableStatement
     *
     * Represents a DROP TABLE statement.
     *
     * @property tableName The name of the table to drop
     * @property ifExists Whether to use IF EXISTS clause
     */
    data class DropTableStatement(val tableName: String, val ifExists: Boolean = false) :
            DdlStatement()

    /**
     * CreateIndexStatement
     *
     * Represents a CREATE INDEX statement.
     *
     * @property indexName The name of the index
     * @property tableName The name of the table
     * @property columns List of column names to index
     * @property unique Whether the index is unique
     */
    data class CreateIndexStatement(
            val indexName: String,
            val tableName: String,
            val columns: List<String>,
            val unique: Boolean = false
    ) : DdlStatement()

    /**
     * DropIndexStatement
     *
     * Represents a DROP INDEX statement.
     *
     * @property indexName The name of the index to drop
     * @property tableName The name of the table
     */
    data class DropIndexStatement(val indexName: String, val tableName: String) : DdlStatement()

    /**
     * TruncateTableStatement
     *
     * Represents a TRUNCATE TABLE statement.
     *
     * @property tableName The name of the table to truncate
     * @property restartIdentity Whether to restart identity sequences (PostgreSQL)
     */
    data class TruncateTableStatement(val tableName: String, val restartIdentity: Boolean = false) :
            DdlStatement()
}
