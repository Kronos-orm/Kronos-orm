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

package com.kotlinorm.wrappers

import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.Types
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.reflect.typeOf

class KronosResultMappingTest {
    @Test
    fun `oracle number boolean mapping reads numeric value before driver object conversion`() {
        val metaData = mockk<ResultSetMetaData>()
        every { metaData.getColumnType(1) } returns Types.NUMERIC
        every { metaData.getColumnTypeName(1) } returns "NUMBER"

        val resultSet = mockk<ResultSet>()
        every { resultSet.metaData } returns metaData
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

        val value = context.config.columnMappers.map(resultSet, 1, typeOf<Boolean>(), context)

        assertEquals(false, value)
        verify(exactly = 0) { resultSet.getObject(1) }
    }

    @Test
    fun `map rows use query result column types case insensitively`() {
        val resultSet = numberResultSet(BigDecimal("5"), "ID")
        val context = oracleContext()
        val task = KronosAtomicQueryTask(
            sql = "SELECT id FROM kt_integration_user",
            targetType = typeOf<Map<String, Any?>>(),
            resultColumnTypes = mapOf("id" to typeOf<Int?>())
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
        every { resultSet.getObject(1) } returns BigDecimal("5")
        every { resultSet.getObject(2) } returns BigDecimal("6")

        val task = KronosAtomicQueryTask(
            sql = "SELECT id, id AS id_1 FROM kt_integration_user",
            targetType = typeOf<Map<String, Any?>>(),
            resultColumnTypes = mapOf(
                "id" to typeOf<Int?>(),
                "id_1" to typeOf<Long?>(),
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
            every { resultSet.getObject(1) } returns value
        }
    }

    private fun oracleContext() = KronosStatementContext(
        originalSql = "SELECT id FROM kt_integration_user",
        jdbcSql = "SELECT id FROM kt_integration_user",
        params = emptyList(),
        parameterNames = emptyList(),
        operationType = KOperationType.SELECT,
        dbType = DBType.Oracle,
        databaseProductName = "Oracle",
        config = KronosJdbcConfig(DBType.Oracle, "Oracle", "jdbc:oracle:thin:@localhost:1521/FREEPDB1", "Oracle JDBC")
    )
}
