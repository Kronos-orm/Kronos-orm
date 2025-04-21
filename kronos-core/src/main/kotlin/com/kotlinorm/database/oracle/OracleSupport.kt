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

package com.kotlinorm.database.oracle

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager.getDBNameFrom
import com.kotlinorm.database.SqlManager.getKotlinColumnType
import com.kotlinorm.database.mssql.MssqlSupport
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.BIGINT
import com.kotlinorm.enums.KColumnType.BINARY
import com.kotlinorm.enums.KColumnType.BIT
import com.kotlinorm.enums.KColumnType.BLOB
import com.kotlinorm.enums.KColumnType.CHAR
import com.kotlinorm.enums.KColumnType.CLOB
import com.kotlinorm.enums.KColumnType.CUSTOM_CRITERIA_SQL
import com.kotlinorm.enums.KColumnType.DATE
import com.kotlinorm.enums.KColumnType.DATETIME
import com.kotlinorm.enums.KColumnType.DECIMAL
import com.kotlinorm.enums.KColumnType.DOUBLE
import com.kotlinorm.enums.KColumnType.ENUM
import com.kotlinorm.enums.KColumnType.FLOAT
import com.kotlinorm.enums.KColumnType.GEOMETRY
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.JSON
import com.kotlinorm.enums.KColumnType.LINESTRING
import com.kotlinorm.enums.KColumnType.LONGBLOB
import com.kotlinorm.enums.KColumnType.LONGTEXT
import com.kotlinorm.enums.KColumnType.LONGVARBINARY
import com.kotlinorm.enums.KColumnType.MEDIUMBLOB
import com.kotlinorm.enums.KColumnType.MEDIUMINT
import com.kotlinorm.enums.KColumnType.MEDIUMTEXT
import com.kotlinorm.enums.KColumnType.NCHAR
import com.kotlinorm.enums.KColumnType.NCLOB
import com.kotlinorm.enums.KColumnType.NUMERIC
import com.kotlinorm.enums.KColumnType.NVARCHAR
import com.kotlinorm.enums.KColumnType.POINT
import com.kotlinorm.enums.KColumnType.REAL
import com.kotlinorm.enums.KColumnType.SERIAL
import com.kotlinorm.enums.KColumnType.SET
import com.kotlinorm.enums.KColumnType.SMALLINT
import com.kotlinorm.enums.KColumnType.TEXT
import com.kotlinorm.enums.KColumnType.TIME
import com.kotlinorm.enums.KColumnType.TIMESTAMP
import com.kotlinorm.enums.KColumnType.TINYINT
import com.kotlinorm.enums.KColumnType.UUID
import com.kotlinorm.enums.KColumnType.VARBINARY
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.enums.KColumnType.XML
import com.kotlinorm.enums.KColumnType.YEAR
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.functions.FunctionManager.getBuiltFunctionField
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.orm.join.JoinClauseInfo
import com.kotlinorm.orm.select.SelectClauseInfo
import com.kotlinorm.utils.trimWhitespace

object OracleSupport : DatabasesSupport {
    override var quotes = Pair("\"", "\"")

    override fun getDBNameFromUrl(wrapper: KronosDataSourceWrapper) = wrapper.userName

