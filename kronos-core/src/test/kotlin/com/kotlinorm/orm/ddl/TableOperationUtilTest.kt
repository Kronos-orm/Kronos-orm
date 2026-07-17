package com.kotlinorm.orm.ddl

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlIndexDefinition
import com.kotlinorm.syntax.statement.SqlPrimaryKeyMode
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.utils.resolveRuntimeMetadata
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ManualDdlKPojo(
    columns: MutableList<Field>,
    indexes: MutableList<KTableIndex> = mutableListOf()
) : KPojo {
    @Ignore([IgnoreAction.ALL])
    override var __kClass: KClass<out KPojo> = KPojo::class
    @Ignore([IgnoreAction.ALL])
    override var __tableName: String = "manual_ddl"
    @Ignore([IgnoreAction.ALL])
    override var __tableComment: String = "manual table"
    @Ignore([IgnoreAction.ALL])
    override var __columns: MutableList<Field> = columns
    @Ignore([IgnoreAction.ALL])
    override var __tableIndexes: MutableList<KTableIndex> = indexes
    @Ignore([IgnoreAction.ALL])
    override var __createTime = Kronos.createTimeStrategy
    @Ignore([IgnoreAction.ALL])
    override var __updateTime = Kronos.updateTimeStrategy
    @Ignore([IgnoreAction.ALL])
    override var __logicDelete = Kronos.logicDeleteStrategy
    @Ignore([IgnoreAction.ALL])
    override var __optimisticLock = Kronos.optimisticLockStrategy
}

class StaticDdlCacheKPojo : KPojo {
    @Ignore([IgnoreAction.ALL])
    override var __kClass: KClass<out KPojo> = StaticDdlCacheKPojo::class
    @Ignore([IgnoreAction.ALL])
    override var __tableName: String = "kt_integration_user"
    @Ignore([IgnoreAction.ALL])
    override var __tableComment: String = "static ddl cache table"
    @Ignore([IgnoreAction.ALL])
    override var __columns: MutableList<Field> = staticColumns
    @Ignore([IgnoreAction.ALL])
    override var __tableIndexes: MutableList<KTableIndex> = mutableListOf()
    @Ignore([IgnoreAction.ALL])
    override var __createTime = Kronos.createTimeStrategy
    @Ignore([IgnoreAction.ALL])
    override var __updateTime = Kronos.updateTimeStrategy
    @Ignore([IgnoreAction.ALL])
    override var __logicDelete = Kronos.logicDeleteStrategy
    @Ignore([IgnoreAction.ALL])
    override var __optimisticLock = Kronos.optimisticLockStrategy

    companion object {
        val staticColumns = mutableListOf(
            Field(
                columnName = "id",
                name = "id",
                type = KColumnType.INT,
                tableName = "kt_integration_user",
                primaryKey = PrimaryKeyType.DEFAULT,
                nullable = false
            ),
            Field(
                columnName = "username",
                name = "username",
                type = KColumnType.VARCHAR,
                length = 255,
                tableName = "kt_integration_user"
            )
        )
    }
}

class TableOperationUtilTest {

    @Test
    fun `column differ reports added modified deleted and moved columns`() {
        val expected = listOf(
            field("id", KColumnType.INT, primaryKey = PrimaryKeyType.IDENTITY, nullable = false),
            field("tenant_id", KColumnType.INT, nullable = false),
            field("name", KColumnType.VARCHAR, length = 64, nullable = false, defaultValue = "'guest'", kDoc = "display name"),
            field("created_at", KColumnType.DATETIME, nullable = false)
        )
        val current = listOf(
            field("id", KColumnType.INT, primaryKey = PrimaryKeyType.IDENTITY, nullable = false),
            field("name", KColumnType.VARCHAR, length = 32, nullable = true),
            field("tenant_id", KColumnType.INT, nullable = false),
            field("legacy", KColumnType.TEXT)
        )

        val diff = columnDiffer(DBType.Mysql, expected, current)

        assertEquals(
            TableColumnDiff(
                toAdd = listOf(expected[3] to expected[2]),
                toModified = listOf(
                    Triple(expected[1], expected[0], current[2]),
                    Triple(expected[2], expected[1], current[1])
                ),
                toDelete = listOf(current[3])
            ),
            diff
        )
        assertEquals(listOf("tenant_id"), moveColumn(expected, current))
    }

    @Test
    fun `column differ returns stable empty diff for matching schemas`() {
        val expected = listOf(
            field("id", KColumnType.INT, primaryKey = PrimaryKeyType.IDENTITY, nullable = false),
            field("name", KColumnType.VARCHAR, length = 64, nullable = false)
        )

        assertEquals(
            TableColumnDiff(toAdd = emptyList(), toModified = emptyList(), toDelete = emptyList()),
            columnDiffer(DBType.Postgres, expected, expected.map { it.copy() })
        )
        assertEquals(emptyList(), moveColumn(emptyList(), expected))
    }

    @Test
    fun `column differ treats postgres physical type equivalents as stable`() {
        val expected = listOf(
            field("occurred_at", KColumnType.DATETIME),
            field("description", KColumnType.VARCHAR)
        )
        val current = listOf(
            field("occurred_at", KColumnType.TIMESTAMP),
            field("description", KColumnType.TEXT)
        )

        assertEquals(
            TableColumnDiff(toAdd = emptyList(), toModified = emptyList(), toDelete = emptyList()),
            columnDiffer(DBType.Postgres, expected, current)
        )
    }

