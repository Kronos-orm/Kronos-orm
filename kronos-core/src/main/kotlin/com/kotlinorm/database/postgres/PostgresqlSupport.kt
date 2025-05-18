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

package com.kotlinorm.database.postgres

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getDBNameFrom
import com.kotlinorm.database.SqlManager.getKotlinColumnType
import com.kotlinorm.database.SqlManager.indexCreateDefSql
import com.kotlinorm.database.mssql.MssqlSupport
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.*
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.functions.FunctionManager.getBuiltFunctionField
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.ddl.TableColumnDiff
import com.kotlinorm.orm.ddl.TableIndexDiff
import com.kotlinorm.orm.join.JoinClauseInfo
import com.kotlinorm.orm.select.SelectClauseInfo
import com.kotlinorm.utils.trimWhitespace

object PostgresqlSupport : DatabasesSupport {
    override var quotes = Pair("\"", "\"")

    override fun getDBNameFromUrl(wrapper: KronosDataSourceWrapper) = wrapper.url.split("//").last().split("/").first()

    override fun getColumnType(type: KColumnType, length: Int, scale: Int): String {
        return when (type) {
            // 布尔类型
            BIT -> "BOOLEAN"

            // 整数类型（优化SERIAL处理）
            TINYINT -> "SMALLINT"  // PostgreSQL无TINYINT，用SMALLINT(-32768 to 32767)
            SMALLINT -> "SMALLINT"
            INT, MEDIUMINT -> "INTEGER"
            BIGINT -> "BIGINT"
            SERIAL -> "SERIAL"      // 自动递增整数(INTEGER)
            YEAR -> "INTEGER"       // 年份用整数存储

            // 浮点类型（支持精度指定）
            REAL -> "REAL"          // 4字节浮点
            FLOAT -> if (length > 0) "FLOAT($length)" else "DOUBLE PRECISION"
            DOUBLE -> "DOUBLE PRECISION"

            // 精确数值（必须处理精度）
            DECIMAL -> when {
                length > 0 && scale > 0 -> "DECIMAL($length,$scale)"
                length > 0 -> "DECIMAL($length,0)"
                else -> "DECIMAL"   // 不指定精度时使用默认
            }
            NUMERIC -> when {
                length > 0 && scale > 0 -> "NUMERIC($length,$scale)"
                length > 0 -> "NUMERIC($length,0)"
                else -> "NUMERIC"
            }

            // 字符类型（处理超长情况）
            CHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            NCHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            VARCHAR -> if (length > 0 && length <= 10485760) "VARCHAR($length)" else "TEXT"
            NVARCHAR -> if (length > 0 && length <= 10485760) "VARCHAR($length)" else "TEXT"
            TEXT, CLOB, MEDIUMTEXT, LONGTEXT -> "TEXT"

            // 二进制类型
            BINARY -> "BYTEA"
            VARBINARY -> "BYTEA"
            LONGVARBINARY, BLOB, MEDIUMBLOB, LONGBLOB -> "BYTEA"

            // 时间类型（支持精度）
            DATE -> "DATE"
            TIME -> "TIME(${scale.coerceIn(0, 6)})"  // 支持0-6位小数秒
            DATETIME -> "TIMESTAMP(${scale.coerceIn(0, 6)})"
            TIMESTAMP -> "TIMESTAMP(${scale.coerceIn(0, 6)})"

            // 特殊类型
            JSON -> "JSONB"         // 推荐使用JSONB（二进制存储）
            XML -> "XML"
            UUID -> "UUID"
            ENUM -> if (length > 0) "VARCHAR($length)" else "VARCHAR(255)"  // 或创建自定义ENUM类型
            SET -> "TEXT"           // 集合类型用TEXT或数组类型

            // 空间类型
            GEOMETRY -> "GEOMETRY"  // PostGIS扩展
            POINT -> "POINT"
            LINESTRING -> "LINESTRING"

            // 默认类型
            else -> "TEXT"          // PostgreSQL更推荐TEXT而非VARCHAR
        }
    }

