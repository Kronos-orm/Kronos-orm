package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType.BIT
import com.kotlinorm.enums.KColumnType.DATETIME
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.serializedValueCodec
import com.kotlinorm.interfaces.valueCodec
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import com.kotlinorm.wrappers.SamplePostgresJdbcWrapper
import java.time.LocalDateTime
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals

internal enum class CodecStatus {
    ACTIVE,
    BLOCKED
}

@Table("codec_parameter_record")
internal data class CodecParameterRecord(
    @PrimaryKey
    var id: Int? = null,
    var status: CodecStatus? = null,
    @Serialize
    var history: List<CodecStatus>? = null,
    @ColumnType(BIT)
    var active: Boolean? = null,
    @ColumnType(DATETIME)
    @DateTimeFormat("yyyy/MM/dd HH:mm:ss")
    var occurredAt: LocalDateTime? = null
) : KPojo

class ValueCodecParameterIntegrationTest {

    @Test
    fun `insert update and upsert encode fields once with dialect targets`() {
        val encodedTypes = mutableListOf<KType>()
        val registration = Kronos.registerValueCodec(
            serializedValueCodec(
                encode = { value, type ->
                    encodedTypes += type
                    (value as List<*>).joinToString(prefix = "[", postfix = "]") {
                        (it as CodecStatus).name
                    }
                },
                decode = { _, _ -> error("decode is not used by parameter preparation") }
            )
        )
        val occurredAt = LocalDateTime.of(2026, 7, 21, 12, 34, 56)
        val record = CodecParameterRecord(
            id = 7,
            status = CodecStatus.ACTIVE,
            history = listOf(CodecStatus.ACTIVE, CodecStatus.BLOCKED),
            active = false,
            occurredAt = occurredAt
        )

        try {
            val mysqlInsert = record.insert().build(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper).component2()
            val postgresInsert = record.insert().build(SamplePostgresJdbcWrapper()).component2()
            val upsert = record.upsert()
                .on { it.id }
                .onConflict()
                .build(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper)
                .component2()
            val update = record.update()
                .set { it.status = CodecStatus.BLOCKED }
                .by { it.id }
                .build(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper)
                .component2()

            assertEquals(
                mapOf(
                    "id" to 7,
                    "status" to "ACTIVE",
                    "history" to "[ACTIVE, BLOCKED]",
                    "active" to 0,
                    "occurredAt" to occurredAt
                ),
                mysqlInsert
            )
            assertEquals(
                mapOf(
                    "id" to 7,
                    "status" to "ACTIVE",
                    "history" to "[ACTIVE, BLOCKED]",
                    "active" to false,
                    "occurredAt" to java.sql.Timestamp.valueOf(occurredAt)
                ),
                postgresInsert
            )
            assertEquals(mapOf("statusNew" to "BLOCKED", "id" to 7), update)
            assertEquals(
                mapOf(
                    "id" to 7,
                    "status" to "ACTIVE",
                    "history" to "[ACTIVE, BLOCKED]",
                    "active" to 0,
                    "occurredAt" to occurredAt
                ),
                upsert
            )
            assertEquals(
                listOf(
                    typeOf<List<CodecStatus>?>(),
                    typeOf<List<CodecStatus>?>(),
                    typeOf<List<CodecStatus>?>()
                ),
                encodedTypes
            )
        } finally {
            registration.close()
        }
    }

    @Test
    fun `enum in parameters are encoded element by element`() {
        val (_, parameters) = CodecParameterRecord()
            .select()
            .where { listOf(CodecStatus.ACTIVE, CodecStatus.BLOCKED).contains(it.status) }
            .build(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper)

        assertEquals(
            mapOf("statusList" to listOf("ACTIVE", "BLOCKED")),
            parameters
        )
    }

    @Test
    fun `strict parameter encoding still honors explicit user codec overrides`() {
        val previousStrict = Kronos.strictSetValue
        val registration = Kronos.registerValueCodec(
            valueCodec(
                supports = { value, context ->
                    context.direction == ValueCodecDirection.ENCODE &&
                        context.storage == ValueStorage.NONE &&
                        context.targetType.classifier == CodecStatus::class &&
                        value is CodecStatus
                },
                convert = { value, _ -> "code:${(value as CodecStatus).name.lowercase()}" }
            )
        )

        try {
            Kronos.strictSetValue = true
            val (_, parameters) = CodecParameterRecord(status = CodecStatus.ACTIVE)
                .insert()
                .build(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper)

            assertEquals("code:active", parameters["status"])
        } finally {
            Kronos.strictSetValue = previousStrict
            registration.close()
        }
    }
}
