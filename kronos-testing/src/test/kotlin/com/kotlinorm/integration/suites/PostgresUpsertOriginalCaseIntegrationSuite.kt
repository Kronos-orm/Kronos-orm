package com.kotlinorm.integration.suites

import com.kotlinorm.Kronos
import com.kotlinorm.beans.generator.customIdGenerator
import com.kotlinorm.database.SqlExecutor.query
import com.kotlinorm.integration.fixtures.IntegrationSerializedListProjectionUser
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.interfaces.KIdGenerator
import com.kotlinorm.interfaces.KronosSerializeProcessor
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.upsert.upsert
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class PostgresUpsertOriginalCaseIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun onConflictUsesCustomPrimaryKeyMetadataAndGeneratorAgainstRealDatabase() {
        withOriginalUpsertTable {
            val result = originalUser("Ada")
                .upsert()
                .onConflict()
                .execute()

            assertEquals(1, result.affectedRows)
            assertEquals("generated-1", insertedIds().single())
        }
    }

    @Test
    fun onPrimaryKeyDoesNotApplyForUpdateToAggregateProbeAgainstRealDatabase() {
        withOriginalUpsertTable {
            val result = originalUser("Grace")
                .upsert()
                .on { listOf(it.id) }
                .execute()

            assertEquals(1, result.affectedRows)
            assertEquals("generated-1", insertedIds().single())
        }
    }

    private fun insertedIds(): List<String> = wrapper.query(
        "SELECT ${quote("id")} FROM ${table("kt_serialized_list_projection_user$TABLE_SUFFIX")}",
    ).map { row -> row.value("id") as String }

    private fun originalUser(name: String) = IntegrationSerializedListProjectionUser(
        userName = name,
        age = 15,
        list = listOf(name),
    ).apply {
        __tableName += TABLE_SUFFIX
    }

    private inline fun withOriginalUpsertTable(block: () -> Unit) {
        requireDatabaseAvailable()
        configureKronos()
        val originalProcessor = Kronos.serializeProcessor
        val originalIdGenerator = customIdGenerator
        val table = originalUser("table-shape")

        try {
            Kronos.serializeProcessor = UpsertKotlinxSerializeProcessor
            customIdGenerator = object : KIdGenerator<String> {
                private var sequence = 0

                override fun nextId(): String = "generated-${++sequence}"
            }
            wrapper.table.dropTable(table)
            wrapper.table.createTable(table)
            block()
        } finally {
            Kronos.serializeProcessor = originalProcessor
            customIdGenerator = originalIdGenerator
            wrapper.table.dropTable(table)
        }
    }

    private companion object {
        const val TABLE_SUFFIX = "_001"
    }
}

private object UpsertKotlinxSerializeProcessor : KronosSerializeProcessor {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun deserialize(serializedStr: String, kType: KType): Any {
        return json.decodeFromString(serializer(kType), serializedStr)
            ?: error("Kotlinx serialization returned null for $kType")
    }

    override fun serialize(obj: Any, kType: KType): String {
        @Suppress("UNCHECKED_CAST")
        val valueSerializer = serializer(kType) as KSerializer<Any>
        return json.encodeToString(valueSerializer, obj)
    }
}