    override fun getKColumnType(type: String, length: Int, scale: Int): KColumnType {
        return when (type) {
            "INTEGER" -> INT
            "BYTEA" -> BLOB
            "BOOLEAN" -> BIT
            else -> super.getKColumnType(type, length, scale)
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String {
        return "${
            quote(column.columnName)
        }${
            if (column.primaryKey == PrimaryKeyType.IDENTITY) " SERIAL" else " ${getColumnType(column.type, column.length, column.scale)}"
        }${
            if (column.nullable) "" else " NOT NULL"
        }${
            if (column.primaryKey != PrimaryKeyType.NOT) " PRIMARY KEY" else ""
        }${
            if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
        }"
    }

    override fun getTableExistenceSql(dbType: DBType): String =
        "select count(1) from pg_class where relname = :tableName"

    override fun getTableTruncateSql(dbType: DBType, tableName: String, restartIdentity: Boolean) =
        "TRUNCATE ${quote(tableName)} ${if (restartIdentity) "RESTART IDENTITY" else ""}"

    override fun getTableDropSql(dbType: DBType, tableName: String) = "DROP TABLE IF EXISTS $tableName"

    override fun getTableCommentSql(dbType: DBType) =
        "select cast(obj_description(relfilenode, 'pg_class') as varchar) as comment  from pg_class c  where relname = :tableName"

    override fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex) =
        "CREATE${if (index.type.isNotEmpty()) " ${index.type} " else " "}INDEX${(if (index.concurrently) " CONCURRENTLY" else "")} ${index.name} ON ${
            tableName
        }${if (index.method.isNotEmpty()) " USING ${index.method}" else ""} (${
            index.columns.joinToString(
                ", "
            ) { quote(it) }
        })"

    override fun getTableCreateSqlList(
        dbType: DBType,
        tableName: String,
        tableComment: String?,
        columns: List<Field>,
        indexes: List<KTableIndex>
    ): List<String> {
        val columnsSql = columns.joinToString(",") { columnCreateDefSql(dbType, it) }
        val indexesSql = indexes.map { indexCreateDefSql(dbType, tableName, it) }
        return listOfNotNull(
            "CREATE TABLE IF NOT EXISTS \"public\".${quote(tableName)} ($columnsSql)",
            *indexesSql.toTypedArray(),
            *columns.asSequence().filter { !it.kDoc.isNullOrEmpty() }.map {
                "COMMENT ON COLUMN \"public\".${quote(tableName)}.${quote(it.columnName)} IS '${it.kDoc}'"
            }.toList().toTypedArray(),
            if (tableComment.isNullOrEmpty()) null else "COMMENT ON TABLE \"public\".${quote(tableName)} IS '$tableComment'"
        )
    }


    override fun getTableColumns(dataSource: KronosDataSourceWrapper, tableName: String): List<Field> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                SELECT 
                    c.column_name AS COLUMN_NAME,
					col_description((current_schema() || '.' || c.table_name)::regclass::oid, c.ordinal_position) AS COLUMN_COMMENT,
                    CASE 
                        WHEN c.data_type IN ('character varying', 'varchar') THEN 'VARCHAR'
                        WHEN c.data_type IN ('integer', 'int') THEN 'INT'
                        WHEN c.data_type IN ('bigint') THEN 'BIGINT'
                        WHEN c.data_type IN ('smallint') THEN 'TINYINT'
                        WHEN c.data_type IN ('decimal', 'numeric') THEN 'DECIMAL'
                        WHEN c.data_type IN ('double precision', 'real') THEN 'DOUBLE'
                        WHEN c.data_type IN ('boolean') THEN 'BOOLEAN'
                        WHEN c.data_type LIKE 'timestamp%' THEN 'TIMESTAMP'
                        WHEN c.data_type LIKE 'date' THEN 'DATE'
                        ELSE c.data_type
                    END AS DATA_TYPE,
                    c.character_maximum_length AS LENGTH,
                    c.numeric_precision AS SCALE,
                    c.is_nullable = 'YES' AS IS_NULLABLE,
                    c.column_default AS COLUMN_DEFAULT,
                    EXISTS (
                        SELECT 1 
                        FROM information_schema.key_column_usage kcu
                        INNER JOIN information_schema.table_constraints tc 
                            ON kcu.constraint_name = tc.constraint_name
                            AND kcu.constraint_schema = tc.constraint_schema
                        WHERE 
                            tc.constraint_type = 'PRIMARY KEY' AND
                            kcu.table_schema = c.table_schema AND 
                            kcu.table_name = c.table_name AND 
                            kcu.column_name = c.column_name
                    ) OR (c.column_name = 'id' AND c.data_type LIKE 'serial%') AS PRIMARY_KEY
                FROM 
                    information_schema.columns c
                WHERE 
                    c.table_schema = current_schema() AND 
                    c.table_name = :tableName
            """.trimWhitespace(), mapOf("tableName" to tableName, "dbName" to getDBNameFrom(dataSource))
            )
        ).map {
            Field(
                columnName = it["column_name"].toString(),
                type = getKotlinColumnType(DBType.Postgres, it["data_type"].toString(), it["length"] as Int? ?: 0, it["scale"] as Int? ?: 0),
                length = it["length"] as Int? ?: 0,
                scale = it["scale"] as Int? ?: 0,
                tableName = tableName,
                nullable = it["is_nullable"] == true,
                primaryKey = when{
                    it["primary_key"] == false -> PrimaryKeyType.NOT
                    it["column_default"]?.toString()?.startsWith("nextval(") == true -> PrimaryKeyType.IDENTITY
                    else -> PrimaryKeyType.DEFAULT
                },
                // 如果defaultValue =  "('${tableName}_id_seq'::regclass)" 设置成 null
                defaultValue = if (it["column_default"]?.toString()?.startsWith("nextval(") == true) {
                    null
                } else {
                    it["column_default"] as String?
                },
                kDoc = it["column_comment"] as String?
            )
        }
    }

    override fun getTableIndexes(
        dataSource: KronosDataSourceWrapper, tableName: String
    ): List<KTableIndex> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                SELECT indexname AS name
                FROM pg_indexes 
                WHERE tablename = :tableName AND 
                    schemaname = 'public' AND 
                    indexname NOT LIKE CONCAT(tablename, '_pkey');
                 """.trimWhitespace(),
                mapOf(
                    "tableName" to tableName
                )
            )
        ).map {
            KTableIndex(it["name"] as String, arrayOf(), "", "")
        }
    }

    override fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        originalTableComment: String?,
        tableComment: String?,
        columns: TableColumnDiff,
        indexes: TableIndexDiff
    ): List<String> {
        val syncSqlList = mutableListOf<String>()

        if (originalTableComment.orEmpty() != tableComment.orEmpty()) {
            syncSqlList.add("COMMENT ON TABLE ${quote("public")}.${quote(tableName)} IS '${tableComment.orEmpty()}'")
        }

        syncSqlList.addAll(
            indexes.toDelete.map {
                "DROP INDEX ${quote("public")}.${it.name};"
            } + columns.toAdd.map {
                "ALTER TABLE ${quote("public")}.${quote(tableName)} ADD COLUMN ${it.first.columnName} ${
                    columnCreateDefSql(
                        DBType.Postgres, it.first
                    )
                }"
            } + columns.toModified.map {
                "ALTER TABLE ${quote("public")}.${quote(tableName)} ALTER COLUMN ${it.first.columnName} TYPE ${
                    getColumnType(it.first.type, it.first.length, it.first.scale)
                } ${if (it.first.defaultValue != null) ",AlTER COLUMN ${it.first.columnName} SET DEFAULT ${it.first.defaultValue}" else ""} ${
                    if (it.first.nullable) ",ALTER COLUMN ${it.first.columnName} DROP NOT NULL" else ",ALTER COLUMN ${it.first.columnName} SET NOT NULL"
                }"
            } + columns.toModified.map {
                if(it.first.kDoc.isNullOrEmpty()) {
                    "COMMENT ON COLUMN ${quote("public")}.${quote(tableName)}.${quote(it.first.columnName)} IS NULL"
                } else {
                    "COMMENT ON COLUMN ${quote("public")}.${quote(tableName)}.${quote(it.first.columnName)} IS '${it.first.kDoc}'"
                }
            } + columns.toDelete.map {
                "ALTER TABLE ${quote("public")}.${quote(tableName)} DROP COLUMN ${it.columnName}"
            } + indexes.toAdd.map {
                getIndexCreateSql(DBType.Postgres, "${quote("public")}.${quote(tableName)}", it)
            }
        )
        return syncSqlList
    }

    override fun getOnConflictSql(conflictResolver: ConflictResolver): String {
        val (tableName, onFields, toUpdateFields, toInsertFields) = conflictResolver
        return """
        INSERT INTO ${
            quote(tableName)
        } (${
            toInsertFields.joinToString { quote(it) }
        }) SELECT  ${
            toInsertFields.joinToString(", ") { ":$it" }
        } WHERE NOT EXISTS (
               SELECT 1 FROM ${
            quote(tableName)
        } WHERE ${onFields.joinToString(" AND ") { equation(it) }}
            );
            UPDATE ${quote(tableName)} SET ${
            toUpdateFields.joinToString(", ") { equation(it) }
        } WHERE ${
            onFields.joinToString(" AND ") { equation(it) }
        };
        """.trimWhitespace()
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

        if (!databaseName.isNullOrEmpty()) throw UnsupportedDatabaseTypeException(
            DBType.Postgres,
            "Postgresql does not support databaseName in select clause because of its dblink-liked configuration mode"
        )

        val selectSql = selectFields.joinToString(", ") {
            when {
                it is FunctionField -> getBuiltFunctionField(it, dataSource)
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
            PessimisticLock.S -> " FOR SHARE"
            else -> null
        }
        return "SELECT${distinctSql.orEmpty()} $selectSql FROM ${
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

        if (databaseOfTable.isNotEmpty()) throw UnsupportedDatabaseTypeException(
            DBType.Postgres,
            "Postgresql does not support databaseName in select clause because of its dblink-liked configuration mode"
        )

        val selectSql = selectFields.joinToString(", ") {
            val field = it.second
            when {
                field is FunctionField -> getBuiltFunctionField(field, dataSource, true)
                field.type == CUSTOM_CRITERIA_SQL -> field.toString()
                field.name != field.columnName -> "${quote(field, true)} AS ${quote(field.name)}"
                else -> "${quote(field, true)} AS ${MssqlSupport.quote(it.first)}"
            }
        }

        val paginationSql = if (pagination) " LIMIT $ps OFFSET ${ps * (pi - 1)}" else null
        val limitSql = if (paginationSql == null && limit != null && limit > 0) " LIMIT $limit" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        return "SELECT${distinctSql.orEmpty()} $selectSql FROM ${
            quote(tableName)
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