package com.kotlinorm.orm.database

import com.kotlinorm.Kronos.defaultLogger
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.logging.KLogMessage.Companion.kMsgOf
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getDBNameFrom
import com.kotlinorm.database.SqlManager.getTableExistenceSql
import com.kotlinorm.enums.ColorPrintCode
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KronosDataSourceWrapper


/**
 * 查询数据库中指定表是否存在。
 * 根据不同的数据库类型构造并执行相应的 SQL 查询，返回查询结果表示表是否存在。
 *
 * @param tableName String 表名。
 * @return Boolean 表示表是否存在的布尔值。
 */
fun queryTableExistence(tableName: String, dataSource: KronosDataSourceWrapper): Boolean = (dataSource.forObject(
    KronosAtomicQueryTask(
        getTableExistenceSql(dataSource.dbType), mapOf(
            "tableName" to tableName,
            "dbName" to getDBNameFrom(dataSource)
        )
    ),
    Int::class
) as Int) > 0


data class TableColumnDiff(
    val toAdd: List<Field>,
    val toModified: List<Field>,
    val toDelete: List<Field>
)


data class TableIndexDiff(
    val toAdd: List<KTableIndex>,
    val toDelete: List<KTableIndex>
)

fun differ(
    dbType: DBType,
    expect: List<Field>,
    current: List<Field>
): TableColumnDiff {
    val toAdd = expect.filter { col -> col.columnName !in current.map { it.columnName } }
    val toModified = expect.filter { col ->
        val tableColumn = current.find { col.columnName == it.columnName }
        tableColumn == null ||
                columnCreateDefSql(dbType, col) != columnCreateDefSql(dbType, tableColumn)
    }.filter {
        // 筛掉不在current中的列
        if (it.columnName !in current.map { f -> f.columnName }) {
            return@filter false
        } else {
            return@filter true
        }
    }
    val toDelete = current.filter { col -> col.columnName !in expect.map { it.columnName } }
    return TableColumnDiff(toAdd, toModified, toDelete)
}

fun TableColumnDiff.doLog(tableName: String) {
    defaultLogger("tableSync").info(
        arrayOf(
            kMsgOf(
                "start sync table $tableName:"
            ).endl(),
            kMsgOf(
                "Add fields\t"
            ),
            kMsgOf(
                toAdd.joinToString(", ") { it.columnName }.ifEmpty { "None" },
                ColorPrintCode.GREEN
            ).endl(),
            kMsgOf(
                "Modify fields\t"
            ),
            kMsgOf(
                toModified.joinToString(", ") { it.columnName }.ifEmpty { "None" },
                ColorPrintCode.YELLOW
            ).endl(),
            kMsgOf(
                "Delete fields\t"
            ),
            kMsgOf(
                toDelete.joinToString(", ") { it.columnName }.ifEmpty { "None" },
                ColorPrintCode.RED
            ).endl()
        )
    )
}