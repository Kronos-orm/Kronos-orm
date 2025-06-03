/**
 * Copyright 2022-2025 kronos-orm
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

package com.kotlinorm.database.mssql

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getKotlinColumnType
import com.kotlinorm.database.SqlManager.sqlColumnType
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.CUSTOM_CRITERIA_SQL
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.functions.FunctionManager.getBuiltFunctionField
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.ddl.TableColumnDiff
import com.kotlinorm.orm.ddl.TableIndexDiff
import com.kotlinorm.orm.join.JoinClauseInfo
import com.kotlinorm.orm.select.SelectClauseInfo
import com.kotlinorm.utils.trimWhitespace

object MssqlSupport : DatabasesSupport {
    override var quotes = Pair("[", "]")

    override fun getDBNameFromUrl(wrapper: KronosDataSourceWrapper) = wrapper.url.split("//").last().split(";").first()

    override fun getColumnType(type: KColumnType, length: Int, scale: Int): String {
        return when (type) {
            KColumnType.BIT -> "BIT"
            KColumnType.TINYINT -> "TINYINT"       // 固定长度（1字节）
            KColumnType.SMALLINT -> "SMALLINT"     // 固定长度（2字节）
            KColumnType.INT, KColumnType.MEDIUMINT, KColumnType.SERIAL, KColumnType.YEAR -> "INT"
            KColumnType.BIGINT -> "BIGINT"

            // 浮点类型处理
            KColumnType.REAL -> "REAL"            // 等价于 FLOAT(24)
            KColumnType.FLOAT -> if (length > 0) "FLOAT($length)" else "FLOAT"  // 默认 FLOAT(53)
            KColumnType.DOUBLE -> "FLOAT(53)"      // 明确双精度

            // 精确数值类型（必须指定精度）
            KColumnType.DECIMAL -> when {
                length > 0 && scale > 0 -> "DECIMAL($length,$scale)"
                length > 0 -> "DECIMAL($length,0)"
                else -> "DECIMAL(18,0)"            // SQL Server 默认精度
            }
            KColumnType.NUMERIC -> when {
                length > 0 && scale > 0 -> "NUMERIC($length,$scale)"
                length > 0 -> "NUMERIC($length,0)"
                else -> "NUMERIC(18,0)"
            }

            // 字符类型（已处理默认长度）
            KColumnType.CHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.VARCHAR -> "VARCHAR(${
                when {
                    length <= 0 -> 255
                    length > 8000 -> "MAX"
                    else -> length
                }
            })"
            KColumnType.NCHAR -> "NVARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NVARCHAR -> "NVARCHAR(${
                when {
                    length <= 0 -> 255
                    length > 4000 -> "MAX"
                    else -> length
                }
            })"

            // 二进制类型处理
            KColumnType.BINARY -> "BINARY(${length.takeIf { it > 0 } ?: 255})"// 默认长度255
            KColumnType.VARBINARY -> "VARBINARY(${length.takeIf { it > 0 } ?: 255})"// 默认长度255
            KColumnType.LONGVARBINARY, KColumnType.BLOB, KColumnType.MEDIUMBLOB, KColumnType.LONGBLOB -> "VARBINARY(MAX)"

            // 其他保持原样...
            KColumnType.TEXT, KColumnType.MEDIUMTEXT, KColumnType.LONGTEXT, KColumnType.CLOB -> "TEXT"
            KColumnType.DATE -> "DATE"
            KColumnType.TIME -> if(scale > 0) "TIME($scale)" else "TIME"
            KColumnType.DATETIME -> if (scale > 0) "DATETIME2($scale)" else "DATETIME"
            KColumnType.TIMESTAMP -> "TIMESTAMP"
            KColumnType.JSON -> "JSON"
            KColumnType.ENUM -> "NVARCHAR(255)"
            KColumnType.NCLOB -> "NTEXT"
            KColumnType.UUID -> "CHAR(36)"
            KColumnType.SET -> "NVARCHAR(255)"
            KColumnType.GEOMETRY -> "GEOMETRY"
            KColumnType.POINT -> "GEOMETRY"
            KColumnType.LINESTRING -> "GEOMETRY"
            KColumnType.XML -> "XML"
            else -> "NVARCHAR(255)"
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String = "${
        quote(column.columnName)
    }${
        " ${sqlColumnType(dbType, column.type, column.length, column.scale)}"
    }${
        if (column.nullable) "" else " NOT NULL"
    }${
        if (column.primaryKey != PrimaryKeyType.NOT) " PRIMARY KEY" else ""
    }${
        if (column.primaryKey == PrimaryKeyType.IDENTITY) " IDENTITY" else ""
    }${
        if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
    }"

    override fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex): String {
        return "CREATE ${index.method}${
            if (index.type == "XML") {
                " PRIMARY"
            } else ""
        } ${index.type} INDEX [${index.name}] ON [dbo].[$tableName] ([${
            index.columns.joinToString(
                "],["
            )
        }])"
    }

    override fun getTableCreateSqlList(
        dbType: DBType, tableName: String, tableComment: String?, columns: List<Field>, indexes: List<KTableIndex>
    ): List<String> {
        val columnsSql = columns.joinToString(",") { columnCreateDefSql(dbType, it) }
        val indexesSql = indexes.map { getIndexCreateSql(dbType, tableName, it) }
        return listOfNotNull(
            "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[$tableName]') AND type in (N'U')) BEGIN CREATE TABLE [dbo].[$tableName]($columnsSql); END;",
            *indexesSql.toTypedArray(),
            *columns.asSequence().filter { !it.kDoc.isNullOrEmpty() }.map {
                "EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'${it.kDoc}', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'$tableName', @level2type=N'COLUMN', @level2name=N'${it.columnName}'"
            }.toList().toTypedArray(),
            if (tableComment != null) "EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'$tableComment', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'$tableName'" else null
        )
    }

    override fun getTableExistenceSql(dbType: DBType) = "select count(1) from sys.objects where name = :tableName"

    override fun getTableTruncateSql(dbType: DBType, tableName: String, restartIdentity: Boolean) =
        "TRUNCATE TABLE ${quote(tableName)}"

    override fun getTableDropSql(dbType: DBType, tableName: String) =
        "IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$tableName') AND type in (N'U')) BEGIN DROP TABLE $tableName END"

    override fun getTableCommentSql(dbType: DBType): String =
        "SELECT ep.value AS TABLE_COMMENT FROM sys.extended_properties ep WHERE ep.major_id = OBJECT_ID(:tableName) AND ep.minor_id = 0 AND ep.name = 'MS_Description'"

    override fun getTableColumns(dataSource: KronosDataSourceWrapper, tableName: String): List<Field> {
        fun removeOuterParentheses(input: String?): String? {
            input ?: return null

            var result: String = input
            while (result.first() == '(' && result.last() == ')') {
                result = result.substring(1, result.length - 1)
            }
            return result
        }

        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                SELECT 
                    c.COLUMN_NAME, 
                    c.DATA_TYPE, 
                    CASE 
                        WHEN c.DATA_TYPE IN ('char', 'nchar', 'varchar', 'nvarchar') THEN c.CHARACTER_MAXIMUM_LENGTH
                        ELSE NULL  
                    END AS CHARACTER_MAXIMUM_LENGTH,
                    CASE 
                        WHEN c.DATA_TYPE IN ('decimal', 'numeric') THEN c.NUMERIC_PRECISION
                        ELSE NULL  
                    END AS NUMERIC_PRECISION,
                    c.IS_NULLABLE,
                    c.COLUMN_DEFAULT,
                    CASE 
                        WHEN EXISTS (
                            SELECT 1 
                            FROM INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu
                            INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc 
                                ON ccu.Constraint_Name = tc.Constraint_Name 
                                AND tc.Constraint_Type = 'PRIMARY KEY'
                            WHERE ccu.COLUMN_NAME = c.COLUMN_NAME AND ccu.TABLE_NAME = c.TABLE_NAME
                        ) THEN 'YES' ELSE 'NO' 
                    END AS PRIMARY_KEY,
                    CASE 
                        WHEN EXISTS(
                            SELECT 1
                            FROM sysobjects a inner join syscolumns b on a.id = b.id
                            WHERE columnproperty(a.id, b.name, 'isIdentity') = 1
                                and objectproperty(a.id, 'isTable') = 1
                                and a.name = 'tb_user'
                                and b.name = c.COLUMN_NAME
                        ) THEN 'YES' ELSE 'NO' 
                    END AS AUTOINCREAMENT,
                    ep.value AS COLUMN_COMMENT
                FROM 
                    INFORMATION_SCHEMA.COLUMNS c
                LEFT JOIN
                    sys.extended_properties ep ON ep.major_id = OBJECT_ID(:tableName) 
                    AND ep.minor_id = c.ORDINAL_POSITION 
                    AND ep.name = 'MS_Description'
                WHERE 
                    c.TABLE_CATALOG = DB_NAME() AND 
                    c.TABLE_NAME = :tableName
            """.trimWhitespace(), mapOf("tableName" to tableName)
            )
        ).map {
            val length = it["CHARACTER_MAXIMUM_LENGTH"] as Int? ?: 0
            val scale = it["NUMERIC_PRECISION"] as Int? ?: 0
            Field(
                columnName = it["COLUMN_NAME"].toString(),
                type = getKotlinColumnType(DBType.Mssql, it["DATA_TYPE"].toString(), length, scale),
                length = length,
                scale = scale,
                tableName = tableName,
                nullable = it["IS_NULLABLE"] == "YES",
                primaryKey = when {
                    it["PRIMARY_KEY"] == "NO" -> PrimaryKeyType.NOT
                    it["AUTOINCREAMENT"] == "YES" -> PrimaryKeyType.IDENTITY
                    else -> PrimaryKeyType.DEFAULT
                },
                defaultValue = removeOuterParentheses(it["COLUMN_DEFAULT"] as String?),
                kDoc = it["COLUMN_COMMENT"] as String?
            )
        }
    }

    override fun getTableIndexes(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ): List<KTableIndex> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                    SELECT 
                        name AS name
                    FROM 
                     sys.indexes
                    WHERE 
                     object_id = object_id(:tableName) AND 
                     name NOT LIKE 'PK__${tableName}__%'  
                """.trimWhitespace(), mapOf(
                    "tableName" to tableName
                )
            )
        ).map {
            KTableIndex(it["name"] as String, arrayOf(), "", "")
        }
    }

    override fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper, tableName: String, originalTableComment: String?, tableComment: String?, columns: TableColumnDiff, indexes: TableIndexDiff
    ): List<String> {
        val syncSqlList = mutableListOf<String>()

        if (originalTableComment.orEmpty() != tableComment.orEmpty()) {
            syncSqlList.add(
                if (originalTableComment == null) {
                    "EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'$tableComment', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'$tableName'"
                } else {
                    "EXEC sys.sp_updateextendedproperty @name=N'MS_Description', @value=N'$tableComment', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'$tableName'"
                }
            )
        }

        val dbType = dataSource.dbType
        return indexes.toDelete.map {
            "DROP INDEX [${it.name}] ON [dbo].[$tableName]"
        } + columns.toDelete.map {
            // 删除默认值约束
            """
                DECLARE @ConstraintName NVARCHAR(128);
                SET @ConstraintName = (
                    SELECT name
                    FROM sys.default_constraints
                    WHERE parent_object_id = OBJECT_ID(N'dbo.$tableName') 
                    AND COL_NAME(parent_object_id, parent_column_id) = N'${it.name}' 
                );

                IF @ConstraintName IS NOT NULL
                    BEGIN
                        DECLARE @DropStmt NVARCHAR(MAX) = N'ALTER TABLE dbo.$tableName DROP CONSTRAINT ' + QUOTENAME(@ConstraintName);
                        EXEC sp_executesql @DropStmt;
                    END
            """.trimWhitespace()
        } + columns.toDelete.map {
            "ALTER TABLE [dbo].[$tableName] DROP COLUMN [${it.columnName}]"
        } + columns.toAdd.map {
            "ALTER TABLE $tableName ADD [${it.first.columnName}] ${it.first.type} ${if (it.first.length > 0 && it.first.type != KColumnType.TINYINT) "(${it.first.length})" else ""} ${if (it.first.primaryKey != PrimaryKeyType.NOT) "PRIMARY KEY" else ""} ${if (it.first.defaultValue != null) "DEFAULT '${it.first.defaultValue}'" else ""} ${if (it.first.nullable) "" else "NOT NULL"};"
        } + columns.toModified.map {
            // 删除默认值约束
            """
                DECLARE @ConstraintName NVARCHAR(128);
                SET @ConstraintName = (
                    SELECT name
                    FROM sys.default_constraints
                    WHERE parent_object_id = OBJECT_ID(N'dbo.$tableName') 
                    AND COL_NAME(parent_object_id, parent_column_id) = N'${it.first.name}' 
                );

                IF @ConstraintName IS NOT NULL
                    BEGIN
                        DECLARE @DropStmt NVARCHAR(MAX) = N'ALTER TABLE dbo.$tableName DROP CONSTRAINT ' + QUOTENAME(@ConstraintName);
                        EXEC sp_executesql @DropStmt;
                    END
                ELSE
                    BEGIN
                        PRINT 'No default constraint found on the specified column.';
                    END
            """.trimWhitespace()
        } + columns.toModified.map {
            "ALTER TABLE [dbo].[$tableName] ALTER COLUMN ${columnCreateDefSql(dbType, it.first)}"
        } + columns.toModified.map {
            if(it.first.kDoc.isNullOrEmpty()) {
                "exec sys.sp_dropextendedproperty @name=N'MS_Description', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'$tableName', @level2type=N'COLUMN', @level2name=N'${it.first.columnName}'"
            } else {
                """
                IF ((SELECT COUNT(*) FROM ::fn_listextendedproperty('MS_Description',
                'SCHEMA', N'dbo',
                'TABLE', N'$tableName',
                'COLUMN', N'${it.first.columnName}')) > 0)
                    BEGIN
                        EXEC sp_updateextendedproperty 'MS_Description', N'${it.first.kDoc}', 'SCHEMA', N'dbo', 'TABLE', N'$tableName', 'COLUMN', N'${it.first.columnName}';
                    END
                ELSE
                    BEGIN
                        EXEC sp_addextendedproperty 'MS_Description', N'${it.first.kDoc}', 'SCHEMA', N'dbo', 'TABLE', N'$tableName', 'COLUMN', N'${it.first.columnName}';
                    END
                """.trimWhitespace()
            }
        } + indexes.toAdd.map {
            getIndexCreateSql(dbType, tableName, it)
        }
    }

    override fun getOnConflictSql(conflictResolver: ConflictResolver): String {
        val (tableName, onFields, toUpdateFields, toInsertFields) = conflictResolver
        return """
            IF EXISTS (SELECT 1 FROM ${quote(tableName)} WHERE ${onFields.joinToString(" AND ") { equation(it) }})
                BEGIN 
                    UPDATE ${quote(tableName)} SET ${toUpdateFields.joinToString { equation(it) }}
                END
            ELSE 
                BEGIN
                    INSERT INTO ${quote(tableName)} (${toInsertFields.joinToString { quote(it) }})
                    VALUES (${toInsertFields.joinToString(", ") { ":$it" }})
                END
    """.trimWhitespace()
    }

    override fun getInsertSql(dataSource: KronosDataSourceWrapper, tableName: String, columns: List<Field>) =
        "INSERT INTO [dbo].${quote(tableName)} (${columns.joinToString { quote(it) }}) VALUES (${columns.joinToString { ":$it" }})"

    override fun getDeleteSql(dataSource: KronosDataSourceWrapper, tableName: String, whereClauseSql: String?) =
        "DELETE FROM [dbo].${quote(tableName)}${whereClauseSql.orEmpty()}"

    override fun getUpdateSql(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        toUpdateFields: List<Field>,
        whereClauseSql: String?,
        plusAssigns: MutableList<Pair<Field, String>>,
        minusAssigns: MutableList<Pair<Field, String>>
    ) =
        "UPDATE [dbo].${quote(tableName)} SET ${toUpdateFields.joinToString { equation(it + "New") }}" +
                plusAssigns.joinToString { ", ${quote(it.first)} = ${quote(it.first)} + :${it.second}" } +
                minusAssigns.joinToString { ", ${quote(it.first)} = ${quote(it.first)} - :${it.second}" } +
                whereClauseSql.orEmpty()

    override fun getSelectSql(dataSource: KronosDataSourceWrapper, selectClause: SelectClauseInfo): String {
        val (databaseName, tableName, selectFields, distinct, pagination, pi, ps, limit, lock, whereClauseSql, groupByClauseSql, orderByClauseSql, havingClauseSql) = selectClause
        val selectSql = selectFields.joinToString(", ") {
            when {
                it is FunctionField -> getBuiltFunctionField(it, dataSource)
                it.type == CUSTOM_CRITERIA_SQL -> it.toString()
                it.name != it.columnName -> "${quote(it.columnName)} AS ${quote(it.name)}"
                else -> quote(it)
            }
        }

        val paginationSql = if (pagination) " OFFSET ${ps * (pi - 1)} ROWS FETCH NEXT $ps ROWS ONLY" else null
        val limitSql = if (paginationSql == null && limit != null && limit > 0) " FETCH NEXT $limit ROWS ONLY" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        val lockSql = if (null != lock) " ROWLOCK" else null
        return "SELECT${distinctSql.orEmpty()} $selectSql FROM ${
            databaseName?.let { quote(it) + "." } ?: ""
        }[dbo].${
            quote(tableName)
        }${
            lockSql.orEmpty()
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
        val selectSql = selectFields.joinToString(", ") {
            val field = it.second
            when {
                field is FunctionField -> getBuiltFunctionField(field, dataSource, true)
                field.type == CUSTOM_CRITERIA_SQL -> field.toString()
                field.name != field.columnName -> "${quote(field, true)} AS ${quote(field.name)}"
                else -> "${SqlManager.quote(dataSource, field, true, databaseOfTable)} AS ${quote(it.first)}"
            }
        }

        val paginationSql = if (pagination) " OFFSET ${ps * (pi - 1)} ROWS FETCH NEXT $ps ROWS ONLY" else null
        val limitSql = if (paginationSql == null && limit != null && limit > 0) " FETCH NEXT $limit ROWS ONLY" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        return "SELECT${distinctSql.orEmpty()} $selectSql FROM [dbo].${
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