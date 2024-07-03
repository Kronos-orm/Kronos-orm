package com.kotlinorm.beans.sql

import com.kotlinorm.beans.dsl.Field

class SqlSegment(
    val type: SqlSegmentType,
    val string: String? = null,
    val field: Field? = null,
    val fieldList: List<Field>? = null
) {
    val value
        get() = when (type) {
            SqlSegmentType.STRING -> string
            SqlSegmentType.FIELD -> field?.quoted()
            SqlSegmentType.FIELD_LIST -> fieldList?.joinToString(", ") { it.quoted() }
        }

    enum class SqlSegmentType {
        STRING, FIELD, FIELD_LIST
    }

    companion object {
        fun of(value: String): SqlSegment {
            return SqlSegment(SqlSegmentType.STRING, value)
        }

        fun of(value: Field): SqlSegment {
            return SqlSegment(SqlSegmentType.FIELD, field = value)
        }

        fun of(value: List<Field>): SqlSegment {
            return SqlSegment(SqlSegmentType.FIELD_LIST, fieldList = value)
        }
    }
}