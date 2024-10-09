package com.kotlinorm.orm.database

import com.kotlinorm.Kronos.defaultLogger
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.logging.KLogMessage.Companion.kMsgOf
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.SqlHandler.execute
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.getDBNameFrom
import com.kotlinorm.database.SqlManager.getTableComment
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

fun queryTableComment(tableName: String, dataSource: KronosDataSourceWrapper): String {
    return dataSource.forObject(
        KronosAtomicQueryTask(
            getTableComment(dataSource),
            mapOf(
                "tableName" to tableName,
                "dbName" to getDBNameFrom(dataSource)
            )
        ),
        String::class
    ) as String
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

fun indexDiffer(
    expect: List<KTableIndex>,
    current: List<KTableIndex>
): TableIndexDiff {

    val toAdd = expect.filter { index -> index !in current }
    val toDelete = current.filter { index -> index !in expect }

    return TableIndexDiff(toAdd, toDelete)
}

fun moveColumn(
    expect: List<Field>,
    current: List<Field>
): List<String> {

    // 取交集
    val filterdExpect =
        expect.map { it.columnName }.filter { name -> name in current.map { it.columnName } }.toMutableList()
    val filterdCurrent =
        current.map { it.columnName }.filter { name -> name in expect.map { it.columnName } }.toMutableList()

    val lFields = mutableListOf<String>()
    val rFields = mutableListOf<String>()

    val size = filterdExpect.size
    if (0 == size) return lFields

    var l = 0
    var r = size - 1
    for (i in 0 until size) {
        // 正向查找向前移动的字段
        if (filterdExpect[i] != filterdCurrent[l] && !lFields.contains(filterdCurrent[l]))
            lFields.add(filterdExpect[i])
        else {
            // 归位
            if (lFields.contains(filterdCurrent[l])) while (lFields.contains(filterdCurrent[l])) l += 1
            l += 1
        }

        // 逆向查找向后移动的字段
        if (filterdExpect[size - i - 1] != filterdCurrent[r] && !rFields.contains(filterdCurrent[r]))
            rFields.add(filterdExpect[size - i - 1])
        else {
            // 归位
            if (rFields.contains(filterdCurrent[r])) while (rFields.contains(filterdCurrent[r])) r -= 1
            r -= 1
        }
    }

    // 选择两种移动方式中移动字段较少的
    return if (lFields.size < rFields.size) lFields else rFields
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
                toAdd.joinToString(", ") { it.first.columnName }.ifEmpty { "None" },
                ColorPrintCode.GREEN
            ).endl(),
            kMsgOf(
                "Modify fields\t"
            ),
            kMsgOf(
                toModified.joinToString(", ") { it.first.columnName }.ifEmpty { "None" },
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