package com.kotlinorm.orm.ddl

import com.kotlinorm.Kronos.defaultLogger
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.logging.log
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getDBNameFrom
import com.kotlinorm.database.SqlManager.getTableCommentSql
import com.kotlinorm.database.SqlManager.getTableExistenceSql
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
    Int::class,
    false,
    listOf()
) as Int) > 0

fun queryTableComment(tableName: String, dataSource: KronosDataSourceWrapper): String {
    return dataSource.forObject(
        KronosAtomicQueryTask(
            getTableCommentSql(dataSource),
            mapOf(
                "tableName" to tableName,
                "dbName" to getDBNameFrom(dataSource)
            )
        ),
        String::class,
        false,
        listOf()
    ) as String? ?: ""
}


data class TableColumnDiff(
    val toAdd: List<Pair<Field, Field?>>, // 新增字段与其前一个字段
    val toModified: List<Pair<Field, Field?>>,
    val toDelete: List<Field>
)

data class TableIndexDiff(
    val toAdd: List<KTableIndex>,
    val toDelete: List<KTableIndex>
)

/**
 * Compares the expected and current table columns to determine the differences.
 *
 * This function identifies columns that need to be added, modified, or deleted
 * by comparing the expected columns with the current columns in the database.
 *
 * @param dbType The type of the database.
 * @param expect The list of expected columns.
 * @param current The list of current columns in the database.
 * @return A `TableColumnDiff` object containing the columns to add, modify, and delete.
 */
fun columnDiffer(
    dbType: DBType,
    expect: List<Field>,
    current: List<Field>
): TableColumnDiff {
    val toAdd = expect.mapIndexedNotNull { index, col ->
        if (col.columnName !in current.map { it.columnName }) {
            Pair(col, if (index == 0) null else expect[index - 1])
        } else null
    }

    val need2Move = moveColumn(expect, current)
    val toModified = expect.mapIndexedNotNull { index, col ->
        val tableColumn = current.find { col.columnName == it.columnName }
        if (tableColumn != null && (columnCreateDefSql(dbType, col) != columnCreateDefSql(dbType, tableColumn) || col.columnName in need2Move)
        ) {
            Pair(col, if (index == 0) null else expect[index - 1])
        } else null
    }

    val toDelete = current.filter { col -> col.columnName !in expect.map { it.columnName } }

    return TableColumnDiff(toAdd, toModified, toDelete)
}

/**
 * Compares the expected and current table indexes to determine the differences.
 *
 * This function identifies indexes that need to be added or deleted
 * by comparing the expected indexes with the current indexes in the database.
 *
 * @param expect The list of expected indexes.
 * @param current The list of current indexes in the database.
 * @return A `TableIndexDiff` object containing the indexes to add and delete.
 */
fun indexDiffer(
    expect: List<KTableIndex>,
    current: List<KTableIndex>
): TableIndexDiff {

    val toAdd = expect.filter { index -> index !in current }
    val toDelete = current.filter { index -> index !in expect }

    return TableIndexDiff(toAdd, toDelete)
}

/**
 * Determines the columns that need to be moved by comparing the expected and current columns.
 *
 * This function identifies columns that need to be moved to match the expected order.
 * It compares the expected columns with the current columns and returns a list of column names
 * that need to be moved.
 *
 * @param expect The list of expected columns.
 * @param current The list of current columns in the database.
 * @return A list of column names that need to be moved.
 */
fun moveColumn(
    expect: List<Field>,
    current: List<Field>
): List<String> {

    // 取交集
    val expectedNames = expect.map { it.columnName }.toSet()
    val currentNames = current.map { it.columnName }.toSet()

    val filteredExpect = expectedNames.intersect(currentNames).toList()
    val filteredCurrent = currentNames.intersect(expectedNames).toList()

    val lFields = mutableListOf<String>()
    val rFields = mutableListOf<String>()

    val size = filteredExpect.size
    if (size == 0) return lFields

    var l = 0
    var r = size - 1

    repeat(size) {
        // 正向查找向前移动的字段
        processLeft(filteredExpect, filteredCurrent, lFields, l)
        if (l < size) l++

        // 逆向查找向后移动的字段
        processRight(filteredExpect, filteredCurrent, rFields, r)
        if (r >= 0) r--
    }

    // 选择两种移动方式中移动字段较少的
    return if (lFields.size < rFields.size) lFields else rFields
}

private fun processLeft(
    filteredExpect: List<String>,
    filteredCurrent: List<String>,
    lFields: MutableList<String>,
    lStart: Int
) {
    var l = lStart
    if (l < filteredCurrent.size && filteredExpect[l] != filteredCurrent[l] && !lFields.contains(filteredCurrent[l])) {
        lFields.add(filteredExpect[l])
    } else {
        while (l < filteredCurrent.size && lFields.contains(filteredCurrent[l])) {
            l++
        }
    }
}

private fun processRight(
    filteredExpect: List<String>,
    filteredCurrent: List<String>,
    rFields: MutableList<String>,
    rStart: Int
) {
    var r = rStart
    if (r >= 0 && filteredExpect[filteredExpect.size - r - 1] != filteredCurrent[r] && !rFields.contains(filteredCurrent[r])) {
        rFields.add(filteredExpect[filteredExpect.size - r - 1])
    } else {
        while (r >= 0 && rFields.contains(filteredCurrent[r])) {
            r--
        }
    }
}


fun TableColumnDiff.doLog(tableName: String) {
    defaultLogger("tableSync").info(
        log {
            +"Start sync table $tableName:"
            -"Add fields\t"[black, bold]
            +toAdd.joinToString(", ") { it.first.columnName }.ifEmpty { "None" }[green]
            -"Modify fields\t"[black, bold]
            +toModified.joinToString(", ") { it.first.columnName }
                .ifEmpty { "None" }[yellow]
            -"Delete fields\t"[black, bold]
            +toDelete.joinToString(", ") { it.columnName }.ifEmpty { "None" }[red]
        }
    )
}