    override fun getColumnType(type: KColumnType, length: Int, scale: Int): String {
        return when (type) {
            // 数值类型处理（Oracle 主要使用 NUMBER）
            BIT -> "NUMBER(1)"
            TINYINT -> "NUMBER(3)"       // -128 到 127
            SMALLINT -> "NUMBER(5)"      // -32,768 到 32,767
            MEDIUMINT -> "NUMBER(7)"     // -8,388,608 到 8,388,607
            INT -> "NUMBER(${length.takeIf { it > 0 } ?: 10})"  // 默认 10 位精度
            BIGINT -> "NUMBER(19)"       // -2^63 到 2^63-1
            SERIAL -> "NUMBER GENERATED ALWAYS AS IDENTITY"  // Oracle 12c+ 自增方式

            // 浮点类型
            REAL -> "BINARY_FLOAT"       // Oracle 32位浮点
            FLOAT -> if (length > 0) "FLOAT($length)" else "BINARY_DOUBLE"  // 默认 64位
            DOUBLE -> "BINARY_DOUBLE"    // 明确双精度

            // 精确数值（必须处理精度）
            DECIMAL -> when {
                length > 0 && scale > 0 -> "NUMBER($length,$scale)"
                length > 0 -> "NUMBER($length,0)"
                else -> "NUMBER(10,0)"   // Oracle 常用默认值
            }

            NUMERIC -> when {
                length > 0 && scale > 0 -> "NUMERIC($length,$scale)"
                else -> "NUMERIC(10,0)"
            }

            // 字符类型
            CHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            VARCHAR -> "VARCHAR2(${length.takeIf { it > 0 } ?: 255})"  // 推荐 VARCHAR2
            NVARCHAR -> "NVARCHAR2(${length.takeIf { it > 0 } ?: 255})"
            NCHAR -> "NCHAR(${length.takeIf { it > 0 } ?: 255})"

            // 大对象类型
            TEXT, MEDIUMTEXT, LONGTEXT -> "CLOB"
            CLOB -> "CLOB"
            NCLOB -> "NCLOB"
            BINARY, VARBINARY -> "RAW(${length.takeIf { it > 0 } ?: 2000})"  // RAW 默认长度
            BLOB, MEDIUMBLOB, LONGBLOB, LONGVARBINARY -> "BLOB"

            // 时间类型
            DATE -> "DATE"               // 包含日期和时间
            TIME -> "TIMESTAMP(0)"       // 单独时间用 TIMESTAMP
            DATETIME -> "TIMESTAMP(6)"   // 高精度时间戳
            TIMESTAMP -> "TIMESTAMP(${scale.coerceIn(0, 9)})"  // 允许指定小数秒精度

            // 其他类型
            JSON -> "JSON"               // Oracle 12c+
            XML -> "XMLType"            // Oracle 专用类型
            UUID -> "CHAR(36)"
            ENUM -> "VARCHAR2(255)"      // Oracle 无 ENUM，用字符串替代
            SET -> "VARCHAR2(1000)"     // 集合类型用长字符串
            GEOMETRY -> "SDO_GEOMETRY"   // Oracle 空间类型
            POINT -> "SDO_GEOMETRY"
            LINESTRING -> "SDO_GEOMETRY"
            YEAR -> "NUMBER(4)"          // 单独年份存储
            else -> "VARCHAR2(255)"
        }
    }

