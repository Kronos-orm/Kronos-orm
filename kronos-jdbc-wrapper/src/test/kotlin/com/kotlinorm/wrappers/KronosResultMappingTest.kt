/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.wrappers

import com.kotlinorm.Kronos
import com.kotlinorm.beans.UnsupportedTypeException
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.ResultColumnMetadata
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.exceptions.InvalidKPojoFactoryResult
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.serializedValueCodec
import com.kotlinorm.interfaces.valueCodec
import com.kotlinorm.utils.KPojoFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.math.BigDecimal
import java.sql.Blob
import java.sql.Clob
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLXML
import java.sql.Types
import java.time.LocalDateTime
import org.postgresql.util.PGobject
import kotlin.reflect.KType
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.reflect.typeOf

class KronosResultMappingTest {
    @Test
    fun `oracle number boolean mapping reads numeric value before driver object conversion`() {
        val metaData = mockk<ResultSetMetaData>()
        every { metaData.getColumnType(1) } returns Types.NUMERIC
        every { metaData.getColumnTypeName(1) } returns "NUMBER"
        every { metaData.getColumnLabel(1) } returns "flag_value"

        val resultSet = mockk<ResultSet>()
        every { resultSet.metaData } returns metaData
        every { resultSet.next() } returnsMany listOf(true, false)
        every { resultSet.getBigDecimal(1) } returns BigDecimal.ZERO
        every { resultSet.getObject(1) } returns true

        val context = KronosStatementContext(
            originalSql = "SELECT flag_value FROM kt_integration_typed_value",
            jdbcSql = "SELECT flag_value FROM kt_integration_typed_value",
            params = emptyList(),
            parameterNames = emptyList(),
            operationType = KOperationType.SELECT,
            dbType = DBType.Oracle,
            databaseProductName = "Oracle",
            config = KronosJdbcConfig(DBType.Oracle, "Oracle", "jdbc:oracle:thin:@localhost:1521/FREEPDB1", "Oracle JDBC")
        )

        val values = KronosResultMappers.toList(resultSet, typeOf<Boolean>(), context)

        assertEquals(listOf(false), values)
        verify(exactly = 0) { resultSet.getObject(1) }
    }

    @Test
    fun `map rows use query result column types case insensitively`() {
        val resultSet = numberResultSet(BigDecimal("5"), "ID")
        val context = oracleContext()
        val task = KronosAtomicQueryTask(
            sql = "SELECT id FROM kt_integration_user",
            targetType = typeOf<Map<String, Any?>>(),
            resultColumns = mapOf("id" to ResultColumnMetadata(typeOf<Int?>(), columnLabel = "id"))
        )

        val rows = KronosResultMappers.toList(resultSet, task, context)

        assertEquals(5, (rows.single() as Map<*, *>)["ID"])
    }

    @Test
    fun `map rows preserve allocated duplicate projection labels and target types`() {
        val metaData = mockk<ResultSetMetaData>()
        every { metaData.columnCount } returns 2
        every { metaData.getColumnLabel(1) } returns "id"
        every { metaData.getColumnLabel(2) } returns "id_1"
        every { metaData.getColumnTypeName(1) } returns "NUMBER"
        every { metaData.getColumnTypeName(2) } returns "NUMBER"

        val resultSet = mockk<ResultSet>()
        every { resultSet.metaData } returns metaData
        every { resultSet.next() } returnsMany listOf(true, false)
        every { resultSet.getBigDecimal(1) } returns BigDecimal("5")
        every { resultSet.getBigDecimal(2) } returns BigDecimal("6")

        val task = KronosAtomicQueryTask(
            sql = "SELECT id, id AS id_1 FROM kt_integration_user",
            targetType = typeOf<Map<String, Any?>>(),
            resultColumns = mapOf(
                "id" to ResultColumnMetadata(typeOf<Int?>(), columnLabel = "id"),
                "id_1" to ResultColumnMetadata(typeOf<Long?>(), columnLabel = "id_1"),
            )
        )

        val rows = KronosResultMappers.toList(resultSet, task, oracleContext())

        assertEquals(
            linkedMapOf<String, Any?>("id" to 5, "id_1" to 6L),
            rows.single()
        )
    }

    @Test
    fun `map rows without query column types preserve jdbc numeric values`() {
        val rawValue = BigDecimal("5")
        val resultSet = numberResultSet(rawValue, "ID")
        val context = oracleContext()
        val task = KronosAtomicQueryTask(
            sql = "SELECT id FROM kt_integration_user",
            targetType = typeOf<Map<String, Any?>>()
        )

        val rows = KronosResultMappers.toList(resultSet, task, context)

        assertEquals(rawValue, (rows.single() as Map<*, *>)["ID"])
    }

