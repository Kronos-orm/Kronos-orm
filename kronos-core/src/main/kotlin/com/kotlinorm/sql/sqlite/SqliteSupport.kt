package com.kotlinorm.sql.sqlite

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.sql.SqlManager.sqlColumnType

object SqliteSupport : DatabasesSupport {
    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            KColumnType.BIT -> "INTEGER"
            KColumnType.TINYINT -> "INTEGER"
            KColumnType.SMALLINT -> "INTEGER"
            KColumnType.INT -> "INTEGER"
            KColumnType.MEDIUMINT -> "INTEGER"
            KColumnType.BIGINT -> "INTEGER"
            KColumnType.REAL -> "REAL"
            KColumnType.FLOAT -> "REAL"
            KColumnType.DOUBLE -> "REAL"
            KColumnType.DECIMAL -> "NUMERIC"
            KColumnType.NUMERIC -> "NUMERIC"
            KColumnType.CHAR -> "TEXT"
            KColumnType.VARCHAR -> "TEXT"
            KColumnType.TEXT -> "TEXT"
            KColumnType.MEDIUMTEXT -> "TEXT"
            KColumnType.LONGTEXT -> "TEXT"
            KColumnType.DATE -> "TEXT"
            KColumnType.TIME -> "TEXT"
            KColumnType.DATETIME -> "TEXT"
            KColumnType.TIMESTAMP -> "TEXT"
            KColumnType.BINARY -> "BLOB"
            KColumnType.VARBINARY -> "BLOB"
            KColumnType.LONGVARBINARY -> "BLOB"
            KColumnType.BLOB -> "BLOB"
            KColumnType.MEDIUMBLOB -> "BLOB"
            KColumnType.LONGBLOB -> "BLOB"
            KColumnType.CLOB -> "TEXT"
            KColumnType.JSON -> "TEXT"
            KColumnType.ENUM -> "TEXT"
            KColumnType.NVARCHAR -> "TEXT"
            KColumnType.NCHAR -> "TEXT"
            KColumnType.NCLOB -> "TEXT"
            KColumnType.UUID -> "TEXT"
            KColumnType.SERIAL -> "INTEGER"
            KColumnType.YEAR -> "INTEGER"
            KColumnType.SET -> "TEXT"
            KColumnType.GEOMETRY -> "TEXT"
            KColumnType.POINT -> "TEXT"
            KColumnType.LINESTRING -> "TEXT"
            KColumnType.XML -> "TEXT"
            else -> "TEXT"
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String = "${
        column.columnName
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
        return "CREATE ${index.method} INDEX IF NOT EXISTS ${index.name} ON $tableName (${
            index.columns.joinToString(",") { column ->
                if (index.type.isNotEmpty()) "$column COLLATE ${index.type}"
                else column
            }
        });"
    }

    override fun getTableExistenceSql(dbType: DBType) =
        "SELECT COUNT(1)  as CNT FROM sqlite_master where type='table' and name= :tableName"

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
                if (sql != null && Regex("""(\w+)\sINTEGER\sNOT\sNULL\sPRIMARY\sKEY\sAUTOINCREMENT""").find(sql)?.groupValues?.get(
                        1
                    ) == it["name"] as String
                ) {
                    identity = true
                }
            }
            Field(
                columnName = it["name"].toString(),
                type = KColumnType.fromString(it["type"].toString().split('(').first()), // 处理类型
                length = extractNumberInParentheses(it["type"].toString()), // 处理长度
                tableName = tableName,
                nullable = it["notnull"] as Int == 0, // 直接使用notnull字段判断是否可空
                primaryKey = it["pk"] as Int == 1,
                identity = identity,
                defaultValue = it["dflt_value"] as String?
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
        dataSource: KronosDataSourceWrapper, tableName: String, columns: TableColumnDiff, indexes: TableIndexDiff
    ): List<String> {
        val dbType = dataSource.dbType
        return indexes.toDelete.map {
            "DROP INDEX ${it.name}"
        } + columns.toDelete.map {
            "ALTER TABLE $tableName ADD COLUMN ${getColumnCreateSql(dbType, it)}"
        } + columns.toModified.map {
            "ALTER TABLE $tableName MODIFY COLUMN ${getColumnCreateSql(dbType, it)}"
        } + columns.toDelete.map {
            "ALTER TABLE $tableName DROP COLUMN ${it.columnName}"
        } + indexes.toAdd.map {
            // CREATE INDEX "aaa" ON "tb_user" ("username" COLLATE RTRIM )  如果${it.type}不是空 需要 在每个column后面加 COLLATE ${it.type} (${it.columns.joinToString(",")})需要改
            "CREATE ${it.method} INDEX ${it.name} ON $tableName (${
                it.columns.joinToString(",") { column ->
                    if (it.type.isNotEmpty()) "$column COLLATE ${it.type}"
                    else column
                }
            })"
        }
    }
}