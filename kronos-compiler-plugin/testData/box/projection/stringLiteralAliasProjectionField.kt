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

// Verifies string literal projection aliases become generated projection properties.

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

@Table("tb_string_literal_projection")
data class StringLiteralProjectionUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

class StringLiteralProjectionWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:string-literal-projection"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val mappedClasses = mutableListOf<KClass<*>>()

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>,
    ): List<Any> = emptyList()

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? = null

    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>,
    ): Any {
        mappedClasses += kClass
        return mapOf("computedAlias" to "ready").mapperTo(kClass as KClass<out KPojo>)
    }

    override fun update(task: KAtomicActionTask): Int = 0

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()

    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?,
    ): Any? = null
}

fun box(): String {
    val wrapper = StringLiteralProjectionWrapper()
    val row = StringLiteralProjectionUser()
        .select { "computedAlias" }
        .queryOne(wrapper)
    val fieldNames = row.kronosColumns().map { it.name }

    val failures = listOfNotNull(
        expectStringLiteralProjection(row.computedAlias == "ready") { "computedAlias was ${row.computedAlias}" },
        expectStringLiteralProjection(fieldNames == listOf("computedAlias")) { "field names were $fieldNames" },
        expectStringLiteralProjection(wrapper.mappedClasses.singleOrNull() != StringLiteralProjectionUser::class) {
            "mapped source class ${StringLiteralProjectionUser::class}"
        },
        expectStringLiteralProjection(wrapper.mappedClasses.singleOrNull()?.simpleName?.startsWith("KronosSelectResult_") == true) {
            "mapped classes were ${wrapper.mappedClasses}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expectStringLiteralProjection(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
