package com.kotlinorm.database

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.database.postgres.PostgresqlSupport.getColumnType
import com.kotlinorm.database.postgres.PostgresqlSupport.getColumnCreateSql
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KPojo
import kotlin.test.Test
import kotlin.test.assertEquals

class PostgresTest {
    data class Account(
        @PrimaryKey(identity = true)
        val id: Long? = null
    ): KPojo

    @Test
    fun testLongColumnTypeInference() {
        val column = Account().kronosColumns()[0]
        val columnType = getColumnType(column.type, column.length, column.scale)
        assertEquals("BIGINT", columnType)
        assertEquals("\"id\" BIGSERIAL NOT NULL PRIMARY KEY",getColumnCreateSql(DBType.Postgres, column))
    }
}