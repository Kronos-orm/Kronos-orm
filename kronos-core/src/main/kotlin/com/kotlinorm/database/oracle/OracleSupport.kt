package com.kotlinorm.database.oracle

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager.getDBNameFrom
import com.kotlinorm.database.SqlManager.getKotlinColumnType
import com.kotlinorm.database.mssql.MssqlSupport
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.*
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.orm.join.JoinClauseInfo
import com.kotlinorm.orm.select.SelectClauseInfo
import java.math.BigDecimal

object OracleSupport : DatabasesSupport {
    override var quotes = Pair("\"", "\"")

    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            BIT -> "NUMBER(1)"
            TINYINT -> "NUMBER(3)"
            SMALLINT -> "NUMBER(5)"
            MEDIUMINT -> "NUMBER(7)"
            INT -> "NUMBER(${length.takeIf { it > 0 } ?: 11})"
            BIGINT -> "NUMBER(19)"
            REAL -> "REAL"
            FLOAT -> "FLOAT"
            DOUBLE -> "DOUBLE"
            DECIMAL -> "DECIMAL"
            NUMERIC -> "NUMERIC"
            CHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            VARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            TEXT, MEDIUMTEXT, LONGTEXT, CLOB -> "CLOB"
            DATE, TIME, DATETIME -> "DATE"
            TIMESTAMP -> "TIMESTAMP"
            BINARY, VARBINARY, LONGVARBINARY, BLOB, MEDIUMBLOB -> "BLOB"
            LONGBLOB -> "LONGBLOB"
            JSON -> "JSON"
            ENUM -> "ENUM"
            NVARCHAR -> "NVARCHAR(${length.takeIf { it > 0 } ?: 255})"
            NCHAR -> "NCHAR(${length.takeIf { it > 0 } ?: 255})"
            NCLOB -> "NCLOB"
            UUID -> "CHAR(36)"
            SERIAL, YEAR -> "NUMBER"
            SET -> "SET"
            GEOMETRY -> "GEOMETRY"
            POINT -> "POINT"
            LINESTRING -> "LINESTRING"
            XML -> "XML"
            else -> "VARCHAR(255)"
        }
    }

    override fun getKColumnType(type: String, length: Int): KColumnType {
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

            else -> super.getKColumnType(type, length)
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String {
        return "${
            quote(column.columnName)
        }${
            " ${getColumnType(column.type, column.length)}"
        }${
            if (column.identity) " GENERATED ALWAYS AS IDENTITY" else ""
        }${
            if (column.nullable) "" else " NOT NULL"
        }${
            if (column.primaryKey) " PRIMARY KEY" else ""
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
        dbType: DBType, tableName: String, columns: List<Field>, indexes: List<KTableIndex>
    ): List<String> {
        val columnsSql = columns.joinToString(",") { getColumnCreateSql(dbType, it) }
        val indexesSql = indexes.map { getIndexCreateSql(dbType, tableName, it) }
        return listOf(
            "CREATE TABLE ${quote(tableName.uppercase())} ($columnsSql)", *indexesSql.toTypedArray()
        )
    }

    override fun getTableExistenceSql(dbType: DBType) =
        "select count(1) from all_objects where object_type in ('TABLE','VIEW') and object_name = :tableName and owner = :dbName"

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
                        ROW_NUMBER() OVER (PARTITION BY cols.column_name ORDER BY CASE WHEN cons.constraint_type = 'P' THEN 0 ELSE 1 END, cons.constraint_type) AS rn
                    FROM 
                        all_tab_columns cols
                    LEFT JOIN 
                        all_cons_columns cons_cols 
                        ON cols.owner = cons_cols.owner AND cols.table_name = cons_cols.table_name AND cols.column_name = cons_cols.column_name
                    LEFT JOIN 
                        all_constraints cons 
                        ON cols.owner = cons.owner AND cons_cols.constraint_name = cons.constraint_name AND cons_cols.table_name = cons.table_name
                    WHERE 
                        cols.table_name = :tableName AND cols.OWNER = :dbName
                )
                SELECT 
                    COLUMN_NAME, DATE_TYPE, LENGTH, PRECISION, IS_NULLABLE, COLUMN_DEFAULT, PRIMARY_KEY
                FROM 
                    RankedColumns
                WHERE 
                    rn = 1
            """.trimIndent(),
                mapOf("tableName" to tableName.uppercase(), "dbName" to getDBNameFrom(dataSource).uppercase())
            )
        ).map {
            val dataType = it["DATE_TYPE"].toString()
            val length = (if (dataType == "NUMBER") {
                it["PRECISION"]
            } else {
                it["LENGTH"]
            } as BigDecimal?)?.toInt() ?: 0
            Field(
                columnName = it["COLUMN_NAME"].toString(),
                type = getKotlinColumnType(DBType.Oracle, it["DATE_TYPE"].toString(), length),
                length = length,
                tableName = tableName.uppercase(),
                nullable = it["IS_NULLABLE"] == "Y",
                primaryKey = it["PRIMARY_KEY"] == "1",
                identity = it["COLUMN_DEFAULT"]?.toString()?.endsWith(".nextval") == true,
                defaultValue = if (it["COLUMN_DEFAULT"]?.toString()?.endsWith(".nextval") == true) {
                    null
                } else {
                    it["COLUMN_DEFAULT"] as String?
                }
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
                """, mapOf(
                    "tableName" to tableName.uppercase(), "dbName" to getDBNameFrom(dataSource).uppercase()
                )
            )
        ).map {
            KTableIndex(it["NAME"] as String, arrayOf(), "", "")
        }
    }

    override fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper, tableName: String, columns: TableColumnDiff, indexes: TableIndexDiff
    ): List<String> {
        val dbType = dataSource.dbType
        val dbName = getDBNameFrom(dataSource)
        return indexes.toDelete.map {
            "DROP INDEX ${quote(dbName)}.\"${it.name}\""
        } + columns.toDelete.map {
            "ALTER TABLE ${quote(tableName)} DROP COLUMN \"${it.columnName}\""
        } + columns.toModified.map {
            "ALTER TABLE ${quote(tableName)} MODIFY(${getColumnCreateSql(dbType, it)})"
        } + columns.toAdd.map {
            "ALTER TABLE ${quote(tableName)} ADD ${getColumnCreateSql(dbType, it)}"
        } + indexes.toAdd.map {
            "CREATE ${it.type} INDEX ${it.name} ON ${quote(dbName)}.${quote(tableName)} (${
                it.columns.joinToString(",") { col ->
                    quote(col.uppercase())
                }
            })"
        }
    }

    override fun getOnConflictSql(conflictResolver: ConflictResolver): String {
        val (tableName, onFields, toUpdateFields, toInsertFields) = conflictResolver
        return """
            |BEGIN
            |    INSERT INTO ${quote(tableName)}
            |        (${toInsertFields.joinToString { quote(it) }})
            |    VALUES 
            |    (${toInsertFields.joinToString(", ") { ":$it" }}) 
            |    EXCEPTION 
            |        WHEN 
            |            DUP_VAL_ON_INDEX 
            |        THEN 
            |            UPDATE ${quote(tableName)}
            |            SET 
            |                ${toUpdateFields.joinToString(", ") { equation(it) }}
            |            WHERE 
            |                ${onFields.joinToString(" AND ") { equation(it) }};
            |END;
        """.trimMargin()
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
        versionField: String?,
        whereClauseSql: String?
    ) =
        "UPDATE ${quote(tableName.uppercase())} SET ${toUpdateFields.joinToString { equation(it + "New") }}" +
                if (!versionField.isNullOrEmpty()) ", ${quote(versionField)} = ${quote(versionField)} + 1" else {
                    ""
                } +
                whereClauseSql.orEmpty()

    override fun getSelectSql(dataSource: KronosDataSourceWrapper, selectClause: SelectClauseInfo): String {
        val (databaseName, tableName, selectFields, distinct, pagination, pi, ps, limit, lock, whereClauseSql, groupByClauseSql, orderByClauseSql, havingClauseSql) = selectClause

        if (!databaseName.isNullOrEmpty()) throw UnsupportedDatabaseTypeException("Oracle does not support databaseName in select clause because of its dblink-liked configuration mode")

        val selectFieldsSql = selectFields.joinToString(", ") {
            when {
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
        return "SELECT${distinctSql.orEmpty()} $selectFieldsSql FROM ${
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
        val (tableName, selectFields, distinct, pagination, pi, ps, limit, databaseOfTable,  whereClauseSql, groupByClauseSql, orderByClauseSql, havingClauseSql, joinSql) = joinClause

        if (databaseOfTable.isNotEmpty()) throw UnsupportedDatabaseTypeException("Oracle does not support databaseName in select clause because of its dblink-liked configuration mode")

        val selectFieldsSql = selectFields.joinToString(", ") {
            when {
                it.second.type == CUSTOM_CRITERIA_SQL -> it.second.toString()
                it.second.name != it.second.columnName -> "${quote(it.second, true)} AS ${quote(it.second, true)}"
                else -> "${quote(it.second, true)} AS ${MssqlSupport.quote(it.first)}"
            }
        }
        val paginationSql = if (pagination) " OFFSET $pi ROWS FETCH NEXT $ps ROWS ONLY" else null
        val limitSql =
            if (paginationSql == null && limit != null && limit > 0) " FETCH FIRST $limit ROWS ONLY" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        return "SELECT${distinctSql.orEmpty()} $selectFieldsSql FROM ${
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