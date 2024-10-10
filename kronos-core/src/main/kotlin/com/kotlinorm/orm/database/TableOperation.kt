/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.orm.database

import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.database.SqlHandler.execute
import com.kotlinorm.database.SqlManager.getTableColumns
import com.kotlinorm.database.SqlManager.getTableCreateSqlList
import com.kotlinorm.database.SqlManager.getTableDropSql
import com.kotlinorm.database.SqlManager.getTableIndexes
import com.kotlinorm.database.SqlManager.getTableSyncSqlList
import com.kotlinorm.database.SqlManager.getTableTruncateSql
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.DataSourceUtil.orDefault
import kotlin.reflect.full.createInstance

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
    ).forEach { dataSource.execute(it) }


    /**
     * Drop table
     *
     * @param instance Table instance
     */
    inline fun <reified T : KPojo> dropTable(instance: T = T::class.createInstance()) {
        dataSource.execute(
            getTableDropSql(dataSource.dbType, instance.kronosTableName())
        )
    }

    /**
     * Drop table
     *
     * @param tableName Table name
     */
    fun dropTable(vararg tableName: String) {
        tableName.forEach {
            dataSource.execute(
                getTableDropSql(dataSource.dbType, it)
            )
        }
    }

    /**
     * Truncate table
     *
     * @param instance Table instance
     * @param restartIdentity Whether to reset the auto-increment value，only for `PostgreSQL` and `sqlite` for `reset auto increment`, default is `true`
     */
    inline fun <reified T : KPojo> truncateTable(
        instance: T = T::class.createInstance(),
        restartIdentity: Boolean = true
    ) {
        dataSource.execute(
            getTableTruncateSql(dataSource.dbType, instance.kronosTableName(), restartIdentity)
        )
    }

    /**
     * Truncate table
     *
     * @param tableName Table name
     * @param restartIdentity Whether to reset the auto-increment value，only for `PostgreSQL` and `sqlite` for `reset auto increment`, default is `true`
     */
    fun truncateTable(vararg tableName: String, restartIdentity: Boolean = true) {
        tableName.forEach {
            dataSource.execute(
                getTableTruncateSql(dataSource.dbType, it, restartIdentity)
            )
        }
    }

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
        val kronosColumns = instance.kronosColumns().filter { it.isColumn }.map { col ->
            if (dbType == DBType.Oracle) {
                col.columnName = col.columnName.uppercase()
            }
            col
        }
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
}