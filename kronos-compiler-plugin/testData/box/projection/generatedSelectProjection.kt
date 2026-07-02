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

// Verifies that bare select lambdas produce runtime-mappable generated projection rows with alias fields.

import com.kotlinorm.Kronos
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.Extensions.mapperTo
import kotlin.reflect.KClass

@Table("tb_projection_source")
data class ProjectionSourceRow(
    var id: Int? = null,
    var name: String? = null,
    var ignoredInProjection: String? = null,
) : KPojo

class ProjectionMappingWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:generated-projection"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val mappedClasses = mutableListOf<KClass<*>>()

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> {
        mappedClasses.add(kClass)
        return [mapOf("id" to 7, "xx" to "Ada").mapperTo(kClass as KClass<out KPojo>)]
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
    val source = ProjectionSourceRow()
    val wrapper = ProjectionMappingWrapper()

    Kronos.init {
        dataSource = { wrapper }
    }

    val rows = source.select { [it.id, it.name.alias("xx")] }.queryList(wrapper)
    val row = rows.singleOrNull()
    val fieldNames = row?.kronosColumns().orEmpty().map { it.name }.toSet()

    val failures = listOfNotNull(
        expect(rows.size == 1) { "row count was ${rows.size}" },
        expect(row?.id == 7) { "generated projection id was ${row?.id}" },
        expect(row?.xx == "Ada") { "generated projection alias xx was ${row?.xx}" },
        expect(row?.__tableName == "tb_projection_source") { "projection table name was ${row?.__tableName}" },
        expect(row?.kronosColumns()?.all { it.tableName == "tb_projection_source" } == true) {
            "projection column table names were ${row?.kronosColumns()?.map { it.tableName }}"
        },
        expect(wrapper.mappedClasses.singleOrNull() != ProjectionSourceRow::class) {
            "queryList mapped with source class ${ProjectionSourceRow::class}"
        },
        expect(wrapper.mappedClasses.singleOrNull()?.simpleName?.startsWith("KronosSelectResult_") == true) {
            "mapped class was ${wrapper.mappedClasses.singleOrNull()}"
        },
        expect("ignoredInProjection" !in fieldNames) {
            "projection field names were $fieldNames"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
