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

package com.kotlinorm.orm.ddl

import com.kotlinorm.Kronos
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TableOperationSqlTest : MysqlTestBase() {

    @Test
    fun `drop table task renders syntax statement metadata`() {
        val (sql, params, tasks) = Kronos.dataSource.table.buildDropTableTask("tb_archive")

        assertEquals("DROP TABLE IF EXISTS `tb_archive`", sql)
        assertEquals(emptyMap(), params)

        val statement = assertIs<SqlDdlStatement.DropTable>(tasks.single().statement)
        assertEquals(SqlIdentifier.of("tb_archive"), statement.tableName)
        assertEquals(true, statement.ifExists)
    }

    @Test
    fun `truncate table task renders syntax statement metadata`() {
        val task = Kronos.dataSource.table.buildTruncateTableTask("tb_archive", restartIdentity = false)
        val atomic = task.atomicTasks.single()

        assertEquals("TRUNCATE TABLE `tb_archive`", atomic.sql)
        assertEquals(emptyMap(), atomic.paramMap)
        assertEquals(
            SqlDmlStatement.Truncate(SqlTable.Ident("tb_archive"), restartIdentity = false),
            atomic.statement
        )
    }
}