    @Test
    fun `column differ preserves postgres physical type differences`() {
        val expected = listOf(
            field("occurred_at", KColumnType.DATETIME, scale = 3),
            field("description", KColumnType.VARCHAR, length = 64)
        )
        val current = listOf(
            field("occurred_at", KColumnType.TIMESTAMP, scale = 6),
            field("description", KColumnType.TEXT)
        )

        assertEquals(
            listOf(
                Triple(expected[0], null, current[0]),
                Triple(expected[1], expected[0], current[1])
            ),
            columnDiffer(DBType.Postgres, expected, current).toModified
        )
    }

    @Test
    fun `column differ ignores postgres column order`() {
        val expected = listOf(
            field("id", KColumnType.INT, nullable = false),
            field("age", KColumnType.BIGINT),
            field("created_at", KColumnType.DATETIME)
        )
        val current = listOf(
            field("id", KColumnType.INT, nullable = false),
            field("created_at", KColumnType.TIMESTAMP),
            field("age", KColumnType.BIGINT)
        )

        assertEquals(
            TableColumnDiff(toAdd = emptyList(), toModified = emptyList(), toDelete = emptyList()),
            columnDiffer(DBType.Postgres, expected, current)
        )
    }

    @Test
    fun `column differ treats sqlite storage-class equivalent definitions as stable`() {
        val expected = listOf(
            field("id", KColumnType.INT, primaryKey = PrimaryKeyType.DEFAULT, nullable = false),
            field("name", KColumnType.VARCHAR, length = 80),
            field("created_at", KColumnType.DATETIME),
            field("amount", KColumnType.DECIMAL, length = 12, scale = 2)
        )
        val current = listOf(
            field("id", KColumnType.INT, primaryKey = PrimaryKeyType.DEFAULT, nullable = false),
            field("name", KColumnType.TEXT),
            field("created_at", KColumnType.TEXT),
            field("amount", KColumnType.NUMERIC)
        )

        assertEquals(
            TableColumnDiff(toAdd = emptyList(), toModified = emptyList(), toDelete = emptyList()),
            columnDiffer(DBType.SQLite, expected, current)
        )
    }

    @Test
    fun `column differ ignores sqlite column order after appended add column`() {
        val expected = listOf(
            field("id", KColumnType.VARCHAR, length = 64, primaryKey = PrimaryKeyType.CUSTOM, nullable = false),
            field("name", KColumnType.VARCHAR, length = 80),
            field("age", KColumnType.BIGINT),
            field("create_time", KColumnType.DATETIME),
            field("update_time", KColumnType.DATETIME)
        )
        val current = listOf(
            field("id", KColumnType.TEXT, primaryKey = PrimaryKeyType.DEFAULT, nullable = false),
            field("name", KColumnType.TEXT),
            field("create_time", KColumnType.TEXT),
            field("update_time", KColumnType.TEXT),
            field("age", KColumnType.INT)
        )

        assertEquals(
            TableColumnDiff(toAdd = emptyList(), toModified = emptyList(), toDelete = emptyList()),
            columnDiffer(DBType.SQLite, expected, current)
        )
    }

    @Test
    fun `column differ matches oracle metadata columns case insensitively`() {
        val expected = listOf(
            field("id", KColumnType.INT, nullable = false),
            field("status", KColumnType.INT)
        )
        val current = listOf(
            field("ID", KColumnType.INT, nullable = false),
            field("STATUS", KColumnType.INT)
        )

        assertEquals(
            TableColumnDiff(toAdd = emptyList(), toModified = emptyList(), toDelete = emptyList()),
            columnDiffer(DBType.Oracle, expected, current)
        )
    }

    @Test
    fun `column differ matches sql server metadata columns case insensitively`() {
        val expected = listOf(
            field("id", KColumnType.INT, primaryKey = PrimaryKeyType.DEFAULT, nullable = false),
            field("name", KColumnType.VARCHAR, length = 80),
            field("created_at", KColumnType.DATETIME)
        )
        val current = listOf(
            field("ID", KColumnType.INT, primaryKey = PrimaryKeyType.DEFAULT, nullable = false),
            field("NAME", KColumnType.VARCHAR, length = 80),
            field("CREATED_AT", KColumnType.DATETIME)
        )

        assertEquals(
            TableColumnDiff(toAdd = emptyList(), toModified = emptyList(), toDelete = emptyList()),
            columnDiffer(DBType.Mssql, expected, current)
        )
        assertEquals(emptyList(), moveColumn(expected, current, DBType.Mssql))
    }

    @Test
    fun `column differ ignores sql server column order while move detection stays case insensitive`() {
        val expected = listOf(
            field("id", KColumnType.INT, primaryKey = PrimaryKeyType.DEFAULT, nullable = false),
            field("name", KColumnType.VARCHAR, length = 80),
            field("created_at", KColumnType.DATETIME)
        )
        val current = listOf(
            field("ID", KColumnType.INT, primaryKey = PrimaryKeyType.DEFAULT, nullable = false),
            field("CREATED_AT", KColumnType.DATETIME),
            field("NAME", KColumnType.VARCHAR, length = 80)
        )

        assertEquals(listOf("NAME"), moveColumn(expected, current, DBType.Mssql))
        assertEquals(
            TableColumnDiff(toAdd = emptyList(), toModified = emptyList(), toDelete = emptyList()),
            columnDiffer(DBType.Mssql, expected, current)
        )
    }

