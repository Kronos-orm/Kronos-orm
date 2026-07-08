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
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.subquery.SubqueryOrderArchive
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlPrimaryKeyMode
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class TableOperationSyntaxTest : MysqlTestBase() {

    @Test
    fun `table operation DDL builders return syntax statements`() {
        val table = Kronos.dataSource.table

        val create = table.buildCreateTableStatement(SubqueryOrderArchive())
        assertEquals("tb_subquery_order_archive", create.tableName.last)
        assertEquals(["id", "user_id", "status"], create.columns.map { it.name.last })
        assertEquals(SqlPrimaryKeyMode.Primary, create.columns.single { it.name.last == "id" }.primaryKey)

        val drop = table.buildDropTableStatement("tb_archive", ifExists = true)
        assertEquals(SqlDdlStatement.DropTable(SqlIdentifier.of("tb_archive"), ifExists = true), drop)

        val truncate = table.buildTruncateTableStatement("tb_archive")
        assertEquals(SqlDmlStatement.Truncate(SqlTable.Ident("tb_archive"), restartIdentity = true), truncate)

        val createIndex = table.buildCreateIndexStatement("idx_archive_status", "tb_archive", ["status"], unique = true)
        assertEquals(SqlIdentifier.of("idx_archive_status"), createIndex.indexName)
        assertEquals([SqlIdentifier.of("status")], createIndex.columns)
        assertEquals(true, createIndex.unique)

        val dropIndex = table.buildDropIndexStatement("idx_archive_status", "tb_archive")
        assertEquals(SqlDdlStatement.DropIndex(SqlIdentifier.of("idx_archive_status"), SqlIdentifier.of("tb_archive")), dropIndex)

        val column = SqlColumnDefinition(
            name = SqlIdentifier.of("flag"),
            type = SqlType.Int,
            nullable = false
        )
        assertEquals(
            SqlDdlStatement.AlterTable.AddColumn(SqlIdentifier.of("tb_archive"), column),
            table.buildAddColumnStatement("tb_archive", column)
        )
        assertEquals(
            SqlDdlStatement.AlterTable.DropColumn(SqlIdentifier.of("tb_archive"), SqlIdentifier.of("flag")),
            table.buildDropColumnStatement("tb_archive", "flag")
        )
        assertEquals(
            SqlDdlStatement.AlterTable.ModifyColumn(SqlIdentifier.of("tb_archive"), column),
            table.buildModifyColumnStatement("tb_archive", column)
        )
    }

    @Test
    fun `table operation converts field types primary keys and indexes to syntax definitions`() {
        val table = Kronos.dataSource.table

        val actualTypes = listOf(
            field("bit", KColumnType.BIT),
            field("int", KColumnType.INT),
            field("bigint", KColumnType.BIGINT),
            field("float", KColumnType.FLOAT),
            field("double", KColumnType.DOUBLE),
            field("decimalDefault", KColumnType.DECIMAL),
            field("decimalSized", KColumnType.DECIMAL, length = 12, scale = 4),
            field("varcharDefault", KColumnType.VARCHAR),
            field("varcharSized", KColumnType.VARCHAR, length = 64),
            field("text", KColumnType.TEXT),
            field("date", KColumnType.DATE),
            field("time", KColumnType.TIME),
            field("timestamp", KColumnType.TIMESTAMP),
            field("json", KColumnType.JSON),
            field("geometry", KColumnType.GEOMETRY),
            field("point", KColumnType.POINT),
            field("linestring", KColumnType.LINESTRING),
            field("binary", KColumnType.BINARY)
        ).associate { column ->
            column.columnName to with(table) { column.toSqlColumnDefinition().type }
        }

        assertEquals(
            mapOf(
                "bit" to SqlType.Boolean,
                "int" to SqlType.Int,
                "bigint" to SqlType.Long,
                "float" to SqlType.Float,
                "double" to SqlType.Double,
                "decimalDefault" to SqlType.Decimal(null),
                "decimalSized" to SqlType.Decimal(12 to 4),
                "varcharDefault" to SqlType.Varchar(null),
                "varcharSized" to SqlType.Varchar(64),
                "text" to SqlType.Named("TEXT"),
                "date" to SqlType.Date,
                "time" to SqlType.Time(),
                "timestamp" to SqlType.Timestamp(),
                "json" to SqlType.Json,
                "geometry" to SqlType.Geometry,
                "point" to SqlType.Point,
                "linestring" to SqlType.LineString,
                "binary" to SqlType.Named("BINARY")
            ),
            actualTypes
        )

        assertEquals(
            mapOf(
                PrimaryKeyType.NOT to SqlPrimaryKeyMode.NotPrimary,
                PrimaryKeyType.DEFAULT to SqlPrimaryKeyMode.Primary,
                PrimaryKeyType.IDENTITY to SqlPrimaryKeyMode.Identity,
                PrimaryKeyType.UUID to SqlPrimaryKeyMode.Uuid,
                PrimaryKeyType.SNOWFLAKE to SqlPrimaryKeyMode.Snowflake,
                PrimaryKeyType.CUSTOM to SqlPrimaryKeyMode.Primary
            ),
            PrimaryKeyType.entries.associateWith { primaryKey ->
                with(table) { field("id_$primaryKey", KColumnType.INT, primaryKey = primaryKey).toSqlColumnDefinition().primaryKey }
            }
        )

        assertEquals(
            listOf(
                SqlIdentifier.of("idx_unique_type") to Pair(true, "BTREE"),
                SqlIdentifier.of("idx_unique_method") to Pair(true, null),
                SqlIdentifier.of("idx_blank_method") to Pair(false, null)
            ),
            listOf(
                KTableIndex("idx_unique_type", arrayOf("a"), "UNIQUE", "BTREE"),
                KTableIndex("idx_unique_method", arrayOf("a"), "NORMAL", "UNIQUE"),
                KTableIndex("idx_blank_method", arrayOf("a"), "NORMAL", "")
            ).map { index ->
                val definition = with(table) { index.toSqlIndexDefinition() }
                definition.name to Pair(definition.unique, definition.method)
            }
        )
    }

    private fun field(
        columnName: String,
        type: KColumnType,
        length: Int = 0,
        scale: Int = 0,
        primaryKey: PrimaryKeyType = PrimaryKeyType.NOT
    ) = Field(
        columnName = columnName,
        type = type,
        length = length,
        scale = scale,
        primaryKey = primaryKey
    )
}
