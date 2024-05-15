package com.kotlinorm.utils.tableCache

import com.kotlinorm.beans.dsl.Field

/**
 * Table object
 *
 * @property columns the table columns
 * @property tableName the table name
 * @constructor Create empty Table object
 * @author ousc
 * @create 2022/11/12 14:19
 */
data class TableObject(
    val columns: List<Field>,
    val tableName: String
)