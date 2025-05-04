package com.kotlinorm.plugins

import com.kotlinorm.Kronos
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.DBType
import com.kotlinorm.orm.insert.InsertClause
import com.kotlinorm.plugins.LastInsertIdPlugin.withId
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LastInsertIdTest {
    @Test
    fun lastInsertIdObtainSqlReturnsCorrectSqlForSupportedDbTypes() {
        assertEquals("SELECT LAST_INSERT_ID()", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.Mysql))
        assertEquals("SELECT LAST_INSERT_ID()", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.H2))
        assertEquals("SELECT LAST_INSERT_ID()", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.OceanBase))
        assertEquals("SELECT * FROM DUAL", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.Oracle))
        assertEquals("SELECT SCOPE_IDENTITY()", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.Mssql))
        assertEquals("SELECT LASTVAL()", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.Postgres))
        assertEquals(
            "SELECT IDENTITY_VAL_LOCAL() FROM SYSIBM.SYSDUMMY1", LastInsertIdPlugin.lastInsertIdObtainSql(
                DBType.DB2
            )
        )
        assertEquals("SELECT @@IDENTITY", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.Sybase))
        assertEquals("SELECT last_insert_rowid()", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.SQLite))
    }

    @Test
    fun lastInsertIdObtainSqlThrowsExceptionForUnsupportedDbType() {
        assertFailsWith<UnsupportedOperationException> { LastInsertIdPlugin.lastInsertIdObtainSql(DBType.Unknown) }
    }
}