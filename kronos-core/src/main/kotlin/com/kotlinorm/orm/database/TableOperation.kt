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
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.DataSourceUtil.orDefault
import kotlin.reflect.full.createInstance

class TableOperation(private val wrapper: KronosDataSourceWrapper) {
    val dataSource by lazy { wrapper.orDefault() }

    inline fun <reified T : KPojo> exists(instance: T = T::class.createInstance()) =
        queryTableExistence(instance.kronosTableName(), dataSource)

    fun exists(tableName: String) = queryTableExistence(tableName, dataSource)

    inline fun <reified T : KPojo> createTable(instance: T = T::class.createInstance()) = getTableCreateSqlList(
        dataSource.dbType,
        instance.kronosTableName(),
        instance.kronosColumns().filter { it.isColumn },
        instance.kronosTableIndex()
    ).forEach { dataSource.execute(it) }

    fun createTables(vararg instance: KPojo) {
        instance.forEach {
            getTableCreateSqlList(
                dataSource.dbType,
                it.kronosTableName(),
                it.kronosColumns().filter { it.isColumn },
                it.kronosTableIndex()
            ).forEach { sql -> dataSource.execute(sql) }
        }
    }

    inline fun <reified T : KPojo> dropTable(instance: T = T::class.createInstance()) {
        dataSource.execute(
            getTableDropSql(dataSource.dbType, instance.kronosTableName())
        )
    }

    @JvmName("dropTablesByInstance")
    fun dropTable(vararg instance: KPojo) {
        instance.forEach {
            dataSource.execute(
                getTableDropSql(dataSource.dbType, it.kronosTableName())
            )
        }
    }

    fun dropTable(vararg tableName: String) {
        tableName.forEach {
            dataSource.execute(
                getTableDropSql(dataSource.dbType, it)
            )
        }
    }

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
        // 获取实际表字段信息
        val tableColumns = getTableColumns(dataSource, tableName)
        // 获取实际表索引信息
        val tableIndexes = getTableIndexes(dataSource, tableName)

        // 新增、修改、删除字段
        val diffColumns = differ(dbType, kronosColumns, tableColumns).apply { doLog(tableName) }
        val diffIndexes = TableIndexDiff(kronosIndexes, tableIndexes)

        dataSource.transact {
            getTableSyncSqlList(dataSource, tableName, diffColumns, diffIndexes).forEach {
                dataSource.execute(it)
            }
        }
        return true
    }

    @JvmName("syncTableByInstance")
    fun syncTables(vararg instance: KPojo): Boolean {
        var result = true
        instance.forEach {
            result = result && syncTable(it)
        }
        return result
    }
}