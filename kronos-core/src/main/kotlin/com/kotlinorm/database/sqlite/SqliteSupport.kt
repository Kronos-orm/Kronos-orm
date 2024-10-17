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

package com.kotlinorm.database.sqlite

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.indexCreateDefSql
import com.kotlinorm.database.SqlManager.sqlColumnType
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.*
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.orm.join.JoinClauseInfo
import com.kotlinorm.orm.select.SelectClauseInfo

object SqliteSupport : DatabasesSupport {
    override var quotes = Pair("\"", "\"")

    override fun getDBNameFromUrl(wrapper: KronosDataSourceWrapper) = wrapper.url.split("//").last()

    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            BIT, TINYINT, SMALLINT, INT, MEDIUMINT, BIGINT, SERIAL, YEAR, SET -> "INTEGER"
            REAL, FLOAT, DOUBLE -> "REAL"
            DECIMAL, NUMERIC -> "NUMERIC"
            CHAR, VARCHAR, TEXT, MEDIUMTEXT, LONGTEXT, DATE, TIME, DATETIME, TIMESTAMP, CLOB, JSON, ENUM, NVARCHAR, NCHAR, NCLOB, UUID, GEOMETRY, POINT, LINESTRING, XML -> "TEXT"
            BINARY, VARBINARY, LONGVARBINARY, BLOB, MEDIUMBLOB, LONGBLOB -> "BLOB"
            else -> "NONE"
        }
    }

    override fun getKColumnType(type: String, length: Int): KColumnType {
        return when (type) {
            "INTEGER" -> INT
            else -> super.getKColumnType(type, length)
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String = "${
        quote(column.columnName)
    }${
        " ${sqlColumnType(dbType, column.type, column.length)}"
    }${
        if (column.nullable) "" else " NOT NULL"
    }${
        if (column.primaryKey) " PRIMARY KEY" else ""
    }${
        if (column.identity) " AUTOINCREMENT" else ""
    }${
        if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
    }"

    // 生成SQLite的列定义字符串
    // 索引 CREATE INDEX "dfsdf"
    //ON "_tb_user_old_20240617" (
    //  "password"
    //);
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

    override fun getTableDropSql(dbType: DBType, tableName: String) = "DROP TABLE IF EXISTS $tableName"

    override fun getTableComment(dbType: DBType) = ""

    override fun getTableColumns(dataSource: KronosDataSourceWrapper, tableName: String): List<Field> {
        fun extractNumberInParentheses(input: String): Int {
            val regex = Regex("""\((\d+)\)""") // 匹配括号内的数字部分
            val matchResult = regex.find(input)

            return matchResult?.groupValues?.get(1)?.toInt() ?: 0
        }
        return dataSource.forList(
            KronosAtomicQueryTask("PRAGMA table_info($tableName)")
        ).map {
            var identity = false
            if (it["pk"] as Int == 1) {
                val sql = dataSource.forObject(
                    KronosAtomicQueryTask(
                        "SELECT sql FROM sqlite_master WHERE tbl_name=:tableName AND sql LIKE '%AUTOINCREMENT%'",
                        mapOf("tableName" to tableName)
                    ), String::class
                ) as String?
                if(sql != null && Regex("""("?\w+"?)\sINTEGER\sNOT\sNULL\sPRIMARY\sKEY\sAUTOINCREMENT""").find(sql)?.groupValues?.get(
                        1
                    ) == quote(it["name"] as String)
                ) {
                    identity = true
                }
            }
            val length = extractNumberInParentheses(it["type"].toString())
            Field(
                columnName = it["name"].toString(),
                type = getKColumnType(it["type"].toString().split('(').first(), length), // 处理类型
                length = length, // 处理长度
                tableName = tableName,
                nullable = it["notnull"] as Int == 0, // 直接使用notnull字段判断是否可空
                primaryKey = it["pk"] as Int == 1,
                identity = identity,
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
                "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name = :tableName", mapOf(
                    "tableName" to tableName
                )
            )
        ).map {
            KTableIndex(it["name"] as String, emptyArray(), "", "")
        }
    }

    override fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper, tableName: String, originalTableComment: String?, tableComment: String?, columns: TableColumnDiff, indexes: TableIndexDiff
    ): List<String> {
        val dbType = dataSource.dbType
        return indexes.toDelete.map {
            "DROP INDEX ${it.name}"
        } + columns.toDelete.map {
            "ALTER TABLE ${quote(tableName)} ADD COLUMN ${getColumnCreateSql(dbType, it)}"
        } + columns.toModified.map {
            "ALTER TABLE ${quote(tableName)} MODIFY COLUMN ${getColumnCreateSql(dbType, it.first)}"
        } + columns.toDelete.map {
            "ALTER TABLE ${quote(tableName)} DROP COLUMN ${it.columnName}"
        } + indexes.toAdd.map {
            // CREATE INDEX "aaa" ON "tb_user" ("username" COLLATE RTRIM )  如果${it.type}不是空 需要 在每个column后面加 COLLATE ${it.type} (${it.columns.joinToString(",")})需要改
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

    override fun getInsertSql(dataSource: KronosDataSourceWrapper, tableName: String, columns: List<Field>) =
        "INSERT INTO ${quote(tableName)} (${columns.joinToString { quote(it) }}) VALUES (${columns.joinToString { ":$it" }})"

    override fun getDeleteSql(dataSource: KronosDataSourceWrapper, tableName: String, whereClauseSql: String?) =
        "DELETE FROM ${quote(tableName)}${whereClauseSql.orEmpty()}"

    override fun getUpdateSql(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        toUpdateFields: List<Field>,
        whereClauseSql: String?,
        plusAssigns: MutableList<Pair<Field, String>>,
        minusAssigns: MutableList<Pair<Field, String>>
    ) =
        "UPDATE ${quote(tableName)} SET ${toUpdateFields.joinToString { equation(it + "New") }}" +
                plusAssigns.joinToString { ", ${quote(it.first)} = ${quote(it.first)} + :${it.second}" } +
                minusAssigns.joinToString { ", ${quote(it.first)} = ${quote(it.first)} - :${it.second}" } +
                whereClauseSql.orEmpty()

    override fun getSelectSql(dataSource: KronosDataSourceWrapper, selectClause: SelectClauseInfo): String {
        val (databaseName, tableName, selectFields, distinct, pagination, pi, ps, limit, lock, whereClauseSql, groupByClauseSql, orderByClauseSql, havingClauseSql) = selectClause
        val selectFieldsSql = selectFields.joinToString(", ") {
            when {
                it.type == CUSTOM_CRITERIA_SQL -> it.toString()
                it.name != it.columnName -> "${quote(it.columnName)} AS ${quote(it.name)}"
                else -> quote(it)
            }
        }
        val paginationSql = if (pagination) " LIMIT $ps OFFSET $pi" else null
        val limitSql = if (paginationSql == null && limit != null && limit > 0) " LIMIT $limit" else null
        val distinctSql = if (distinct) " DISTINCT" else null

        if (null != lock) throw UnsupportedDatabaseTypeException(
            DBType.SQLite,
            "Sqlite doesn't support the lock() method because Sqlite only has database locks"
        )

        return "SELECT${distinctSql.orEmpty()} $selectFieldsSql FROM ${
            databaseName?.let { quote(it) + "." } ?: ""
        }${
            quote(tableName)
        }${
            whereClauseSql.orEmpty()
        }${
            groupByClauseSql.orEmpty()
        }${
            havingClauseSql.orEmpty()
        }${
            orderByClauseSql.orEmpty()
        }${
            paginationSql ?: limitSql ?: ""
        }"
    }

    override fun getJoinSql(dataSource: KronosDataSourceWrapper, joinClause: JoinClauseInfo): String {
        val (tableName, selectFields, distinct, pagination, pi, ps, limit, databaseOfTable, whereClauseSql, groupByClauseSql, orderByClauseSql, havingClauseSql, joinSql) = joinClause
        val selectFieldsSql = selectFields.joinToString(", ") {
            when {
                it.second.type == CUSTOM_CRITERIA_SQL -> it.second.toString()
                it.second.name != it.second.columnName -> "${quote(it.second, true)} AS ${quote(it.second.name)}"
                else -> "${SqlManager.quote(dataSource, it.second, true, databaseOfTable)} AS ${quote(it.first)}"
            }
        }
        val paginationSql = if (pagination) " LIMIT $ps OFFSET $pi" else null
        val limitSql = if (paginationSql == null && limit != null && limit > 0) " LIMIT $limit" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        return "SELECT${distinctSql.orEmpty()} $selectFieldsSql FROM ${
            SqlManager.quote(dataSource, tableName, true, map = databaseOfTable)
        }${
            joinSql.orEmpty()
        }${
            whereClauseSql.orEmpty()
        }${
            groupByClauseSql.orEmpty()
        }${
            havingClauseSql.orEmpty()
        }${
            orderByClauseSql.orEmpty()
        }${
            paginationSql ?: limitSql ?: ""
        }"
    }
}