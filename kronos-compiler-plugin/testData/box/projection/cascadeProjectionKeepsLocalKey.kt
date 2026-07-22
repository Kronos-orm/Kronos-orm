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

// Verifies cascade-only projections retain source table metadata and hidden local keys from reverse cascade refs.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Cascade
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
import kotlin.reflect.KType

@Table("tb_projection_parent")
data class ProjectionParent(
    val id: Int? = null,
    val children: List<ProjectionChild>? = null,
) : KPojo

@Table("tb_projection_child")
data class ProjectionChild(
    val id: Int? = null,
    val parentId: Int? = null,
    @Cascade(["parentId"], ["id"])
    val parent: ProjectionParent? = null,
) : KPojo

class CascadeProjectionWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:cascade-projection"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val mappedTypes = mutableListOf<KType>()

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        mappedTypes.add(task.targetType)
        return [mapOf("id" to 42).mapperTo(task.targetType)]
    }

    override fun first(task: KAtomicQueryTask): Any? = null
    override fun update(task: KAtomicActionTask): Int = 0
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
    override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? = null
}

fun box(): String {
    val wrapper = CascadeProjectionWrapper()
    with(Kronos) {
        dataSource = { wrapper }
    }

    val rows = ProjectionParent().select { it.children }.cascade(false).toList(wrapper)
    val row = rows.singleOrNull()

    val columns = row?.__columns.orEmpty()
    val fields = row?.toDataMap().orEmpty()
    val mappedType = wrapper.mappedTypes.singleOrNull()

    val failures = listOfNotNull(
        expect(rows.size == 1) { "row count was ${rows.size}" },
        expect(row?.__tableName == "tb_projection_parent") { "table name was ${row?.__tableName}" },
        expect(fields["id"] == 42) { "hidden id field was ${fields["id"]}" },
        expect(columns.any { it.name == "children" && !it.isColumn && it.tableName == "tb_projection_parent" }) {
            "children metadata was $columns"
        },
        expect(columns.any { it.name == "id" && it.tableName == "tb_projection_parent" }) {
            "hidden id metadata was $columns"
        },
        expect(mappedType != null && !mappedType.isMarkedNullable && mappedType.arguments.isEmpty()) {
            "mapped type was $mappedType"
        },
        expect((mappedType?.classifier as? KClass<*>)?.simpleName?.startsWith("KronosSelectResult_") == true) {
            "mapped type was $mappedType"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
