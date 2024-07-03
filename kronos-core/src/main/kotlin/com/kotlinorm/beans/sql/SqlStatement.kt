package com.kotlinorm.beans.sql

import com.kotlinorm.beans.dsl.Field

class SqlStatement {
    private val listOfSegments = mutableListOf<SqlSegment>()
    operator fun String.unaryPlus() {
        listOfSegments.add(SqlSegment.of(this))
    }

    operator fun Field.unaryPlus() {
        listOfSegments.add(SqlSegment.of(this))
    }

    operator fun List<Field>.unaryPlus() {
        listOfSegments.add(SqlSegment.of(this))
    }

    operator fun Set<Field>.unaryPlus() {
        listOfSegments.add(SqlSegment.of(this.toList()))
    }

    companion object {
        fun sql(segments: SqlStatement.() -> Unit) = SqlStatement().apply(segments)
    }

    fun toSqlString(): String {
        return listOfSegments.filter { it.value != null }.joinToString(" ")
    }
}