    @Test
    fun `ddl sync normalizes oracle columns on copies without mutating source fields`() {
        val table = TableOperation(RecordingWrapper(DBType.Oracle))
        val fields = listOf(
            field("id", KColumnType.INT, nullable = false),
            field("username", KColumnType.VARCHAR, length = 64)
        )

        val oracleFields = with(table) { fields.forDdlSync(DBType.Oracle) }
        val postgresFields = with(table) { fields.forDdlSync(DBType.Postgres) }

        assertEquals(listOf("ID", "USERNAME"), oracleFields.map { it.columnName })
        assertEquals(listOf("id", "username"), fields.map { it.columnName })
        assertNotSame(fields[0], oracleFields[0])
        assertNotSame(fields[1], oracleFields[1])
        assertSame(fields, postgresFields)
    }

    @Test
    fun `column differ treats oracle default numeric precision as stable`() {
        val expected = listOf(
            field("id", KColumnType.INT, primaryKey = PrimaryKeyType.DEFAULT, nullable = false),
            field("score", KColumnType.INT),
            field("flag", KColumnType.BIT)
        )
        val current = listOf(
            field("ID", KColumnType.INT, length = 10, primaryKey = PrimaryKeyType.DEFAULT, nullable = false),
            field("SCORE", KColumnType.INT, length = 10),
            field("FLAG", KColumnType.BIT, length = 1)
        )

        assertEquals(
            TableColumnDiff(toAdd = emptyList(), toModified = emptyList(), toDelete = emptyList()),
            columnDiffer(DBType.Oracle, expected, current)
        )
    }

    @Test
    fun `column differ treats dialect rendered default types as stable`() {
        listOf(DBType.Mysql, DBType.Mssql).forEach { dbType ->
            val expected = listOf(field("description", KColumnType.VARCHAR))
            val current = listOf(field("description", KColumnType.VARCHAR, length = 255))

            assertEquals(
                TableColumnDiff(toAdd = emptyList(), toModified = emptyList(), toDelete = emptyList()),
                columnDiffer(dbType, expected, current),
                "default VARCHAR length for $dbType"
            )
        }

        val oracleExpected = listOf(
            field("description", KColumnType.VARCHAR),
            field("created_at", KColumnType.DATETIME)
        )
        val oracleCurrent = listOf(
            field("DESCRIPTION", KColumnType.VARCHAR, length = 255),
            field("CREATED_AT", KColumnType.TIMESTAMP, length = 11, scale = 6)
        )

        assertEquals(
            TableColumnDiff(toAdd = emptyList(), toModified = emptyList(), toDelete = emptyList()),
            columnDiffer(DBType.Oracle, oracleExpected, oracleCurrent)
        )
    }

    @Test
    fun `column differ treats generated and custom primary keys as database primary keys`() {
        val dbTypes = listOf(DBType.Mysql, DBType.Postgres, DBType.SQLite, DBType.Mssql, DBType.Oracle)

        dbTypes.forEach { dbType ->
            val expected = listOf(
                field("id", KColumnType.VARCHAR, length = 64, primaryKey = PrimaryKeyType.CUSTOM, nullable = false),
                field("name", KColumnType.VARCHAR, length = 80),
                field("age", KColumnType.BIGINT),
                field("create_time", KColumnType.DATETIME),
                field("update_time", KColumnType.DATETIME)
            )
            val current = listOf(
                field(
                    if (dbType == DBType.Oracle) "ID" else "id",
                    KColumnType.VARCHAR,
                    length = 64,
                    primaryKey = PrimaryKeyType.DEFAULT,
                    nullable = false
                ),
                field(if (dbType == DBType.Oracle) "NAME" else "name", KColumnType.VARCHAR, length = 80),
                field(if (dbType == DBType.Oracle) "CREATE_TIME" else "create_time", KColumnType.DATETIME),
                field(if (dbType == DBType.Oracle) "UPDATE_TIME" else "update_time", KColumnType.DATETIME)
            )

            val diff = columnDiffer(dbType, expected, current)

            assertEquals(listOf(expected[2] to expected[1]), diff.toAdd, "toAdd for $dbType")
            assertEquals(emptyList(), diff.toModified, "toModified for $dbType")
            assertEquals(emptyList(), diff.toDelete, "toDelete for $dbType")
        }
    }

    @Test
    fun `index differ reports exact additions and deletions`() {
        val expected = listOf(
            KTableIndex("idx_account_name", arrayOf("tenant_id", "name"), "UNIQUE", "BTREE"),
            KTableIndex("idx_account_created", arrayOf("created_at"), "NORMAL", "BTREE")
        )
        val current = listOf(
            KTableIndex("idx_account_name", arrayOf("tenant_id", "name"), "UNIQUE", "BTREE"),
            KTableIndex("idx_account_legacy", arrayOf("legacy"), "NORMAL", "BTREE")
        )

        assertEquals(
            TableIndexDiff(
                toAdd = listOf(expected[1]),
                toDelete = listOf(current[1])
            ),
            indexDiffer(expected, current)
        )
    }

