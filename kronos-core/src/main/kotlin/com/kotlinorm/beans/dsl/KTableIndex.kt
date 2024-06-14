package com.kotlinorm.beans.dsl

data class KTableIndex(
    val name: String,
    val columns: Array<String>,
    val type: String,
    val method: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KTableIndex

        if (name != other.name) return false
        if (!columns.contentEquals(other.columns)) return false
        if (type != other.type) return false
        if (method != other.method) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + columns.contentHashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + method.hashCode()
        return result
    }
}