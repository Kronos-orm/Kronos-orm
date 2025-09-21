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
package com.kotlinorm.database.sqlite

import com.kotlinorm.ast.AstSqlRenderer
import com.kotlinorm.ast.DeleteStatement
import com.kotlinorm.ast.InsertStatement
import com.kotlinorm.ast.SelectStatement
import com.kotlinorm.ast.UpdateStatement
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.indexCreateDefSql
import com.kotlinorm.database.SqlManager.sqlColumnType
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.*
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.ddl.TableColumnDiff
import com.kotlinorm.orm.ddl.TableIndexDiff
import com.kotlinorm.utils.extractNumberInParentheses

object SqliteSupport : DatabasesSupport {
    override var quotes = Pair("\"", "\"")

    override fun getDBNameFromUrl(wrapper: KronosDataSourceWrapper) = wrapper.url.split("//").last()

    override fun getColumnType(type: KColumnType, length: Int, scale: Int): String {
        return when (type) {
            BIT, TINYINT, SMALLINT, INT, MEDIUMINT, BIGINT, SERIAL, YEAR, SET -> "INTEGER"
            REAL, FLOAT, DOUBLE -> "REAL"
            DECIMAL, NUMERIC -> "NUMERIC"
            CHAR,
            VARCHAR,
            TEXT,
            MEDIUMTEXT,
            LONGTEXT,
            DATE,
            TIME,
            DATETIME,
            TIMESTAMP,
            CLOB,
            JSON,
            ENUM,
            NVARCHAR,
            NCHAR,
            NCLOB,
            UUID,
            GEOMETRY,
            POINT,
            LINESTRING,
            XML -> "TEXT"
            BINARY, VARBINARY, LONGVARBINARY, BLOB, MEDIUMBLOB, LONGBLOB -> "BLOB"
            else -> "NOT"
        }
    }