    @Test
    fun `postgres index differ normalizes method case and implicit btree defaults`() {
        val expected = listOf(
            KTableIndex("idx_explicit", arrayOf("value"), "NORMAL", "BTREE"),
            KTableIndex("idx_default", arrayOf("category")),
            KTableIndex("idx_unique", arrayOf("code"), "", "UNIQUE")
        )
        val current = listOf(
            KTableIndex("IDX_EXPLICIT", arrayOf("value"), "normal", "btree"),
            KTableIndex("IDX_DEFAULT", arrayOf("category"), "NORMAL", "btree"),
            KTableIndex("IDX_UNIQUE", arrayOf("code"), "UNIQUE", "btree")
        )

        assertEquals(
            TableIndexDiff(toAdd = emptyList(), toDelete = emptyList()),
            indexDiffer(expected, current, DBType.Postgres)
        )
    }

    @Test
    fun `index differ applies dialect default index definitions`() {
        val cases = listOf(
            DBType.Mysql to KTableIndex("IDX_DEFAULT", arrayOf("value"), "NORMAL", "BTREE"),
            DBType.Postgres to KTableIndex("IDX_DEFAULT", arrayOf("value"), "NORMAL", "btree"),
            DBType.SQLite to KTableIndex("IDX_DEFAULT", arrayOf("value"), "NORMAL", ""),
            DBType.Mssql to KTableIndex("IDX_DEFAULT", arrayOf("VALUE"), "NONCLUSTERED", ""),
            DBType.Oracle to KTableIndex("IDX_DEFAULT", arrayOf("VALUE"), "NORMAL", "")
        )

        cases.forEach { (dbType, current) ->
            assertEquals(
                TableIndexDiff(toAdd = emptyList(), toDelete = emptyList()),
                indexDiffer(listOf(KTableIndex("idx_default", arrayOf("value"))), listOf(current), dbType),
                dbType.name
            )
        }
    }

    @Test
    fun `postgres index differ preserves semantic index changes`() {
        val expected = listOf(
            KTableIndex("idx_method", arrayOf("value"), "NORMAL", "HASH"),
            KTableIndex("idx_unique", arrayOf("code"), "UNIQUE", "BTREE"),
            KTableIndex("idx_columns", arrayOf("tenant_id", "value"), "NORMAL", "BTREE")
        )
        val current = listOf(
            KTableIndex("idx_method", arrayOf("value"), "NORMAL", "btree"),
            KTableIndex("idx_unique", arrayOf("code"), "NORMAL", "btree"),
            KTableIndex("idx_columns", arrayOf("value", "tenant_id"), "NORMAL", "btree")
        )

        assertEquals(
            TableIndexDiff(toAdd = expected, toDelete = current),
            indexDiffer(expected, current, DBType.Postgres)
        )
    }

    @Test
    fun `metadata queries render syntax statements and map rows`() {
        val wrapper = RecordingWrapper(
            dbType = DBType.Mysql,
            objectResult = 1,
            listResult = listOf(
                mapOf(
                    "COLUMN_NAME" to "id",
                    "DATA_TYPE" to "int",
                    "LENGTH" to 0,
                    "SCALE" to 0,
                    "COLUMN_TYPE" to "int(11)",
                    "IS_NULLABLE" to "NO",
                    "PRIMARY_KEY" to "YES",
                    "IDENTITY" to "YES"
                )
            )
        )

        val exists = queryTableExistence("account", wrapper)
        wrapper.objectResult = "account table"
        val comment = queryTableComment("account", wrapper)
        val columns = queryTableColumns("account", wrapper).map { it.toFieldShape() }

        assertEquals(true, exists)
        assertEquals("account table", comment)
        assertEquals(
            listOf(FieldShape("id", KColumnType.INT, 11, 0, "account", false, PrimaryKeyType.IDENTITY, null, null)),
            columns
        )
        assertEquals(
            listOf(
                QueryShape(
                    sql = "SELECT COUNT(*) FROM `INFORMATION_SCHEMA`.`TABLES` WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :tableName",
                    params = mapOf("tableName" to "account"),
                    statementType = "Select"
                ),
                QueryShape(
                    sql = "SELECT TABLE_COMMENT FROM `INFORMATION_SCHEMA`.`TABLES` WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :tableName",
                    params = mapOf("tableName" to "account"),
                    statementType = "Select"
                ),
                QueryShape(
                    sql = "SELECT c.COLUMN_NAME, c.DATA_TYPE, c.CHARACTER_MAXIMUM_LENGTH AS LENGTH, c.NUMERIC_SCALE AS SCALE, c.COLUMN_TYPE, c.IS_NULLABLE, c.COLUMN_DEFAULT, c.COLUMN_COMMENT, CASE WHEN c.EXTRA = 'auto_increment' THEN 'YES' ELSE 'NO' END AS IDENTITY, CASE WHEN c.COLUMN_KEY = 'PRI' THEN 'YES' ELSE 'NO' END AS PRIMARY_KEY FROM `INFORMATION_SCHEMA`.`COLUMNS` AS `c` WHERE c.TABLE_SCHEMA = DATABASE() AND c.TABLE_NAME = :tableName ORDER BY ORDINAL_POSITION ASC",
                    params = mapOf("tableName" to "account"),
                    statementType = "Select"
                )
            ),
            wrapper.queries.map { it.toQueryShape() }
        )
    }

