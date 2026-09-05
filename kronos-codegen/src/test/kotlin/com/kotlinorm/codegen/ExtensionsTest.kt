package com.kotlinorm.codegen

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionsTest {

    private fun fieldOfType(type: KColumnType): Field = Field(columnName = "test_col", type = type)

    @Test
    fun bitMapsToBoolean() {
        assertEquals("Boolean", fieldOfType(BIT).kotlinType)
    }

    @Test
    fun tinyintMapsToByte() {
        assertEquals("Byte", fieldOfType(TINYINT).kotlinType)
    }

    @Test
    fun smallintMapsToShort() {
        assertEquals("Short", fieldOfType(SMALLINT).kotlinType)
    }

    @Test
    fun intMapsToInt() {
        assertEquals("Int", fieldOfType(INT).kotlinType)
    }

    @Test
    fun mediumintMapsToInt() {
        assertEquals("Int", fieldOfType(MEDIUMINT).kotlinType)
    }

    @Test
    fun serialMapsToInt() {
        assertEquals("Int", fieldOfType(SERIAL).kotlinType)
    }

    @Test
    fun yearMapsToInt() {
        assertEquals("Int", fieldOfType(YEAR).kotlinType)
    }

    @Test
    fun bigintMapsToLong() {
        assertEquals("Long", fieldOfType(BIGINT).kotlinType)
    }

    @Test
    fun realMapsToFloat() {
        assertEquals("Float", fieldOfType(REAL).kotlinType)
    }

    @Test
    fun floatMapsToFloat() {
        assertEquals("Float", fieldOfType(FLOAT).kotlinType)
    }

    @Test
    fun doubleMapsToDouble() {
        assertEquals("Double", fieldOfType(DOUBLE).kotlinType)
    }

    @Test
    fun decimalMapsToBigDecimal() {
        assertEquals("java.math.BigDecimal", fieldOfType(DECIMAL).kotlinType)
    }

    @Test
    fun numericMapsToBigDecimal() {
        assertEquals("java.math.BigDecimal", fieldOfType(NUMERIC).kotlinType)
    }

    @Test
    fun charMapsToChar() {
        assertEquals("Char", fieldOfType(CHAR).kotlinType)
    }

    @Test
    fun varcharMapsToString() {
        assertEquals("String", fieldOfType(VARCHAR).kotlinType)
    }

    @Test
    fun textMapsToString() {
        assertEquals("String", fieldOfType(TEXT).kotlinType)
    }

    @Test
    fun longtextMapsToString() {
        assertEquals("String", fieldOfType(LONGTEXT).kotlinType)
    }

    @Test
    fun mediumtextMapsToString() {
        assertEquals("String", fieldOfType(MEDIUMTEXT).kotlinType)
    }

    @Test
    fun nvarcharMapsToString() {
        assertEquals("String", fieldOfType(NVARCHAR).kotlinType)
    }

    @Test
    fun ncharMapsToString() {
        assertEquals("String", fieldOfType(NCHAR).kotlinType)
    }

    @Test
    fun nclobMapsToString() {
        assertEquals("String", fieldOfType(NCLOB).kotlinType)
    }

    @Test
    fun clobMapsToString() {
        assertEquals("String", fieldOfType(CLOB).kotlinType)
    }

    @Test
    fun jsonMapsToString() {
        assertEquals("String", fieldOfType(JSON).kotlinType)
    }

    @Test
    fun enumMapsToString() {
        assertEquals("String", fieldOfType(ENUM).kotlinType)
    }

    @Test
    fun setMapsToString() {
        assertEquals("String", fieldOfType(SET).kotlinType)
    }

    @Test
    fun geometryMapsToString() {
        assertEquals("String", fieldOfType(GEOMETRY).kotlinType)
    }

    @Test
    fun pointMapsToString() {
        assertEquals("String", fieldOfType(POINT).kotlinType)
    }

    @Test
    fun linestringMapsToString() {
        assertEquals("String", fieldOfType(LINESTRING).kotlinType)
    }

    @Test
    fun xmlMapsToString() {
        assertEquals("String", fieldOfType(XML).kotlinType)
    }

    @Test
    fun undefinedMapsToString() {
        assertEquals("String", fieldOfType(UNDEFINED).kotlinType)
    }

    @Test
    fun uuidMapsToUUID() {
        assertEquals("java.util.UUID", fieldOfType(UUID).kotlinType)
    }

    @Test
    fun dateMapsToLocalDate() {
        assertEquals("java.time.LocalDate", fieldOfType(DATE).kotlinType)
    }

    @Test
    fun timeMapsToLocalTime() {
        assertEquals("java.time.LocalTime", fieldOfType(TIME).kotlinType)
    }

    @Test
    fun datetimeMapsToLocalDateTime() {
        assertEquals("java.time.LocalDateTime", fieldOfType(DATETIME).kotlinType)
    }

    @Test
    fun timestampMapsToInstant() {
        assertEquals("java.time.Instant", fieldOfType(TIMESTAMP).kotlinType)
    }

    @Test
    fun binaryMapsByteArray() {
        assertEquals("ByteArray", fieldOfType(BINARY).kotlinType)
    }

    @Test
    fun varbinaryMapsByteArray() {
        assertEquals("ByteArray", fieldOfType(VARBINARY).kotlinType)
    }

    @Test
    fun longvarbinaryMapsByteArray() {
        assertEquals("ByteArray", fieldOfType(LONGVARBINARY).kotlinType)
    }

    @Test
    fun blobMapsByteArray() {
        assertEquals("ByteArray", fieldOfType(BLOB).kotlinType)
    }

    @Test
    fun mediumblobMapsByteArray() {
        assertEquals("ByteArray", fieldOfType(MEDIUMBLOB).kotlinType)
    }

    @Test
    fun longblobMapsByteArray() {
        assertEquals("ByteArray", fieldOfType(LONGBLOB).kotlinType)
    }
}