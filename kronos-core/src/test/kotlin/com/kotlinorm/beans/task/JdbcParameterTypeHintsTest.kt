package com.kotlinorm.beans.task

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.KColumnType
import java.sql.Types
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals

class JdbcParameterTypeHintsTest {
    @Test
    fun `stash extraction keeps only named numeric jdbc types`() {
        val rawHints = linkedMapOf<Any?, Any?>(
            "integer" to Types.INTEGER,
            "bigint" to Types.BIGINT.toLong(),
            "decimal" to Types.DECIMAL.toDouble(),
            7 to Types.SMALLINT,
            "text" to "VARCHAR",
            "missing" to null
        )

        assertEquals(emptyMap(), JdbcParameterTypeHints.from(null))
        assertEquals(emptyMap(), JdbcParameterTypeHints.from(emptyMap()))
        assertEquals(
            emptyMap(),
            JdbcParameterTypeHints.from(mapOf(JdbcParameterTypeHints.STASH_KEY to "not a map"))
        )
        assertEquals(
            mapOf(
                "integer" to Types.INTEGER,
                "bigint" to Types.BIGINT,
                "decimal" to Types.DECIMAL
            ),
            JdbcParameterTypeHints.from(mapOf(JdbcParameterTypeHints.STASH_KEY to rawHints))
        )
    }

    @Test
    fun `stash creation omits empty hints and preserves populated hints`() {
        val hints = linkedMapOf("createdAt" to Types.TIMESTAMP, "payload" to Types.LONGVARBINARY)

        assertEquals(emptyMap(), JdbcParameterTypeHints.stashFor(emptyMap()))
        assertEquals(
            mutableMapOf<String, Any?>(JdbcParameterTypeHints.STASH_KEY to hints),
            JdbcParameterTypeHints.stashFor(hints)
        )
    }

    @Test
    fun `null parameter hint collection filters missing non-null and unknown fields`() {
        val fields = listOf(
            Field("missing", "missing", type = KColumnType.INT),
            Field("present_value", "presentValue", type = KColumnType.INT),
            Field("present_null", "presentNull", type = KColumnType.INT),
            Field("unknown", "unknown", kType = typeOf<List<String>>())
        )

        assertEquals(
            mapOf("presentNull" to Types.INTEGER),
            fields.jdbcNullParameterTypeHints(
                mapOf(
                    "presentValue" to 7,
                    "presentNull" to null,
                    "unknown" to null
                )
            )
        )
    }

    @Test
    fun `every declared column type maps to its jdbc null type`() {
        val expected = mapOf(
            KColumnType.UNDEFINED to null,
            KColumnType.BIT to Types.BIT,
            KColumnType.TINYINT to Types.TINYINT,
            KColumnType.SMALLINT to Types.SMALLINT,
            KColumnType.INT to Types.INTEGER,
            KColumnType.BIGINT to Types.BIGINT,
            KColumnType.REAL to Types.REAL,
            KColumnType.FLOAT to Types.FLOAT,
            KColumnType.DOUBLE to Types.DOUBLE,
            KColumnType.DECIMAL to Types.DECIMAL,
            KColumnType.CHAR to Types.CHAR,
            KColumnType.VARCHAR to Types.VARCHAR,
            KColumnType.TEXT to Types.LONGVARCHAR,
            KColumnType.LONGTEXT to Types.LONGVARCHAR,
            KColumnType.DATE to Types.DATE,
            KColumnType.TIME to Types.TIME,
            KColumnType.DATETIME to Types.TIMESTAMP,
            KColumnType.TIMESTAMP to Types.TIMESTAMP,
            KColumnType.BINARY to Types.BINARY,
            KColumnType.VARBINARY to Types.VARBINARY,
            KColumnType.LONGVARBINARY to Types.LONGVARBINARY,
            KColumnType.BLOB to Types.LONGVARBINARY,
            KColumnType.CLOB to Types.LONGVARCHAR,
            KColumnType.JSON to Types.VARCHAR,
            KColumnType.ENUM to Types.VARCHAR,
            KColumnType.NVARCHAR to Types.NVARCHAR,
            KColumnType.NCHAR to Types.NCHAR,
            KColumnType.NCLOB to Types.NCLOB,
            KColumnType.UUID to Types.VARCHAR,
            KColumnType.SERIAL to Types.INTEGER,
            KColumnType.YEAR to Types.SMALLINT,
            KColumnType.MEDIUMINT to Types.INTEGER,
            KColumnType.NUMERIC to Types.DECIMAL,
            KColumnType.MEDIUMTEXT to Types.LONGVARCHAR,
            KColumnType.MEDIUMBLOB to Types.LONGVARBINARY,
            KColumnType.LONGBLOB to Types.LONGVARBINARY,
            KColumnType.SET to Types.VARCHAR,
            KColumnType.GEOMETRY to Types.VARBINARY,
            KColumnType.POINT to Types.VARBINARY,
            KColumnType.LINESTRING to Types.VARBINARY,
            KColumnType.XML to Types.VARCHAR
        )

        assertEquals(
            expected,
            KColumnType.entries.associateWith { type ->
                Field(type.name.lowercase(), type.name.lowercase(), type = type).jdbcNullType()
            }
        )
    }

    @Test
    fun `undefined column type infers every supported kotlin type`() {
        val cases = listOf(
            typeOf<Boolean?>() to Types.BIT,
            typeOf<Byte?>() to Types.TINYINT,
            typeOf<Short?>() to Types.SMALLINT,
            typeOf<Int?>() to Types.INTEGER,
            typeOf<Long?>() to Types.BIGINT,
            typeOf<Float?>() to Types.FLOAT,
            typeOf<Double?>() to Types.DOUBLE,
            typeOf<String?>() to Types.VARCHAR,
            typeOf<Char?>() to Types.VARCHAR,
            typeOf<java.util.UUID?>() to Types.VARCHAR,
            typeOf<ByteArray?>() to Types.VARBINARY,
            typeOf<java.time.LocalDate?>() to Types.DATE,
            typeOf<java.sql.Date?>() to Types.DATE,
            typeOf<java.time.LocalTime?>() to Types.TIME,
            typeOf<java.sql.Time?>() to Types.TIME,
            typeOf<java.time.LocalDateTime?>() to Types.TIMESTAMP,
            typeOf<java.time.Instant?>() to Types.TIMESTAMP,
            typeOf<java.time.ZonedDateTime?>() to Types.TIMESTAMP,
            typeOf<java.time.OffsetDateTime?>() to Types.TIMESTAMP,
            typeOf<java.sql.Timestamp?>() to Types.TIMESTAMP
        )

        assertEquals(
            cases,
            cases.mapIndexed { index, (kType, _) ->
                kType to Field("value_$index", "value$index", kType = kType).jdbcNullType()
            }
        )
        assertEquals(null, Field("unknown", "unknown", kType = typeOf<java.util.Date>()).jdbcNullType())
        assertEquals(null, Field("untyped", "untyped").jdbcNullType())
    }
}
