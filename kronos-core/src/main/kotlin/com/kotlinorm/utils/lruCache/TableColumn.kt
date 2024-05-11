package com.kotlinorm.utils.lruCache

/**
 * Created by sundaiyue on 2022/11/12 14:19
 */

/**
 * Table column
 * @property name
 * @property type
 * @constructor Create empty Table column
 * @author ousc
 */
data class TableColumn(
    var name: String,
    var type: String,
    var primaryKey: Boolean = false
)