    override fun getKColumnType(type: String, length: Int, scale: Int): KColumnType {
        return when (type) {
            "INTEGER" -> INT
            else -> super.getKColumnType(type, length, scale)
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String =
            "${
        quote(column.columnName)
    }${
        " ${sqlColumnType(dbType, column.type, column.length, column.scale)}"
    }${
        if (column.nullable) "" else " NOT NULL"
    }${
        if (column.primaryKey != PrimaryKeyType.NOT) " PRIMARY KEY" else ""
    }${
        if (column.primaryKey == PrimaryKeyType.IDENTITY) " AUTOINCREMENT" else ""
    }${
        if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
    }"

    // 生成SQLite的列定义字符串
    // 索引 CREATE INDEX "dfsdf"
    // ON "_tb_user_old_20240617" (
    //  "password"
    // );
    override fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex): String {
        return "CREATE ${index.type} INDEX IF NOT EXISTS ${index.name} ON ${quote(tableName)} (${
            index.columns.joinToString(", ") { column ->
                if (index.method.isNotEmpty()) "${quote(column)} COLLATE ${index.method}"
                else quote(column)
            }
        })"
    }

    override fun getTableCreateSqlList(
            dbType: DBType,
            tableName: String,
            tableComment: String?,
            columns: List<Field>,
            indexes: List<KTableIndex>
    ): List<String> {
        val columnsSql = columns.joinToString(",") { columnCreateDefSql(dbType, it) }
        val indexesSql = indexes.map { indexCreateDefSql(dbType, tableName, it) }
        return listOf(
                "CREATE TABLE IF NOT EXISTS ${quote(tableName)} ($columnsSql)",
                *indexesSql.toTypedArray()
        )
    }

    override fun getTableExistenceSql(dbType: DBType) =
            "SELECT COUNT(1)  as CNT FROM sqlite_master where type='table' and name= :tableName"

    override fun getTableTruncateSql(dbType: DBType, tableName: String, restartIdentity: Boolean) =
            """ DELETE FROM '$tableName';
            VACUUM;
            ${if (restartIdentity) "DELETE FROM sqlite_sequence WHERE name='$tableName';" else ""}
        """.trimIndent()

    override fun getTableDropSql(dbType: DBType, tableName: String) =
            "DROP TABLE IF EXISTS $tableName"

    override fun getTableCommentSql(dbType: DBType) = ""

    override fun getTableColumns(
            dataSource: KronosDataSourceWrapper,
            tableName: String
    ): List<Field> {
        return dataSource.forList(KronosAtomicQueryTask("PRAGMA table_info($tableName)")).map {
            var identity = false
            if (it["pk"] as Int == 1) {
                val sql =
                        dataSource.forObject(
                                KronosAtomicQueryTask(
                                        "SELECT sql FROM sqlite_master WHERE tbl_name=:tableName AND sql LIKE '%AUTOINCREMENT%'",
                                        mapOf("tableName" to tableName)
                                ),
                                String::class,
                                false,
                                listOf()
                        ) as
                                String?
                if (sql != null &&
                                Regex(
                                                """("?\w+"?)\sINTEGER\sNOT\sNULL\sPRIMARY\sKEY\sAUTOINCREMENT"""
                                        )
                                        .find(sql)
                                        ?.groupValues
                                        ?.get(1) == quote(it["name"] as String)
                ) {
                    identity = true
                }
            }
            val (length, scale) = extractNumberInParentheses(it["type"].toString())
            Field(
                    columnName = it["name"].toString(),
                    type = getKColumnType(it["type"].toString().split('(').first(), length), // 处理类型
                    length = length, // 处理长度
                    scale = scale, // 处理精度
                    tableName = tableName,
                    nullable = it["notnull"] as Int == 0, // 直接使用notnull字段判断是否可空
                    primaryKey =
                            when {
                                it["pk"] as Int == 0 -> PrimaryKeyType.NOT
                                identity -> PrimaryKeyType.IDENTITY
                                else -> PrimaryKeyType.DEFAULT
                            },
                    defaultValue = it["dflt_value"] as String?,
                    // SQLITE DO NOT SUPPORT COMMENT FOR COLUMN/TABLE
                    )
        }
    }

    override fun getTableIndexes(
            dataSource: KronosDataSourceWrapper,
            tableName: String,
    ): List<KTableIndex> {
        return dataSource.forList(
                        KronosAtomicQueryTask(
                                "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name = :tableName",
                                mapOf("tableName" to tableName)
                        )
                )
                .map { KTableIndex(it["name"] as String, emptyArray(), "", "") }
    }

    override fun getTableSyncSqlList(
            dataSource: KronosDataSourceWrapper,
            tableName: String,
            originalTableComment: String?,
            tableComment: String?,
            columns: TableColumnDiff,
            indexes: TableIndexDiff
    ): List<String> {
        val dbType = dataSource.dbType
        return indexes.toDelete.map { "DROP INDEX ${it.name}" } +
                columns.toDelete.map {
                    "ALTER TABLE ${quote(tableName)} ADD COLUMN ${getColumnCreateSql(dbType, it)}"
                } +
                columns.toModified.map {
                    "ALTER TABLE ${quote(tableName)} MODIFY COLUMN ${getColumnCreateSql(dbType, it.first)}"
                } +
                columns.toDelete.map {
                    "ALTER TABLE ${quote(tableName)} DROP COLUMN ${it.columnName}"
                } +
                indexes.toAdd.map {
                    // CREATE INDEX "aaa" ON "tb_user" ("username" COLLATE RTRIM )  如果${it.type}不是空
                    // 需要 在每个column后面加 COLLATE ${it.type} (${it.columns.joinToString(",")})需要改
                    "CREATE ${it.method} INDEX ${it.name} ON ${quote(tableName)} (${
                it.columns.joinToString(",") { column ->
                    if (it.type.isNotEmpty()) "${quote(column)} COLLATE ${it.type}"
                    else quote(column)
                }
            })"
                }
    }

    override fun getOnConflictSql(conflictResolver: ConflictResolver): String {
        val (tableName, onFields, toUpdateFields, toInsertFields) = conflictResolver
        return """
            INSERT OR REPLACE INTO ${
            quote(tableName)
        }(${toInsertFields.joinToString { quote(it) }}) VALUES (${
            toInsertFields.joinToString(", ") { ":$it" }
        }) ON CONFLICT (${
            onFields.joinToString(", ") { quote(it) }
        }) DO UPDATE SET ${
            toUpdateFields.joinToString(", ") { equation(it) }
        }
        """.trimIndent()
    }

    override fun getInsertSql(
            dataSource: KronosDataSourceWrapper,
            tableName: String,
            columns: List<Field>
    ) =
            "INSERT INTO ${quote(tableName)} (${columns.joinToString { quote(it) }}) VALUES (${columns.joinToString { ":$it" }})"

    override fun getDeleteSql(
            dataSource: KronosDataSourceWrapper,
            tableName: String,
            whereClauseSql: String?
    ) = "DELETE FROM ${quote(tableName)}${whereClauseSql.orEmpty()}"

    override fun getUpdateSql(
            dataSource: KronosDataSourceWrapper,
            tableName: String,
            toUpdateFields: List<Field>,
            whereClauseSql: String?,
            plusAssigns: MutableList<Pair<Field, String>>,
            minusAssigns: MutableList<Pair<Field, String>>
    ) =
            "UPDATE ${quote(tableName)} SET ${toUpdateFields.joinToString { equation(it + "New") }}" +
                    plusAssigns.joinToString {
                        ", ${quote(it.first)} = ${quote(it.first)} + :${it.second}"
                    } +
                    minusAssigns.joinToString {
                        ", ${quote(it.first)} = ${quote(it.first)} - :${it.second}"
                    } +
                    whereClauseSql.orEmpty()

    // AST-based SQL rendering methods
    override fun getSelectSql(
            dataSource: KronosDataSourceWrapper,
            select: SelectStatement
    ): String = AstSqlRenderer.renderSelect(dataSource, this, select)

    override fun getInsertSql(
            dataSource: KronosDataSourceWrapper,
            insert: InsertStatement
    ): String = AstSqlRenderer.renderInsert(dataSource, this, insert)

    override fun getUpdateSql(
            dataSource: KronosDataSourceWrapper,
            update: UpdateStatement
    ): String = AstSqlRenderer.renderUpdate(dataSource, this, update)

    override fun getDeleteSql(
            dataSource: KronosDataSourceWrapper,
            delete: DeleteStatement
    ): String = AstSqlRenderer.renderDelete(dataSource, this, delete)

    override fun getSelectSqlWithParams(
            dataSource: KronosDataSourceWrapper,
            select: SelectStatement
    ): AstSqlRenderer.RenderedSql = AstSqlRenderer.renderSelectWithParams(dataSource, this, select)

    override fun getInsertSqlWithParams(
            dataSource: KronosDataSourceWrapper,
            insert: InsertStatement
    ): AstSqlRenderer.RenderedSql = AstSqlRenderer.renderInsertWithParams(dataSource, this, insert)

    override fun getUpdateSqlWithParams(
            dataSource: KronosDataSourceWrapper,
            update: UpdateStatement
    ): AstSqlRenderer.RenderedSql = AstSqlRenderer.renderUpdateWithParams(dataSource, this, update)

    override fun getDeleteSqlWithParams(
            dataSource: KronosDataSourceWrapper,
            delete: DeleteStatement
    ): AstSqlRenderer.RenderedSql = AstSqlRenderer.renderDeleteWithParams(dataSource, this, delete)
}
