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
import com.kotlinorm.ast.DeleteStatement
import com.kotlinorm.ast.InsertStatement
import com.kotlinorm.ast.SelectStatement
import com.kotlinorm.ast.UpdateStatement
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.orm.ddl.TableColumnDiff
import com.kotlinorm.orm.ddl.TableIndexDiff

interface DatabasesSupport {
        var quotes: Pair<String, String>

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

        fun getInsertSql(
                dataSource: KronosDataSourceWrapper,
                tableName: String,
                columns: List<Field>
        ): String

        fun getDeleteSql(
                dataSource: KronosDataSourceWrapper,
                tableName: String,
                whereClauseSql: String?
        ): String

        fun getUpdateSql(
                dataSource: KronosDataSourceWrapper,
                tableName: String,
                toUpdateFields: List<Field>,
                whereClauseSql: String?,
                plusAssigns: MutableList<Pair<Field, String>>,
                minusAssigns: MutableList<Pair<Field, String>>
        ): String

        // AST-based SQL rendering methods - to be implemented by each database support
        fun getSelectSql(dataSource: KronosDataSourceWrapper, select: SelectStatement): String

        fun getInsertSql(dataSource: KronosDataSourceWrapper, insert: InsertStatement): String

        fun getUpdateSql(dataSource: KronosDataSourceWrapper, update: UpdateStatement): String

        fun getDeleteSql(dataSource: KronosDataSourceWrapper, delete: DeleteStatement): String

        // AST-based SQL rendering with parameters - to be implemented by each database support
        fun getSelectSqlWithParams(
                dataSource: KronosDataSourceWrapper,
                select: SelectStatement
        ): AstSqlRenderer.RenderedSql

        fun getInsertSqlWithParams(
                dataSource: KronosDataSourceWrapper,
                insert: InsertStatement
        ): AstSqlRenderer.RenderedSql

        fun getUpdateSqlWithParams(
                dataSource: KronosDataSourceWrapper,
                update: UpdateStatement
        ): AstSqlRenderer.RenderedSql

        fun getDeleteSqlWithParams(
                dataSource: KronosDataSourceWrapper,
                delete: DeleteStatement
        ): AstSqlRenderer.RenderedSql

        fun renderCriteria(dataSource: KronosDataSourceWrapper, criteria: Criteria): String =
                com.kotlinorm.ast.AstSqlRenderer.renderCriteriaDirect(dataSource, this, criteria)
}