    @Test
    fun `table operation builds drop and truncate tasks through database statements`() {
        val wrapper = RecordingWrapper(DBType.SQLite)
        val table = TableOperation(wrapper)
        val dropTask = table.buildDropTableTask("account")
        val truncateTask = table.buildTruncateTableTask("account", restartIdentity = true)

        assertEquals(
            listOf(
                ActionShape(
                    sql = """DROP TABLE IF EXISTS "account"""",
                    params = emptyMap(),
                    operationType = KOperationType.DROP,
                    statement = SqlDdlStatement.DropTable(SqlIdentifier.of("account"), ifExists = true)
                )
            ),
            dropTask.atomicTasks.map { it.toActionShape() }
        )
        assertEquals(
            listOf(
                ActionShape(
                    sql = """DELETE FROM "account"""",
                    params = emptyMap(),
                    operationType = KOperationType.TRUNCATE,
                    statement = SqlDmlStatement.Delete(SqlTable.Ident("account"))
                )
            ),
            truncateTask.atomicTasks.map { it.toActionShape() }
        )
    }

    @Test
    fun `oracle drop table task skips missing table`() {
        val wrapper = RecordingWrapper(DBType.Oracle, objectResults = mutableListOf(0))
        val table = TableOperation(wrapper)

        table.buildDropTableTask("account").execute(wrapper)

        assertEquals(emptyList(), wrapper.actions)
        assertEquals(
            listOf(
                QueryShape(
                    sql = """SELECT COUNT(*) FROM "ALL_TABLES" WHERE owner = :dbName AND table_name = UPPER(:tableName)""",
                    params = mapOf("dbName" to "KRONOS", "tableName" to "account"),
                    statementType = "Select"
                )
            ),
            wrapper.queries.map { it.toQueryShape() }
        )
    }

    @Test
    fun `table operation exposes syntax DDL builder shapes`() {
        val table = TableOperation(RecordingWrapper(DBType.Mysql))
        val column = SqlColumnDefinition(
            name = SqlIdentifier.of("flag"),
            type = SqlType.Boolean,
            nullable = false,
            primaryKey = SqlPrimaryKeyMode.NotPrimary,
            defaultValue = SqlExpr.StringLiteral("0")
        )

        assertEquals(
            listOf(
                SqlDdlStatement.CreateIndex(
                    indexName = SqlIdentifier.of("idx_archive_status"),
                    tableName = SqlIdentifier.of("tb_archive"),
                    columns = listOf(SqlIdentifier.of("status")),
                    unique = true
                ),
                SqlDdlStatement.DropIndex(SqlIdentifier.of("idx_archive_status"), SqlIdentifier.of("tb_archive")),
                SqlDdlStatement.AlterTable.AddColumn(SqlIdentifier.of("tb_archive"), column),
                SqlDdlStatement.AlterTable.DropColumn(SqlIdentifier.of("tb_archive"), SqlIdentifier.of("flag")),
                SqlDdlStatement.AlterTable.ModifyColumn(SqlIdentifier.of("tb_archive"), column),
                SqlDmlStatement.Truncate(SqlTable.Ident("tb_archive"), restartIdentity = false)
            ),
            listOf(
                table.buildCreateIndexStatement("idx_archive_status", "tb_archive", listOf("status"), unique = true),
                table.buildDropIndexStatement("idx_archive_status", "tb_archive"),
                table.buildAddColumnStatement("tb_archive", column),
                table.buildDropColumnStatement("tb_archive", "flag"),
                table.buildModifyColumnStatement("tb_archive", column),
                table.buildTruncateTableStatement("tb_archive", restartIdentity = false)
            )
        )
    }

    @Test
    fun `create table statement maps column types primary keys defaults and indexes`() {
        val fields = mutableListOf(
            ddlField("bit_col", KColumnType.BIT, primaryKey = PrimaryKeyType.DEFAULT, nullable = false),
            ddlField("int_col", KColumnType.INT, primaryKey = PrimaryKeyType.IDENTITY),
            ddlField("long_col", KColumnType.BIGINT, primaryKey = PrimaryKeyType.UUID),
            ddlField("float_col", KColumnType.FLOAT, primaryKey = PrimaryKeyType.SNOWFLAKE),
            ddlField("double_col", KColumnType.DOUBLE, primaryKey = PrimaryKeyType.CUSTOM),
            ddlField("decimal_col", KColumnType.DECIMAL, length = 12, scale = 4),
            ddlField("numeric_col", KColumnType.NUMERIC),
            ddlField("varchar_col", KColumnType.VARCHAR, length = 64, defaultValue = "guest"),
            ddlField("text_col", KColumnType.TEXT),
            ddlField("date_col", KColumnType.DATE),
            ddlField("time_col", KColumnType.TIME),
            ddlField("timestamp_col", KColumnType.TIMESTAMP),
            ddlField("json_col", KColumnType.JSON),
            ddlField("geometry_col", KColumnType.GEOMETRY),
            ddlField("point_col", KColumnType.POINT),
            ddlField("line_col", KColumnType.LINESTRING),
            ddlField("binary_col", KColumnType.BINARY),
            ddlField("undefined_col", KColumnType.UNDEFINED)
        )
        val indexes = mutableListOf(
            KTableIndex("idx_manual_unique", arrayOf("varchar_col"), "NORMAL", "UNIQUE"),
            KTableIndex("idx_manual_hash", arrayOf("json_col"), "NORMAL", "HASH")
        )

        val statement = TableOperation(RecordingWrapper(DBType.Mysql))
            .buildCreateTableStatement(ManualDdlKPojo(fields, indexes))

        assertEquals(
            SqlDdlStatement.CreateTable(
                tableName = SqlIdentifier.of("manual_ddl"),
                columns = listOf(
                    ddlColumn("bit_col", SqlType.Boolean, SqlPrimaryKeyMode.Primary, nullable = false),
                    ddlColumn("int_col", SqlType.Int, SqlPrimaryKeyMode.Identity),
                    ddlColumn("long_col", SqlType.Long, SqlPrimaryKeyMode.Uuid),
                    ddlColumn("float_col", SqlType.Float, SqlPrimaryKeyMode.Snowflake),
                    ddlColumn("double_col", SqlType.Double, SqlPrimaryKeyMode.Primary),
                    ddlColumn("decimal_col", SqlType.Decimal(12 to 4)),
                    ddlColumn("numeric_col", SqlType.Decimal(null)),
                    ddlColumn("varchar_col", SqlType.Varchar(64), defaultValue = SqlExpr.StringLiteral("guest")),
                    ddlColumn("text_col", SqlType.Named("TEXT")),
                    ddlColumn("date_col", SqlType.Date),
                    ddlColumn("time_col", SqlType.Time()),
                    ddlColumn("timestamp_col", SqlType.Timestamp()),
                    ddlColumn("json_col", SqlType.Json),
                    ddlColumn("geometry_col", SqlType.Geometry),
                    ddlColumn("point_col", SqlType.Point),
                    ddlColumn("line_col", SqlType.LineString),
                    ddlColumn("binary_col", SqlType.Named("BINARY")),
                    ddlColumn("undefined_col", SqlType.Named("UNDEFINED"))
                ),
                indexes = listOf(
                    SqlIndexDefinition(
                        name = SqlIdentifier.of("idx_manual_unique"),
                        columns = listOf(SqlIdentifier.of("varchar_col")),
                        unique = true,
                        method = null
                    ),
                    SqlIndexDefinition(
                        name = SqlIdentifier.of("idx_manual_hash"),
                        columns = listOf(SqlIdentifier.of("json_col")),
                        unique = false,
                        method = "HASH"
                    )
                ),
                comment = "manual table"
            ),
            statement
        )
    }

