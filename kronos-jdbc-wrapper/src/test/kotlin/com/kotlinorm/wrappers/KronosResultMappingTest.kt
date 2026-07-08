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

        val value = context.config.columnMappers.map(resultSet, 1, Boolean::class, emptyList(), context)

        assertEquals(false, value)
        verify(exactly = 0) { resultSet.getObject(1) }
    }
}
