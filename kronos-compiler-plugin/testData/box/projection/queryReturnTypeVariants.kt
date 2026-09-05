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

// Verifies toList/first/firstOrNull all refine to the generated projection row type.

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
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Table("tb_projection_return_source")
data class ProjectionReturnSource(
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
) : KPojo

class ProjectionReturnWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:projection-return"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val mappedTypes = mutableListOf<KType>()

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        mappedTypes += task.targetType
        return listOf(rowData(1).mapperTo(task.targetType))
    }

    override fun first(task: KAtomicQueryTask): Any? {
        mappedTypes += task.targetType
        return rowData(mappedTypes.size).mapperTo(task.targetType)
    }

    override fun update(task: KAtomicActionTask): Int = 0

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()

    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?,
    ): Any? = null
}

fun rowData(id: Int): Map<String, Any> =
    mapOf("id" to id, "aliasName" to "Ada$id")

fun box(): String {
    val wrapper = ProjectionReturnWrapper()
    Kronos.dataSource = { wrapper }

    val clause = ProjectionReturnSource()
        .select { [it.id, it.name.alias("aliasName")] }

    val listRow = clause.toList(wrapper).singleOrNull()
    val one = clause.first(wrapper)
    val oneOrNull = clause.firstOrNull(wrapper)
    val fieldNames = one.__columns.map { it.name }.toSet()
    val listType = wrapper.mappedTypes.getOrNull(0)
    val firstType = wrapper.mappedTypes.getOrNull(1)
    val firstOrNullType = wrapper.mappedTypes.getOrNull(2)

    val failures = listOfNotNull(
        expect(listRow?.id == 1) { "toList id was ${listRow?.id}" },
        expect(listRow?.aliasName == "Ada1") { "toList alias was ${listRow?.aliasName}" },
        expect(one.id == 2) { "first id was ${one.id}" },
        expect(one.aliasName == "Ada2") { "first alias was ${one.aliasName}" },
        expect(oneOrNull?.id == 3) { "firstOrNull id was ${oneOrNull?.id}" },
        expect(oneOrNull?.aliasName == "Ada3") { "firstOrNull alias was ${oneOrNull?.aliasName}" },
        expect(fieldNames == setOf("id", "aliasName")) { "field names were $fieldNames" },
        expect(wrapper.mappedTypes.size == 3) { "mapped types were ${wrapper.mappedTypes}" },
        expect(wrapper.mappedTypes.all { it.classifier != typeOf<ProjectionReturnSource>().classifier }) {
            "mapped source type appeared in ${wrapper.mappedTypes}"
        },
        expect(listType != null && !listType.isMarkedNullable && listType.arguments.isEmpty()) {
            "toList mapped type was $listType"
        },
        expect(firstType == listType) { "first mapped type was $firstType, expected $listType" },
        expect(firstOrNullType?.isMarkedNullable == true) {
            "firstOrNull mapped type was $firstOrNullType"
        },
        expect(firstOrNullType?.classifier == listType?.classifier && firstOrNullType?.arguments == listType?.arguments) {
            "mapped type structures were ${wrapper.mappedTypes}"
        },
        expect(listType?.classifier.toString().substringAfterLast('.').startsWith("KronosSelectResult_") == true) {
            "mapped types were ${wrapper.mappedTypes}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