    @Test
    fun `table operation public drop and truncate entrypoints execute rendered tasks`() {
        val wrapper = RecordingWrapper(DBType.SQLite)
        val table = TableOperation(wrapper)

        table.dropTable("account", "audit_log")
        table.truncateTable("account", restartIdentity = false)

        assertEquals(
            listOf(
                ActionShape(
                    sql = """DROP TABLE IF EXISTS "account"""",
                    params = emptyMap(),
                    operationType = KOperationType.DROP,
                    statement = SqlDdlStatement.DropTable(SqlIdentifier.of("account"), ifExists = true)
                ),
                ActionShape(
                    sql = """DROP TABLE IF EXISTS "audit_log"""",
                    params = emptyMap(),
                    operationType = KOperationType.DROP,
                    statement = SqlDdlStatement.DropTable(SqlIdentifier.of("audit_log"), ifExists = true)
                ),
                ActionShape(
                    sql = """DELETE FROM "account"""",
                    params = emptyMap(),
                    operationType = KOperationType.TRUNCATE,
                    statement = SqlDmlStatement.Delete(SqlTable.Ident("account"))
                )
            ),
            wrapper.actions.map { it.toActionShape() }
        )
        assertEquals(2, wrapper.transactionCount)
    }

    @Test
    fun `table operation public create exists drop and truncate instance entrypoints execute syntax tasks`() {
        val wrapper = RecordingWrapper(DBType.Mysql, objectResults = mutableListOf(1))
        val table = TableOperation(wrapper)
        val pojo = ManualDdlKPojo(
            mutableListOf(
                ddlField("id", KColumnType.INT, primaryKey = PrimaryKeyType.IDENTITY, nullable = false),
                ddlField("name", KColumnType.VARCHAR, length = 32)
            ),
            mutableListOf(KTableIndex("idx_manual_name", arrayOf("name"), "NORMAL", "BTREE"))
        )

        val exists = table.exists(pojo)
        table.createTable(pojo)
        table.dropTable(pojo)
        table.truncateTable(pojo, restartIdentity = false)

        assertEquals(true, exists)
        assertEquals(
            listOf(
                ActionMetadataShape(KOperationType.CREATE, "CreateTable"),
                ActionMetadataShape(KOperationType.CREATE, "CreateIndex"),
                ActionMetadataShape(KOperationType.DROP, "DropTable"),
                ActionMetadataShape(KOperationType.TRUNCATE, "Truncate")
            ),
            wrapper.actions.map { it.toActionMetadataShape() }
        )
        assertEquals(3, wrapper.transactionCount)
    }

