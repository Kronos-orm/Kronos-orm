package com.kotlinorm.integration.suites

import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.integration.fixtures.SchemaSyncUserV1
import com.kotlinorm.integration.fixtures.SchemaSyncUserV2
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.queryTableColumns
import com.kotlinorm.orm.ddl.table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class SchemaSyncRegressionSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun syncTableAddsColumnAfterCustomStringPrimaryKeyAndTimestampColumns() {
        requireDatabaseAvailable()
        configureKronos()

        wrapper.table.dropTable(SchemaSyncUserV1())
        wrapper.table.createTable(SchemaSyncUserV1())

        val existing = wrapper.table.syncTable(SchemaSyncUserV2())
        val existingAfterSecondSync = wrapper.table.syncTable(SchemaSyncUserV2())

        assertTrue(existing)
        assertTrue(existingAfterSecondSync)
        val columns = queryTableColumns("kt_schema_sync_user", wrapper)
        val columnNames = columns.map { it.columnName.lowercase() }.toSet()
        val age = columns.single { it.columnName.equals("age", ignoreCase = true) }
        val id = columns.single { it.columnName.equals("id", ignoreCase = true) }

        assertEquals(setOf("id", "name", "age", "create_time", "update_time"), columnNames)
        assertEquals(if (wrapper.dbType == DBType.SQLite) KColumnType.INT else KColumnType.BIGINT, age.type)
        assertEquals(true, age.nullable)
        assertEquals(PrimaryKeyType.NOT, age.primaryKey)
        assertTrue(id.primaryKey != PrimaryKeyType.NOT)
    }
}
