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
    val mappedClasses = mutableListOf<KClass<*>>()

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> {
        mappedClasses.add(kClass)
        return [mapOf("id" to 42).mapperTo(kClass as KClass<out KPojo>)]
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? = null
    override fun forObject(task: KAtomicQueryTask, kClass: KClass<*>, isKPojo: Boolean, superTypes: List<String>): Any? = null
    override fun update(task: KAtomicActionTask): Int = 0
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
    override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? = null
}

fun box(): String {
    val wrapper = CascadeProjectionWrapper()
    with(Kronos) {
        dataSource = { wrapper }
    }

    val rows = ProjectionParent().select { it.children }.cascade(false).queryList(wrapper)
    val row = rows.singleOrNull()

    val columns = row?.kronosColumns().orEmpty()
    val fields = row?.toDataMap().orEmpty()

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
        expect(wrapper.mappedClasses.singleOrNull()?.simpleName?.startsWith("KronosSelectResult_") == true) {
            "mapped class was ${wrapper.mappedClasses.singleOrNull()}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
