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
import kotlin.reflect.KType
import kotlin.reflect.typeOf

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
    val mappedTypes = mutableListOf<KType>()

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        mappedTypes.add(task.targetType)
        return [mapOf("id" to 7, "xx" to "Ada").mapperTo(task.targetType)]
    }

    override fun first(task: KAtomicQueryTask): Any? = null

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

    with(Kronos) {
        dataSource = { wrapper }
    }

    val rows = source.select { [it.id, it.name.alias("xx")] }.toList(wrapper)
    val row = rows.singleOrNull()
    val fieldNames = row?.__columns.orEmpty().map { it.name }.toSet()
    val mappedType = wrapper.mappedTypes.singleOrNull()

    val failures = listOfNotNull(
        expect(rows.size == 1) { "row count was ${rows.size}" },
        expect(row?.id == 7) { "generated projection id was ${row?.id}" },
        expect(row?.xx == "Ada") { "generated projection alias xx was ${row?.xx}" },
        expect(row?.__tableName == "tb_projection_source") { "projection table name was ${row?.__tableName}" },
        expect(row?.__columns?.all { it.tableName == "tb_projection_source" } == true) {
            "projection column table names were ${row?.__columns?.map { it.tableName }}"
        },
        expect(mappedType != typeOf<ProjectionSourceRow>()) {
            "toList mapped with source type ${typeOf<ProjectionSourceRow>()}"
        },
        expect(mappedType != null && !mappedType.isMarkedNullable && mappedType.arguments.isEmpty()) {
            "mapped type was $mappedType"
        },
        expect((mappedType?.classifier as? KClass<*>)?.simpleName?.startsWith("KronosSelectResult_") == true) {
            "mapped type was $mappedType"
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
