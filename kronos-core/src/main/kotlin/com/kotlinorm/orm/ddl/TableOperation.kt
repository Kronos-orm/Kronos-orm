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

import com.kotlinorm.ast.DdlStatement
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.database.SqlHandler.execute
import com.kotlinorm.database.SqlManager.getTableColumns
import com.kotlinorm.database.SqlManager.getTableCreateSqlList
import com.kotlinorm.database.SqlManager.getTableDropSql
import com.kotlinorm.database.SqlManager.getTableIndexes
import com.kotlinorm.database.SqlManager.getTableSyncSqlList
import com.kotlinorm.database.SqlManager.getTableTruncateSql
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
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
        queryTableExistence(instance.kronosTableName(), dataSource)

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
    inline fun <reified T : KPojo> createTable(instance: T = T::class.createInstance()) = getTableCreateSqlList(
        dataSource.dbType,
        instance.kronosTableName(),
        instance.kronosTableComment(),
        instance.kronosColumns().filter { it.isColumn },
        instance.kronosTableIndex()
    ).map {
        KronosAtomicActionTask(
            it,
            mapOf("tableName" to instance.kronosTableName()),
            KOperationType.CREATE,
            DDLInfo(T::class, instance.kronosTableName())
        )
    }.toKronosActionTask().execute(dataSource)

    /**
     * Drop table
     *
     * @param instance Table instance
     */
    inline fun <reified T : KPojo> dropTable(instance: T = T::class.createInstance()) =
        KronosAtomicActionTask(
            getTableDropSql(dataSource.dbType, instance.kronosTableName()),
            mapOf("tableName" to instance.kronosTableName()),
            KOperationType.DROP,
            DDLInfo(T::class, instance.kronosTableName())
        ).toKronosActionTask().execute(dataSource)

    /**
     * Drop table
     *
     * @param tableNames Table names
     */
    fun dropTable(vararg tableNames: String) =
        tableNames.map { tableName ->
            KronosAtomicActionTask(
                getTableDropSql(dataSource.dbType, tableName),
                mapOf("tableName" to tableName),
                KOperationType.DROP,
                DDLInfo(null, tableName)
            )
        }.toKronosActionTask().execute(dataSource)

    /**
     * Truncate table
     *
     * @param instance Table instance
     * @param restartIdentity Whether to reset the auto-increment value，only for `PostgreSQL` and `sqlite` for `reset auto increment`, default is `true`
     */
    inline fun <reified T : KPojo> truncateTable(
        instance: T = T::class.createInstance(),
        restartIdentity: Boolean = true
    ) = KronosAtomicActionTask(
        getTableTruncateSql(
            dataSource.dbType,
            instance.kronosTableName(),
            restartIdentity
        ),
        mapOf("tableName" to instance.kronosTableName()),
        KOperationType.TRUNCATE,
        DDLInfo(T::class, instance.kronosTableName())
    ).toKronosActionTask().execute(dataSource)

    /**
     * Truncate table
     *
     * @param tableName Table name
     * @param restartIdentity Whether to reset the auto-increment value，only for `PostgreSQL` and `sqlite` for `reset auto increment`, default is `true`
     */
    fun truncateTable(vararg tableName: String, restartIdentity: Boolean = true) =
        tableName.map { name ->
            KronosAtomicActionTask(
                getTableTruncateSql(dataSource.dbType, name, restartIdentity),
                mapOf("tableName" to name),
                KOperationType.TRUNCATE,
                DDLInfo(null, name)
            )
        }.toKronosActionTask().execute(dataSource)

    /**
     * Synchronize table structure
     *
     * @param instance Table instance
     * @return Whether the table is created
     */
    inline fun <reified T : KPojo> syncTable(instance: T = T::class.createInstance()): Boolean {
        // 表名
        val tableName = instance.kronosTableName()

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
        val tableComment = instance.kronosTableComment()

        // 获取实际表字段信息
        val tableColumns = getTableColumns(dataSource, tableName)
        // 获取实际表索引信息
        val tableIndexes = getTableIndexes(dataSource, tableName)

        // 新增、修改、删除字段
        val diffColumns = columnDiffer(dbType, kronosColumns, tableColumns).apply { doLog(tableName) }
        val diffIndexes = indexDiffer(kronosIndexes, tableIndexes)

        dataSource.transact {
            getTableSyncSqlList(dataSource, tableName, originalTableComment, tableComment, diffColumns, diffIndexes).forEach {
                dataSource.execute(it)
            }
        }
        return true
    }

    /**
     * Build CreateTableStatement AST from KPojo instance
     *
     * @param instance Table instance
     * @return CreateTableStatement AST node
     */
    inline fun <reified T : KPojo> buildCreateTableStatement(instance: T = T::class.createInstance()): DdlStatement.CreateTableStatement {
        val columns = instance.kronosColumns()
            .filter { it.isColumn }
            .map { field ->
                com.kotlinorm.ast.ColumnDefinition(
                    name = field.columnName,
                    type = field.type,
                    length = field.length,
                    scale = field.scale,
                    nullable = field.nullable,
                    primaryKey = field.primaryKey,
                    defaultValue = field.defaultValue?.let { com.kotlinorm.ast.Literal.StringLiteral(it) }
                )
            }

        return DdlStatement.CreateTableStatement(
            tableName = instance.kronosTableName(),
            columns = columns,
            indexes = instance.kronosTableIndex(),
            comment = instance.kronosTableComment()
        )
    }

    /**
     * Build DropTableStatement AST
     *
     * @param tableName Table name
     * @param ifExists Whether to use IF EXISTS clause
     * @return DropTableStatement AST node
     */
    fun buildDropTableStatement(tableName: String, ifExists: Boolean = false): DdlStatement.DropTableStatement {
        return DdlStatement.DropTableStatement(
            tableName = tableName,
            ifExists = ifExists
        )
    }

    /**
     * Build DropTableStatement AST from KPojo instance
     *
     * @param instance Table instance
     * @param ifExists Whether to use IF EXISTS clause
     * @return DropTableStatement AST node
     */
    inline fun <reified T : KPojo> buildDropTableStatement(
        instance: T = T::class.createInstance(),
        ifExists: Boolean = false
    ): DdlStatement.DropTableStatement {
        return buildDropTableStatement(instance.kronosTableName(), ifExists)
    }

    /**
     * Build TruncateTableStatement AST
     *
     * @param tableName Table name
     * @param restartIdentity Whether to restart identity sequences
     * @return TruncateTableStatement AST node
     */
    fun buildTruncateTableStatement(tableName: String, restartIdentity: Boolean = true): DdlStatement.TruncateTableStatement {
        return DdlStatement.TruncateTableStatement(
            tableName = tableName,
            restartIdentity = restartIdentity
        )
    }

    /**
     * Build TruncateTableStatement AST from KPojo instance
     *
     * @param instance Table instance
     * @param restartIdentity Whether to restart identity sequences
     * @return TruncateTableStatement AST node
     */
    inline fun <reified T : KPojo> buildTruncateTableStatement(
        instance: T = T::class.createInstance(),
        restartIdentity: Boolean = true
    ): DdlStatement.TruncateTableStatement {
        return buildTruncateTableStatement(instance.kronosTableName(), restartIdentity)
    }

    /**
     * Build CreateIndexStatement AST
     *
     * @param indexName Index name
     * @param tableName Table name
     * @param columns Column names to index
     * @param unique Whether the index is unique
     * @return CreateIndexStatement AST node
     */
    fun buildCreateIndexStatement(
        indexName: String,
        tableName: String,
        columns: List<String>,
        unique: Boolean = false
    ): DdlStatement.CreateIndexStatement {
        return DdlStatement.CreateIndexStatement(
            indexName = indexName,
            tableName = tableName,
            columns = columns,
            unique = unique
        )
    }

    /**
     * Build DropIndexStatement AST
     *
     * @param indexName Index name
     * @param tableName Table name
     * @return DropIndexStatement AST node
     */
    fun buildDropIndexStatement(indexName: String, tableName: String): DdlStatement.DropIndexStatement {
        return DdlStatement.DropIndexStatement(
            indexName = indexName,
            tableName = tableName
        )
    }

    /**
     * Build AddColumnStatement AST
     *
     * @param tableName Table name
     * @param column Column definition
     * @return AddColumnStatement AST node
     */
    fun buildAddColumnStatement(
        tableName: String,
        column: com.kotlinorm.ast.ColumnDefinition
    ): DdlStatement.AlterTableStatement.AddColumnStatement {
        return DdlStatement.AlterTableStatement.AddColumnStatement(
            tableName = tableName,
            column = column
        )
    }

    /**
     * Build DropColumnStatement AST
     *
     * @param tableName Table name
     * @param columnName Column name to drop
     * @return DropColumnStatement AST node
     */
    fun buildDropColumnStatement(tableName: String, columnName: String): DdlStatement.AlterTableStatement.DropColumnStatement {
        return DdlStatement.AlterTableStatement.DropColumnStatement(
            tableName = tableName,
            columnName = columnName
        )
    }

    /**
     * Build ModifyColumnStatement AST
     *
     * @param tableName Table name
     * @param column Column definition
     * @return ModifyColumnStatement AST node
     */
    fun buildModifyColumnStatement(
        tableName: String,
        column: com.kotlinorm.ast.ColumnDefinition
    ): DdlStatement.AlterTableStatement.ModifyColumnStatement {
        return DdlStatement.AlterTableStatement.ModifyColumnStatement(
            tableName = tableName,
            column = column
        )
    }
}