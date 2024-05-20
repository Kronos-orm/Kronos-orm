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

package com.kotlinorm.beans.dsw

import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.NoDataSourceException
import com.kotlinorm.i18n.Noun.noDataSourceMessage
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import kotlin.reflect.KClass

/**
 * None DataSource Wrapper.
 *
 * Default implementation of [KronosDataSourceWrapper] for when no data source is provided.
 *
 * @author OUSC
 * @see [KronosDataSourceWrapper]
 *
 */
class NoneDataSourceWrapper : KronosDataSourceWrapper {
    override val url: String
        get() = throw NoDataSourceException(noDataSourceMessage)
    override val dbType: DBType
        get() = throw NoDataSourceException(noDataSourceMessage)

    override fun forList(task: KronosAtomicActionTask): List<Map<String, Any>> {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun forList(task: KronosAtomicActionTask, kClass: KClass<*>): List<Any> {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun forMap(task: KronosAtomicActionTask): Map<String, Any>? {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun forObject(task: KronosAtomicActionTask, kClass: KClass<*>): Any? {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun update(task: KronosAtomicActionTask): Int {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
        throw NoDataSourceException(noDataSourceMessage)
    }
}