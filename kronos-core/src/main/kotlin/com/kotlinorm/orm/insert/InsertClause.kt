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

package com.kotlinorm.orm.insert

import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.toLinkedSet

class InsertClause<T : KPojo>(pojo: T) {
    private var paramMap = pojo.toDataMap()
    private var tableName = pojo.kronosTableName()
    private var createTimeStrategy = pojo.kronosCreateTime()
    private var updateTimeStrategy = pojo.kronosUpdateTime()
    private var logicDeleteStrategy = pojo.kronosLogicDelete()
    private var allFields = pojo.kronosColumns().toLinkedSet()
    private val toInsertFields = linkedSetOf<Field>()

    private val updateInsertFields = { field: Field, value: Any? ->
        if (value != null) {
            toInsertFields += field
            paramMap[field.name] = value
        }
    }

    fun build(): KronosAtomicActionTask {
        toInsertFields.addAll(allFields.filter { it.name in paramMap.keys && paramMap[it.name] != null })

        setCommonStrategy(createTimeStrategy, true, callBack = updateInsertFields)
        setCommonStrategy(updateTimeStrategy, true, callBack = updateInsertFields)
        setCommonStrategy(logicDeleteStrategy, false, callBack = updateInsertFields)

        var sql = ""
        if (dataSource().dbType == DBType.Oracle) {
            sql = """
                INSERT INTO $tableName (${toInsertFields.joinToString { it.columnName.uppercase() }}) VALUES (${toInsertFields.joinToString { ":$it" }})
        """.trimIndent()
        } else if (dataSource().dbType == DBType.Mysql) {
            sql = """
            INSERT INTO `$tableName` (${toInsertFields.joinToString { it.quoted() }}) VALUES (${toInsertFields.joinToString { ":$it" }})
        """.trimIndent()
        }

        return KronosAtomicActionTask(
            sql,
            paramMap,
            operationType = KOperationType.INSERT
        )

    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build().execute(wrapper)
    }

    companion object {
        fun <T : KPojo> List<InsertClause<T>>.build(): KronosAtomicBatchTask {
            val tasks = this.map { it.build() }
            return KronosAtomicBatchTask(
                sql = tasks.first().sql,
                paramMapArr = tasks.map { it.paramMap }.toTypedArray(),
                operationType = KOperationType.INSERT
            )
        }

        fun <T : KPojo> List<InsertClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }
    }
}