package com.kotlinorm.utils.lruCache

/**
 * Table object
 *
 * @property fields the table columns
 * @property tableName the table name
 * @constructor Create empty Table object
 * @author ousc
 * @create 2022/11/12 14:19
 */
data class TableObject(
    val fields: List<TableColumn>,
    val tableName: String
)