    @Test
    fun `table operation public statement and task builders expose instance metadata`() {
        val wrapper = RecordingWrapper(DBType.Postgres)
        val table = TableOperation(wrapper)
        val pojo = ManualDdlKPojo(
            mutableListOf(
                ddlField("id", KColumnType.INT, primaryKey = PrimaryKeyType.DEFAULT, nullable = false),
                ddlField("name", KColumnType.VARCHAR, length = 32)
            ),
            mutableListOf(KTableIndex("idx_manual_name", arrayOf("name"), "UNIQUE", "BTREE"))
        )

        val create = table.buildCreateTableStatement(pojo)
        val drop = table.buildDropTableStatement(pojo, ifExists = true)
        val truncate = table.buildTruncateTableStatement(pojo, restartIdentity = false)
        val truncateTask = table.buildTruncateTableTask("manual_ddl", restartIdentity = false).atomicTasks.single()
        table.truncateTable("manual_ddl", "manual_archive", restartIdentity = false)

        assertEquals(SqlIdentifier.of("manual_ddl"), create.tableName)
        assertEquals(listOf("id", "name"), create.columns.map { it.name.canonical })
        assertEquals(listOf("idx_manual_name"), create.indexes.map { it.name.canonical })
        assertEquals(true, create.indexes.single().unique)
        assertEquals(SqlIdentifier.of("manual_ddl"), drop.tableName)
        assertEquals(true, drop.ifExists)
        assertEquals("manual_ddl", truncate.table.name)
        assertEquals(false, truncate.restartIdentity)
        assertEquals("""TRUNCATE TABLE "public"."manual_ddl"""", truncateTask.sql)
        assertEquals(
            listOf(
                ActionMetadataShape(KOperationType.TRUNCATE, "Truncate"),
                ActionMetadataShape(KOperationType.TRUNCATE, "Truncate")
            ),
            wrapper.actions.map { it.toActionMetadataShape() }
        )
    }

    @Test
    fun `sync table creates missing table and returns false`() {
        val wrapper = RecordingWrapper(DBType.Mysql, objectResults = mutableListOf(0))
        val table = TableOperation(wrapper)
        val pojo = ManualDdlKPojo(mutableListOf(ddlField("id", KColumnType.INT, primaryKey = PrimaryKeyType.IDENTITY)))

        val created = table.syncTable(pojo)

        assertEquals(false, created)
        assertEquals(
            listOf(ActionMetadataShape(KOperationType.CREATE, "CreateTable")),
            wrapper.actions.map { it.toActionMetadataShape() }
        )
        assertEquals(1, wrapper.transactionCount)
    }

    @Test
    fun `sync table diffs existing table and executes alter statements`() {
        val wrapper = RecordingWrapper(
            dbType = DBType.Mysql,
            objectResults = mutableListOf(1, "old comment"),
            listResults = mutableListOf(emptyList(), emptyList())
        )
        val table = TableOperation(wrapper)
        val pojo = ManualDdlKPojo(
            mutableListOf(
                ddlField("id", KColumnType.INT, primaryKey = PrimaryKeyType.IDENTITY, nullable = false),
                ddlField("name", KColumnType.VARCHAR, length = 32)
            ),
            mutableListOf(KTableIndex("idx_manual_name", arrayOf("name"), "NORMAL", "BTREE"))
        )

        val existing = table.syncTable(pojo)

        assertEquals(true, existing)
        assertEquals(
            listOf(
                ActionMetadataShape(KOperationType.ALTER, "SetTableComment"),
                ActionMetadataShape(KOperationType.ALTER, "AddColumn"),
                ActionMetadataShape(KOperationType.ALTER, "AddColumn"),
                ActionMetadataShape(KOperationType.ALTER, "CreateIndex")
            ),
            wrapper.actions.map { it.toActionMetadataShape() }
        )
        assertEquals(1, wrapper.transactionCount)
    }

    @Test
    fun `oracle sync table does not uppercase static kpojo column metadata`() {
        val wrapper = RecordingWrapper(
            dbType = DBType.Oracle,
            objectResults = mutableListOf(1, "static ddl cache table"),
            listResults = mutableListOf(
                listOf(
                    oracleColumnRow("ID", "NUMBER", length = 22, precision = 10, nullable = "N", primaryKey = "1"),
                    oracleColumnRow("USERNAME", "VARCHAR2", length = 255)
                ),
                emptyList()
            )
        )
        val table = TableOperation(wrapper)
        val pojo = StaticDdlCacheKPojo()
        val originalColumnNames = listOf("id", "username")

        assertEquals(originalColumnNames, pojo.resolveRuntimeMetadata().allColumns.map { it.columnName })

        val existing = table.syncTable(pojo)

        assertEquals(true, existing)
        assertEquals(originalColumnNames, StaticDdlCacheKPojo.staticColumns.map { it.columnName })
        assertEquals(originalColumnNames, StaticDdlCacheKPojo().resolveRuntimeMetadata().allColumns.map { it.columnName })
        assertEquals(emptyList(), wrapper.actions)
    }

    @Test
    fun `execute ddl statements runs concurrent index outside transactional batch`() {
        val wrapper = RecordingWrapper(DBType.Postgres)
        val table = TableOperation(wrapper)
        val normal = SqlDdlStatement.CreateIndex(
            indexName = SqlIdentifier.of("idx_account_name"),
            tableName = SqlIdentifier.of("account"),
            columns = listOf(SqlIdentifier.of("name"))
        )
        val concurrent = SqlDdlStatement.CreateIndex(
            indexName = SqlIdentifier.of("idx_account_status"),
            tableName = SqlIdentifier.of("account"),
            columns = listOf(SqlIdentifier.of("status")),
            concurrently = true
        )

        table.executeDdlStatements(listOf(normal, concurrent), KOperationType.CREATE)

        assertEquals(
            listOf(
                ActionShape(
                    sql = """CREATE INDEX "idx_account_name" ON "account" ("name")""",
                    params = emptyMap(),
                    operationType = KOperationType.CREATE,
                    statement = normal
                ),
                ActionShape(
                    sql = """CREATE INDEX CONCURRENTLY "idx_account_status" ON "account" ("status")""",
                    params = emptyMap(),
                    operationType = KOperationType.CREATE,
                    statement = concurrent
                )
            ),
            wrapper.actions.map { it.toActionShape() }
        )
        assertEquals(1, wrapper.transactionCount)
    }

    private fun field(
        name: String,
        type: KColumnType,
        length: Int = 0,
        scale: Int = 0,
        primaryKey: PrimaryKeyType = PrimaryKeyType.NOT,
        nullable: Boolean = true,
        defaultValue: String? = null,
        kDoc: String? = null
    ) = Field(
        columnName = name,
        type = type,
        length = length,
        scale = scale,
        tableName = "account",
        primaryKey = primaryKey,
        nullable = nullable,
        defaultValue = defaultValue,
        kDoc = kDoc
    )

    private fun ddlField(
        name: String,
        type: KColumnType,
        length: Int = 0,
        scale: Int = 0,
        primaryKey: PrimaryKeyType = PrimaryKeyType.NOT,
        nullable: Boolean = true,
        defaultValue: String? = null
    ) = Field(
        columnName = name,
        type = type,
        length = length,
        scale = scale,
        tableName = "manual_ddl",
        primaryKey = primaryKey,
        nullable = nullable,
        defaultValue = defaultValue
    )

    private fun oracleColumnRow(
        name: String,
        dataType: String,
        length: Int,
        precision: Int = 0,
        scale: Int = 0,
        nullable: String = "Y",
        primaryKey: String = "0",
        defaultValue: String? = null,
        comment: String? = null
    ): Map<String, Any> = mutableMapOf<String, Any>(
        "COLUMN_NAME" to name,
        "DATA_TYPE" to dataType,
        "LENGTH" to length,
        "PRECISION" to precision,
        "SCALE" to scale,
        "IS_NULLABLE" to nullable,
        "PRIMARY_KEY" to primaryKey
    ).apply {
        defaultValue?.let { put("COLUMN_DEFAULT", it) }
        comment?.let { put("COLUMN_COMMENT", it) }
    }

    private fun ddlColumn(
        name: String,
        type: SqlType,
        primaryKey: SqlPrimaryKeyMode = SqlPrimaryKeyMode.NotPrimary,
        nullable: Boolean = true,
        defaultValue: SqlExpr? = null
    ) = SqlColumnDefinition(
        name = SqlIdentifier.of(name),
        type = type,
        nullable = nullable,
        primaryKey = primaryKey,
        defaultValue = defaultValue
    )

    private data class FieldShape(
        val columnName: String,
        val type: KColumnType,
        val length: Int,
        val scale: Int,
        val tableName: String,
        val nullable: Boolean,
        val primaryKey: PrimaryKeyType,
        val defaultValue: String?,
        val kDoc: String?
    )

    private fun Field.toFieldShape() = FieldShape(
        columnName = columnName,
        type = type,
        length = length,
        scale = scale,
        tableName = tableName,
        nullable = nullable,
        primaryKey = primaryKey,
        defaultValue = defaultValue,
        kDoc = kDoc
    )

    private data class QueryShape(
        val sql: String,
        val params: Map<String, Any?>,
        val statementType: String
    )

    private fun KAtomicQueryTask.toQueryShape() = QueryShape(
        sql = sql,
        params = paramMap,
        statementType = statement!!::class.simpleName!!
    )

    private data class ActionShape(
        val sql: String,
        val params: Map<String, Any?>,
        val operationType: KOperationType,
        val statement: Any?
    )

    private fun KAtomicActionTask.toActionShape() = ActionShape(sql, paramMap, operationType, statement)

    private data class ActionMetadataShape(
        val operationType: KOperationType,
        val statementType: String
    )

    private fun KAtomicActionTask.toActionMetadataShape() = ActionMetadataShape(
        operationType = operationType,
        statementType = statement!!::class.simpleName!!
    )

    private class RecordingWrapper(
        override val dbType: DBType,
        var objectResult: Any? = null,
        private val listResult: List<Map<String, Any>> = emptyList(),
        val objectResults: MutableList<Any?> = mutableListOf(),
        val listResults: MutableList<List<Map<String, Any>>> = mutableListOf()
    ) : KronosDataSourceWrapper {
        val queries = mutableListOf<KAtomicQueryTask>()
        val actions = mutableListOf<KAtomicActionTask>()
        val batchActions = mutableListOf<KronosAtomicBatchTask>()
        var transactionCount = 0
        override val url: String = when (dbType) {
            DBType.Mysql -> "jdbc:mysql://localhost:3306/kronos"
            DBType.Postgres -> "jdbc:postgresql://localhost:5432/kronos"
            DBType.SQLite -> "jdbc:sqlite://tmp/kronos.db"
            DBType.Mssql -> "jdbc:sqlserver://localhost:1433;databaseName=test"
            DBType.Oracle -> "jdbc:oracle:thin:@localhost:1521/FREEPDB1"
            else -> "jdbc:mysql://localhost:3306/kronos"
        }
        override val userName: String = "kronos"
        override fun toList(task: KAtomicQueryTask): List<Any?> {
            queries += task
            return if (listResults.isNotEmpty()) listResults.removeAt(0) else listResult
        }

        override fun first(task: KAtomicQueryTask): Any? {
            queries += task
            return if (objectResults.isNotEmpty()) objectResults.removeAt(0) else objectResult
        }
        override fun update(task: KAtomicActionTask): Int {
            actions += task
            return 1
        }
        override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
            batchActions += task
            return IntArray(task.paramMapArr?.size ?: 0) { 1 }
        }
        override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? {
            transactionCount++
            return TransactionScope().block()
        }
    }
}