    @Test
    fun `Map star and Any values bypass semantic codecs`() {
        var conversions = 0
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, context ->
                context.direction == ValueCodecDirection.DECODE &&
                    context.origin == ValueCodecOrigin.DATABASE &&
                    context.targetType in setOf(typeOf<Any>(), typeOf<Any?>())
            },
            convert = { _, _ ->
                conversions++
                "converted"
            }
        ))
        try {
            val rowsByType = listOf(
                typeOf<Map<String, *>>(),
                typeOf<Map<String, Any>>(),
                typeOf<Map<String, Any?>>()
            ).map { targetType ->
                KronosResultMappers.toList(
                    singleColumnResultSet(listOf("raw"), "payload"),
                    targetType,
                    genericContext()
                )
            }

            assertEquals(
                List(3) { listOf(mapOf("payload" to "raw")) },
                rowsByType
            )
            assertEquals(0, conversions)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `typed scalar invokes the unified database codec exactly once`() {
        var conversions = 0
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, context ->
                context.direction == ValueCodecDirection.DECODE &&
                    context.origin == ValueCodecOrigin.DATABASE &&
                    context.targetType == typeOf<DatabaseMarker>()
            },
            convert = { value, _ ->
                conversions++
                DatabaseMarker(value.toString())
            }
        ))
        try {
            val task = KronosAtomicQueryTask(
                sql = "SELECT marker",
                targetType = typeOf<DatabaseMarker>(),
                resultColumns = mapOf(
                    "marker" to ResultColumnMetadata(typeOf<DatabaseMarker>(), columnLabel = "marker")
                )
            )

            val rows = KronosResultMappers.toList(
                singleColumnResultSet(listOf("raw"), "marker"),
                task,
                genericContext()
            )

            assertEquals(listOf(DatabaseMarker("raw")), rows)
            assertEquals(1, conversions)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `untyped scalar Any bypasses semantic codecs without result metadata`() {
        var conversions = 0
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, context ->
                context.direction == ValueCodecDirection.DECODE &&
                    context.origin == ValueCodecOrigin.DATABASE &&
                    context.targetType == typeOf<Any?>()
            },
            convert = { _, _ ->
                conversions++
                "converted"
            }
        ))
        try {
            val rows = KronosResultMappers.toList(
                singleColumnResultSet(listOf("raw"), "payload"),
                typeOf<Any?>(),
                genericContext()
            )

            assertEquals(listOf("raw"), rows)
            assertEquals(0, conversions)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `scalar enum decoding uses generated metadata in strict and non strict modes`() {
        val previousStrict = Kronos.strictSetValue
        try {
            listOf(false, true).forEach { strict ->
                Kronos.strictSetValue = strict
                val rows = KronosResultMappers.toList(
                    singleColumnResultSet(listOf("READY"), "statusAlias"),
                    typeOf<JdbcStatus>(),
                    genericContext()
                )

                assertEquals(listOf(JdbcStatus.READY), rows)
            }
        } finally {
            Kronos.strictSetValue = previousStrict
        }
    }

    @Test
    fun `typed Map value type decodes enum and date aliases without field metadata`() {
        val enumTask = KronosAtomicQueryTask(
            sql = "SELECT status AS statusAlias",
            targetType = typeOf<Map<String, JdbcStatus>>()
        )
        val dateTask = KronosAtomicQueryTask(
            sql = "SELECT created_at AS createdAlias",
            targetType = typeOf<Map<String, LocalDateTime>>()
        )

        val enumRows = KronosResultMappers.toList(
            singleColumnResultSet(listOf("ARCHIVED"), "statusAlias"),
            enumTask,
            genericContext()
        )
        val dateRows = KronosResultMappers.toList(
            singleColumnResultSet(listOf("2026-07-21 13:14:15"), "createdAlias"),
            dateTask,
            genericContext()
        )

        assertEquals(listOf(mapOf("statusAlias" to JdbcStatus.ARCHIVED)), enumRows)
        assertEquals(
            listOf(mapOf("createdAlias" to LocalDateTime.of(2026, 7, 21, 13, 14, 15))),
            dateRows
        )
    }

    @Test
    fun `typed Map value type overrides planner type while preserving field storage metadata`() {
        val requestedType = typeOf<Int>()
        val plannerType = typeOf<DatabaseMarker>()
        val field = Field(
            columnName = "payload",
            name = "payload",
            kType = plannerType,
            serializable = true,
        )
        val seenTargets = mutableListOf<KType>()
        val seenFields = mutableListOf<Field?>()
        val seenStorage = mutableListOf<ValueStorage>()
        val supportedTypes = setOf(requestedType, plannerType)
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { _, context ->
                context.direction == ValueCodecDirection.DECODE &&
                    context.origin == ValueCodecOrigin.DATABASE &&
                    context.targetType in supportedTypes
            },
            convert = { value, context ->
                seenTargets += context.targetType
                seenFields += context.field
                seenStorage += context.storage
                if (context.targetType == requestedType) {
                    value.toString().toInt()
                } else {
                    DatabaseMarker(value.toString())
                }
            }
        ))
        try {
            val targetTypes = listOf(
                typeOf<Map<String, Int>>(),
                typeOf<MutableMap<String, Int>>(),
            )
            val rows = targetTypes.map { targetType ->
                KronosResultMappers.toList(
                    singleColumnResultSet(listOf("41"), "payloadAlias"),
                    KronosAtomicQueryTask(
                        sql = "SELECT payload AS payloadAlias",
                        targetType = targetType,
                        resultColumns = mapOf(
                            "payloadAlias" to ResultColumnMetadata(
                                type = plannerType,
                                field = field,
                                columnLabel = "payloadAlias",
                            )
                        )
                    ),
                    genericContext()
                )
            }

            assertEquals(
                listOf(
                    listOf(mapOf("payloadAlias" to 41)),
                    listOf(mapOf("payloadAlias" to 41)),
                ),
                rows
            )
            assertEquals(listOf(requestedType, requestedType), seenTargets)
            assertEquals(listOf(field, field), seenFields)
            assertEquals(listOf(ValueStorage.SERIALIZED, ValueStorage.SERIALIZED), seenStorage)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `custom enum codec uses INT and BIGINT field metadata for numeric database values`() {
        val statusType = typeOf<JdbcStatus>()
        val registration = Kronos.registerValueCodec(valueCodec(
            supports = { value, context ->
                context.direction == ValueCodecDirection.DECODE &&
                    context.origin == ValueCodecOrigin.DATABASE &&
                    context.targetType == statusType &&
                    context.field?.type in setOf(KColumnType.INT, KColumnType.BIGINT) &&
                    value is Number
            },
            convert = { value, _ ->
                when ((value as Number).toInt()) {
                    42 -> JdbcStatus.ARCHIVED
                    7 -> JdbcStatus.READY
                    else -> error("unknown numeric status $value")
                }
            }
        ))
        try {
            val rows = listOf(
                KColumnType.INT to 42,
                KColumnType.BIGINT to 7L,
            ).map { (columnType, rawValue) ->
                val field = Field(
                    columnName = "status",
                    name = "status",
                    type = columnType,
                    kType = statusType,
                )
                val task = KronosAtomicQueryTask(
                    sql = "SELECT status AS statusAlias",
                    targetType = typeOf<Map<String, JdbcStatus>>(),
                    resultColumns = mapOf(
                        "statusAlias" to ResultColumnMetadata(
                            type = statusType,
                            field = field,
                            columnLabel = "statusAlias",
                        )
                    )
                )
                KronosResultMappers.toList(
                    singleColumnResultSet(listOf(rawValue), "statusAlias"),
                    task,
                    genericContext()
                )
            }

            assertEquals(
                listOf(
                    listOf(mapOf("statusAlias" to JdbcStatus.ARCHIVED)),
                    listOf(mapOf("statusAlias" to JdbcStatus.READY)),
                ),
                rows
            )
        } finally {
            registration.close()
        }
    }

    @Test
    fun `nullable typealias Map with nullable String key decodes its value type`() {
        val rows = KronosResultMappers.toList(
            singleColumnResultSet(listOf("READY"), "statusAlias"),
            typeOf<NullableJdbcStatusMap?>(),
            genericContext()
        )

        assertEquals(listOf(mapOf("statusAlias" to JdbcStatus.READY)), rows)
    }

    @Test
    fun `mutable and star keyed Map targets retain map row semantics`() {
        val targetTypes = listOf(
            typeOf<MutableMap<String, JdbcStatus>>(),
            typeOf<Map<*, JdbcStatus>>()
        )

        val rowsByType = targetTypes.map { targetType ->
            KronosResultMappers.toList(
                singleColumnResultSet(listOf("ARCHIVED"), "statusAlias"),
                targetType,
                genericContext()
            )
        }

        assertEquals(
            listOf(
                listOf(mapOf("statusAlias" to JdbcStatus.ARCHIVED)),
                listOf(mapOf("statusAlias" to JdbcStatus.ARCHIVED))
            ),
            rowsByType
        )
    }

    @Test
    fun `fixed and reordered Map subtypes are rejected before reading rows`() {
        val targetTypes = listOf(
            typeOf<JdbcStringMap>(),
            typeOf<JdbcReorderedMap<JdbcStatus, String>>()
        )

        targetTypes.forEach { targetType ->
            val resultSet = mockk<ResultSet>(relaxed = true)

            val exception = assertFailsWith<UnsupportedTypeException> {
                KronosResultMappers.toList(resultSet, targetType, genericContext())
            }

            assertEquals(
                "Unsupported Map result type $targetType: JDBC row mapping returns LinkedHashMap " +
                    "and supports only direct Map or MutableMap declarations",
                exception.message
            )
            verify(exactly = 0) { resultSet.next() }
            verify(exactly = 0) { resultSet.getObject(1) }
        }
    }

    @Test
    fun `direct Map with non String key is rejected before reading rows`() {
        val resultSet = mockk<ResultSet>(relaxed = true)
        val targetType = typeOf<Map<Int, JdbcStatus>>()

        val exception = assertFailsWith<UnsupportedTypeException> {
            KronosResultMappers.toList(resultSet, targetType, genericContext())
        }

        assertEquals(
            "Unsupported Map key type ${typeOf<Int>()} in $targetType: " +
                "JDBC row labels require String, String?, or *",
            exception.message
        )
        verify(exactly = 0) { resultSet.next() }
        verify(exactly = 0) { resultSet.getObject(1) }
    }

    @Test
    fun `serialized enum list uses field storage and complete KType in strict mode`() {
        var decodedType: KType? = null
        val registration = Kronos.registerValueCodec(serializedValueCodec(
            encode = { _, _ -> error("encode must not be called while reading a result") },
            decode = { text, type ->
                decodedType = type
                text.removePrefix("[").removeSuffix("]")
                    .split(',')
                    .filter(String::isNotBlank)
                    .map { JdbcStatus.valueOf(it.trim()) }
            }
        ))
        val previousStrict = Kronos.strictSetValue
        try {
            Kronos.strictSetValue = true
            val requestedValueType = typeOf<List<JdbcStatus>>()
            val fieldType = typeOf<List<JdbcStatus>?>()
            val field = Field("statuses", "statuses", kType = fieldType, serializable = true)
            val task = KronosAtomicQueryTask(
                sql = "SELECT statuses AS statusesAlias",
                targetType = typeOf<Map<String, List<JdbcStatus>>>(),
                resultColumns = mapOf(
                    "statusesAlias" to ResultColumnMetadata(fieldType, field, columnLabel = "statusesAlias")
                )
            )

            val rows = KronosResultMappers.toList(
                singleColumnResultSet(listOf("[READY, ARCHIVED]"), "statusesAlias"),
                task,
                genericContext()
            )

            assertEquals(
                listOf(mapOf("statusesAlias" to listOf(JdbcStatus.READY, JdbcStatus.ARCHIVED))),
                rows
            )
            assertEquals(requestedValueType, decodedType)
        } finally {
            Kronos.strictSetValue = previousStrict
            registration.close()
        }
    }

    @Test
    fun `typed database date decoding honors field date format`() {
        val targetType = typeOf<LocalDateTime?>()
        val field = Field(
            columnName = "created_at",
            name = "createdAt",
            dateFormat = "dd/MM/yyyy HH:mm",
            kType = targetType
        )
        val task = KronosAtomicQueryTask(
            sql = "SELECT created_at",
            targetType = typeOf<Map<String, Any?>>(),
            resultColumns = mapOf(
                "created_at" to ResultColumnMetadata(targetType, field, columnLabel = "created_at")
            )
        )

        val rows = KronosResultMappers.toList(
            singleColumnResultSet(listOf("21/07/2026 13:14"), "created_at"),
            task,
            genericContext()
        )

        assertEquals(
            listOf(mapOf("created_at" to LocalDateTime.of(2026, 7, 21, 13, 14))),
            rows
        )
    }

    @Test
    fun `serialized scalar uses result column storage metadata`() {
        val registration = Kronos.registerValueCodec(serializedValueCodec(
            encode = { _, _ -> error("encode must not be called while reading a result") },
            decode = { text, _ -> listOf(JdbcStatus.valueOf(text)) }
        ))
        try {
            val type = typeOf<List<JdbcStatus>>()
            val field = Field("statuses", "statuses", kType = type, serializable = true)
            val task = KronosAtomicQueryTask(
                sql = "SELECT statuses AS statusesAlias",
                targetType = type,
                resultColumns = mapOf(
                    "statusesAlias" to ResultColumnMetadata(type, field, columnLabel = "statusesAlias")
                )
            )

            val rows = KronosResultMappers.toList(
                singleColumnResultSet(listOf("READY"), "statusesAlias"),
                task,
                genericContext()
            )

            assertEquals(listOf(listOf(JdbcStatus.READY)), rows)
        } finally {
            registration.close()
        }
    }

    @Test
    fun `KPojo aliases use task metadata before target field fallback`() {
        val registration = Kronos.registerValueCodec(serializedValueCodec(
            encode = { _, _ -> error("encode must not be called while reading a result") },
            decode = { text, _ -> text.split(',').map(JdbcStatus::valueOf) }
        ))
        val factory = Kronos.registerKPojoFactory(
            typeOf<JdbcProjectionPojo>(),
            KPojoFactory { JdbcProjectionPojo() }
        )
        try {
            val statusType = typeOf<JdbcStatus?>()
            val statusesType = typeOf<List<JdbcStatus>?>()
            val dateType = typeOf<LocalDateTime?>()
            val task = KronosAtomicQueryTask(
                sql = "SELECT status AS statusAlias, statuses AS statusesAlias, created_at AS createdAlias",
                targetType = typeOf<JdbcProjectionPojo>(),
                resultColumns = mapOf(
                    "statusAlias" to ResultColumnMetadata(
                        statusType,
                        Field("status", "status", kType = statusType),
                        columnLabel = "statusAlias"
                    ),
                    "statusesAlias" to ResultColumnMetadata(
                        statusesType,
                        Field("statuses", "statuses", kType = statusesType, serializable = true),
                        columnLabel = "statusesAlias"
                    ),
                    "createdAlias" to ResultColumnMetadata(
                        dateType,
                        Field("created_at", "createdAt", dateFormat = "dd/MM/yyyy HH:mm", kType = dateType),
                        columnLabel = "createdAlias"
                    )
                )
            )

            val rows = KronosResultMappers.toList(
                singleRowResultSet(
                    "statusAlias" to "READY",
                    "statusesAlias" to "READY,ARCHIVED",
                    "createdAlias" to "21/07/2026 13:14"
                ),
                task,
                genericContext()
            ).map { it as JdbcProjectionPojo }

            assertEquals(JdbcStatus.READY, rows.single().statusAlias)
            assertEquals(listOf(JdbcStatus.READY, JdbcStatus.ARCHIVED), rows.single().statusesAlias)
            assertEquals(LocalDateTime.of(2026, 7, 21, 13, 14), rows.single().createdAlias)
        } finally {
            factory.close()
            registration.close()
        }
    }

    @Test
    fun `KPojo aggregate aliases use target KTypes while preserving source storage metadata`() {
        val historyType = typeOf<List<JdbcStatus>?>()
        val decodedTypes = mutableListOf<KType>()
        val codec = Kronos.registerValueCodec(serializedValueCodec(
            encode = { _, _ -> error("encode must not be called while reading a result") },
            decode = { text, type ->
                decodedTypes += type
                listOf(JdbcStatus.valueOf(text))
            }
        ))
        val factory = Kronos.registerKPojoFactory(
            typeOf<JdbcAggregateProjectionPojo>(),
            KPojoFactory { JdbcAggregateProjectionPojo() }
        )
        val metaData = mockk<ResultSetMetaData>()
        every { metaData.columnCount } returns 2
        every { metaData.getColumnLabel(1) } returns "totalAmount"
        every { metaData.getColumnLabel(2) } returns "statusHistory"
        val resultSet = mockk<ResultSet>()
        every { resultSet.metaData } returns metaData
        every { resultSet.next() } returnsMany listOf(true, true, false)
        every { resultSet.getObject(1) } returnsMany listOf(70L, 40L)
        every { resultSet.getObject(2) } returnsMany listOf("READY", "ARCHIVED")
        val task = KronosAtomicQueryTask(
            sql = "SELECT SUM(amount) AS totalAmount, statuses AS statusHistory",
            targetType = typeOf<JdbcAggregateProjectionPojo>(),
            resultColumns = mapOf(
                "totalAmount" to ResultColumnMetadata(typeOf<Long?>(), columnLabel = "totalAmount"),
                "statusHistory" to ResultColumnMetadata(
                    historyType,
                    Field(
                        "statuses",
                        "statuses",
                        kType = historyType,
                        serializable = true
                    ),
                    columnLabel = "statusHistory"
                )
            )
        )

        try {
            val rows = KronosResultMappers.toList(resultSet, task, genericContext())
                .map { it as JdbcAggregateProjectionPojo }

            assertEquals(
                listOf(
                    JdbcAggregateProjectionValues(70, listOf(JdbcStatus.READY)),
                    JdbcAggregateProjectionValues(40, listOf(JdbcStatus.ARCHIVED))
                ),
                rows.map { JdbcAggregateProjectionValues(it.totalAmount, it.statusHistory) }
            )
            assertEquals(listOf(historyType, historyType), decodedTypes)
        } finally {
            try {
                factory.close()
            } finally {
                codec.close()
            }
        }
    }

    @Test
    fun `KPojo rows receive one codec conversion each and fresh factory instances`() {
        var conversions = 0
        val codec = markerCodec { conversions++ }
        val factory = Kronos.registerKPojoFactory(
            typeOf<JdbcMappedPojo>(),
            KPojoFactory { JdbcMappedPojo() }
        )
        try {
            val rows = KronosResultMappers.toList(
                singleColumnResultSet(listOf("first", "second"), "marker"),
                typeOf<JdbcMappedPojo>(),
                genericContext()
            ).map { it as JdbcMappedPojo }

            assertEquals(
                listOf(DatabaseMarker("first"), DatabaseMarker("second")),
                rows.map { it.marker }
            )
            assertEquals(false, rows[0] === rows[1])
            assertEquals(2, conversions)
        } finally {
            factory.close()
            codec.close()
        }
    }

    @Test
    fun `indirect KPojo result type follows its declared KType supertypes`() {
        val factory = Kronos.registerKPojoFactory(
            typeOf<JdbcIndirectMappedPojo>(),
            KPojoFactory { JdbcIndirectMappedPojo() }
        )
        try {
            val rows = KronosResultMappers.toList(
                singleColumnResultSet(listOf(BigDecimal("7")), "id"),
                typeOf<JdbcIndirectMappedPojo>(),
                genericContext()
            ).map { it as JdbcIndirectMappedPojo }

            assertEquals(7, rows.single().id)
        } finally {
            factory.close()
        }
    }

    @Test
    fun `KPojo field lookup accepts case indexes that point to the same field`() {
        val factory = Kronos.registerKPojoFactory(
            typeOf<JdbcMappedPojo>(),
            KPojoFactory { JdbcMappedPojo() }
        )
        try {
            val rows = KronosResultMappers.toList(
                singleColumnResultSet(listOf(BigDecimal("7")), "Id"),
                typeOf<JdbcMappedPojo>(),
                genericContext()
            ).map { it as JdbcMappedPojo }

            assertEquals(7, rows.single().id)
        } finally {
            factory.close()
        }
    }

    @Test
    fun `KPojo result mapping rejects a factory that reuses an instance`() {
        val singleton = JdbcMappedPojo()
        val codec = markerCodec()
        val factory = Kronos.registerKPojoFactory(
            typeOf<JdbcMappedPojo>(),
            KPojoFactory { singleton }
        )
        try {
            val exception = assertFailsWith<InvalidKPojoFactoryResult> {
                KronosResultMappers.toList(
                    singleColumnResultSet(listOf("first", "second"), "marker"),
                    typeOf<JdbcMappedPojo>(),
                    genericContext()
                )
            }

            assertEquals(typeOf<JdbcMappedPojo>(), exception.targetType)
            assertEquals(typeOf<JdbcMappedPojo>(), exception.actualType)
        } finally {
            factory.close()
            codec.close()
        }
    }

    @Test
    fun `Oracle LONG override is physically read once then decoded once`() {
        var conversions = 0
        val registration = markerCodec { conversions++ }
        try {
            val metaData = mockk<ResultSetMetaData>()
            every { metaData.columnCount } returns 1
            every { metaData.getColumnLabel(1) } returns "payload"
            every { metaData.getColumnTypeName(1) } returns "LONG"
            every { metaData.getColumnType(1) } returns Types.LONGVARCHAR
            val resultSet = mockk<ResultSet>()
            every { resultSet.metaData } returns metaData
            every { resultSet.next() } returnsMany listOf(true, false)
            every { resultSet.getObject(1) } returns "raw-long"
            val context = oracleContext().also {
                it.config.oracleLongColumnStrategy = KronosOracleLongColumnStrategy.READ_FIRST
            }
            val task = KronosAtomicQueryTask(
                sql = "SELECT payload",
                targetType = typeOf<Map<String, Any?>>(),
                resultColumns = mapOf(
                    "payload" to ResultColumnMetadata(typeOf<DatabaseMarker>(), columnLabel = "payload")
                )
            )

            val rows = KronosResultMappers.toList(resultSet, task, context)

            assertEquals(listOf(mapOf("payload" to DatabaseMarker("raw-long"))), rows)
            assertEquals(1, conversions)
            verify(exactly = 1) { resultSet.getObject(1) }
        } finally {
            registration.close()
        }
    }

    @Test
    fun `custom vendor reader remains a physical extension for raw Map results`() {
        val readers = KronosColumnMapperRegistry.defaults().apply {
            registerVendorReader(KronosVendorValueReader { _, _, value, _ ->
                (value as? VendorPayload)?.text
            })
        }
        val context = genericContext(KronosJdbcConfig(
            DBType.H2,
            "H2",
            "jdbc:h2:mem:test",
            "H2",
            columnMappers = readers
        ))
        val task = KronosAtomicQueryTask(
            sql = "SELECT payload",
            targetType = typeOf<Map<String, Any?>>()
        )

        val rows = KronosResultMappers.toList(
            singleColumnResultSet(listOf(VendorPayload("physical")), "payload"),
            task,
            context
        )

        assertEquals(listOf(mapOf("payload" to "physical")), rows)
    }

    @Test
    fun `physical reader runs before Oracle fallback and can handle null`() {
        val readers = KronosColumnMapperRegistry.defaults().apply {
            register(KronosPhysicalValueReader { _, position, _ ->
                if (position == 1) {
                    KronosPhysicalReadResult.Handled(null)
                } else {
                    KronosPhysicalReadResult.NotHandled
                }
            })
        }
        val context = oracleContext(KronosJdbcConfig(
            DBType.Oracle,
            "Oracle",
            "jdbc:oracle:thin:@localhost:1521/FREEPDB1",
            "Oracle JDBC",
            columnMappers = readers
        ))
        val metaData = mockk<ResultSetMetaData>()
        every { metaData.getColumnType(1) } returns Types.NUMERIC
        every { metaData.getColumnTypeName(1) } returns "NUMBER"
        val resultSet = mockk<ResultSet>()
        every { resultSet.metaData } returns metaData

        val value = readers.readJdbcValue(resultSet, 1, context)

        assertEquals(null, value)
        verify(exactly = 0) { resultSet.getBigDecimal(1) }
        verify(exactly = 0) { resultSet.getObject(1) }
    }

    @Test
    fun `Oracle LONG RAW remains physical for untyped Map results`() {
        val bytes = byteArrayOf(1, 2, 3)
        val metaData = mockk<ResultSetMetaData>()
        every { metaData.columnCount } returns 1
        every { metaData.getColumnLabel(1) } returns "payload"
        every { metaData.getColumnTypeName(1) } returns "LONG RAW"
        val resultSet = mockk<ResultSet>()
        every { resultSet.metaData } returns metaData
        every { resultSet.next() } returnsMany listOf(true, false)
        every { resultSet.getObject(1) } returns bytes
        val context = oracleContext().also {
            it.config.oracleLongColumnStrategy = KronosOracleLongColumnStrategy.READ_FIRST
        }

        val rows = KronosResultMappers.toList(
            resultSet,
            KronosAtomicQueryTask("SELECT payload", targetType = typeOf<Map<String, Any?>>()),
            context
        )

        assertContentEquals(bytes, (rows.single() as Map<*, *>)["payload"] as ByteArray)
        verify(exactly = 1) { resultSet.getObject(1) }
    }

    @Test
    fun `physical reader preserves LOB SQLXML and PGobject normalization`() {
        val clob = mockk<Clob>()
        every { clob.characterStream } returns StringReader("clob-text")
        val blob = mockk<Blob>()
        every { blob.binaryStream } returns ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val sqlxml = mockk<SQLXML>()
        every { sqlxml.string } returns "<value>sqlxml</value>"
        val pgobject = PGobject("json-value")
        val resultSet = mockk<ResultSet>()
        every { resultSet.getObject(1) } returns clob
        every { resultSet.getObject(2) } returns blob
        every { resultSet.getObject(3) } returns sqlxml
        every { resultSet.getObject(4) } returns pgobject
        val context = genericContext()

        val values = (1..4).map { position ->
            context.config.columnMappers.readJdbcValue(resultSet, position, context)
        }

        assertEquals("clob-text", values[0])
        assertContentEquals(byteArrayOf(1, 2, 3), values[1] as ByteArray)
        assertEquals("<value>sqlxml</value>", values[2])
        assertEquals("json-value", values[3])
    }

    private fun numberResultSet(value: BigDecimal, label: String): ResultSet {
        val metaData = mockk<ResultSetMetaData>()
        every { metaData.columnCount } returns 1
        every { metaData.getColumnLabel(1) } returns label
        every { metaData.getColumnTypeName(1) } returns "NUMBER"
        every { metaData.getPrecision(1) } returns 10
        every { metaData.getScale(1) } returns 0

        return mockk<ResultSet>().also { resultSet ->
            every { resultSet.metaData } returns metaData
            every { resultSet.next() } returnsMany listOf(true, false)
            every { resultSet.getBigDecimal(1) } returns value
        }
    }

    private fun singleColumnResultSet(values: List<Any?>, label: String): ResultSet {
        val metaData = mockk<ResultSetMetaData>()
        every { metaData.columnCount } returns 1
        every { metaData.getColumnLabel(1) } returns label
        every { metaData.getColumnTypeName(1) } returns "VARCHAR"
        every { metaData.getColumnType(1) } returns Types.VARCHAR
        return mockk<ResultSet>().also { resultSet ->
            every { resultSet.metaData } returns metaData
            every { resultSet.next() } returnsMany (List(values.size) { true } + false)
            every { resultSet.getObject(1) } returnsMany values
        }
    }

    private fun singleRowResultSet(vararg columns: Pair<String, Any?>): ResultSet {
        val metaData = mockk<ResultSetMetaData>()
        every { metaData.columnCount } returns columns.size
        columns.forEachIndexed { index, (label, _) ->
            every { metaData.getColumnLabel(index + 1) } returns label
            every { metaData.getColumnTypeName(index + 1) } returns "VARCHAR"
            every { metaData.getColumnType(index + 1) } returns Types.VARCHAR
        }
        return mockk<ResultSet>().also { resultSet ->
            every { resultSet.metaData } returns metaData
            every { resultSet.next() } returnsMany listOf(true, false)
            columns.forEachIndexed { index, (_, value) ->
                every { resultSet.getObject(index + 1) } returns value
            }
        }
    }

    private fun markerCodec(onConvert: () -> Unit = {}) = Kronos.registerValueCodec(valueCodec(
        supports = { _, context ->
            context.direction == ValueCodecDirection.DECODE &&
                context.origin == ValueCodecOrigin.DATABASE &&
                context.targetType in setOf(typeOf<DatabaseMarker>(), typeOf<DatabaseMarker?>())
        },
        convert = { value, _ ->
            onConvert()
            DatabaseMarker(value.toString())
        }
    ))

    private fun genericContext(
        config: KronosJdbcConfig = KronosJdbcConfig(DBType.H2, "H2", "jdbc:h2:mem:test", "H2")
    ) = KronosStatementContext(
        originalSql = "SELECT value",
        jdbcSql = "SELECT value",
        params = emptyList(),
        parameterNames = emptyList(),
        operationType = KOperationType.SELECT,
        dbType = DBType.H2,
        databaseProductName = "H2",
        config = config
    )

    private fun oracleContext(
        config: KronosJdbcConfig = KronosJdbcConfig(
            DBType.Oracle,
            "Oracle",
            "jdbc:oracle:thin:@localhost:1521/FREEPDB1",
            "Oracle JDBC"
        )
    ) = KronosStatementContext(
        originalSql = "SELECT id FROM kt_integration_user",
        jdbcSql = "SELECT id FROM kt_integration_user",
        params = emptyList(),
        parameterNames = emptyList(),
        operationType = KOperationType.SELECT,
        dbType = DBType.Oracle,
        databaseProductName = "Oracle",
        config = config
    )
}

