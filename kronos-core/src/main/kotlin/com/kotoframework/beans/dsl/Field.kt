package com.kotoframework.beans.dsl

import com.kotoframework.utils.fieldDb2k

class Field(
    val columnName: String,
    val name: String = fieldDb2k(columnName)
){
    override fun toString(): String {
        return name
    }
    fun quotedColumnName(): String = "`$columnName`"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Field

        if (columnName != other.columnName) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = columnName.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}