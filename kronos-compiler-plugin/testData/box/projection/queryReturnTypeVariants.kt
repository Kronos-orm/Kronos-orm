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

// Verifies queryList/queryOne/queryOneOrNull all refine to the generated projection row type.

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
    val mappedClasses = mutableListOf<KClass<*>>()

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>,
    ): List<Any> {
        mappedClasses += kClass
        return listOf(rowData(1).mapperTo(kClass as KClass<out KPojo>))
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? = null

    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>,
    ): Any {
        mappedClasses += kClass
        return rowData(mappedClasses.size).mapperTo(kClass as KClass<out KPojo>)
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

    val listRow = clause.queryList(wrapper).singleOrNull()
    val one = clause.queryOne(wrapper)
    val oneOrNull = clause.queryOneOrNull(wrapper)
    val fieldNames = one.kronosColumns().map { it.name }.toSet()

    val failures = listOfNotNull(
        expect(listRow?.id == 1) { "queryList id was ${listRow?.id}" },
        expect(listRow?.aliasName == "Ada1") { "queryList alias was ${listRow?.aliasName}" },
        expect(one.id == 2) { "queryOne id was ${one.id}" },
        expect(one.aliasName == "Ada2") { "queryOne alias was ${one.aliasName}" },
        expect(oneOrNull?.id == 3) { "queryOneOrNull id was ${oneOrNull?.id}" },
        expect(oneOrNull?.aliasName == "Ada3") { "queryOneOrNull alias was ${oneOrNull?.aliasName}" },
        expect(fieldNames == setOf("id", "aliasName")) { "field names were $fieldNames" },
        expect(wrapper.mappedClasses.size == 3) { "mapped classes were ${wrapper.mappedClasses}" },
        expect(wrapper.mappedClasses.all { it != ProjectionReturnSource::class }) {
            "mapped source class appeared in ${wrapper.mappedClasses}"
        },
        expect(wrapper.mappedClasses.map { it.simpleName }.distinct().singleOrNull()?.startsWith("KronosSelectResult_") == true) {
            "mapped classes were ${wrapper.mappedClasses}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