private data class DatabaseMarker(val value: String)

private data class VendorPayload(val text: String)

private typealias NullableJdbcStatusMap = Map<String?, JdbcStatus>

private class JdbcStringMap : Map<String, Any?> by emptyMap()

private class JdbcReorderedMap<V, K> : Map<K, V> by emptyMap<K, V>()

internal enum class JdbcStatus {
    READY,
    ARCHIVED
}

private class JdbcMappedPojo : KPojo {
    override var __kType: KType = typeOf<JdbcMappedPojo>()
    override var __columns: MutableList<Field> = mutableListOf(
        Field("id", "id", kType = typeOf<Int?>()),
        Field("marker", "marker", kType = typeOf<DatabaseMarker?>())
    )

    var id: Int? = null
    var marker: DatabaseMarker? = null

    override fun set(name: String, value: Any?) {
        when (name) {
            "id" -> id = value as Int?
            "marker" -> marker = value as DatabaseMarker?
        }
    }
}

private interface JdbcIndirectKPojo : KPojo

private class JdbcIndirectMappedPojo : JdbcIndirectKPojo {
    override var __kType: KType = typeOf<JdbcIndirectMappedPojo>()
    override var __columns: MutableList<Field> = mutableListOf(
        Field("id", "id", kType = typeOf<Int?>())
    )

