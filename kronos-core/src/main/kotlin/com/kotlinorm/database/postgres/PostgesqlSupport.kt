package com.kotlinorm.database.postgres

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getDBNameFrom
import com.kotlinorm.database.SqlManager.getKotlinColumnType
import com.kotlinorm.database.mssql.MssqlSupport
import com.kotlinorm.database.oracle.OracleSupport.orEmpty
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.*
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.orm.join.JoinClauseInfo
import com.kotlinorm.orm.select.SelectClauseInfo

object PostgesqlSupport : DatabasesSupport {
    override var quotes = Pair("\"", "\"")

    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            BIT -> "BOOLEAN"
            TINYINT, SMALLINT, YEAR -> "SMALLINT"
            INT, MEDIUMINT -> "INTEGER"
            BIGINT -> "BIGINT"
            REAL -> "REAL"
            FLOAT -> "FLOAT"
            DOUBLE -> "DOUBLE"
            DECIMAL -> "DECIMAL"
            NUMERIC -> "NUMERIC"
            CHAR, NCHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            VARCHAR, NVARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            TEXT, CLOB, MEDIUMTEXT, LONGTEXT, ENUM, NCLOB, SET -> "TEXT"
            DATE -> "DATE"
            TIME -> "TIME"
            DATETIME, TIMESTAMP -> "TIMESTAMP"
            BINARY, VARBINARY, LONGVARBINARY, BLOB, MEDIUMBLOB, LONGBLOB -> "BYTEA"
            JSON -> "JSON"
            UUID -> "UUID"
            SERIAL -> "SERIAL"
            GEOMETRY -> "GEOMETRY"
            POINT -> "POINT"
            LINESTRING -> "LINESTRING"
            XML -> "XML"
            else -> "VARCHAR(255)"
        }
    }

    override fun getKColumnType(type: String, length: Int): KColumnType {
        return when (type) {
            "INTEGER" -> INT
            "BYTEA" -> BLOB
            "BOOLEAN" -> BIT
            else -> super.getKColumnType(type, length)
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String {
        return "${
            quote(column.columnName)
        }${
            if (column.identity) " SERIAL" else " ${getColumnType(column.type, column.length)}"
        }${
            if (column.nullable) "" else " NOT NULL"
        }${
            if (column.primaryKey) " PRIMARY KEY" else ""
        }${
            if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
        }"
    }

    override fun getTableExistenceSql(dbType: DBType): String =
        "select count(1) from pg_class where relname = :tableName"

    override fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex) =
        "CREATE${if (index.type.isNotEmpty()) " ${index.type}" else ""} INDEX${(if (index.concurrently) " CONCURRENTLY" else "")} ${index.name} ON ${
            quote(tableName)
        }${if (index.method.isNotEmpty()) " USING ${index.method}" else ""} (${
            index.columns.joinToString(
                ", "
            ) { quote(it) }
        })"


    override fun getTableColumns(dataSource: KronosDataSourceWrapper, tableName: String): List<Field> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                SELECT 
                    c.column_name AS COLUMN_NAME,
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
                    COALESCE(c.character_maximum_length, c.numeric_precision) AS LENGTH,
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
            """.trimIndent(), mapOf("tableName" to tableName, "dbName" to getDBNameFrom(dataSource))
            )
        ).map {
            Field(
                columnName = it["column_name"].toString(),
                type = getKotlinColumnType(DBType.Postgres, it["data_type"].toString(), it["length"] as Int? ?: 0),
                length = it["length"] as Int? ?: 0,
                tableName = tableName,
                nullable = it["is_nullable"] == true,
                primaryKey = it["primary_key"] == true,
                identity = it["column_default"]?.toString()?.startsWith("nextval(") == true,
                // 如果defaultValue =  "('${tableName}_id_seq'::regclass)" 设置成 null
                defaultValue = if (it["column_default"]?.toString()?.startsWith("nextval(") == true) {
                    null
                } else {
                    it["column_default"] as String?
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
                    SELECT 
                        indexname AS name
                    FROM 
                        pg_indexes 
                    WHERE 
                        tablename = :tableName AND 
                        schemaname = 'public' AND 
                        indexname NOT LIKE CONCAT(tablename, '_pkey');
                     """, mapOf(
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
        columns: TableColumnDiff,
        indexes: TableIndexDiff
    ): List<String> {
        return indexes.toDelete.map {
            "DROP INDEX ${quote("public")}.${it.name};"
        } + columns.toAdd.map {
            "ALTER TABLE ${quote("public")}.${quote(tableName)} ADD COLUMN ${it.columnName} ${
                columnCreateDefSql(
                    DBType.Postgres, it
                )
            }"
        } + columns.toModified.map {
            "ALTER TABLE ${quote("public")}.${quote(tableName)} ALTER COLUMN ${it.columnName} TYPE ${
                getColumnType(it.type, it.length)
            } ${if (it.defaultValue != null) ",AlTER COLUMN ${it.columnName} SET DEFAULT ${it.defaultValue}" else ""} ${
                if (it.nullable) ",ALTER COLUMN ${it.columnName} DROP NOT NULL" else ",ALTER COLUMN ${it.columnName} SET NOT NULL"
            }"
        } + columns.toDelete.map {
            "ALTER TABLE ${quote("public")}.${quote(tableName)} DROP COLUMN ${it.columnName}"
        } + indexes.toAdd.map {
            getIndexCreateSql(DBType.Postgres, "${quote("public")}.${quote(tableName)}", it)
        }
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
        versionField: String?,
        whereClauseSql: String?
    ) =
        "UPDATE ${quote(tableName)} SET ${toUpdateFields.joinToString { equation(it + "New") }}" +
        if (!versionField.isNullOrEmpty()) ", ${quote(versionField)} = ${quote(versionField)} + 1" else { "" } +
        whereClauseSql.orEmpty()

    override fun getSelectSql(dataSource: KronosDataSourceWrapper, selectClause: SelectClauseInfo): String {
        val (tableName, selectFields, distinct, pagination, pi, ps, limit, lock, whereClauseSql, groupByClauseSql, orderByClauseSql, havingClauseSql) = selectClause
        val selectFieldsSql = selectFields.joinToString(", ") {
            when {
                it.type == CUSTOM_CRITERIA_SQL -> it.toString()
                it.name != it.columnName -> "${quote(it.columnName)} AS ${quote(it)}"
                else -> quote(it)
            }
        }
        val paginationSql = if (pagination) " LIMIT $ps OFFSET ${ps * (pi - 1)}" else null
        val limitSql = if (paginationSql == null && limit != null && limit > 0) " LIMIT $limit" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        val lockSql = when(lock) {
            PessimisticLock.X -> " FOR UPDATE"
            PessimisticLock.S -> " FOR SHARE"
            else-> null
        }
        return "SELECT${distinctSql.orEmpty()} $selectFieldsSql FROM ${
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
        val (tableName, selectFields, distinct, pagination, pi, ps, limit, whereClauseSql, groupByClauseSql, orderByClauseSql, havingClauseSql, joinSql) = joinClause
        val selectFieldsSql = selectFields.joinToString(", ") {
            when {
                it.second.type == CUSTOM_CRITERIA_SQL -> it.toString()
                else -> "${quote(it.second, true)} AS ${MssqlSupport.quote(it.first)}"
            }
        }
        val paginationSql = if (pagination) " LIMIT $ps OFFSET ${ps * (pi - 1)}" else null
        val limitSql = if (paginationSql == null && limit != null && limit > 0) " LIMIT $limit" else null
        val distinctSql = if (distinct) " DISTINCT" else null
        return "SELECT${distinctSql.orEmpty()} $selectFieldsSql FROM ${
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