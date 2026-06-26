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
package com.kotlinorm.interfaces

import com.kotlinorm.ast.AstSqlRenderer
import com.kotlinorm.ast.DdlStatement
import com.kotlinorm.ast.DeleteStatement
import com.kotlinorm.ast.InsertStatement
import com.kotlinorm.ast.RenderContext
import com.kotlinorm.ast.RenderedSql
import com.kotlinorm.ast.SelectStatement
import com.kotlinorm.ast.SqlRenderer
import com.kotlinorm.ast.UnionStatement
import com.kotlinorm.ast.UpdateStatement
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.orm.ddl.TableColumnDiff
import com.kotlinorm.orm.ddl.TableIndexDiff
import com.kotlinorm.utils.getTypeSafeValue

interface DatabasesSupport {
        var quotes: Pair<String, String>
        
        // Each database support implementation should provide its specific renderer
        val renderer: SqlRenderer

        fun getDBNameFromUrl(wrapper: KronosDataSourceWrapper): String

        fun String?.orEmpty(): String = this ?: ""

        fun quote(str: String): String = "${quotes.first}$str${quotes.second}"

        fun quote(field: Field): String =
                if (field.tableName.isNotBlank()) {
                    "${quote(field.tableName)}.${quote(field.columnName)}"
                } else {
                    quote(field.columnName)
                }

        fun equation(field: Field): String =
                "${quote(field)} = :${field.name}"

        fun getColumnType(type: KColumnType, length: Int, scale: Int): String

        fun getKColumnType(type: String, length: Int = 0, scale: Int = 0): KColumnType =
                KColumnType.fromString(type)

        fun getColumnCreateSql(dbType: DBType, column: Field): String

        fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex): String

        fun getTableCreateSqlList(
                dbType: DBType,
                tableName: String,
                tableComment: String?,
                columns: List<Field>,
                indexes: List<KTableIndex>
        ): List<String>

        fun getTableExistenceSql(dbType: DBType): String

        fun getTableTruncateSql(dbType: DBType, tableName: String, restartIdentity: Boolean): String
        fun getTableDropSql(dbType: DBType, tableName: String): String

        fun getTableCommentSql(dbType: DBType): String

        fun getTableColumns(
                dataSource: KronosDataSourceWrapper,
                tableName: String,
        ): List<Field>

        fun getTableIndexes(
                dataSource: KronosDataSourceWrapper,
                tableName: String,
        ): List<KTableIndex>

        fun getTableSyncSqlList(
                dataSource: KronosDataSourceWrapper,
                tableName: String,
                originalTableComment: String?,
                tableComment: String?,
                columns: TableColumnDiff,
                indexes: TableIndexDiff,
        ): List<String>

        fun getOnConflictSql(conflictResolver: ConflictResolver): String

        // AST-based SQL rendering with parameters
        fun getSelectSqlWithParams(
                dataSource: KronosDataSourceWrapper,
                select: SelectStatement,
                fieldsMap: Map<String, Field> = emptyMap()
        ): RenderedSql {
                val context = RenderContext(quotes = quotes)
                val rendered = renderer.render(select, context)
                // Process parameters for database-specific type conversion
                val processedParams = rendered.parameters.mapValues { (key, value) ->
                        val field = fieldsMap[key]
                        if (field != null && value != null) {
                                com.kotlinorm.utils.processParams(dataSource, field, value)
                        } else {
                                value
                        }
                }
                return RenderedSql(rendered.sql, processedParams)
        }

        fun getInsertSqlWithParams(
                dataSource: KronosDataSourceWrapper,
                insert: InsertStatement,
                fieldsMap: Map<String, Field> = emptyMap()
        ): RenderedSql {
                val context = RenderContext(quotes = quotes)
                val rendered = renderer.render(insert, context)
                // Process parameters for database-specific type conversion
                val processedParams = rendered.parameters.mapValues { (key, value) ->
                        val field = fieldsMap[key]
                        if (field != null && value != null) {
                                com.kotlinorm.utils.processParams(dataSource, field, value)
                        } else {
                                value
                        }
                }
                return RenderedSql(rendered.sql, processedParams)
        }

        fun getUpdateSqlWithParams(
                dataSource: KronosDataSourceWrapper,
                update: UpdateStatement,
                fieldsMap: Map<String, Field> = emptyMap()
        ): RenderedSql {
                val context = RenderContext(quotes = quotes)
                val rendered = renderer.render(update, context)
                // Process parameters for database-specific type conversion
                val processedParams = rendered.parameters.mapValues { (key, value) ->
                        val field = fieldsMap[key]
                        if (field != null && value != null) {
                                com.kotlinorm.utils.processParams(dataSource, field, value)
                        } else {
                                value
                        }
                }
                return RenderedSql(rendered.sql, processedParams)
        }

        fun getDeleteSqlWithParams(
                dataSource: KronosDataSourceWrapper,
                delete: DeleteStatement,
                fieldsMap: Map<String, Field> = emptyMap()
        ): RenderedSql {
                val context = RenderContext(quotes = quotes)
                val rendered = renderer.render(delete, context)
                // Process parameters for database-specific type conversion
                val processedParams = rendered.parameters.mapValues { (key, value) ->
                        val field = fieldsMap[key]
                        if (field != null && value != null) {
                                com.kotlinorm.utils.processParams(dataSource, field, value)
                        } else {
                                value
                        }
                }
                return RenderedSql(rendered.sql, processedParams)
        }

        fun getUnionSqlWithParams(
                dataSource: KronosDataSourceWrapper,
                union: UnionStatement,
                fieldsMap: Map<String, Field> = emptyMap()
        ): RenderedSql {
                val context = RenderContext(quotes = quotes)
                val rendered = renderer.render(union, context)
                // Process parameters for database-specific type conversion
                val processedParams = rendered.parameters.mapValues { (key, value) ->
                        val field = fieldsMap[key]
                        if (field != null && value != null) {
                                com.kotlinorm.utils.processParams(dataSource, field, value)
                        } else {
                                value
                        }
                }
                return RenderedSql(rendered.sql, processedParams)
        }

        // AST-based DDL rendering methods
        fun getCreateTableSql(
                dataSource: KronosDataSourceWrapper,
                createTable: DdlStatement.CreateTableStatement
        ): String {
                val context = RenderContext(quotes = quotes)
                return renderer.renderDdlStatement(createTable, context)
        }

        fun getAlterTableSql(
                dataSource: KronosDataSourceWrapper,
                alterTable: DdlStatement.AlterTableStatement
        ): String {
                val context = RenderContext(quotes = quotes)
                return renderer.renderDdlStatement(alterTable, context)
        }

        fun getDropTableSql(
                dataSource: KronosDataSourceWrapper,
                dropTable: DdlStatement.DropTableStatement
        ): String {
                val context = RenderContext(quotes = quotes)
                return renderer.renderDdlStatement(dropTable, context)
        }

        fun getCreateIndexSql(
                dataSource: KronosDataSourceWrapper,
                createIndex: DdlStatement.CreateIndexStatement
        ): String {
                val context = RenderContext(quotes = quotes)
                return renderer.renderDdlStatement(createIndex, context)
        }

        fun getDropIndexSql(
                dataSource: KronosDataSourceWrapper,
                dropIndex: DdlStatement.DropIndexStatement
        ): String {
                val context = RenderContext(quotes = quotes)
                return renderer.renderDdlStatement(dropIndex, context)
        }

        fun getTruncateTableSql(
                dataSource: KronosDataSourceWrapper,
                truncate: DdlStatement.TruncateTableStatement
        ): String {
                val context = RenderContext(quotes = quotes)
                return renderer.renderDdlStatement(truncate, context)
        }

        /**
         * Process parameter values for database-specific type conversion.
         * This handles field type conversion based on Field metadata.
         *
         * @param wrapper Data source wrapper
         * @param field Field metadata
         * @param value Parameter value to process
         * @return Processed value suitable for the database
         */
        fun processParams(
                wrapper: KronosDataSourceWrapper,
                field: Field,
                value: Any?
        ): Any? {
                if (value == null) return null
                if (field.serializable) return com.kotlinorm.Kronos.serializeProcessor.serialize(value)

                return when {
                        // Check if field is TIMESTAMP or PostgreSQL DATETIME
                        field.type == KColumnType.TIMESTAMP || 
                        (wrapper.dbType == DBType.Postgres && field.type == KColumnType.DATETIME) ->
                                getTypeSafeValue("java.sql.Timestamp", value, field.superTypes, field.dateFormat)
                        else -> value
                }
        }

        fun renderCriteria(dataSource: KronosDataSourceWrapper, criteria: Criteria): String =
                AstSqlRenderer.renderCriteriaDirect(dataSource, this, criteria)
}
