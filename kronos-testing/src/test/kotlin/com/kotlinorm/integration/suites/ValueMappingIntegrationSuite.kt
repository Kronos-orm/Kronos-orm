package com.kotlinorm.integration.suites

import com.kotlinorm.Kronos
import com.kotlinorm.database.SqlExecutor.query
import com.kotlinorm.integration.fixtures.IntegrationValueMappingRecord
import com.kotlinorm.integration.fixtures.IntegrationValueMappingRow
import com.kotlinorm.integration.fixtures.IntegrationValueMappingStatus
import com.kotlinorm.integration.fixtures.IntegrationNumericEnumRow
import com.kotlinorm.integration.fixtures.IntegrationNumericEnumStatus
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.interfaces.serializedValueCodec
import com.kotlinorm.interfaces.valueCodec
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.Extensions.safeMapperTo
import java.time.LocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

abstract class ValueMappingIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun enumSerializedListAndFormattedDateRoundTripThroughTypedResultsAgainstRealDatabase() {
        requireDatabaseAvailable()
        configureKronos()
        val table = IntegrationValueMappingRow()
        val historyType = typeOf<List<IntegrationValueMappingStatus?>?>()
        val encodedTypes = mutableListOf<KType>()
        val decodedTypes = mutableListOf<KType>()
        val serializationRegistration = Kronos.registerValueCodec(serializedValueCodec(
            encode = { value, type ->
                encodedTypes += type
                ValueMappingJson.encode(value, type)
            },
            decode = { text, type ->
                decodedTypes += type
                ValueMappingJson.decode(text, type)
            }
        ))
        val expectedRows = listOf(
            IntegrationValueMappingRow(
                id = 1,
                status = IntegrationValueMappingStatus.READY,
                nullableStatus = IntegrationValueMappingStatus.ARCHIVED,
                history = listOf(IntegrationValueMappingStatus.READY, null, IntegrationValueMappingStatus.ARCHIVED),
                occurredAt = LocalDateTime.of(2026, 7, 21, 13, 14, 15),
            ),
            IntegrationValueMappingRow(
                id = 2,
                status = IntegrationValueMappingStatus.ARCHIVED,
                nullableStatus = null,
                history = listOf(IntegrationValueMappingStatus.ARCHIVED),
                occurredAt = LocalDateTime.of(2026, 7, 22, 1, 2, 3),
            ),
        )

