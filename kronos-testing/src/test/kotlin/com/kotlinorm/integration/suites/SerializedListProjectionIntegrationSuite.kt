package com.kotlinorm.integration.suites

import com.kotlinorm.Kronos
import com.kotlinorm.beans.generator.customIdGenerator
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.integration.fixtures.IntegrationSerializedListProjectionUser
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.interfaces.KIdGenerator
import com.kotlinorm.interfaces.serializedValueCodec
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class SerializedListProjectionIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun serializedListSurvivesWindowDerivedProjectionAgainstRealDatabase() {
        requireDatabaseAvailable()
        configureKronos()
        val originalIdGenerator = customIdGenerator
        val table = IntegrationSerializedListProjectionUser()
        val serializationRegistration = Kronos.registerValueCodec(serializedValueCodec(
            encode = KotlinxIntegrationFormat::encode,
            decode = KotlinxIntegrationFormat::decode
        ))

        try {
            val generatedIds = listOf("user-1", "user-2", "user-3").iterator()
            customIdGenerator = object : KIdGenerator<String> {
                override fun nextId(): String = generatedIds.next()
            }
            wrapper.table.dropTable(table)
            wrapper.table.createTable(table)

            listOf(
                IntegrationSerializedListProjectionUser("user-1", "Quinn", 15, listOf("Quinn")),
                IntegrationSerializedListProjectionUser("user-2", "Frankie", 15, listOf("Frankie")),
                IntegrationSerializedListProjectionUser("user-3", "Cameron", 21, listOf("Cameron")),
            ).forEach { assertEquals(1, it.insert().execute().affectedRows) }

            val (total, pageUsers, pageCount) = IntegrationSerializedListProjectionUser()
                .select()
                .orderBy { it.id.asc() }
                .page(pageIndex = 1, pageSize = 10)
                .withTotal()
                .toList()

            assertEquals(3, total)
            assertEquals(1, pageCount)
            assertEquals(
                listOf(listOf("Quinn"), listOf("Frankie"), listOf("Cameron")),
                pageUsers.map { it.list },
            )
            KotlinxIntegrationFormat.clearTrace()

            val ranked = IntegrationSerializedListProjectionUser()
                .select {
                    [
                        it.id,
                        it.userName,
                        it.age,
                        it.list,
                        f.rowNumber()
                            .over {
                                partitionBy(it.age)
                                orderBy(it.id.desc())
                            }
                            .alias("rn"),
                    ]
                }

            val rankedType = ranked.build(wrapper).atomicTask.targetType
            val rankedListField = Kronos.createKPojo(rankedType).__columns.single { it.name == "list" }
            val derived = ranked
                .select()
                .where { it.rn == 1 }
                .orderBy { it.id.asc() }
            val derivedType = derived.build(wrapper).atomicTask.targetType
            val derivedListField = Kronos.createKPojo(derivedType).__columns.single { it.name == "list" }
            val rankedUsers = derived.toList()

            assertEquals(listOf("user-2", "user-3"), rankedUsers.map { it.id })
            assertEquals(
                listOf(listOf("Frankie"), listOf("Cameron")),
                rankedUsers.map { it.list },
                "first projection serializable=${rankedListField.serializable}, " +
                    "derived projection serializable=${derivedListField.serializable}, " +
                    "decode returned ${KotlinxIntegrationFormat.decodedValues}",
            )
        } finally {
            serializationRegistration.close()
            customIdGenerator = originalIdGenerator
            wrapper.table.dropTable(table)
        }
    }
}

private object KotlinxIntegrationFormat {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    val decodedValues = mutableListOf<Any>()

    fun clearTrace() = decodedValues.clear()

    fun decode(serializedStr: String, kType: KType): Any {
        return (json.decodeFromString(serializer(kType), serializedStr)
            ?: error("Kotlinx serialization returned null for $kType")
        ).also(decodedValues::add)
    }

    fun encode(obj: Any, kType: KType): String {
        @Suppress("UNCHECKED_CAST")
        val valueSerializer = serializer(kType) as KSerializer<Any>
        return json.encodeToString(valueSerializer, obj)
    }
}