    override fun getKColumnType(type: String, length: Int, scale: Int): KColumnType {
        return when (type) {
            "NUMBER" -> when (length) {
                1 -> BIT
                3 -> TINYINT
                5 -> SMALLINT
                7 -> MEDIUMINT
                11 -> INT
                19 -> BIGINT
                else -> INT
            }

            "VARCHAR2" -> VARCHAR

            else -> super.getKColumnType(type, length, scale)
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String {
        return "${
            quote(column.columnName)
        }${
            " ${getColumnType(column.type, column.length, column.scale)}"
        }${
            if (column.primaryKey == PrimaryKeyType.IDENTITY) " GENERATED ALWAYS AS IDENTITY" else ""
        }${
            if (column.nullable) "" else " NOT NULL"
        }${
            if (column.primaryKey != PrimaryKeyType.NOT) " PRIMARY KEY" else ""
        }${
            if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
        }"
    }

    override fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex) =
        "CREATE ${index.type.uppercase()} INDEX ${index.name} ON ${quote(tableName)} (${
            index.columns.joinToString(
                ", "
            ) { quote(it) }
        })"

    override fun getTableCreateSqlList(
        dbType: DBType, tableName: String, tableComment: String?, columns: List<Field>, indexes: List<KTableIndex>
    ): List<String> {
        val columnsSql = columns.joinToString(",") { getColumnCreateSql(dbType, it) }
        val indexesSql = indexes.map { getIndexCreateSql(dbType, tableName, it) }
        return listOfNotNull(
            "CREATE TABLE ${quote(tableName.uppercase())} ($columnsSql)",
            *indexesSql.toTypedArray(),
            *columns.asSequence().filter { !it.kDoc.isNullOrEmpty() }.map {
                "COMMENT ON COLUMN ${quote(tableName)}.${quote(it.columnName)} IS '${it.kDoc}'"
            }.toList().toTypedArray(),
            if (tableComment.isNullOrEmpty()) null else "COMMENT ON TABLE ${quote(tableName)} IS '$tableComment'"
        )
    }

    override fun getTableExistenceSql(dbType: DBType) =
        "select count(1) from all_objects where object_type in ('TABLE','VIEW') and object_name = :tableName and owner = :dbName"

    override fun getTableTruncateSql(dbType: DBType, tableName: String, restartIdentity: Boolean) =
        "TRUNCATE TABLE ${quote(tableName)}"

    override fun getTableDropSql(dbType: DBType, tableName: String) = """
            BEGIN
               EXECUTE IMMEDIATE 'DROP TABLE ${quote(tableName)}';
            EXCEPTION
               WHEN OTHERS THEN
                  IF SQLCODE != -942 THEN
                     RAISE;
                  END IF;
            END;
        """.trimIndent()

    override fun getTableComment(dbType: DBType): String =
        "SELECT comments FROM all_tab_comments WHERE table_name = :tableName AND owner = :dbName"

    override fun getTableColumns(dataSource: KronosDataSourceWrapper, tableName: String): List<Field> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                WITH RankedColumns AS (
                    SELECT 
                        cols.column_name AS COLUMN_NAME,
                        cols.data_type AS DATE_TYPE,
                        cols.data_length AS LENGTH,
                        cols.data_precision AS PRECISION,
                        cols.nullable AS IS_NULLABLE,
                        cols.data_default AS COLUMN_DEFAULT,
                        CASE WHEN cons.constraint_type = 'P' THEN '1' ELSE '0' END AS PRIMARY_KEY,
                        col_comments.comments AS COLUMN_COMMENT,
                        ROW_NUMBER() OVER (PARTITION BY cols.column_name ORDER BY CASE WHEN cons.constraint_type = 'P' THEN 0 ELSE 1 END, cons.constraint_type) AS rn
                    FROM 
                        all_tab_columns cols
                    LEFT JOIN 
                        all_cons_columns cons_cols 
                        ON cols.owner = cons_cols.owner AND cols.table_name = cons_cols.table_name AND cols.column_name = cons_cols.column_name
                    LEFT JOIN 
                        all_constraints cons 
                        ON cols.owner = cons.owner AND cons_cols.constraint_name = cons.constraint_name AND cons_cols.table_name = cons.table_name
                    LEFT JOIN 
                        all_col_comments col_comments
                        ON cols.owner = col_comments.owner AND cols.table_name = col_comments.table_name AND cols.column_name = col_comments.column_name
                    WHERE 
                        cols.table_name = :tableName AND cols.OWNER = :dbName
                )
                SELECT 
                    COLUMN_NAME, DATE_TYPE, LENGTH, PRECISION, IS_NULLABLE, COLUMN_DEFAULT, PRIMARY_KEY, COLUMN_COMMENT
                FROM 
                    RankedColumns
                WHERE 
                    rn = 1
            """.trimWhitespace(),
                mapOf("tableName" to tableName.uppercase(), "dbName" to getDBNameFrom(dataSource).uppercase())
            )
        ).map {
            val dataType = it["DATE_TYPE"].toString()
            val length = it["LENGTH"]?.toString()?.toIntOrNull() ?: 0
            val precision = it["PRECISION"]?.toString()?.toIntOrNull() ?: 0
            Field(
                columnName = it["COLUMN_NAME"].toString(),
                type = getKotlinColumnType(DBType.Oracle, dataType, length, precision),
                length = length,
                scale = precision,
                tableName = tableName.uppercase(),
                nullable = it["IS_NULLABLE"] == "Y",
                primaryKey = when{
                    it["PRIMARY_KEY"] == 0 -> PrimaryKeyType.NOT
                    it["COLUMN_DEFAULT"]?.toString()?.endsWith(".nextval") == true -> PrimaryKeyType.IDENTITY
                    else -> PrimaryKeyType.DEFAULT
                },
                defaultValue = if (it["COLUMN_DEFAULT"]?.toString()?.endsWith(".nextval") == true) {
                    null
                } else {
                    it["COLUMN_DEFAULT"] as String?
                },
                kDoc = it["COLUMN_COMMENT"] as String?
            )
        }
    }

    override fun getTableIndexes(
        dataSource: KronosDataSourceWrapper, tableName: String
    ): List<KTableIndex> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                SELECT DISTINCT i.INDEX_NAME AS NAME
                FROM ALL_INDEXES i
                JOIN ALL_IND_COLUMNS ic ON i.INDEX_NAME = ic.INDEX_NAME
                WHERE i.TABLE_NAME = UPPER(:tableName) 
                AND i.OWNER = :dbName
                AND i.INDEX_NAME NOT LIKE UPPER('SYS_%')
                """.trimWhitespace(),
                mapOf(
                    "tableName" to tableName.uppercase(), "dbName" to getDBNameFrom(dataSource).uppercase()
                )
            )
        ).map {
            KTableIndex(it["NAME"] as String, arrayOf(), "", "")
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
            syncSqlList.add("COMMENT ON TABLE ${quote(tableName)} IS '$tableComment'")
        }
        val dbType = dataSource.dbType
        val dbName = getDBNameFrom(dataSource)
        syncSqlList.addAll(
            indexes.toDelete.map {
                "DROP INDEX ${quote(dbName)}.\"${it.name}\""
            } + columns.toDelete.map {
                "ALTER TABLE ${quote(tableName)} DROP COLUMN \"${it.columnName}\""
            } + columns.toModified.map {
                "ALTER TABLE ${quote(tableName)} MODIFY(${getColumnCreateSql(dbType, it.first)})"
            } + columns.toAdd.map {
                "ALTER TABLE ${quote(tableName)} ADD ${getColumnCreateSql(dbType, it.first)}"
            } + columns.toModified.map {
                if (it.first.kDoc.isNullOrEmpty()) {
                    "COMMENT ON COLUMN ${quote(dbName)}.${quote(tableName)}.${quote(it.first.columnName)} IS NULL"
                } else {
                    "COMMENT ON COLUMN ${quote(dbName)}.${quote(tableName)}.${quote(it.first.columnName)} IS '${it.first.kDoc}'"
                }
            } + indexes.toAdd.map {
                "CREATE ${it.type} INDEX ${it.name} ON ${quote(dbName)}.${quote(tableName)} (${
                    it.columns.joinToString(",") { col ->
                        quote(col.uppercase())
                    }
                })"
            }
        )
        return syncSqlList
    }

    override fun getOnConflictSql(conflictResolver: ConflictResolver): String {
        val (tableName, onFields, toUpdateFields, toInsertFields) = conflictResolver
        return """
            BEGIN
                INSERT INTO ${quote(tableName)}
                    (${toInsertFields.joinToString { quote(it) }})
               VALUES 
                (${toInsertFields.joinToString(", ") { ":$it" }}) 
                EXCEPTION 
                    WHEN 
                        DUP_VAL_ON_INDEX 
                    THEN 
                        UPDATE ${quote(tableName)}
                        SET 
                            ${toUpdateFields.joinToString(", ") { equation(it) }}
                        WHERE 
                            ${onFields.joinToString(" AND ") { equation(it) }};
            END;
        """.trimWhitespace()
    }

    override fun getInsertSql(dataSource: KronosDataSourceWrapper, tableName: String, columns: List<Field>) =
        "INSERT INTO ${quote(tableName.uppercase())} (${
            columns.joinToString {
                quote(it.columnName.uppercase())
            }
        }) VALUES (${columns.joinToString { ":$it" }})"

    override fun getDeleteSql(dataSource: KronosDataSourceWrapper, tableName: String, whereClauseSql: String?) =
        "DELETE FROM ${quote(tableName.uppercase())}${whereClauseSql.orEmpty()}"

    override fun getUpdateSql(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        toUpdateFields: List<Field>,
        whereClauseSql: String?,
        plusAssigns: MutableList<Pair<Field, String>>,
        minusAssigns: MutableList<Pair<Field, String>>
    ) =
        "UPDATE ${quote(tableName.uppercase())} SET ${toUpdateFields.joinToString { equation(it + "New") }}" +
                plusAssigns.joinToString { ", ${quote(it.first)} = ${quote(it.first)} + :${it.second}" } +
                minusAssigns.joinToString { ", ${quote(it.first)} = ${quote(it.first)} - :${it.second}" } +
                whereClauseSql.orEmpty()

    override fun getSelectSql(dataSource: KronosDataSourceWrapper, selectClause: SelectClauseInfo): String {
        val (databaseName, tableName, selectFields, distinct, pagination, pi, ps, limit, lock, whereClauseSql, groupByClauseSql, orderByClauseSql, havingClauseSql) = selectClause

        if (!databaseName.isNullOrEmpty()) throw UnsupportedDatabaseTypeException(
            DBType.Oracle,
            "Oracle does not support databaseName in select clause because of its dblink-liked configuration mode"
        )

        val selectSql = selectFields.joinToString(", ") {
            when {
                it is FunctionField -> getBuiltFunctionField(it, dataSource)
                it.type == CUSTOM_CRITERIA_SQL -> it.toString()
                it.name != it.columnName -> "${quote(it.columnName.uppercase())} AS ${quote(it.name)}"
                else -> quote(it)
            }
        }

        val paginationSql = if (pagination) " OFFSET $pi ROWS FETCH NEXT $ps ROWS ONLY" else null
        val limitSql =
            if (paginationSql == null && limit != null && limit > 0) " FETCH FIRST $limit ROWS ONLY" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        val lockSql = when (lock) {
            PessimisticLock.X -> " FOR UPDATE(NOWAIT)"
            PessimisticLock.S -> " LOCK IN SHARE MODE"
            else -> null
        }
        return "SELECT${distinctSql.orEmpty()} $selectSql FROM ${
            quote(tableName.uppercase())
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

        if (databaseOfTable.isNotEmpty()) throw UnsupportedDatabaseTypeException(
            DBType.Oracle,
            "Oracle does not support databaseName in select clause because of its dblink-liked configuration mode"
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

        val paginationSql = if (pagination) " OFFSET $pi ROWS FETCH NEXT $ps ROWS ONLY" else null
        val limitSql =
            if (paginationSql == null && limit != null && limit > 0) " FETCH FIRST $limit ROWS ONLY" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        return "SELECT${distinctSql.orEmpty()} $selectSql FROM ${
            quote(tableName.uppercase())
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