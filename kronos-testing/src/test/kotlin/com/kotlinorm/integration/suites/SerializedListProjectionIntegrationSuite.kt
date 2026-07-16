package com.kotlinorm.integration.suites

import com.kotlinorm.Kronos
import com.kotlinorm.beans.generator.customIdGenerator
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.integration.fixtures.IntegrationSerializedListProjectionUser
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.interfaces.KIdGenerator
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosSerializeProcessor
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.createInstance
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
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
        val originalProcessor = Kronos.serializeProcessor
        val originalIdGenerator = customIdGenerator
        val table = IntegrationSerializedListProjectionUser()

        try {
            Kronos.serializeProcessor = KotlinxIntegrationSerializeProcessor
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
                .withTotal()
                .page(pi = 1, ps = 10)
                .toList()

            assertEquals(3, total)
            assertEquals(1, pageCount)
            assertEquals(
                listOf(listOf("Quinn"), listOf("Frankie"), listOf("Cameron")),
                pageUsers.map { it.list },
            )
            KotlinxIntegrationSerializeProcessor.clearTrace()

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

            @Suppress("UNCHECKED_CAST")
            val rankedClass = ranked.build(wrapper).atomicTask.targetType.classifier as KClass<out KPojo>
            val rankedListField = rankedClass.createInstance().__columns.single { it.name == "list" }
            val derived = ranked
                .select()
                .where { it.rn == 1 }
                .orderBy { it.id.asc() }
            @Suppress("UNCHECKED_CAST")
            val derivedClass = derived.build(wrapper).atomicTask.targetType.classifier as KClass<out KPojo>
            val derivedListField = derivedClass.createInstance().__columns.single { it.name == "list" }
            val rankedUsers = derived.toList()

            assertEquals(listOf("user-2", "user-3"), rankedUsers.map { it.id })
            assertEquals(
                listOf(listOf("Frankie"), listOf("Cameron")),
                rankedUsers.map { it.list },
                "first projection serializable=${rankedListField.serializable}, " +
                    "derived projection serializable=${derivedListField.serializable}, " +
                    "deserialize returned ${KotlinxIntegrationSerializeProcessor.deserializedValues}",
            )
        } finally {
            Kronos.serializeProcessor = originalProcessor
            customIdGenerator = originalIdGenerator
            wrapper.table.dropTable(table)
        }
    }
}

private object KotlinxIntegrationSerializeProcessor : KronosSerializeProcessor {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    val deserializedValues = mutableListOf<Any>()

    fun clearTrace() = deserializedValues.clear()

    override fun deserialize(serializedStr: String, kType: KType): Any {
        return (json.decodeFromString(serializer(kType), serializedStr)
            ?: error("Kotlinx serialization returned null for $kType")
        ).also(deserializedValues::add)
    }

    override fun serialize(obj: Any, kType: KType): String {
        @Suppress("UNCHECKED_CAST")
        val valueSerializer = serializer(kType) as KSerializer<Any>
        return json.encodeToString(valueSerializer, obj)
    }
}
