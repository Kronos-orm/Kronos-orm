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

// Verifies function-local projection DTOs can be used by select(...).queryList() mapping.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.Extensions.mapperTo
import kotlin.reflect.KClass

@Table("tb_local_projection_source")
data class LocalProjectionSource(
    var id: Int? = null,
    var name: String? = null,
    var ignored: String? = null,
) : KPojo

class LocalProjectionWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:local-projection"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    var mappedClass: KClass<*>? = null

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> {
        mappedClass = kClass
        return [mapOf("id" to 11, "name" to "Local").mapperTo(kClass as KClass<out KPojo>)]
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? = null

    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): Any? = null

    override fun update(task: KAtomicActionTask): Int = 0

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()

    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?
    ): Any? = null
}

fun box(): String {
    data class LocalProjectionRow(
        var id: Int? = null,
        var name: String? = null,
    ) : KPojo

    val wrapper = LocalProjectionWrapper()

    with(Kronos) {
        dataSource = { wrapper }
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val rows = LocalProjectionSource()
        .select(LocalProjectionRow::class) { [it.id, it.name] }
        .queryList(wrapper)
    val row = rows.singleOrNull()

    val failures = listOfNotNull(
        expect(wrapper.mappedClass == LocalProjectionRow::class) { "mapped class was ${wrapper.mappedClass}" },
        expect(row is LocalProjectionRow) { "row type was ${row?.let { it::class }}" },
        expect(row?.id == 11) { "row id was ${row?.id}" },
        expect(row?.name == "Local") { "row name was ${row?.name}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
