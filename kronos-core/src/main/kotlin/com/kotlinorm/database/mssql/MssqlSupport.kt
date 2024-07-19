package com.kotlinorm.database.mssql

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getKotlinColumnType
import com.kotlinorm.database.SqlManager.sqlColumnType
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.CUSTOM_CRITERIA_SQL
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.orm.select.SelectClauseInfo

object MssqlSupport : DatabasesSupport {
    override var quotes = Pair("[", "]")
    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            KColumnType.BIT -> "BIT"
            KColumnType.TINYINT -> "TINYINT"
            KColumnType.SMALLINT -> "SMALLINT"
            KColumnType.INT -> "INT"
            KColumnType.MEDIUMINT -> "INT"
            KColumnType.BIGINT -> "BIGINT"
            KColumnType.REAL -> "REAL"
            KColumnType.FLOAT -> "FLOAT"
            KColumnType.DOUBLE -> "DOUBLE"
            KColumnType.DECIMAL -> "DECIMAL"
            KColumnType.NUMERIC -> "NUMERIC"
            KColumnType.CHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.VARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.TEXT -> "TEXT"
            KColumnType.MEDIUMTEXT -> "TEXT"
            KColumnType.LONGTEXT -> "TEXT"
            KColumnType.DATE -> "DATE"
            KColumnType.TIME -> "TIME"
            KColumnType.DATETIME -> "DATETIME"
            KColumnType.TIMESTAMP -> "TIMESTAMP"
            KColumnType.BINARY -> "BINARY"
            KColumnType.VARBINARY -> "VARBINARY"
            KColumnType.LONGVARBINARY -> "IMAGE"
            KColumnType.BLOB -> "IMAGE"
            KColumnType.MEDIUMBLOB -> "IMAGE"
            KColumnType.LONGBLOB -> "IMAGE"
            KColumnType.CLOB -> "TEXT"
            KColumnType.JSON -> "JSON"
            KColumnType.ENUM -> "ENUM"
            KColumnType.NVARCHAR -> "NVARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCHAR -> "NCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCLOB -> "NTTEXT"
            KColumnType.UUID -> "CHAR(36)"
            KColumnType.SERIAL -> "INT"
            KColumnType.YEAR -> "INT"
            KColumnType.SET -> "SET"
            KColumnType.GEOMETRY -> "GEOMETRY"
            KColumnType.POINT -> "POINT"
            KColumnType.LINESTRING -> "LINESTRING"
            KColumnType.XML -> "XML"
            else -> "NVARCHAR(255)"
        }
    }

    override

    fun getColumnCreateSql(dbType: DBType, column: Field): String = "${
        column.columnName
    }${
        " ${sqlColumnType(dbType, column.type, column.length)}"
    }${
        if (column.nullable) "" else " NOT NULL"
    }${
        if (column.primaryKey) " PRIMARY KEY" else ""
    }${
        if (column.identity) " IDENTITY" else ""
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

    //"IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[$kronosTableName]') AND type in (N'U')) BEGIN CREATE TABLE [dbo].[$kronosTableName]($columnDefinitions)"


    override fun getTableCreateSqlList(
        dbType: DBType, tableName: String, columns: List<Field>, indexes: List<KTableIndex>
    ): List<String> {
        val columnsSql = columns.joinToString(",") { columnCreateDefSql(dbType, it) }
        val indexesSql = indexes.map { getIndexCreateSql(dbType, tableName, it) }
        return listOf(
            "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[$tableName]') AND type in (N'U')) BEGIN CREATE TABLE [dbo].[$tableName]($columnsSql); END;",
            *indexesSql.toTypedArray()
        )
    }

    override fun getTableExistenceSql(dbType: DBType) = "select count(1) from sys.objects where name = :tableName"

    override fun getTableDropSql(dbType: DBType, tableName: String) =
        "IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$tableName') AND type in (N'U')) BEGIN DROP TABLE $tableName END"

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
                    END AS AUTOINCREAMENT
                FROM 
                    INFORMATION_SCHEMA.COLUMNS c
                WHERE 
                    c.TABLE_CATALOG = DB_NAME() AND 
                    c.TABLE_NAME = :tableName
            """.trimIndent(), mapOf("tableName" to tableName)
            )
        ).map {
            val length = it["CHARACTER_MAXIMUM_LENGTH"] as Int? ?: 0
            Field(
                columnName = it["COLUMN_NAME"].toString(),
                type = getKotlinColumnType(DBType.Mssql, it["DATA_TYPE"].toString(), length),
                length = length,
                tableName = tableName,
                nullable = it["IS_NULLABLE"] == "YES",
                primaryKey = it["PRIMARY_KEY"] == "YES",
                identity = it["AUTOINCREAMENT"] == "YES",
                defaultValue = removeOuterParentheses(it["COLUMN_DEFAULT"] as String?)
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
                """, mapOf(
                    "tableName" to tableName
                )
            )
        ).map {
            KTableIndex(it["name"] as String, arrayOf(), "", "")
        }
    }

    override fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper, tableName: String, columns: TableColumnDiff, indexes: TableIndexDiff
    ): List<String> {
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
            """.trimIndent()
        } + columns.toDelete.map {
            "ALTER TABLE [dbo].[$tableName] DROP COLUMN [${it.columnName}]"
        } + columns.toAdd.map {
            "ALTER TABLE $tableName ADD [${it.columnName}] ${it.type} ${if (it.length > 0 && it.type != KColumnType.TINYINT) "(${it.length})" else ""} ${if (it.primaryKey) "PRIMARY KEY" else ""} ${if (it.defaultValue != null) "DEFAULT '${it.defaultValue}'" else ""} ${if (it.nullable) "" else "NOT NULL"};"
        } + columns.toModified.map {
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
                ELSE
                BEGIN
                    PRINT 'No default constraint found on the specified column.';
                END
            """.trimIndent()
        } + columns.toModified.map {
            "ALTER TABLE [dbo].[$tableName] ALTER COLUMN ${columnCreateDefSql(dbType, it)}"
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
    """
    }

    override fun getInsertSql(dataSource: KronosDataSourceWrapper, tableName: String, columns: List<Field>) =
        "INSERT INTO [dbo].${quote(tableName)} (${columns.joinToString { quote(it) }}) VALUES (${columns.joinToString { ":$it" }})"

    override fun getDeleteSql(dataSource: KronosDataSourceWrapper, tableName: String, whereClauseSql: String?) =
        "DELETE FROM [dbo].${quote(tableName)}${whereClauseSql.orEmpty()}"

    override fun getUpdateSql(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        toUpdateFields: List<Field>,
        whereClauseSql: String?
    ) =
        "UPDATE [dbo].${quote(tableName)} SET ${toUpdateFields.joinToString { equation(it + "New") }}${whereClauseSql.orEmpty()}"

    override fun getSelectSql(dataSource: KronosDataSourceWrapper, selectClause: SelectClauseInfo): String {
        val (tableName, selectFields, distinct, pagination, pi, ps, limit, whereClauseSql, groupByClauseSql, orderByClauseSql, havingClauseSql) = selectClause
        val selectFieldsSql = selectFields.joinToString(", ") {
            when {
                it.type == CUSTOM_CRITERIA_SQL -> it.toString()
                it.name != it.columnName -> "${quote(it.columnName)} AS ${quote(it)}"
                else -> quote(it)
            }
        }
        val paginationSql = if (pagination) " OFFSET ${ps * (pi - 1)} ROWS FETCH NEXT $ps ROWS ONLY" else null
        val limitSql = if (paginationSql == null && limit != null) " FETCH NEXT $limit ROWS ONLY" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        return "SELECT${distinctSql.orEmpty()} $selectFieldsSql FROM [dbo].${
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
}