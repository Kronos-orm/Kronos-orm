/**
 * Copyright 2022-2025 kronos-orm
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

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.database.DatabaseCreateTable
import com.kotlinorm.database.DatabaseSyncTable
import com.kotlinorm.database.SqlManager.renderStatement
import com.kotlinorm.database.SqlManager.statementsOf
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.union.UnionClause
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.statement.SqlIndexDefinition
import com.kotlinorm.syntax.statement.SqlPrimaryKeyMode
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.createInstance

class TableOperation(private val wrapper: KronosDataSourceWrapper) {
    val dataSource by lazy { wrapper.orDefault() }


    /**
     * Query table existence
     *
     * @param instance Table instance
     */
    inline fun <reified T : KPojo> exists(instance: T = T::class.createInstance()) =
        queryTableExistence(instance.__tableName, dataSource)

    /**
     * Query table existence
     *
     * @param tableName Table name
     */
    fun exists(tableName: String) = queryTableExistence(tableName, dataSource)


    /**
     * Create table
     *
     * @param instance Table instance
     */
    inline fun <reified T : KPojo> createTable(instance: T = T::class.createInstance()) {
        val statements = statementsOf(dataSource.dbType).createTable(
            DatabaseCreateTable(
                tableName = instance.__tableName,
                tableComment = instance.__tableComment,
                columns = instance.kronosColumns().filter { it.isColumn },
                indexes = instance.kronosTableIndex()
            )
        )
        executeDdlStatements(statements, KOperationType.CREATE)
    }

    inline fun <reified T : KPojo> createTable(
        instance: T = T::class.createInstance(),
        query: KSelectable<*>
    ) {
        buildCreateTableAsSelectTask(instance, query).execute(dataSource)
    }

    inline fun <reified T : KPojo> createTable(
        instance: T = T::class.createInstance(),
        query: UnionClause<*>
    ) {
        buildCreateTableAsSelectTask(instance, query).execute(dataSource)
    }

    inline fun <reified T : KPojo> buildCreateTableAsSelectTask(
        instance: T = T::class.createInstance(),
        query: KSelectable<*>
    ): KronosActionTask {
        return buildCreateTableAsSelectTaskForQuery(instance.__tableName, query)
    }

    inline fun <reified T : KPojo> buildCreateTableAsSelectTask(
        instance: T = T::class.createInstance(),
        query: UnionClause<*>
    ): KronosActionTask {
        return buildCreateTableAsSelectTaskForQuery(instance.__tableName, query)
    }

    @PublishedApi
    internal fun buildCreateTableAsSelectTaskForQuery(
        tableName: String,
        query: KSelectable<*>
    ): KronosActionTask {
        val plan = query.toSqlQueryPlan(dataSource)
        val statement = SqlDdlStatement.CreateTableAsSelect(
            tableName = SqlIdentifier.of(tableName),
            query = plan.query
        )
        return buildCreateTableAsSelectTaskFromQuery(
            statement,
            plan.parameters,
            fieldsMapCache[query.pojo.kClass()] ?: emptyMap()
        )
    }

    fun buildCreateTableAsSelectTaskFromQuery(
        statement: SqlDdlStatement.CreateTableAsSelect,
        parameterValues: Map<String, Any?> = emptyMap(),
        fieldsMap: Map<String, Field> = emptyMap()
    ): KronosActionTask {
        val rendered = renderStatement(dataSource, statement, parameterValues, fieldsMap)
        return KronosAtomicActionTask(
            rendered.sql,
            rendered.parameters,
            KOperationType.CREATE,
            statement = statement
        ).toKronosActionTask()
    }

    /**
     * Drop table
     *
     * @param instance Table instance
     */
    inline fun <reified T : KPojo> dropTable(instance: T = T::class.createInstance()) =
        buildDropTableTask(instance.__tableName).execute(dataSource)

    /**
     * Drop table
     *
     * @param tableNames Table names
     */
    fun dropTable(vararg tableNames: String) =
        tableNames.map { buildDropTableTask(it) }
            .merge()
            .execute(dataSource)

    /**
     * Truncate table
     *
     * @param instance Table instance
     * @param restartIdentity Whether to reset the auto-increment value，only for `PostgreSQL` and `sqlite` for `reset auto increment`, default is `true`
     */
    inline fun <reified T : KPojo> truncateTable(
        instance: T = T::class.createInstance(),
        restartIdentity: Boolean = true
    ) = buildTruncateTableTask(instance.__tableName, restartIdentity).execute(dataSource)

    /**
     * Truncate table
     *
     * @param tableName Table name
     * @param restartIdentity Whether to reset the auto-increment value，only for `PostgreSQL` and `sqlite` for `reset auto increment`, default is `true`
     */
    fun truncateTable(vararg tableName: String, restartIdentity: Boolean = true) =
        tableName.map { name ->
            buildTruncateTableTask(name, restartIdentity).atomicTasks
        }.flatten().toKronosActionTask().execute(dataSource)

    /**
     * Synchronize table structure
     *
     * @param instance Table instance
     * @return Whether the table is created
     */
    inline fun <reified T : KPojo> syncTable(instance: T = T::class.createInstance()): Boolean {
        // 表名
        val tableName = instance.__tableName

        // 不存在就创建
        if (!queryTableExistence(tableName, dataSource)) {
            createTable(instance)
            // Table does not exist, create it successfully and return false
            return false
        }
        // 数据库类型
        val dbType = dataSource.dbType

        // 实体类列信息
        val kronosColumns = instance.kronosColumns().asSequence().filter { it.isColumn }.map { col ->
            if (dbType == DBType.Oracle) {
                col.columnName = col.columnName.uppercase()
            }
            col
        }.toList()
        // 从实例中获取索引(oracle 需要 转大写)
        val kronosIndexes = instance.kronosTableIndex()
        val originalTableComment = queryTableComment(tableName, dataSource)
        val tableComment = instance.__tableComment

        // 获取实际表字段信息
        val tableColumns = queryTableColumns(tableName, dataSource)
        // 获取实际表索引信息
        val tableIndexes = queryTableIndexes(tableName, dataSource)

        // 新增、修改、删除字段
        val diffColumns = columnDiffer(dbType, kronosColumns, tableColumns).apply { doLog(tableName) }
        val diffIndexes = indexDiffer(kronosIndexes, tableIndexes)

        val statements = statementsOf(dataSource.dbType).syncTable(
            DatabaseSyncTable(tableName, originalTableComment, tableComment, diffColumns, diffIndexes)
        )
        executeDdlStatements(statements, KOperationType.ALTER)
        return true
    }

    @PublishedApi
    internal fun executeDdlStatements(statements: List<SqlStatement>, operationType: KOperationType) {
        val (concurrent, transactional) = statements.partition { it.isConcurrentIndexStatement() }
        if (transactional.isNotEmpty()) {
            transactional.map { it.toActionTask(operationType) }.toKronosActionTask().execute(dataSource)
        }
        concurrent.forEach { dataSource.update(it.toActionTask(operationType)) }
    }

    private fun SqlStatement.isConcurrentIndexStatement(): Boolean =
        this is SqlDdlStatement.CreateIndex && concurrently

    private fun SqlStatement.toActionTask(operationType: KOperationType): KronosAtomicActionTask {
        val rendered = renderStatement(dataSource, this)
        return KronosAtomicActionTask(
            rendered.sql,
            rendered.parameters,
            operationType,
            statement = this
        )
    }

    @PublishedApi
    internal fun Field.toSqlColumnDefinition(): SqlColumnDefinition =
        SqlColumnDefinition(
            name = SqlIdentifier.of(columnName),
            type = type.toSqlType(length, scale),
            nullable = nullable,
            primaryKey = primaryKey.toSqlPrimaryKeyMode(),
            defaultValue = defaultValue?.let { SqlExpr.StringLiteral(it) }
        )

    @PublishedApi
    internal fun KTableIndex.toSqlIndexDefinition(): SqlIndexDefinition {
        val normalizedType = type.uppercase()
        val normalizedMethod = method.uppercase()
        return SqlIndexDefinition(
            name = SqlIdentifier.of(name),
            columns = columns.map { SqlIdentifier.of(it) },
            unique = normalizedType == "UNIQUE" || normalizedMethod == "UNIQUE",
            method = method.takeUnless {
                it.isBlank() ||
                    it.equals("UNIQUE", ignoreCase = true) ||
                    it.equals(type, ignoreCase = true)
            }
        )
    }

    inline fun <reified T : KPojo> buildCreateTableStatement(
        instance: T = T::class.createInstance()
    ): SqlDdlStatement.CreateTable =
        SqlDdlStatement.CreateTable(
            tableName = SqlIdentifier.of(instance.__tableName),
            columns = instance.kronosColumns()
                .filter { it.isColumn }
                .map { it.toSqlColumnDefinition() },
            indexes = instance.kronosTableIndex().map { it.toSqlIndexDefinition() },
            comment = instance.__tableComment
        )

    inline fun <reified T : KPojo> buildCreateTableAsSelectStatement(
        instance: T = T::class.createInstance(),
        query: KSelectable<*>,
        parameterValues: MutableMap<String, Any?> = mutableMapOf()
    ): SqlDdlStatement.CreateTableAsSelect {
        return buildCreateTableAsSelectSyntaxStatement(instance.__tableName, query, parameterValues)
    }

    inline fun <reified T : KPojo> buildCreateTableAsSelectStatement(
        instance: T = T::class.createInstance(),
        query: UnionClause<*>,
        parameterValues: MutableMap<String, Any?> = mutableMapOf(),
        parameterFields: MutableMap<String, Field> = mutableMapOf()
    ): SqlDdlStatement.CreateTableAsSelect {
        return buildCreateTableAsSelectSyntaxStatement(instance.__tableName, query, parameterValues, parameterFields)
    }

    @PublishedApi
    internal fun buildCreateTableAsSelectSyntaxStatement(
        tableName: String,
        query: KSelectable<*>,
        parameterValues: MutableMap<String, Any?>
    ): SqlDdlStatement.CreateTableAsSelect {
        val plan = query.toSqlQueryPlan(dataSource)
        parameterValues.putAll(plan.parameters)
        return SqlDdlStatement.CreateTableAsSelect(
            tableName = SqlIdentifier.of(tableName),
            query = plan.query
        )
    }

    @PublishedApi
    internal fun buildCreateTableAsSelectSyntaxStatement(
        tableName: String,
        query: UnionClause<*>,
        parameterValues: MutableMap<String, Any?>,
        parameterFields: MutableMap<String, Field>
    ): SqlDdlStatement.CreateTableAsSelect {
        val statement = buildCreateTableAsSelectSyntaxStatement(tableName, query as KSelectable<*>, parameterValues)
        parameterFields.putAll(fieldsMapCache[query.pojo.kClass()] ?: emptyMap())
        return statement
    }

    fun buildDropTableStatement(tableName: String, ifExists: Boolean = false): SqlDdlStatement.DropTable =
        SqlDdlStatement.DropTable(
            tableName = SqlIdentifier.of(tableName),
            ifExists = ifExists
        )

    inline fun <reified T : KPojo> buildDropTableStatement(
        instance: T = T::class.createInstance(),
        ifExists: Boolean = false
    ): SqlDdlStatement.DropTable =
        buildDropTableStatement(instance.__tableName, ifExists)

    fun buildDropTableTask(tableName: String): KronosActionTask {
        return statementsOf(dataSource.dbType)
            .dropTable(tableName, true)
            .map { it.toActionTask(KOperationType.DROP) }
            .toKronosActionTask()
    }

    fun buildTruncateTableStatement(tableName: String, restartIdentity: Boolean = true): SqlDmlStatement.Truncate =
        SqlDmlStatement.Truncate(
            table = SqlTable.Ident(tableName),
            restartIdentity = restartIdentity
        )

    inline fun <reified T : KPojo> buildTruncateTableStatement(
        instance: T = T::class.createInstance(),
        restartIdentity: Boolean = true
    ): SqlDmlStatement.Truncate =
        buildTruncateTableStatement(instance.__tableName, restartIdentity)

    fun buildTruncateTableTask(tableName: String, restartIdentity: Boolean = true): KronosActionTask {
        return statementsOf(dataSource.dbType)
            .truncateTable(tableName, restartIdentity)
            .map { it.toActionTask(KOperationType.TRUNCATE) }
            .toKronosActionTask()
    }

    fun buildCreateIndexStatement(
        indexName: String,
        tableName: String,
        columns: List<String>,
        unique: Boolean = false
    ): SqlDdlStatement.CreateIndex =
        SqlDdlStatement.CreateIndex(
            indexName = SqlIdentifier.of(indexName),
            tableName = SqlIdentifier.of(tableName),
            columns = columns.map { SqlIdentifier.of(it) },
            unique = unique
        )

    fun buildDropIndexStatement(indexName: String, tableName: String): SqlDdlStatement.DropIndex =
        SqlDdlStatement.DropIndex(
            indexName = SqlIdentifier.of(indexName),
            tableName = SqlIdentifier.of(tableName)
        )

    fun buildAddColumnStatement(
        tableName: String,
        column: SqlColumnDefinition
    ): SqlDdlStatement.AlterTable.AddColumn =
        SqlDdlStatement.AlterTable.AddColumn(
            tableName = SqlIdentifier.of(tableName),
            column = column
        )

    fun buildDropColumnStatement(tableName: String, columnName: String): SqlDdlStatement.AlterTable.DropColumn =
        SqlDdlStatement.AlterTable.DropColumn(
            tableName = SqlIdentifier.of(tableName),
            columnName = SqlIdentifier.of(columnName)
        )

    fun buildModifyColumnStatement(
        tableName: String,
        column: SqlColumnDefinition
    ): SqlDdlStatement.AlterTable.ModifyColumn =
        SqlDdlStatement.AlterTable.ModifyColumn(
            tableName = SqlIdentifier.of(tableName),
            column = column
        )

    private fun PrimaryKeyType.toSqlPrimaryKeyMode(): SqlPrimaryKeyMode =
        when (this) {
            PrimaryKeyType.NOT -> SqlPrimaryKeyMode.NotPrimary
            PrimaryKeyType.DEFAULT -> SqlPrimaryKeyMode.Primary
            PrimaryKeyType.IDENTITY -> SqlPrimaryKeyMode.Identity
            PrimaryKeyType.UUID -> SqlPrimaryKeyMode.Uuid
            PrimaryKeyType.SNOWFLAKE -> SqlPrimaryKeyMode.Snowflake
            PrimaryKeyType.CUSTOM -> SqlPrimaryKeyMode.Primary
        }

    private fun KColumnType.toSqlType(length: Int, scale: Int): SqlType =
        when (this) {
            KColumnType.BIT -> SqlType.Boolean
            KColumnType.TINYINT,
            KColumnType.SMALLINT,
            KColumnType.INT,
            KColumnType.MEDIUMINT,
            KColumnType.YEAR,
            KColumnType.SERIAL -> SqlType.Int
            KColumnType.BIGINT -> SqlType.Long
            KColumnType.REAL,
            KColumnType.FLOAT -> SqlType.Float
            KColumnType.DOUBLE -> SqlType.Double
            KColumnType.DECIMAL,
            KColumnType.NUMERIC -> SqlType.Decimal((length to scale).takeIf { length > 0 })
            KColumnType.CHAR,
            KColumnType.VARCHAR,
            KColumnType.NCHAR,
            KColumnType.NVARCHAR -> SqlType.Varchar(length.takeIf { it > 0 })
            KColumnType.TEXT,
            KColumnType.LONGTEXT,
            KColumnType.MEDIUMTEXT,
            KColumnType.CLOB,
            KColumnType.NCLOB -> SqlType.Named(type)
            KColumnType.DATE -> SqlType.Date
            KColumnType.TIME -> SqlType.Time()
            KColumnType.DATETIME,
            KColumnType.TIMESTAMP -> SqlType.Timestamp()
            KColumnType.JSON -> SqlType.Json
            KColumnType.GEOMETRY -> SqlType.Geometry
            KColumnType.POINT -> SqlType.Point
            KColumnType.LINESTRING -> SqlType.LineString
            KColumnType.BINARY,
            KColumnType.VARBINARY,
            KColumnType.LONGVARBINARY,
            KColumnType.BLOB,
            KColumnType.MEDIUMBLOB,
            KColumnType.LONGBLOB,
            KColumnType.UUID,
            KColumnType.ENUM,
            KColumnType.SET,
            KColumnType.XML,
            KColumnType.UNDEFINED -> SqlType.Named(type)
        }
    }
