package com.kotlinorm.sql.postgres

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.sql.SqlManager.columnCreateDefSql
import com.kotlinorm.sql.SqlManager.getDBNameFrom

object PostgesqlSupport : DatabasesSupport {
    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            KColumnType.BIT -> "BOOLEAN"
            KColumnType.TINYINT -> "SMALLINT"
            KColumnType.SMALLINT -> "SMALLINT"
            KColumnType.INT -> "INTEGER"
            KColumnType.MEDIUMINT -> "INTEGER"
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
            KColumnType.DATETIME -> "TIMESTAMP"
            KColumnType.TIMESTAMP -> "TIMESTAMP"
            KColumnType.BINARY -> "BYTEA"
            KColumnType.VARBINARY -> "BYTEA"
            KColumnType.LONGVARBINARY -> "BYTEA"
            KColumnType.BLOB -> "BYTEA"
            KColumnType.MEDIUMBLOB -> "BYTEA"
            KColumnType.LONGBLOB -> "BYTEA"
            KColumnType.CLOB -> "TEXT"
            KColumnType.JSON -> "JSON"
            KColumnType.ENUM -> "TEXT"
            KColumnType.NVARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCLOB -> "TEXT"
            KColumnType.UUID -> "UUID"
            KColumnType.SERIAL -> "SERIAL"
            KColumnType.YEAR -> "SMALLINT"
            KColumnType.SET -> "TEXT"
            KColumnType.GEOMETRY -> "GEOMETRY"
            KColumnType.POINT -> "POINT"
            KColumnType.LINESTRING -> "LINESTRING"
            KColumnType.XML -> "XML"
            else -> "VARCHAR(255)"
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String {
        return "${
            column.columnName
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
                type = KColumnType.fromString(it["data_type"].toString()),
                length = it["length"] as Int? ?: 0,
                tableName = tableName,
                nullable = it["is_nullable"] == true,
                primaryKey = it["primary_key"] == true,
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
            "DROP INDEX \"public\".${it.name};"
        } + columns.toAdd.map {
            "ALTER TABLE \"public\".$tableName ADD COLUMN ${it.columnName} ${
                columnCreateDefSql(
                    DBType.Postgres, it
                )
            }"
        } + columns.toModified.map {
            "ALTER TABLE \"public\".$tableName ALTER COLUMN ${it.columnName} TYPE ${
                columnCreateDefSql(
                    DBType.Postgres, it
                )
            } ${if (it.defaultValue != null) ",AlTER COLUMN ${it.columnName} SET DEFAULT ${it.defaultValue}" else ""} ${
                if (it.nullable) ",ALTER COLUMN ${it.columnName} DROP NOT NULL" else ",ALTER COLUMN ${it.columnName} SET NOT NULL"
            }"
        } + columns.toDelete.map {
            "ALTER TABLE \"public\".$tableName DROP COLUMN ${it.columnName}"
        } + indexes.toAdd.map {
            "CREATE ${if (it.method == "UNIQUE") "UNIQUE" else ""} INDEX ${it.name} ON \"public\".$tableName USING ${it.type} (${
                it.columns.joinToString(",") { col -> "\"$col\"" }
            })"
        }
    }
}