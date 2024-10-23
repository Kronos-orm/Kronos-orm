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

package com.kotlinorm.database.mysql

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getKotlinColumnType
import com.kotlinorm.database.SqlManager.indexCreateDefSql
import com.kotlinorm.database.SqlManager.sqlColumnType
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.*
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.functions.FunctionManager.getFunctionTransformed
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.orm.join.JoinClauseInfo
import com.kotlinorm.orm.select.SelectClauseInfo
import com.kotlinorm.utils.trimWhitespace

object MysqlSupport : DatabasesSupport {
    override var quotes = Pair("`", "`")

    override fun getDBNameFromUrl(wrapper: KronosDataSourceWrapper) =
        wrapper.url.split("?").first().split("//")[1].split("/").last()

    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            BIT -> "TINYINT(1)"
            TINYINT -> "TINYINT"
            SMALLINT -> "SMALLINT"
            INT, SERIAL -> "INT"
            MEDIUMINT -> "MEDIUMINT"
            BIGINT -> "BIGINT"
            REAL -> "REAL"
            FLOAT -> "FLOAT"
            DOUBLE -> "DOUBLE"
            DECIMAL -> "DECIMAL"
            NUMERIC -> "NUMERIC"
            CHAR, NCHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            VARCHAR, NVARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            TEXT, XML -> "TEXT"
            MEDIUMTEXT -> "MEDIUMTEXT"
            LONGTEXT -> "LONGTEXT"
            DATE -> "DATE"
            TIME -> "TIME"
            DATETIME -> "DATETIME"
            TIMESTAMP -> "TIMESTAMP"
            BINARY -> "BINARY"
            VARBINARY -> "VARBINARY"
            LONGVARBINARY, LONGBLOB -> "LONGBLOB"
            BLOB -> "BLOB"
            MEDIUMBLOB -> "MEDIUMBLOB"
            CLOB -> "CLOB"
            JSON -> "JSON"
            ENUM -> "ENUM"
            NCLOB -> "NCLOB"
            UUID -> "CHAR(36)"
            YEAR -> "YEAR"
            SET -> "SET"
            GEOMETRY -> "GEOMETRY"
            POINT -> "POINT"
            LINESTRING -> "LINESTRING"
            else -> "VARCHAR(255)"
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String =
        "${
            quote(column.columnName)
        }${
            " ${sqlColumnType(dbType, column.type, column.length)}"
        }${
            if (column.nullable) "" else " NOT NULL"
        }${
            if (column.primaryKey) " PRIMARY KEY" else ""
        }${
            if (column.identity) " AUTO_INCREMENT" else ""
        }${
            if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
        } COMMENT '${column.kDoc.orEmpty()}'"

    override fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex) =
        "CREATE${if (index.type == "NORMAL") " " else " ${index.type} "}INDEX ${index.name} ON ${quote(tableName)} (${
            index.columns.joinToString(
                ","
            ) { quote(it) }
        }) USING ${index.method.ifEmpty { "BTREE" }}"

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
            "CREATE TABLE IF NOT EXISTS ${quote(tableName)} ($columnsSql)" + if (tableComment.isNullOrEmpty()) "" else " COMMENT = '$tableComment'",
            *indexesSql.toTypedArray()
        )
    }

    override fun getTableExistenceSql(dbType: DBType) =
        "SELECT COUNT(1) FROM information_schema.tables WHERE table_name = :tableName AND table_schema = :dbName"

    override fun getTableTruncateSql(dbType: DBType, tableName: String, restartIdentity: Boolean) =
        "TRUNCATE TABLE ${quote(tableName)}"

    override fun getTableDropSql(dbType: DBType, tableName: String) = "DROP TABLE IF EXISTS $tableName"

    override fun getTableComment(dbType: DBType) =
        "SELECT `TABLE_COMMENT` FROM information_schema.TABLES WHERE TABLE_NAME = :tableName AND TABLE_SCHEMA = :dbName"

    override fun getTableColumns(dataSource: KronosDataSourceWrapper, tableName: String): List<Field> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                SELECT 
                    c.COLUMN_NAME, 
                    c.DATA_TYPE, 
                    c.CHARACTER_MAXIMUM_LENGTH LENGTH, 
                    c.IS_NULLABLE,
                    c.COLUMN_DEFAULT,
                    c.COLUMN_COMMENT,
                    (CASE WHEN c.EXTRA = 'auto_increment' THEN 'YES' ELSE 'NO' END) AS IDENTITY,
                    (CASE WHEN c.COLUMN_KEY = 'PRI' THEN 'YES' ELSE 'NO' END) AS PRIMARY_KEY
                FROM 
                    INFORMATION_SCHEMA.COLUMNS c
                WHERE 
                 c.TABLE_SCHEMA = DATABASE() AND 
                 c.TABLE_NAME = :tableName
                ORDER BY ORDINAL_POSITION
            """.trimWhitespace(), mapOf("tableName" to tableName)
            )
        ).map {
            Field(
                columnName = it["COLUMN_NAME"].toString(),
                type = getKotlinColumnType(
                    DBType.Mysql, it["DATA_TYPE"].toString(), (it["LENGTH"] as Long? ?: 0).toInt()
                ),
                length = (it["LENGTH"] as Long? ?: 0).toInt(),
                tableName = tableName,
                nullable = it["IS_NULLABLE"] == "YES",
                primaryKey = it["PRIMARY_KEY"] == "YES",
                identity = it["IDENTITY"] == "YES",
                defaultValue = it["COLUMN_DEFAULT"] as String?,
                kDoc = it["COLUMN_COMMENT"] as String?
            )
        }
    }

    override fun getTableIndexes(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ): List<KTableIndex> {

        val resultSet = queryTableIndexes(dataSource, tableName)
        val indexMap = resultSet.groupBy { it["indexName"].toString() }

        return indexMap.mapNotNull { (indexName, columns) ->
            columns.sortedBy { it["seqInIndex"] as Long }

            val exp = columns.firstOrNull() ?: return@mapNotNull null

            val method = exp["indexType"].toString()
            val type = when {
                exp["indexType"] == "FULLTEXT" -> "FULLTEXT"
                exp["indexType"] == "SPATIAL" -> "SPATIAL"
                exp["nonUnique"] as Int == 0 -> "UNIQUE"
                else -> "NORMAL"
            }

            KTableIndex(
                name = indexName,
                columns = columns.map { it["columnName"].toString() }.toTypedArray(),
                type = type,
                method = method
            )
        }
    }

    private fun queryTableIndexes(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ) = dataSource.forList(
        KronosAtomicQueryTask(
            """
            SELECT DISTINCT
                INDEX_NAME AS `indexName`,
                COLUMN_NAME AS `columnName`,
                SEQ_IN_INDEX AS `seqInIndex`,
                NON_UNIQUE AS `nonUnique`,
                INDEX_TYPE AS `indexType`
            FROM 
                INFORMATION_SCHEMA.STATISTICS
            WHERE 
                TABLE_SCHEMA = DATABASE() AND 
                TABLE_NAME = :tableName AND 
                INDEX_NAME != 'PRIMARY'
            """.trimWhitespace(), mapOf(
                "tableName" to tableName
            )
        )
    )

    override fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        originalTableComment: String?,
        tableComment: String?,
        columns: TableColumnDiff,
        indexes: TableIndexDiff,
    ): List<String> {
        val syncSqlList = mutableListOf<String>()

        if (originalTableComment.orEmpty() != tableComment.orEmpty()) {
            syncSqlList.add("ALTER TABLE ${quote(tableName)} COMMENT '${tableComment.orEmpty()}'")
        }

        syncSqlList.addAll(indexes.toDelete.map {
            "ALTER TABLE ${quote(tableName)} DROP INDEX ${it.name}"
        } + columns.toAdd.map {
            "ALTER TABLE ${quote(tableName)} ADD COLUMN ${
                columnCreateDefSql(
                    DBType.Mysql, it.first
                )
            } " + if (it.second != null) "AFTER ${quote(it.second!!)}" else "FIRST"
        } + columns.toModified.map {
            "ALTER TABLE ${quote(tableName)} MODIFY COLUMN ${
                columnCreateDefSql(
                    DBType.Mysql, it.first
                ).replace(" PRIMARY KEY", "")
            } ${if (it.second != null) "AFTER ${quote(it.second!!)}" else "FIRST"} ${
                if (it.first.primaryKey) ", DROP PRIMARY KEY, ADD PRIMARY KEY (${
                    quote(
                        it.first
                    )
                })" else ""
            }"
        } + columns.toDelete.map {
            "ALTER TABLE ${quote(tableName)} DROP COLUMN ${quote(it)}"
        } + indexes.toAdd.map {
            "ALTER TABLE ${quote(tableName)} ADD${if (it.type == "NORMAL") " " else " ${it.type} "}INDEX ${it.name} (${
                it.columns.joinToString(", ") { f -> quote(f) }
            }) USING ${it.method}"
        })

        return syncSqlList
    }

    override fun getOnConflictSql(conflictResolver: ConflictResolver): String {
        val (tableName, _, toUpdateFields, toInsertFields) = conflictResolver
        return "INSERT INTO ${quote(tableName)} (${toInsertFields.joinToString { quote(it) }}) " + "VALUES (${
            toInsertFields.joinToString(
                ", "
            ) { ":$it" }
        }) " + "ON DUPLICATE KEY UPDATE ${toUpdateFields.joinToString(", ") { equation(it) }}"
    }

    override fun getInsertSql(dataSource: KronosDataSourceWrapper, tableName: String, columns: List<Field>) =
        "INSERT INTO ${quote(tableName)} (${columns.joinToString { quote(it) }}) " + "VALUES (${columns.joinToString { ":$it" }})"

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
        val selectSql = selectFields.joinToString(", ") {
            when {
                it is FunctionField -> getFunctionTransformed(it, dataSource)
                it.type == CUSTOM_CRITERIA_SQL -> it.toString()
                it.name != it.columnName -> "${quote(it.columnName)} AS ${quote(it.name)}"
                else -> quote(it)
            }
        }

        val paginationSql = if (pagination) " LIMIT $ps OFFSET ${ps * (pi - 1)}" else null
        val limitSql = if (paginationSql == null && limit != null && limit > 0) " LIMIT $limit" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        val lockSql = when (lock) {
            PessimisticLock.X -> " FOR UPDATE"
            PessimisticLock.S -> " LOCK IN SHARE MODE"
            else -> null
        }
        return "SELECT${distinctSql.orEmpty()} $selectSql FROM ${
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
        }${
            lockSql.orEmpty()
        }"
    }

    override fun getJoinSql(dataSource: KronosDataSourceWrapper, joinClause: JoinClauseInfo): String {
        val (tableName, selectFields, distinct, pagination, pi, ps, limit, databaseOfTable, whereClauseSql, groupByClauseSql, orderByClauseSql, havingClauseSql, joinSql) = joinClause

        val selectSql = selectFields.joinToString(", ") {
            val field = it.second
            when {
                field is FunctionField -> getFunctionTransformed(field, dataSource, true)
                field.type == CUSTOM_CRITERIA_SQL -> field.toString()
                field.name != field.columnName -> "${quote(field, true)} AS ${quote(field.name)}"
                else -> "${SqlManager.quote(dataSource, field, true, databaseOfTable)} AS ${quote(it.first)}"
            }
        }

        val paginationSql = if (pagination) " LIMIT $ps OFFSET ${ps * (pi - 1)}" else null
        val limitSql = if (paginationSql == null && limit != null && limit > 0) " LIMIT $limit" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        return "SELECT${distinctSql.orEmpty()} $selectSql FROM ${
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