    var id: Int? = null

    override fun set(name: String, value: Any?) {
        if (name == "id") id = value as Int?
    }
}

private class JdbcProjectionPojo : KPojo {
    override var __kType: KType = typeOf<JdbcProjectionPojo>()
    override var __columns: MutableList<Field> = mutableListOf(
        Field("statusAlias", "statusAlias", kType = typeOf<JdbcStatus?>()),
        Field("statusesAlias", "statusesAlias", kType = typeOf<List<JdbcStatus>?>()),
        Field("createdAlias", "createdAlias", kType = typeOf<LocalDateTime?>())
    )

    var statusAlias: JdbcStatus? = null
    var statusesAlias: List<JdbcStatus>? = null
    var createdAlias: LocalDateTime? = null

    @Suppress("UNCHECKED_CAST")
    override fun set(name: String, value: Any?) {
        when (name) {
            "statusAlias" -> statusAlias = value as JdbcStatus?
            "statusesAlias" -> statusesAlias = value as List<JdbcStatus>?
            "createdAlias" -> createdAlias = value as LocalDateTime?
        }
    }
}

private data class JdbcAggregateProjectionValues(
    val totalAmount: Int?,
    val statusHistory: List<JdbcStatus>?
)

private class JdbcAggregateProjectionPojo : KPojo {
    override var __kType: KType = typeOf<JdbcAggregateProjectionPojo>()
    override var __columns: MutableList<Field> = mutableListOf(
        Field("total_amount", "totalAmount", kType = typeOf<Int?>()),
        Field("status_history", "statusHistory", kType = typeOf<List<JdbcStatus>?>())
    )

    var totalAmount: Int? = null
    var statusHistory: List<JdbcStatus>? = null

    @Suppress("UNCHECKED_CAST")
    override fun set(name: String, value: Any?) {
        when (name) {
            "totalAmount" -> totalAmount = value as Int?
            "statusHistory" -> statusHistory = value as List<JdbcStatus>?
        }
    }
}