        try {
            wrapper.table.dropTable(table)
            wrapper.table.createTable(table)
            expectedRows.forEach { row ->
                assertEquals(1, row.insert().execute().affectedRows)
            }
            assertEquals(listOf(historyType, historyType), encodedTypes)

            val rawRows = wrapper.query(
                "SELECT status, nullable_status, history, occurred_at " +
                    "FROM kt_integration_value_mapping ORDER BY id"
            )
            assertEquals(
                listOf(
                    listOf("READY", "ARCHIVED", "[\"READY\",null,\"ARCHIVED\"]", "21/07/2026 13:14:15"),
                    listOf("ARCHIVED", null, "[\"ARCHIVED\"]", "22/07/2026 01:02:03"),
                ),
                rawRows.map { row ->
                    listOf(
                        row.value("status"),
                        row.value("nullable_status"),
                        row.value("history"),
                        row.value("occurred_at"),
                    )
                }
            )
            assertEquals(emptyList(), decodedTypes)

            val pojoRows = table.select()
                .orderBy { it.id.asc() }
                .toList<IntegrationValueMappingRow>()
            assertEquals(expectedRows, pojoRows)
            assertFalse(pojoRows[0] === pojoRows[1])
            assertEquals(listOf(historyType, historyType), decodedTypes)

            decodedTypes.clear()
            val scalarStatuses = table.select { it.status }
                .orderBy { it.id.asc() }
                .toList<IntegrationValueMappingStatus>()
            assertEquals(
                listOf(IntegrationValueMappingStatus.READY, IntegrationValueMappingStatus.ARCHIVED),
                scalarStatuses
            )
            assertEquals(emptyList(), decodedTypes)

            val projection = table.select {
                [it.id, it.status, it.nullableStatus, it.history, it.occurredAt]
            }.orderBy { it.id.asc() }
            val projectionRows = projection.toList()
            assertEquals(expectedRows.map(IntegrationValueMappingRow::toRecord), projectionRows.map { row ->
                IntegrationValueMappingRecord(
                    row.id,
                    row.status,
                    row.nullableStatus,
                    row.history,
                    row.occurredAt,
                )
            })
            assertEquals(listOf(historyType, historyType), decodedTypes)

            decodedTypes.clear()
            val mapRows = projection.toMapList()
            assertEquals(
                expectedRows.map { row ->
                    linkedMapOf<String, Any?>(
                        "id" to row.id,
                        "status" to row.status,
                        "nullableStatus" to row.nullableStatus,
                        "history" to row.history,
                        "occurredAt" to row.occurredAt,
                    )
                },
                mapRows
            )
            assertEquals(listOf(historyType, historyType), decodedTypes)
        } finally {
            try {
                wrapper.table.dropTable(table)
            } finally {
                serializationRegistration.close()
            }
        }
    }

    /**
     * Exercises field-aware built-in enum ordinal conversion against SQLite's integer affinity.
     *
     * Integer column metadata selects ordinal storage while string-compatible fields retain
     * the default name protocol. The same metadata must survive parameter binding, safe Map
     * mapping, JDBC KPojo reads and generated projections. It is invoked only by the SQLite
     * integration test because the test's purpose is the SQLite physical integer round trip.
     */
    protected fun verifyNumericEnumFieldMetadataAgainstSqlite() {
        requireDatabaseAvailable()
        configureKronos()
        val table = IntegrationNumericEnumRow()
        try {
            wrapper.table.dropTable(table)
            wrapper.table.createTable(table)
            listOf(
                IntegrationNumericEnumRow(
                    id = 1,
                    intStatus = IntegrationNumericEnumStatus.ACTIVE,
                    bigStatus = IntegrationNumericEnumStatus.ARCHIVED,
                ),
                IntegrationNumericEnumRow(
                    id = 2,
                    intStatus = IntegrationNumericEnumStatus.ARCHIVED,
                    bigStatus = null,
                ),
            ).forEach { row ->
                assertEquals(1, row.insert().execute().affectedRows)
            }

            val rawRows = wrapper.query(
                "SELECT id, int_status, big_status FROM kt_integration_numeric_enum ORDER BY id"
            )
            assertEquals(listOf(0L, 1L), rawRows.map { (it.value("int_status") as Number).toLong() })
            assertEquals(listOf(1L, null), rawRows.map { it.value("big_status")?.let { value -> (value as Number).toLong() } })

            val typedRows = table.select().orderBy { it.id.asc() }.toList<IntegrationNumericEnumRow>()
            assertEquals(
                listOf(
                    IntegrationNumericEnumStatus.ACTIVE to IntegrationNumericEnumStatus.ARCHIVED,
                    IntegrationNumericEnumStatus.ARCHIVED to null,
                ),
                typedRows.map { it.intStatus to it.bigStatus },
            )

            val scalarStatuses = table.select { it.intStatus }
                .orderBy { it.id.asc() }
                .toList<IntegrationNumericEnumStatus>()
            assertEquals(
                listOf(
                    IntegrationNumericEnumStatus.ACTIVE,
                    IntegrationNumericEnumStatus.ARCHIVED,
                ),
                scalarStatuses,
            )

            val intInRows = table.select()
                .where { listOf(IntegrationNumericEnumStatus.ACTIVE).contains(it.intStatus) }
                .toList<IntegrationNumericEnumRow>()
            assertEquals(listOf(1), intInRows.map { it.id })

            val bigInRows = table.select()
                .where { listOf(IntegrationNumericEnumStatus.ARCHIVED).contains(it.bigStatus) }
                .toList<IntegrationNumericEnumRow>()
            assertEquals(listOf(1), bigInRows.map { it.id })

            val safeMapped = mapOf<String, Any?>(
                "id" to 9,
                "intStatus" to 1,
                "bigStatus" to 1L,
            ).safeMapperTo<IntegrationNumericEnumRow>()
            assertEquals(IntegrationNumericEnumStatus.ARCHIVED, safeMapped.intStatus)
            assertEquals(IntegrationNumericEnumStatus.ARCHIVED, safeMapped.bigStatus)

            val projection = table.select { [it.id, it.intStatus, it.bigStatus] }
                .orderBy { it.id.asc() }
            val projectionRows = projection.toList()
            assertEquals(listOf(1, 2), projectionRows.map { it.id })
            assertEquals(
                listOf(
                    IntegrationNumericEnumStatus.ACTIVE to IntegrationNumericEnumStatus.ARCHIVED,
                    IntegrationNumericEnumStatus.ARCHIVED to null,
                ),
                projectionRows.map { it.intStatus to it.bigStatus },
            )
            assertEquals(
                listOf(
                    IntegrationNumericEnumStatus.ACTIVE to IntegrationNumericEnumStatus.ARCHIVED,
                    IntegrationNumericEnumStatus.ARCHIVED to null,
                ),
                projection.toMapList().map { it["intStatus"] to it["bigStatus"] },
            )

            val overrideRegistration = Kronos.registerValueCodec(
                valueCodec(
                    supports = { value, context ->
                        context.direction == ValueCodecDirection.ENCODE &&
                            context.storage == ValueStorage.NONE &&
                            context.targetType == typeOf<IntegrationNumericEnumStatus>() &&
                            context.field?.type == KColumnType.INT &&
                            value is IntegrationNumericEnumStatus
                    },
                    convert = { value, _ -> "override:${(value as IntegrationNumericEnumStatus).name}" },
                )
            )
            try {
                val parameters = IntegrationNumericEnumRow(
                    id = 10,
                    intStatus = IntegrationNumericEnumStatus.ACTIVE,
                ).insert().build(wrapper).component2()
                assertEquals("override:ACTIVE", parameters["intStatus"])
            } finally {
                overrideRegistration.close()
            }
        } finally {
            wrapper.table.dropTable(table)
        }
    }
}

private fun IntegrationValueMappingRow.toRecord() = IntegrationValueMappingRecord(
    id,
    status,
    nullableStatus,
    history,
    occurredAt,
)

private object ValueMappingJson {
    private val json = Json

    fun decode(serializedText: String, type: KType): Any =
        json.decodeFromString(serializer(type), serializedText)
            ?: error("Kotlinx serialization returned null for $type")

    fun encode(value: Any, type: KType): String {
        @Suppress("UNCHECKED_CAST")
        val valueSerializer = serializer(type) as KSerializer<Any>
        return json.encodeToString(valueSerializer, value)
    }
}
