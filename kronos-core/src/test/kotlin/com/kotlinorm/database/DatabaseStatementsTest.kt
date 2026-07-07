package com.kotlinorm.database

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.database.mssql.MssqlStatements
import com.kotlinorm.database.mysql.MysqlStatements
import com.kotlinorm.database.oracle.OracleStatements
import com.kotlinorm.database.postgres.PostgresqlStatements
import com.kotlinorm.database.sqlite.SqliteStatements
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.ddl.TableColumnDiff
import com.kotlinorm.orm.ddl.TableIndexDiff
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.statement.SqlColumnPosition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlServerExtendedPropertyOperation
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.token.SqlUnsafeToken
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseStatementsTest {

    @Test
    fun `database names are parsed from wrapper state`() {
        val actual = mapOf(
            "mysql" to MysqlStatements.databaseName(wrapper(DBType.Mysql, "jdbc:mysql://localhost:3306/kronos?useSSL=false")),
            "postgres" to PostgresqlStatements.databaseName(wrapper(DBType.Postgres, "jdbc:postgresql://localhost:5432/kronos")),
            "sqlite" to SqliteStatements.databaseName(wrapper(DBType.SQLite, "jdbc:sqlite://tmp/kronos.db")),
            "mssql" to MssqlStatements.databaseName(wrapper(DBType.Mssql, "jdbc:sqlserver://localhost:1433;databaseName=test")),
            "oracle" to OracleStatements.databaseName(wrapper(DBType.Oracle, "jdbc:oracle:thin:@localhost:1521/FREEPDB1", "kronos"))
        )

        assertEquals(
            mapOf(
                "mysql" to "kronos",
                "postgres" to "kronos",
                "sqlite" to "tmp/kronos.db",
                "mssql" to "localhost:1433",
                "oracle" to "KRONOS"
            ),
            actual
        )
    }

    @Test
    fun `oracle and mssql metadata queries expose complete syntax shapes`() {
        val actual = mapOf(
            "oracleExists" to OracleStatements.tableExists().toQueryShape(),
            "oracleComment" to OracleStatements.tableComment()!!.toQueryShape(),
            "oracleColumns" to OracleStatements.tableColumns("account").toQueryShape(),
            "oracleIndexes" to OracleStatements.tableIndexes("account").toQueryShape(),
            "mssqlExists" to MssqlStatements.tableExists().toQueryShape(),
            "mssqlComment" to MssqlStatements.tableComment()!!.toQueryShape(),
            "mssqlColumns" to MssqlStatements.tableColumns("account").toQueryShape(),
            "mssqlIndexes" to MssqlStatements.tableIndexes("account").toQueryShape()
        )

        assertEquals(
            mapOf(
                "oracleExists" to QueryShape(
                    select = listOf("COUNT(*)"),
                    from = listOf("all_tables"),
                    where = "owner = :dbName AND table_name = UPPER(:tableName)"
                ),
                "oracleComment" to QueryShape(
                    select = listOf("comments"),
                    from = listOf("all_tab_comments"),
                    where = "owner = :dbName AND table_name = UPPER(:tableName)"
                ),
                "oracleColumns" to QueryShape(
                    select = listOf(
                        "cols.column_name AS COLUMN_NAME",
                        "cols.data_type AS DATA_TYPE",
                        "cols.data_length AS LENGTH",
                        "cols.data_precision AS PRECISION",
                        "cols.nullable AS IS_NULLABLE",
                        "cols.data_default AS COLUMN_DEFAULT",
                        "CASE WHEN EXISTS (SELECT 1 FROM all_cons_columns cons_cols JOIN all_constraints cons ON cons.owner = cons_cols.owner AND cons.constraint_name = cons_cols.constraint_name AND cons.table_name = cons_cols.table_name WHERE cons.constraint_type = 'P' AND cons_cols.owner = cols.owner AND cons_cols.table_name = cols.table_name AND cons_cols.column_name = cols.column_name) THEN '1' ELSE '0' END AS PRIMARY_KEY",
                        "(SELECT comments FROM all_col_comments cc WHERE cc.owner = cols.owner AND cc.table_name = cols.table_name AND cc.column_name = cols.column_name) AS COLUMN_COMMENT"
                    ),
                    from = listOf("all_tab_columns AS cols"),
                    where = "cols.table_name = UPPER(:tableName) AND cols.owner = :dbName"
                ),
                "oracleIndexes" to QueryShape(
                    select = listOf(
                        "i.INDEX_NAME AS NAME",
                        "ic.COLUMN_NAME AS COLUMN_NAME",
                        "ic.COLUMN_POSITION AS SEQ_IN_INDEX",
                        "i.UNIQUENESS AS UNIQUENESS",
                        "i.INDEX_TYPE AS INDEX_TYPE"
                    ),
                    from = listOf("ALL_INDEXES AS i", "ALL_IND_COLUMNS AS ic"),
                    where = "i.OWNER = ic.INDEX_OWNER AND i.INDEX_NAME = ic.INDEX_NAME AND i.TABLE_NAME = UPPER(:tableName) AND i.OWNER = :dbName AND i.INDEX_NAME NOT LIKE UPPER('SYS_%')"
                ),
                "mssqlExists" to QueryShape(
                    select = listOf("COUNT(*)"),
                    from = listOf("sys.tables AS t", "sys.schemas AS s"),
                    where = "t.schema_id = s.schema_id AND s.name = 'dbo' AND t.name = :tableName"
                ),
                "mssqlComment" to QueryShape(
                    select = listOf("CAST(ep.value AS NVARCHAR(MAX))"),
                    from = listOf("sys.extended_properties AS ep", "sys.tables AS t", "sys.schemas AS s"),
                    where = "ep.major_id = t.object_id AND t.schema_id = s.schema_id AND ep.minor_id = 0 AND ep.name = 'MS_Description' AND s.name = 'dbo' AND t.name = :tableName"
                ),
                "mssqlColumns" to QueryShape(
                    select = listOf(
                        "c.COLUMN_NAME",
                        "c.DATA_TYPE",
                        "CASE WHEN c.DATA_TYPE IN ('char', 'nchar', 'varchar', 'nvarchar') THEN c.CHARACTER_MAXIMUM_LENGTH ELSE NULL END AS CHARACTER_MAXIMUM_LENGTH",
                        "CASE WHEN c.DATA_TYPE IN ('decimal', 'numeric') THEN c.NUMERIC_PRECISION ELSE NULL END AS NUMERIC_PRECISION",
                        "c.IS_NULLABLE",
                        "c.COLUMN_DEFAULT",
                        "CASE WHEN EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc ON ccu.Constraint_Name = tc.Constraint_Name AND tc.Constraint_Type = 'PRIMARY KEY' WHERE ccu.COLUMN_NAME = c.COLUMN_NAME AND ccu.TABLE_NAME = c.TABLE_NAME) THEN 'YES' ELSE 'NO' END AS PRIMARY_KEY",
                        "CASE WHEN EXISTS (SELECT 1 FROM sysobjects a INNER JOIN syscolumns b ON a.id = b.id WHERE columnproperty(a.id, b.name, 'isIdentity') = 1 AND objectproperty(a.id, 'isTable') = 1 AND a.name = :tableName AND b.name = c.COLUMN_NAME) THEN 'YES' ELSE 'NO' END AS AUTOINCREAMENT",
                        "CAST((SELECT ep.value FROM sys.extended_properties ep WHERE ep.major_id = OBJECT_ID(:tableName) AND ep.minor_id = c.ORDINAL_POSITION AND ep.name = 'MS_Description') AS NVARCHAR(MAX)) AS COLUMN_COMMENT"
                    ),
                    from = listOf("INFORMATION_SCHEMA.COLUMNS AS c"),
                    where = "c.TABLE_CATALOG = DB_NAME() AND c.TABLE_NAME = :tableName"
                ),
                "mssqlIndexes" to QueryShape(
                    select = listOf(
                        "i.name AS name",
                        "c.name AS columnName",
                        "ic.key_ordinal AS seqInIndex",
                        "i.is_unique AS isUnique",
                        "i.type_desc AS indexType"
                    ),
                    from = listOf("sys.indexes AS i", "sys.index_columns AS ic", "sys.columns AS c", "sys.tables AS t", "sys.schemas AS s"),
                    where = "i.object_id = ic.object_id AND i.index_id = ic.index_id AND c.object_id = ic.object_id AND c.column_id = ic.column_id AND t.object_id = i.object_id AND s.schema_id = t.schema_id AND s.name = 'dbo' AND t.name = :tableName AND i.is_primary_key = 0 AND i.name IS NOT NULL"
                )
            ),
            actual
        )
    }

    @Test
    fun `postgres and sqlite metadata queries expose complete syntax shapes`() {
        val actual = mapOf(
            "postgresExists" to PostgresqlStatements.tableExists().toQueryShape(),
            "postgresComment" to PostgresqlStatements.tableComment()!!.toQueryShape(),
            "postgresColumns" to PostgresqlStatements.tableColumns("account").toQueryShape(),
            "postgresIndexes" to PostgresqlStatements.tableIndexes("account").toQueryShape(),
            "sqliteExists" to SqliteStatements.tableExists().toQueryShape(),
            "sqliteColumns" to SqliteStatements.tableColumns("account").toQueryShape(),
            "sqliteIndexes" to SqliteStatements.tableIndexes("account").toQueryShape()
        )

        assertEquals(
            mapOf(
                "postgresExists" to QueryShape(
                    select = listOf("COUNT(*)"),
                    from = listOf("information_schema.tables"),
                    where = "table_schema = current_schema() AND table_name = :tableName"
                ),
                "postgresComment" to QueryShape(
                    select = listOf("obj_description((current_schema() || '.' || :tableName)::regclass::oid)"),
                    from = emptyList(),
                    where = null
                ),
                "postgresColumns" to QueryShape(
                    select = listOf(
                        "c.column_name AS COLUMN_NAME",
                        "col_description((current_schema() || '.' || c.table_name)::regclass::oid, c.ordinal_position) AS COLUMN_COMMENT",
                        "CASE WHEN c.data_type IN ('character varying', 'varchar') THEN 'VARCHAR' WHEN c.data_type IN ('integer', 'int') THEN 'INT' WHEN c.data_type IN ('bigint') THEN 'BIGINT' WHEN c.data_type IN ('smallint') THEN 'TINYINT' WHEN c.data_type IN ('decimal', 'numeric') THEN 'DECIMAL' WHEN c.data_type IN ('double precision', 'real') THEN 'DOUBLE' WHEN c.data_type IN ('boolean') THEN 'BOOLEAN' WHEN c.data_type LIKE 'timestamp%' THEN 'TIMESTAMP' WHEN c.data_type LIKE 'date' THEN 'DATE' ELSE c.data_type END AS DATA_TYPE",
                        "c.character_maximum_length AS LENGTH",
                        "c.numeric_precision AS SCALE",
                        "c.is_nullable = 'YES' AS IS_NULLABLE",
                        "c.column_default AS COLUMN_DEFAULT",
                        "EXISTS (SELECT 1 FROM information_schema.key_column_usage kcu INNER JOIN information_schema.table_constraints tc ON kcu.constraint_name = tc.constraint_name AND kcu.constraint_schema = tc.constraint_schema WHERE tc.constraint_type = 'PRIMARY KEY' AND kcu.table_schema = c.table_schema AND kcu.table_name = c.table_name AND kcu.column_name = c.column_name) OR (c.column_name = 'id' AND c.data_type LIKE 'serial%') AS PRIMARY_KEY"
                    ),
                    from = listOf("information_schema.columns AS c"),
                    where = "c.table_schema = current_schema() AND c.table_name = :tableName"
                ),
                "postgresIndexes" to QueryShape(
                    select = listOf("indexname AS name", "indexdef AS indexDef"),
                    from = listOf("pg_indexes"),
                    where = "tablename = :tableName AND schemaname = current_schema() AND indexname NOT LIKE CONCAT(tablename, '_pkey')"
                ),
                "sqliteExists" to QueryShape(
                    select = listOf("COUNT(*)"),
                    from = listOf("sqlite_master"),
                    where = "type = 'table' AND name = :tableName"
                ),
                "sqliteColumns" to QueryShape(
                    select = listOf("p.name", "p.type", "p.\"notnull\"", "p.dflt_value", "p.pk", "m.sql AS table_sql"),
                    from = listOf("pragma_table_info", "sqlite_master AS m"),
                    where = "m.type = 'table' AND m.tbl_name = :tableName"
                ),
                "sqliteIndexes" to QueryShape(
                    select = listOf("name", "sql"),
                    from = listOf("sqlite_master"),
                    where = "type = 'index' AND tbl_name = :tableName AND sql IS NOT NULL"
                )
            ),
            actual
        )
    }

    @Test
    fun `mysql column definitions cover every type branch`() {
        val cases = listOf(
            type("undefined", KColumnType.UNDEFINED),
            type("bit", KColumnType.BIT),
            type("tinyint-default", KColumnType.TINYINT),
            type("tinyint-sized", KColumnType.TINYINT, 2),
            type("smallint-default", KColumnType.SMALLINT),
            type("smallint-sized", KColumnType.SMALLINT, 3),
            type("int-default", KColumnType.INT),
            type("int-sized", KColumnType.INT, 9),
            type("serial", KColumnType.SERIAL),
            type("mediumint-default", KColumnType.MEDIUMINT),
            type("mediumint-sized", KColumnType.MEDIUMINT, 7),
            type("bigint-default", KColumnType.BIGINT),
            type("bigint-sized", KColumnType.BIGINT, 18),
            type("float-default", KColumnType.FLOAT),
            type("float-sized", KColumnType.FLOAT, 8, 2),
            type("double-default", KColumnType.DOUBLE),
            type("double-sized", KColumnType.DOUBLE, 9, 3),
            type("decimal-default", KColumnType.DECIMAL),
            type("decimal-length", KColumnType.DECIMAL, 12),
            type("decimal-scale", KColumnType.DECIMAL, 12, 4),
            type("numeric-default", KColumnType.NUMERIC),
            type("numeric-length", KColumnType.NUMERIC, 10),
            type("numeric-scale", KColumnType.NUMERIC, 10, 2),
            type("real", KColumnType.REAL),
            type("char-default", KColumnType.CHAR),
            type("char-sized", KColumnType.CHAR, 8),
            type("nchar", KColumnType.NCHAR, 9),
            type("varchar-default", KColumnType.VARCHAR),
            type("varchar-sized", KColumnType.VARCHAR, 64),
            type("nvarchar", KColumnType.NVARCHAR, 128),
            type("text", KColumnType.TEXT),
            type("xml", KColumnType.XML),
            type("mediumtext", KColumnType.MEDIUMTEXT),
            type("longtext", KColumnType.LONGTEXT),
            type("date", KColumnType.DATE),
            type("time", KColumnType.TIME),
            type("datetime", KColumnType.DATETIME),
            type("timestamp", KColumnType.TIMESTAMP),
            type("binary-default", KColumnType.BINARY),
            type("binary-sized", KColumnType.BINARY, 16),
            type("varbinary-default", KColumnType.VARBINARY),
            type("varbinary-sized", KColumnType.VARBINARY, 32),
            type("longvarbinary", KColumnType.LONGVARBINARY),
            type("longblob", KColumnType.LONGBLOB),
            type("blob", KColumnType.BLOB),
            type("mediumblob", KColumnType.MEDIUMBLOB),
            type("clob", KColumnType.CLOB),
            type("json", KColumnType.JSON),
            type("enum", KColumnType.ENUM),
            type("nclob", KColumnType.NCLOB),
            type("uuid", KColumnType.UUID),
            type("year", KColumnType.YEAR),
            type("set", KColumnType.SET),
            type("geometry", KColumnType.GEOMETRY),
            type("point", KColumnType.POINT),
            type("linestring", KColumnType.LINESTRING)
        )

        assertEquals(
            mapOf(
                "undefined" to "VARCHAR(255)",
                "bit" to "TINYINT(1)",
                "tinyint-default" to "TINYINT(4)",
                "tinyint-sized" to "TINYINT(2)",
                "smallint-default" to "SMALLINT(6)",
                "smallint-sized" to "SMALLINT(3)",
                "int-default" to "INT(11)",
                "int-sized" to "INT(9)",
                "serial" to "INT(11)",
                "mediumint-default" to "MEDIUMINT(9)",
                "mediumint-sized" to "MEDIUMINT(7)",
                "bigint-default" to "BIGINT(20)",
                "bigint-sized" to "BIGINT(18)",
                "float-default" to "FLOAT",
                "float-sized" to "FLOAT(8,2)",
                "double-default" to "DOUBLE",
                "double-sized" to "DOUBLE(9,3)",
                "decimal-default" to "DECIMAL(10,0)",
                "decimal-length" to "DECIMAL(12,0)",
                "decimal-scale" to "DECIMAL(12,4)",
                "numeric-default" to "NUMERIC(10,0)",
                "numeric-length" to "NUMERIC(10,0)",
                "numeric-scale" to "NUMERIC(10,2)",
                "real" to "REAL",
                "char-default" to "CHAR(255)",
                "char-sized" to "CHAR(8)",
                "nchar" to "CHAR(9)",
                "varchar-default" to "VARCHAR(255)",
                "varchar-sized" to "VARCHAR(64)",
                "nvarchar" to "VARCHAR(128)",
                "text" to "TEXT",
                "xml" to "TEXT",
                "mediumtext" to "MEDIUMTEXT",
                "longtext" to "LONGTEXT",
                "date" to "DATE",
                "time" to "TIME",
                "datetime" to "DATETIME",
                "timestamp" to "TIMESTAMP",
                "binary-default" to "BINARY(255)",
                "binary-sized" to "BINARY(16)",
                "varbinary-default" to "VARBINARY(255)",
                "varbinary-sized" to "VARBINARY(32)",
                "longvarbinary" to "LONGBLOB",
                "longblob" to "LONGBLOB",
                "blob" to "BLOB",
                "mediumblob" to "MEDIUMBLOB",
                "clob" to "CLOB",
                "json" to "JSON",
                "enum" to "ENUM",
                "nclob" to "NCLOB",
                "uuid" to "CHAR(36)",
                "year" to "YEAR",
                "set" to "SET",
                "geometry" to "GEOMETRY",
                "point" to "POINT",
                "linestring" to "LINESTRING"
            ),
            cases.associate { it.label to columnType(MysqlStatements, it) }
        )
    }

    @Test
    fun `postgres column definitions cover every type branch`() {
        val cases = listOf(
            type("undefined", KColumnType.UNDEFINED),
            type("bit", KColumnType.BIT),
            type("tinyint", KColumnType.TINYINT),
            type("smallint", KColumnType.SMALLINT),
            type("int", KColumnType.INT),
            type("mediumint", KColumnType.MEDIUMINT),
            type("bigint", KColumnType.BIGINT),
            type("serial", KColumnType.SERIAL),
            type("year", KColumnType.YEAR),
            type("real", KColumnType.REAL),
            type("float-default", KColumnType.FLOAT),
            type("float-sized", KColumnType.FLOAT, 8),
            type("double", KColumnType.DOUBLE),
            type("decimal-default", KColumnType.DECIMAL),
            type("decimal-length", KColumnType.DECIMAL, 12),
            type("decimal-scale", KColumnType.DECIMAL, 12, 4),
            type("numeric-default", KColumnType.NUMERIC),
            type("numeric-length", KColumnType.NUMERIC, 11),
            type("numeric-scale", KColumnType.NUMERIC, 11, 3),
            type("char-default", KColumnType.CHAR),
            type("char-sized", KColumnType.CHAR, 8),
            type("nchar", KColumnType.NCHAR, 9),
            type("varchar-sized", KColumnType.VARCHAR, 64),
            type("varchar-text", KColumnType.VARCHAR, 10485761),
            type("nvarchar-sized", KColumnType.NVARCHAR, 128),
            type("text", KColumnType.TEXT),
            type("clob", KColumnType.CLOB),
            type("mediumtext", KColumnType.MEDIUMTEXT),
            type("longtext", KColumnType.LONGTEXT),
            type("binary", KColumnType.BINARY),
            type("varbinary", KColumnType.VARBINARY),
            type("longvarbinary", KColumnType.LONGVARBINARY),
            type("blob", KColumnType.BLOB),
            type("mediumblob", KColumnType.MEDIUMBLOB),
            type("longblob", KColumnType.LONGBLOB),
            type("date", KColumnType.DATE),
            type("time-default", KColumnType.TIME),
            type("time-scale", KColumnType.TIME, 0, 4),
            type("datetime", KColumnType.DATETIME, 0, 3),
            type("timestamp", KColumnType.TIMESTAMP, 0, 8),
            type("json", KColumnType.JSON),
            type("xml", KColumnType.XML),
            type("uuid", KColumnType.UUID),
            type("enum-default", KColumnType.ENUM),
            type("enum-sized", KColumnType.ENUM, 12),
            type("set", KColumnType.SET),
            type("geometry", KColumnType.GEOMETRY),
            type("point", KColumnType.POINT),
            type("linestring", KColumnType.LINESTRING)
        )

        assertEquals(
            mapOf(
                "undefined" to "TEXT",
                "bit" to "BOOLEAN",
                "tinyint" to "SMALLINT",
                "smallint" to "SMALLINT",
                "int" to "INTEGER",
                "mediumint" to "INTEGER",
                "bigint" to "BIGINT",
                "serial" to "SERIAL",
                "year" to "INTEGER",
                "real" to "REAL",
                "float-default" to "DOUBLE PRECISION",
                "float-sized" to "FLOAT(8)",
                "double" to "DOUBLE PRECISION",
                "decimal-default" to "DECIMAL",
                "decimal-length" to "DECIMAL(12,0)",
                "decimal-scale" to "DECIMAL(12,4)",
                "numeric-default" to "NUMERIC",
                "numeric-length" to "NUMERIC(11,0)",
                "numeric-scale" to "NUMERIC(11,3)",
                "char-default" to "CHAR(255)",
                "char-sized" to "CHAR(8)",
                "nchar" to "CHAR(9)",
                "varchar-sized" to "VARCHAR(64)",
                "varchar-text" to "TEXT",
                "nvarchar-sized" to "VARCHAR(128)",
                "text" to "TEXT",
                "clob" to "TEXT",
                "mediumtext" to "TEXT",
                "longtext" to "TEXT",
                "binary" to "BYTEA",
                "varbinary" to "BYTEA",
                "longvarbinary" to "BYTEA",
                "blob" to "BYTEA",
                "mediumblob" to "BYTEA",
                "longblob" to "BYTEA",
                "date" to "DATE",
                "time-default" to "TIME(0)",
                "time-scale" to "TIME(4)",
                "datetime" to "TIMESTAMP(3)",
                "timestamp" to "TIMESTAMP(6)",
                "json" to "JSONB",
                "xml" to "XML",
                "uuid" to "UUID",
                "enum-default" to "VARCHAR(255)",
                "enum-sized" to "VARCHAR(12)",
                "set" to "TEXT",
                "geometry" to "GEOMETRY",
                "point" to "POINT",
                "linestring" to "LINESTRING"
            ),
            cases.associate { it.label to columnType(PostgresqlStatements, it) }
        )
        assertEquals(
            "SERIAL",
            columnType(PostgresqlStatements, type("identity-int", KColumnType.INT, primaryKey = PrimaryKeyType.IDENTITY))
        )
        assertEquals(
            "BIGSERIAL",
            columnType(PostgresqlStatements, type("identity-bigint", KColumnType.BIGINT, primaryKey = PrimaryKeyType.IDENTITY))
        )
    }

    @Test
    fun `sqlite column definitions cover every type branch`() {
        val cases = listOf(
            type("undefined", KColumnType.UNDEFINED),
            type("bit", KColumnType.BIT),
            type("tinyint", KColumnType.TINYINT),
            type("smallint", KColumnType.SMALLINT),
            type("int", KColumnType.INT),
            type("mediumint", KColumnType.MEDIUMINT),
            type("bigint", KColumnType.BIGINT),
            type("serial", KColumnType.SERIAL),
            type("year", KColumnType.YEAR),
            type("set", KColumnType.SET),
            type("real", KColumnType.REAL),
            type("float", KColumnType.FLOAT),
            type("double", KColumnType.DOUBLE),
            type("decimal", KColumnType.DECIMAL),
            type("numeric", KColumnType.NUMERIC),
            type("char", KColumnType.CHAR),
            type("varchar", KColumnType.VARCHAR),
            type("text", KColumnType.TEXT),
            type("mediumtext", KColumnType.MEDIUMTEXT),
            type("longtext", KColumnType.LONGTEXT),
            type("date", KColumnType.DATE),
            type("time", KColumnType.TIME),
            type("datetime", KColumnType.DATETIME),
            type("timestamp", KColumnType.TIMESTAMP),
            type("clob", KColumnType.CLOB),
            type("json", KColumnType.JSON),
            type("enum", KColumnType.ENUM),
            type("nvarchar", KColumnType.NVARCHAR),
            type("nchar", KColumnType.NCHAR),
            type("nclob", KColumnType.NCLOB),
            type("uuid", KColumnType.UUID),
            type("geometry", KColumnType.GEOMETRY),
            type("point", KColumnType.POINT),
            type("linestring", KColumnType.LINESTRING),
            type("xml", KColumnType.XML),
            type("binary", KColumnType.BINARY),
            type("varbinary", KColumnType.VARBINARY),
            type("longvarbinary", KColumnType.LONGVARBINARY),
            type("blob", KColumnType.BLOB),
            type("mediumblob", KColumnType.MEDIUMBLOB),
            type("longblob", KColumnType.LONGBLOB)
        )

        assertEquals(
            mapOf(
                "undefined" to "NOT",
                "bit" to "INTEGER",
                "tinyint" to "INTEGER",
                "smallint" to "INTEGER",
                "int" to "INTEGER",
                "mediumint" to "INTEGER",
                "bigint" to "INTEGER",
                "serial" to "INTEGER",
                "year" to "INTEGER",
                "set" to "INTEGER",
                "real" to "REAL",
                "float" to "REAL",
                "double" to "REAL",
                "decimal" to "NUMERIC",
                "numeric" to "NUMERIC",
                "char" to "TEXT",
                "varchar" to "TEXT",
                "text" to "TEXT",
                "mediumtext" to "TEXT",
                "longtext" to "TEXT",
                "date" to "TEXT",
                "time" to "TEXT",
                "datetime" to "TEXT",
                "timestamp" to "TEXT",
                "clob" to "TEXT",
                "json" to "TEXT",
                "enum" to "TEXT",
                "nvarchar" to "TEXT",
                "nchar" to "TEXT",
                "nclob" to "TEXT",
                "uuid" to "TEXT",
                "geometry" to "TEXT",
                "point" to "TEXT",
                "linestring" to "TEXT",
                "xml" to "TEXT",
                "binary" to "BLOB",
                "varbinary" to "BLOB",
                "longvarbinary" to "BLOB",
                "blob" to "BLOB",
                "mediumblob" to "BLOB",
                "longblob" to "BLOB"
            ),
            cases.associate { it.label to columnType(SqliteStatements, it) }
        )
    }

    @Test
    fun `mssql column definitions cover every type branch`() {
        val cases = listOf(
            type("undefined", KColumnType.UNDEFINED),
            type("bit", KColumnType.BIT),
            type("tinyint", KColumnType.TINYINT),
            type("smallint", KColumnType.SMALLINT),
            type("int", KColumnType.INT),
            type("mediumint", KColumnType.MEDIUMINT),
            type("serial", KColumnType.SERIAL),
            type("year", KColumnType.YEAR),
            type("bigint", KColumnType.BIGINT),
            type("real", KColumnType.REAL),
            type("float-default", KColumnType.FLOAT),
            type("float-sized", KColumnType.FLOAT, 10),
            type("double", KColumnType.DOUBLE),
            type("decimal-default", KColumnType.DECIMAL),
            type("decimal-length", KColumnType.DECIMAL, 12),
            type("decimal-scale", KColumnType.DECIMAL, 12, 4),
            type("numeric-default", KColumnType.NUMERIC),
            type("numeric-length", KColumnType.NUMERIC, 9),
            type("numeric-scale", KColumnType.NUMERIC, 9, 3),
            type("char", KColumnType.CHAR, 8),
            type("varchar-default", KColumnType.VARCHAR),
            type("varchar-sized", KColumnType.VARCHAR, 64),
            type("varchar-max", KColumnType.VARCHAR, 8001),
            type("nchar", KColumnType.NCHAR, 9),
            type("nvarchar-default", KColumnType.NVARCHAR),
            type("nvarchar-sized", KColumnType.NVARCHAR, 128),
            type("nvarchar-max", KColumnType.NVARCHAR, 4001),
            type("binary", KColumnType.BINARY, 16),
            type("varbinary", KColumnType.VARBINARY, 32),
            type("longvarbinary", KColumnType.LONGVARBINARY),
            type("blob", KColumnType.BLOB),
            type("mediumblob", KColumnType.MEDIUMBLOB),
            type("longblob", KColumnType.LONGBLOB),
            type("text", KColumnType.TEXT),
            type("mediumtext", KColumnType.MEDIUMTEXT),
            type("longtext", KColumnType.LONGTEXT),
            type("clob", KColumnType.CLOB),
            type("date", KColumnType.DATE),
            type("time-default", KColumnType.TIME),
            type("time-scale", KColumnType.TIME, 0, 3),
            type("datetime-default", KColumnType.DATETIME),
            type("datetime-scale", KColumnType.DATETIME, 0, 4),
            type("timestamp", KColumnType.TIMESTAMP),
            type("json", KColumnType.JSON),
            type("enum", KColumnType.ENUM),
            type("nclob", KColumnType.NCLOB),
            type("uuid", KColumnType.UUID),
            type("set", KColumnType.SET),
            type("geometry", KColumnType.GEOMETRY),
            type("point", KColumnType.POINT),
            type("linestring", KColumnType.LINESTRING),
            type("xml", KColumnType.XML)
        )

        assertEquals(
            mapOf(
                "undefined" to "NVARCHAR(255)",
                "bit" to "BIT",
                "tinyint" to "TINYINT",
                "smallint" to "SMALLINT",
                "int" to "INT",
                "mediumint" to "INT",
                "serial" to "INT",
                "year" to "INT",
                "bigint" to "BIGINT",
                "real" to "REAL",
                "float-default" to "FLOAT",
                "float-sized" to "FLOAT(10)",
                "double" to "FLOAT(53)",
                "decimal-default" to "DECIMAL(18,0)",
                "decimal-length" to "DECIMAL(12,0)",
                "decimal-scale" to "DECIMAL(12,4)",
                "numeric-default" to "NUMERIC(18,0)",
                "numeric-length" to "NUMERIC(9,0)",
                "numeric-scale" to "NUMERIC(9,3)",
                "char" to "CHAR(8)",
                "varchar-default" to "VARCHAR(255)",
                "varchar-sized" to "VARCHAR(64)",
                "varchar-max" to "VARCHAR(MAX)",
                "nchar" to "NVARCHAR(9)",
                "nvarchar-default" to "NVARCHAR(255)",
                "nvarchar-sized" to "NVARCHAR(128)",
                "nvarchar-max" to "NVARCHAR(MAX)",
                "binary" to "BINARY(16)",
                "varbinary" to "VARBINARY(32)",
                "longvarbinary" to "VARBINARY(MAX)",
                "blob" to "VARBINARY(MAX)",
                "mediumblob" to "VARBINARY(MAX)",
                "longblob" to "VARBINARY(MAX)",
                "text" to "TEXT",
                "mediumtext" to "TEXT",
                "longtext" to "TEXT",
                "clob" to "TEXT",
                "date" to "DATE",
                "time-default" to "TIME",
                "time-scale" to "TIME(3)",
                "datetime-default" to "DATETIME",
                "datetime-scale" to "DATETIME2(4)",
                "timestamp" to "TIMESTAMP",
                "json" to "JSON",
                "enum" to "NVARCHAR(255)",
                "nclob" to "NTEXT",
                "uuid" to "CHAR(36)",
                "set" to "NVARCHAR(255)",
                "geometry" to "GEOMETRY",
                "point" to "GEOMETRY",
                "linestring" to "GEOMETRY",
                "xml" to "XML"
            ),
            cases.associate { it.label to columnType(MssqlStatements, it) }
        )
    }

    @Test
    fun `oracle column definitions cover every type branch`() {
        val cases = listOf(
            type("undefined", KColumnType.UNDEFINED),
            type("bit", KColumnType.BIT),
            type("tinyint", KColumnType.TINYINT),
            type("smallint", KColumnType.SMALLINT),
            type("mediumint", KColumnType.MEDIUMINT),
            type("int-default", KColumnType.INT),
            type("int-sized", KColumnType.INT, 8),
            type("bigint", KColumnType.BIGINT),
            type("serial", KColumnType.SERIAL),
            type("real", KColumnType.REAL),
            type("float-default", KColumnType.FLOAT),
            type("float-sized", KColumnType.FLOAT, 8),
            type("double", KColumnType.DOUBLE),
            type("decimal-default", KColumnType.DECIMAL),
            type("decimal-length", KColumnType.DECIMAL, 12),
            type("decimal-scale", KColumnType.DECIMAL, 12, 4),
            type("numeric-default", KColumnType.NUMERIC),
            type("numeric-scale", KColumnType.NUMERIC, 10, 2),
            type("char", KColumnType.CHAR, 8),
            type("varchar", KColumnType.VARCHAR, 64),
            type("nvarchar", KColumnType.NVARCHAR, 128),
            type("nchar", KColumnType.NCHAR, 9),
            type("text", KColumnType.TEXT),
            type("mediumtext", KColumnType.MEDIUMTEXT),
            type("longtext", KColumnType.LONGTEXT),
            type("clob", KColumnType.CLOB),
            type("nclob", KColumnType.NCLOB),
            type("binary", KColumnType.BINARY, 16),
            type("varbinary", KColumnType.VARBINARY, 32),
            type("blob", KColumnType.BLOB),
            type("mediumblob", KColumnType.MEDIUMBLOB),
            type("longblob", KColumnType.LONGBLOB),
            type("longvarbinary", KColumnType.LONGVARBINARY),
            type("date", KColumnType.DATE),
            type("time", KColumnType.TIME),
            type("datetime", KColumnType.DATETIME),
            type("timestamp-default", KColumnType.TIMESTAMP),
            type("timestamp-scale", KColumnType.TIMESTAMP, 0, 4),
            type("json", KColumnType.JSON),
            type("xml", KColumnType.XML),
            type("uuid", KColumnType.UUID),
            type("enum", KColumnType.ENUM),
            type("set", KColumnType.SET),
            type("geometry", KColumnType.GEOMETRY),
            type("point", KColumnType.POINT),
            type("linestring", KColumnType.LINESTRING),
            type("year", KColumnType.YEAR)
        )

        assertEquals(
            mapOf(
                "undefined" to "VARCHAR2(255)",
                "bit" to "NUMBER(1)",
                "tinyint" to "NUMBER(3)",
                "smallint" to "NUMBER(5)",
                "mediumint" to "NUMBER(7)",
                "int-default" to "NUMBER(10)",
                "int-sized" to "NUMBER(8)",
                "bigint" to "NUMBER(19)",
                "serial" to "NUMBER",
                "real" to "BINARY_FLOAT",
                "float-default" to "BINARY_DOUBLE",
                "float-sized" to "FLOAT(8)",
                "double" to "BINARY_DOUBLE",
                "decimal-default" to "NUMBER(10,0)",
                "decimal-length" to "NUMBER(12,0)",
                "decimal-scale" to "NUMBER(12,4)",
                "numeric-default" to "NUMERIC(10,0)",
                "numeric-scale" to "NUMERIC(10,2)",
                "char" to "CHAR(8)",
                "varchar" to "VARCHAR2(64)",
                "nvarchar" to "NVARCHAR2(128)",
                "nchar" to "NCHAR(9)",
                "text" to "CLOB",
                "mediumtext" to "CLOB",
                "longtext" to "CLOB",
                "clob" to "CLOB",
                "nclob" to "NCLOB",
                "binary" to "RAW(16)",
                "varbinary" to "RAW(32)",
                "blob" to "BLOB",
                "mediumblob" to "BLOB",
                "longblob" to "BLOB",
                "longvarbinary" to "BLOB",
                "date" to "DATE",
                "time" to "TIMESTAMP(0)",
                "datetime" to "TIMESTAMP(6)",
                "timestamp-default" to "TIMESTAMP(0)",
                "timestamp-scale" to "TIMESTAMP(4)",
                "json" to "JSON",
                "xml" to "XMLType",
                "uuid" to "CHAR(36)",
                "enum" to "VARCHAR2(255)",
                "set" to "VARCHAR2(1000)",
                "geometry" to "SDO_GEOMETRY",
                "point" to "SDO_GEOMETRY",
                "linestring" to "SDO_GEOMETRY",
                "year" to "NUMBER(4)"
            ),
            cases.associate { it.label to columnType(OracleStatements, it) }
        )
    }

    @Test
    fun `mysql maps schema rows into field and index shapes`() {
        val fields = MysqlStatements.mapColumns(
            "account",
            listOf(
                mapOf(
                    "COLUMN_NAME" to "id",
                    "DATA_TYPE" to "int",
                    "LENGTH" to 0,
                    "SCALE" to 0,
                    "COLUMN_TYPE" to "int(11)",
                    "IS_NULLABLE" to "NO",
                    "PRIMARY_KEY" to "YES",
                    "IDENTITY" to "YES",
                    "COLUMN_COMMENT" to "identifier"
                ),
                mapOf(
                    "COLUMN_NAME" to "enabled",
                    "DATA_TYPE" to "tinyint",
                    "LENGTH" to 1,
                    "SCALE" to 0,
                    "COLUMN_TYPE" to "tinyint(1)",
                    "IS_NULLABLE" to "YES",
                    "PRIMARY_KEY" to "NO",
                    "IDENTITY" to "NO",
                    "COLUMN_DEFAULT" to "1"
                )
            )
        ).map { it.toFieldShape() }

        val indexes = MysqlStatements.mapIndexes(
            "account",
            listOf(
                mapOf("indexName" to "idx_account", "columnName" to "name", "seqInIndex" to 2, "nonUnique" to 0, "indexType" to "BTREE"),
                mapOf("indexName" to "idx_account", "columnName" to "tenant_id", "seqInIndex" to 1, "nonUnique" to 0, "indexType" to "BTREE"),
                mapOf("indexName" to "idx_spatial", "columnName" to "shape", "seqInIndex" to 1, "nonUnique" to 1, "indexType" to "SPATIAL")
            )
        ).map { it.toIndexShape() }

        assertEquals(
            listOf(
                FieldShape("id", KColumnType.INT, 11, 0, "account", false, PrimaryKeyType.IDENTITY, null, "identifier"),
                FieldShape("enabled", KColumnType.BIT, 1, 0, "account", true, PrimaryKeyType.NOT, "1", null)
            ),
            fields
        )
        assertEquals(
            listOf(
                IndexShape("idx_account", listOf("tenant_id", "name"), "UNIQUE", "BTREE"),
                IndexShape("idx_spatial", listOf("shape"), "SPATIAL", "SPATIAL")
            ),
            indexes
        )
    }

    @Test
    fun `postgres maps schema rows into field and index shapes`() {
        val fields = PostgresqlStatements.mapColumns(
            "account",
            listOf(
                mapOf(
                    "COLUMN_NAME" to "id",
                    "DATA_TYPE" to "INT",
                    "LENGTH" to 0,
                    "SCALE" to 0,
                    "IS_NULLABLE" to false,
                    "PRIMARY_KEY" to true,
                    "COLUMN_DEFAULT" to "nextval('account_id_seq'::regclass)",
                    "COLUMN_COMMENT" to "identifier"
                ),
                mapOf(
                    "COLUMN_NAME" to "payload",
                    "DATA_TYPE" to "BYTEA",
                    "LENGTH" to 0,
                    "SCALE" to 0,
                    "IS_NULLABLE" to true,
                    "PRIMARY_KEY" to false
                )
            )
        ).map { it.toFieldShape() }

        val indexes = PostgresqlStatements.mapIndexes(
            "account",
            listOf(
                mapOf("name" to "idx_account", "indexDef" to """CREATE UNIQUE INDEX idx_account ON public.account USING btree (tenant_id, "name")"""),
                mapOf("name" to "idx_payload", "indexDef" to "CREATE INDEX idx_payload ON public.account USING hash (payload)")
            )
        ).map { it.toIndexShape() }

        assertEquals(
            listOf(
                FieldShape("id", KColumnType.INT, 0, 0, "account", false, PrimaryKeyType.IDENTITY, null, "identifier"),
                FieldShape("payload", KColumnType.BLOB, 0, 0, "account", true, PrimaryKeyType.NOT, null, null)
            ),
            fields
        )
        assertEquals(
            listOf(
                IndexShape("idx_account", listOf("tenant_id", "name"), "UNIQUE", "btree"),
                IndexShape("idx_payload", listOf("payload"), "NORMAL", "hash")
            ),
            indexes
        )
    }

    @Test
    fun `sqlite maps schema rows into field and index shapes`() {
        val fields = SqliteStatements.mapColumns(
            "account",
            listOf(
                mapOf(
                    "name" to "id",
                    "type" to "INTEGER",
                    "notnull" to 1,
                    "pk" to 1,
                    "table_sql" to """CREATE TABLE account ("id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, name TEXT)"""
                ),
                mapOf(
                    "name" to "amount",
                    "type" to "DECIMAL(12,2)",
                    "notnull" to 0,
                    "dflt_value" to "0",
                    "pk" to 0,
                    "table_sql" to ""
                )
            )
        ).map { it.toFieldShape() }

        val indexes = SqliteStatements.mapIndexes(
            "account",
            listOf(
                mapOf("name" to "idx_account", "sql" to """CREATE UNIQUE INDEX idx_account ON account ("tenant_id" COLLATE BINARY, [name])"""),
                mapOf("name" to "idx_amount", "sql" to "CREATE INDEX idx_amount ON account (amount)")
            )
        ).map { it.toIndexShape() }

        assertEquals(
            listOf(
                FieldShape("id", KColumnType.INT, 0, 0, "account", false, PrimaryKeyType.IDENTITY, null, null),
                FieldShape("amount", KColumnType.DECIMAL, 12, 2, "account", true, PrimaryKeyType.NOT, "0", null)
            ),
            fields
        )
        assertEquals(
            listOf(
                IndexShape("idx_account", listOf("tenant_id", "name"), "UNIQUE", ""),
                IndexShape("idx_amount", listOf("amount"), "NORMAL", "")
            ),
            indexes
        )
    }

    @Test
    fun `mssql maps schema rows into field and index shapes`() {
        val fields = MssqlStatements.mapColumns(
            "account",
            listOf(
                mapOf(
                    "COLUMN_NAME" to "id",
                    "DATA_TYPE" to "int",
                    "CHARACTER_MAXIMUM_LENGTH" to 0,
                    "NUMERIC_PRECISION" to 0,
                    "IS_NULLABLE" to "NO",
                    "PRIMARY_KEY" to "YES",
                    "AUTOINCREAMENT" to "YES",
                    "COLUMN_DEFAULT" to "((0))",
                    "COLUMN_COMMENT" to "identifier"
                ),
                mapOf(
                    "COLUMN_NAME" to "name",
                    "DATA_TYPE" to "nvarchar",
                    "CHARACTER_MAXIMUM_LENGTH" to 64,
                    "NUMERIC_PRECISION" to 0,
                    "IS_NULLABLE" to "YES",
                    "PRIMARY_KEY" to "NO",
                    "AUTOINCREAMENT" to "NO",
                    "COLUMN_DEFAULT" to "('guest')"
                )
            )
        ).map { it.toFieldShape() }

        val indexes = MssqlStatements.mapIndexes(
            "account",
            listOf(
                mapOf("name" to "idx_account", "columnName" to "name", "seqInIndex" to 2, "isUnique" to 1, "indexType" to "NONCLUSTERED"),
                mapOf("name" to "idx_account", "columnName" to "tenant_id", "seqInIndex" to 1, "isUnique" to 1, "indexType" to "NONCLUSTERED")
            )
        ).map { it.toIndexShape() }

        assertEquals(
            listOf(
                FieldShape("id", KColumnType.INT, 0, 0, "account", false, PrimaryKeyType.IDENTITY, "0", "identifier"),
                FieldShape("name", KColumnType.NVARCHAR, 64, 0, "account", true, PrimaryKeyType.NOT, "'guest'", null)
            ),
            fields
        )
        assertEquals(
            listOf(IndexShape("idx_account", listOf("tenant_id", "name"), "NONCLUSTERED", "UNIQUE")),
            indexes
        )
    }

    @Test
    fun `oracle maps schema rows into field and index shapes`() {
        val fields = OracleStatements.mapColumns(
            "account",
            listOf(
                mapOf(
                    "COLUMN_NAME" to "ID",
                    "DATA_TYPE" to "NUMBER",
                    "LENGTH" to 19,
                    "PRECISION" to 0,
                    "IS_NULLABLE" to "N",
                    "PRIMARY_KEY" to "1",
                    "COLUMN_DEFAULT" to "SEQ_ACCOUNT.nextval",
                    "COLUMN_COMMENT" to "identifier"
                ),
                mapOf(
                    "COLUMN_NAME" to "NAME",
                    "DATA_TYPE" to "VARCHAR2",
                    "LENGTH" to 64,
                    "PRECISION" to 0,
                    "IS_NULLABLE" to "Y",
                    "PRIMARY_KEY" to "0",
                    "COLUMN_DEFAULT" to "'guest'"
                )
            )
        ).map { it.toFieldShape() }

        val indexes = OracleStatements.mapIndexes(
            "account",
            listOf(
                mapOf("NAME" to "IDX_ACCOUNT", "COLUMN_NAME" to "NAME", "SEQ_IN_INDEX" to 2, "UNIQUENESS" to "UNIQUE", "INDEX_TYPE" to "NORMAL"),
                mapOf("NAME" to "IDX_ACCOUNT", "COLUMN_NAME" to "TENANT_ID", "SEQ_IN_INDEX" to 1, "UNIQUENESS" to "UNIQUE", "INDEX_TYPE" to "NORMAL"),
                mapOf("NAME" to "IDX_SPATIAL", "COLUMN_NAME" to "SHAPE", "SEQ_IN_INDEX" to 1, "UNIQUENESS" to "NONUNIQUE", "INDEX_TYPE" to "DOMAIN")
            )
        ).map { it.toIndexShape() }

        assertEquals(
            listOf(
                FieldShape("ID", KColumnType.BIGINT, 19, 0, "ACCOUNT", false, PrimaryKeyType.IDENTITY, null, "identifier"),
                FieldShape("NAME", KColumnType.VARCHAR, 64, 0, "ACCOUNT", true, PrimaryKeyType.NOT, "'guest'", null)
            ),
            fields
        )
        assertEquals(
            listOf(
                IndexShape("IDX_ACCOUNT", listOf("TENANT_ID", "NAME"), "UNIQUE", ""),
                IndexShape("IDX_SPATIAL", listOf("SHAPE"), "NORMAL", "DOMAIN")
            ),
            indexes
        )
    }

    @Test
    fun `mysql builds complete DDL statement definitions`() {
        assertEquals(
            listOf(
                StatementShape("CreateTable", "account", columns = listOf(
                    ColumnShape("id", "INT(11)", false, "Identity", null),
                    ColumnShape("name", "VARCHAR(32)", false, "NotPrimary", "'guest'")
                ), comment = "accounts", ifNotExists = true),
                StatementShape("CreateIndex", "account", indexName = "idx_account_name", columns = listOf("tenant_id", "name"), unique = true, method = "BTREE")
            ),
            MysqlStatements.createTable(createInput()).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(StatementShape("DropTable", "account", ifExists = true)),
            MysqlStatements.dropTable("account", true).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(StatementShape("Truncate", "account", restartIdentity = false)),
            MysqlStatements.truncateTable("account", true).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(
                StatementShape("SetTableComment", "account", comment = "accounts"),
                StatementShape("DropIndex", "account", indexName = "idx_old"),
                StatementShape("AddColumn", "account", columns = listOf(ColumnShape("nickname", "VARCHAR(16)", true, "NotPrimary", "'nick'")), position = "AFTER name"),
                StatementShape("ModifyColumn", "account", columns = listOf(ColumnShape("name", "VARCHAR(64)", false, "NotPrimary", "'new'")), position = "AFTER id"),
                StatementShape("DropColumn", "account", columnName = "legacy"),
                StatementShape("CreateIndex", "account", indexName = "idx_new", columns = listOf("nickname"), method = "BTREE")
            ),
            MysqlStatements.syncTable(syncInput()).map { it.toStatementShape() }
        )
    }

    @Test
    fun `postgres builds complete DDL statement definitions`() {
        assertEquals(
            listOf(
                StatementShape("CreateTable", "public.account", columns = listOf(
                    ColumnShape("id", "SERIAL", false, "Primary", null),
                    ColumnShape("name", "VARCHAR(32)", false, "NotPrimary", "'guest'")
                ), ifNotExists = true),
                StatementShape("CreateIndex", "public.account", indexName = "idx_account_name", columns = listOf("tenant_id", "name"), unique = true, method = "BTREE"),
                StatementShape("CommentOnColumn", "public.account", columnName = "id", comment = "identifier"),
                StatementShape("CommentOnColumn", "public.account", columnName = "name", comment = "display name"),
                StatementShape("CommentOnTable", "public.account", comment = "accounts")
            ),
            PostgresqlStatements.createTable(createInput()).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(StatementShape("DropTable", "public.account", ifExists = true)),
            PostgresqlStatements.dropTable("account", true).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(StatementShape("Truncate", "public.account", restartIdentity = true)),
            PostgresqlStatements.truncateTable("account", true).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(
                StatementShape("CommentOnTable", "public.account", comment = "accounts"),
                StatementShape("DropIndex", null, indexName = "public.idx_old"),
                StatementShape("AddColumn", "public.account", columns = listOf(ColumnShape("nickname", "VARCHAR(16)", true, "NotPrimary", "'nick'"))),
                StatementShape("ModifyColumn", "public.account", columns = listOf(ColumnShape("name", "VARCHAR(64)", false, "NotPrimary", "'new'"))),
                StatementShape("AlterColumnDefault", "public.account", columnName = "name", defaultValue = "'new'"),
                StatementShape("AlterColumnNullable", "public.account", columnName = "name", nullable = false),
                StatementShape("CommentOnColumn", "public.account", columnName = "name", comment = "new display"),
                StatementShape("DropColumn", "public.account", columnName = "legacy"),
                StatementShape("CreateIndex", "public.account", indexName = "idx_new", columns = listOf("nickname"), method = "BTREE")
            ),
            PostgresqlStatements.syncTable(syncInput()).map { it.toStatementShape() }
        )
    }

    @Test
    fun `sqlite builds complete DDL statement definitions`() {
        assertEquals(
            listOf(
                StatementShape("CreateTable", "account", columns = listOf(
                    ColumnShape("id", "INTEGER", false, "Identity", null),
                    ColumnShape("name", "TEXT", false, "NotPrimary", "'guest'")
                ), ifNotExists = true),
                StatementShape("CreateIndex", "account", indexName = "idx_account_name", columns = listOf("tenant_id", "name"), unique = true, method = "BTREE", ifNotExists = true)
            ),
            SqliteStatements.createTable(createInput()).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(StatementShape("DropTable", "account", ifExists = true)),
            SqliteStatements.dropTable("account", true).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(StatementShape("Delete", "account")),
            SqliteStatements.truncateTable("account", true).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(
                StatementShape("DropIndex", null, indexName = "idx_old", ifExists = true),
                StatementShape("AddColumn", "account", columns = listOf(ColumnShape("nickname", "TEXT", true, "NotPrimary", "'nick'"))),
                StatementShape("ModifyColumn", "account", columns = listOf(ColumnShape("name", "TEXT", false, "NotPrimary", "'new'"))),
                StatementShape("DropColumn", "account", columnName = "legacy"),
                StatementShape("CreateIndex", "account", indexName = "idx_new", columns = listOf("nickname"), method = "BTREE")
            ),
            SqliteStatements.syncTable(syncInput()).map { it.toStatementShape() }
        )
    }

    @Test
    fun `mssql builds complete DDL statement definitions`() {
        assertEquals(
            listOf(
                StatementShape("CreateTable", "dbo.account", columns = listOf(
                    ColumnShape("id", "INT", false, "Identity", null),
                    ColumnShape("name", "VARCHAR(32)", false, "NotPrimary", "'guest'")
                ), ifNotExists = true),
                StatementShape("CreateIndex", "dbo.account", indexName = "idx_account_name", columns = listOf("tenant_id", "name"), unique = true, method = "BTREE"),
                StatementShape("SqlServerExtendedPropertyComment", "dbo.account", columnName = "id", comment = "identifier", operation = "Add"),
                StatementShape("SqlServerExtendedPropertyComment", "dbo.account", columnName = "name", comment = "display name", operation = "Add"),
                StatementShape("SqlServerExtendedPropertyComment", "dbo.account", comment = "accounts", operation = "Add")
            ),
            MssqlStatements.createTable(createInput()).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(StatementShape("DropTable", "dbo.account", ifExists = true)),
            MssqlStatements.dropTable("account", true).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(StatementShape("Truncate", "dbo.account", restartIdentity = false)),
            MssqlStatements.truncateTable("account", true).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(
                StatementShape("SqlServerExtendedPropertyComment", "dbo.account", comment = "accounts", operation = "Update"),
                StatementShape("DropIndex", "dbo.account", indexName = "idx_old"),
                StatementShape("SqlServerDropDefaultConstraint", "dbo.account", columnName = "legacy"),
                StatementShape("DropColumn", "dbo.account", columnName = "legacy"),
                StatementShape("AddColumn", "dbo.account", columns = listOf(ColumnShape("nickname", "VARCHAR(16)", true, "NotPrimary", "'nick'"))),
                StatementShape("SqlServerDropDefaultConstraint", "dbo.account", columnName = "name"),
                StatementShape("ModifyColumn", "dbo.account", columns = listOf(ColumnShape("name", "VARCHAR(64)", false, "NotPrimary", "'new'"))),
                StatementShape("SqlServerExtendedPropertyComment", "dbo.account", columnName = "name", comment = "new display", operation = "Update"),
                StatementShape("CreateIndex", "dbo.account", indexName = "idx_new", columns = listOf("nickname"), method = "BTREE")
            ),
            MssqlStatements.syncTable(syncInput()).map { it.toStatementShape() }
        )
    }

    @Test
    fun `oracle builds complete DDL statement definitions`() {
        assertEquals(
            listOf(
                StatementShape("CreateTable", "ACCOUNT", columns = listOf(
                    ColumnShape("ID", "NUMBER(10)", false, "Identity", null),
                    ColumnShape("NAME", "VARCHAR2(32)", false, "NotPrimary", "'guest'")
                )),
                StatementShape("CreateIndex", "ACCOUNT", indexName = "idx_account_name", columns = listOf("tenant_id", "name"), unique = true, method = "BTREE"),
                StatementShape("CommentOnColumn", "ACCOUNT", columnName = "ID", comment = "identifier"),
                StatementShape("CommentOnColumn", "ACCOUNT", columnName = "NAME", comment = "display name"),
                StatementShape("CommentOnTable", "ACCOUNT", comment = "accounts")
            ),
            OracleStatements.createTable(createInput()).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(StatementShape("DropTable", "ACCOUNT", ifExists = false)),
            OracleStatements.dropTable("account", true).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(StatementShape("Truncate", "ACCOUNT", restartIdentity = false)),
            OracleStatements.truncateTable("account", true).map { it.toStatementShape() }
        )
        assertEquals(
            listOf(
                StatementShape("CommentOnTable", "ACCOUNT", comment = "accounts"),
                StatementShape("DropIndex", null, indexName = "idx_old"),
                StatementShape("DropColumn", "ACCOUNT", columnName = "LEGACY"),
                StatementShape("ModifyColumn", "ACCOUNT", columns = listOf(ColumnShape("NAME", "VARCHAR2(64)", false, "NotPrimary", "'new'"))),
                StatementShape("AddColumn", "ACCOUNT", columns = listOf(ColumnShape("NICKNAME", "VARCHAR2(16)", true, "NotPrimary", "'nick'"))),
                StatementShape("CommentOnColumn", "ACCOUNT", columnName = "NAME", comment = "new display"),
                StatementShape("CreateIndex", "ACCOUNT", indexName = "idx_new", columns = listOf("nickname"), method = "BTREE")
            ),
            OracleStatements.syncTable(syncInput()).map { it.toStatementShape() }
        )
    }

    private fun columnType(statements: DatabaseStatements, case: TypeCase): String {
        val field = Field(
            columnName = case.label.replace('-', '_'),
            type = case.type,
            primaryKey = case.primaryKey,
            length = case.length,
            scale = case.scale,
            nullable = false
        )
        val create = statements.createTable(DatabaseCreateTable("type_probe", null, listOf(field), emptyList()))
            .first() as SqlDdlStatement.CreateTable
        val type = create.columns.single().type as SqlType.UnsafeCustom
        return type.tokens.joinToString("") { token ->
            when (token) {
                is SqlUnsafeToken.Text -> token.value
                is SqlUnsafeToken.Identifier -> token.value
                is SqlUnsafeToken.Expr -> token.value.toString()
            }
        }
    }

    private data class StatementShape(
        val kind: String,
        val table: String?,
        val indexName: String? = null,
        val columnName: String? = null,
        val columns: List<Any> = emptyList(),
        val unique: Boolean = false,
        val method: String? = null,
        val type: String? = null,
        val comment: String? = null,
        val ifExists: Boolean = false,
        val ifNotExists: Boolean = false,
        val restartIdentity: Boolean = false,
        val position: String? = null,
        val defaultValue: String? = null,
        val nullable: Boolean? = null,
        val operation: String? = null,
        val condition: String? = null
    )

    private data class QueryShape(
        val select: List<String>,
        val from: List<String>,
        val where: String?
    )

    private data class ColumnShape(
        val name: String,
        val type: String,
        val nullable: Boolean,
        val primaryKey: String,
        val defaultValue: String?
    )

    private fun SqlStatement.toStatementShape(): StatementShape = when (this) {
        is SqlDdlStatement.CreateTable -> StatementShape(
            kind = "CreateTable",
            table = tableName.canonical,
            columns = columns.map { it.toColumnShape() },
            comment = comment,
            ifNotExists = ifNotExists
        )
        is SqlDdlStatement.DropTable -> StatementShape(
            kind = "DropTable",
            table = tableName.canonical,
            ifExists = ifExists
        )
        is SqlDdlStatement.CreateIndex -> StatementShape(
            kind = "CreateIndex",
            table = tableName.canonical,
            indexName = indexName.canonical,
            columns = columns.map { it.canonical },
            unique = unique,
            method = method,
            type = type,
            ifNotExists = ifNotExists
        )
        is SqlDdlStatement.DropIndex -> StatementShape(
            kind = "DropIndex",
            table = tableName?.canonical,
            indexName = indexName.canonical,
            ifExists = ifExists
        )
        is SqlDdlStatement.AlterTable.SetTableComment -> StatementShape(
            kind = "SetTableComment",
            table = tableName.canonical,
            comment = comment
        )
        is SqlDdlStatement.AlterTable.AddColumn -> StatementShape(
            kind = "AddColumn",
            table = tableName.canonical,
            columns = listOf(column.toColumnShape()),
            position = position?.toPositionShape()
        )
        is SqlDdlStatement.AlterTable.ModifyColumn -> StatementShape(
            kind = "ModifyColumn",
            table = tableName.canonical,
            columns = listOf(column.toColumnShape()),
            position = position?.toPositionShape()
        )
        is SqlDdlStatement.AlterTable.DropColumn -> StatementShape(
            kind = "DropColumn",
            table = tableName.canonical,
            columnName = columnName.canonical
        )
        is SqlDdlStatement.AlterTable.AlterColumnDefault -> StatementShape(
            kind = "AlterColumnDefault",
            table = tableName.canonical,
            columnName = columnName.canonical,
            defaultValue = defaultValue?.toExprShape()
        )
        is SqlDdlStatement.AlterTable.AlterColumnNullable -> StatementShape(
            kind = "AlterColumnNullable",
            table = tableName.canonical,
            columnName = columnName.canonical,
            nullable = nullable
        )
        is SqlDdlStatement.CommentOnTable -> StatementShape(
            kind = "CommentOnTable",
            table = tableName.canonical,
            comment = comment
        )
        is SqlDdlStatement.CommentOnColumn -> StatementShape(
            kind = "CommentOnColumn",
            table = tableName.canonical,
            columnName = columnName.canonical,
            comment = comment
        )
        is SqlDdlStatement.SqlServerExtendedPropertyComment -> StatementShape(
            kind = "SqlServerExtendedPropertyComment",
            table = tableName.canonical,
            columnName = columnName?.canonical,
            comment = comment,
            operation = operation.name
        )
        is SqlDdlStatement.SqlServerDropDefaultConstraint -> StatementShape(
            kind = "SqlServerDropDefaultConstraint",
            table = tableName.canonical,
            columnName = columnName.canonical
        )
        is SqlDdlStatement.Vacuum -> StatementShape(kind = "Vacuum", table = schemaName?.canonical)
        is SqlDmlStatement.Truncate -> StatementShape(
            kind = "Truncate",
            table = table.toTableShape(),
            restartIdentity = restartIdentity
        )
        is SqlDmlStatement.Delete -> StatementShape(
            kind = "Delete",
            table = table.toTableShape(),
            condition = where?.toExprShape()
        )
        else -> StatementShape(kind = this::class.simpleName ?: "Unknown", table = null)
    }

    private fun SqlQuery.toQueryShape(): QueryShape {
        val select = this as SqlQuery.Select
        return QueryShape(
            select = select.select.map { it.toSelectItemShape() },
            from = select.from.map { it.toTableWithAliasShape() },
            where = select.where?.toExprShape()
        )
    }

    private fun SqlSelectItem.toSelectItemShape(): String = when (this) {
        is SqlSelectItem.Asterisk -> qualifier?.canonical?.let { "$it.*" } ?: "*"
        is SqlSelectItem.Expr -> expr.toExprShape() + alias?.let { " AS $it" }.orEmpty()
    }

    private fun com.kotlinorm.syntax.statement.SqlColumnDefinition.toColumnShape() = ColumnShape(
        name = name.canonical,
        type = type.toTypeShape(),
        nullable = nullable,
        primaryKey = primaryKey.name,
        defaultValue = defaultValue?.toExprShape()
    )

    private fun SqlType.toTypeShape(): String = when (this) {
        is SqlType.UnsafeCustom -> tokens.joinToString("") { token ->
            when (token) {
                is SqlUnsafeToken.Text -> token.value
                is SqlUnsafeToken.Identifier -> token.value
                is SqlUnsafeToken.Expr -> token.value.toExprShape()
            }
        }
        is SqlType.Varchar -> maxLength?.let { "VARCHAR($it)" } ?: "VARCHAR"
        SqlType.Int -> "INT"
        SqlType.Long -> "BIGINT"
        SqlType.Float -> "FLOAT"
        SqlType.Double -> "DOUBLE"
        is SqlType.Decimal -> precision?.let { "DECIMAL(${it.first},${it.second})" } ?: "DECIMAL"
        SqlType.Date -> "DATE"
        is SqlType.Time -> "TIME"
        is SqlType.Timestamp -> "TIMESTAMP"
        SqlType.Json -> "JSON"
        SqlType.Boolean -> "BOOLEAN"
        SqlType.Interval -> "INTERVAL"
        SqlType.Geometry -> "GEOMETRY"
        SqlType.Point -> "POINT"
        SqlType.LineString -> "LINESTRING"
        SqlType.Polygon -> "POLYGON"
        SqlType.MultiPoint -> "MULTIPOINT"
        SqlType.MultiLineString -> "MULTILINESTRING"
        SqlType.MultiPolygon -> "MULTIPOLYGON"
        SqlType.GeometryCollection -> "GEOMETRYCOLLECTION"
        is SqlType.Array -> "ARRAY(${type.toTypeShape()})"
        is SqlType.Named -> listOf(name).plus(arguments.map { it.toString() }).joinToString(":")
    }

    private fun SqlColumnPosition.toPositionShape(): String = when (this) {
        SqlColumnPosition.First -> "FIRST"
        is SqlColumnPosition.After -> "AFTER ${columnName.canonical}"
    }

    private fun SqlTable.toTableShape(): String = when (this) {
        is SqlTable.Ident -> identifier.canonical
        is SqlTable.Func -> identifier.canonical
        is SqlTable.Subquery -> alias?.alias ?: "subquery"
        is SqlTable.Join -> "${left.toTableShape()} JOIN ${right.toTableShape()}"
        is SqlTable.Json -> alias?.alias ?: "json"
        is SqlTable.Graph -> alias?.alias ?: name
    }

    private fun SqlTable.toTableWithAliasShape(): String = when (this) {
        is SqlTable.Ident -> identifier.canonical + alias?.let { " AS ${it.alias}" }.orEmpty()
        else -> toTableShape()
    }

    private fun SqlExpr.toExprShape(): String = when (this) {
        is SqlExpr.UnsafeRaw -> sql
        is SqlExpr.StringLiteral -> "'$string'"
        is SqlExpr.NumberLiteral -> number
        is SqlExpr.BooleanLiteral -> boolean.toString()
        SqlExpr.NullLiteral -> "NULL"
        is SqlExpr.Parameter -> when (val parameter = parameter) {
            is com.kotlinorm.syntax.expr.SqlParameter.Named -> ":${parameter.name}"
            is com.kotlinorm.syntax.expr.SqlParameter.Positional -> "?${parameter.index}"
        }
        is SqlExpr.Column -> "\"${identifier.canonical}\""
        is SqlExpr.Function -> "${name.canonical}(${args.joinToString(", ") { it.toExprShape() }})"
        is SqlExpr.Binary -> "${left.toExprShape()} ${operator.toOperatorShape()} ${right.toExprShape()}"
        else -> toString()
    }

    private fun SqlBinaryOperator.toOperatorShape(): String = when (this) {
        SqlBinaryOperator.Times -> "*"
        SqlBinaryOperator.Div -> "/"
        SqlBinaryOperator.Mod -> "%"
        SqlBinaryOperator.Plus -> "+"
        SqlBinaryOperator.Minus -> "-"
        SqlBinaryOperator.Concat -> "||"
        SqlBinaryOperator.Equal -> "="
        SqlBinaryOperator.NotEqual -> "<>"
        is SqlBinaryOperator.IsDistinctFrom -> if (withNot) "IS NOT DISTINCT FROM" else "IS DISTINCT FROM"
        is SqlBinaryOperator.Is -> if (withNot) "IS NOT" else "IS"
        SqlBinaryOperator.GreaterThan -> ">"
        SqlBinaryOperator.GreaterThanEqual -> ">="
        SqlBinaryOperator.LessThan -> "<"
        SqlBinaryOperator.LessThanEqual -> "<="
        SqlBinaryOperator.Overlaps -> "OVERLAPS"
        SqlBinaryOperator.Regexp -> "REGEXP"
        SqlBinaryOperator.NotRegexp -> "NOT REGEXP"
        SqlBinaryOperator.BitwiseAnd -> "&"
        SqlBinaryOperator.BitwiseOr -> "|"
        SqlBinaryOperator.BitwiseXor -> "^"
        SqlBinaryOperator.BitwiseLeftShift -> "<<"
        SqlBinaryOperator.BitwiseRightShift -> ">>"
        SqlBinaryOperator.And -> "AND"
        SqlBinaryOperator.Or -> "OR"
        is SqlBinaryOperator.UnsafeCustom -> tokens.joinToString("") { token ->
            when (token) {
                is SqlUnsafeToken.Text -> token.value
                is SqlUnsafeToken.Identifier -> token.value
                is SqlUnsafeToken.Expr -> token.value.toExprShape()
            }
        }
    }

    private fun createInput() = DatabaseCreateTable(
        tableName = "account",
        tableComment = "accounts",
        columns = listOf(
            statementField("id", KColumnType.INT, primaryKey = PrimaryKeyType.IDENTITY, nullable = false, kDoc = "identifier"),
            statementField("name", KColumnType.VARCHAR, length = 32, nullable = false, defaultValue = "'guest'", kDoc = "display name")
        ),
        indexes = listOf(KTableIndex("idx_account_name", arrayOf("tenant_id", "name"), "UNIQUE", "BTREE"))
    )

    private fun syncInput() = DatabaseSyncTable(
        tableName = "account",
        originalTableComment = "old accounts",
        tableComment = "accounts",
        columns = TableColumnDiff(
            toAdd = listOf(statementField("nickname", KColumnType.VARCHAR, length = 16, defaultValue = "'nick'") to statementField("name", KColumnType.VARCHAR)),
            toModified = listOf(
                Triple(
                    statementField("name", KColumnType.VARCHAR, length = 64, nullable = false, defaultValue = "'new'", kDoc = "new display"),
                    statementField("id", KColumnType.INT),
                    statementField("name", KColumnType.VARCHAR, length = 32, nullable = true, defaultValue = "'old'", kDoc = "old display")
                )
            ),
            toDelete = listOf(statementField("legacy", KColumnType.TEXT))
        ),
        indexes = TableIndexDiff(
            toAdd = listOf(KTableIndex("idx_new", arrayOf("nickname"), "NORMAL", "BTREE")),
            toDelete = listOf(KTableIndex("idx_old", arrayOf("legacy"), "NORMAL", "BTREE"))
        )
    )

    private fun statementField(
        columnName: String,
        type: KColumnType,
        length: Int = 0,
        scale: Int = 0,
        primaryKey: PrimaryKeyType = PrimaryKeyType.NOT,
        nullable: Boolean = true,
        defaultValue: String? = null,
        kDoc: String? = null
    ) = Field(
        columnName = columnName,
        type = type,
        primaryKey = primaryKey,
        length = length,
        scale = scale,
        nullable = nullable,
        defaultValue = defaultValue,
        kDoc = kDoc
    )

    private data class TypeCase(
        val label: String,
        val type: KColumnType,
        val length: Int,
        val scale: Int,
        val primaryKey: PrimaryKeyType
    )

    private fun type(
        label: String,
        type: KColumnType,
        length: Int = 0,
        scale: Int = 0,
        primaryKey: PrimaryKeyType = PrimaryKeyType.NOT
    ) = TypeCase(label, type, length, scale, primaryKey)

    private data class FieldShape(
        val columnName: String,
        val type: KColumnType,
        val length: Int,
        val scale: Int,
        val tableName: String,
        val nullable: Boolean,
        val primaryKey: PrimaryKeyType,
        val defaultValue: String?,
        val kDoc: String?
    )

    private fun Field.toFieldShape() = FieldShape(
        columnName = columnName,
        type = type,
        length = length,
        scale = scale,
        tableName = tableName,
        nullable = nullable,
        primaryKey = primaryKey,
        defaultValue = defaultValue,
        kDoc = kDoc
    )

    private data class IndexShape(
        val name: String,
        val columns: List<String>,
        val type: String,
        val method: String
    )

    private fun KTableIndex.toIndexShape() = IndexShape(name, columns.toList(), type, method)

    private fun wrapper(dbType: DBType, url: String, userName: String = "kronos") =
        object : KronosDataSourceWrapper {
            override val url: String = url
            override val userName: String = userName
            override val dbType: DBType = dbType
            override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()
            override fun forList(task: KAtomicQueryTask, kClass: KClass<*>, isKPojo: Boolean, superTypes: List<String>): List<Any> = emptyList()
            override fun forMap(task: KAtomicQueryTask): Map<String, Any>? = null
            override fun forObject(task: KAtomicQueryTask, kClass: KClass<*>, isKPojo: Boolean, superTypes: List<String>): Any? = null
            override fun update(task: KAtomicActionTask): Int = 0
            override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
            override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? = null
        }
}
