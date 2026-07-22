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

// Verifies function projection aliases become generated projection properties accessible after query mapping.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.Extensions.mapperTo
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Table("tb_projection_function_alias_access")
data class GeneratedFunctionAliasUser(
    var id: Int? = null,
    var username: String? = null,
    var ignored: String? = null,
) : KPojo

class GeneratedFunctionAliasWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:generated-function-alias"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val mappedTypes = mutableListOf<KType>()

    override fun toList(task: KAtomicQueryTask): List<Any?> = emptyList()

    override fun first(task: KAtomicQueryTask): Any? {
        mappedTypes += task.targetType
        return mapOf("id" to 9, "nameLength" to 4).mapperTo(task.targetType)
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
    val wrapper = GeneratedFunctionAliasWrapper()
    Kronos.dataSource = { wrapper }

    val row = GeneratedFunctionAliasUser()
        .select { [it.id, f.length(it.username).alias("nameLength")] }
        .first(wrapper)
    val fieldNames = row.__columns.map { it.name }.toSet()
    val mappedType = wrapper.mappedTypes.singleOrNull()

    val failures = listOfNotNull(
        expectGeneratedFunctionAlias(row.id == 9) { "id was ${row.id}" },
        expectGeneratedFunctionAlias(row.nameLength == 4) { "nameLength was ${row.nameLength}" },
        expectGeneratedFunctionAlias(fieldNames == setOf("id", "nameLength")) { "field names were $fieldNames" },
        expectGeneratedFunctionAlias(mappedType != typeOf<GeneratedFunctionAliasUser>()) {
            "mapped source type ${typeOf<GeneratedFunctionAliasUser>()}"
        },
        expectGeneratedFunctionAlias(mappedType != null && !mappedType.isMarkedNullable && mappedType.arguments.isEmpty()) {
            "mapped type was $mappedType"
        },
        expectGeneratedFunctionAlias((mappedType?.classifier as? KClass<*>)?.simpleName?.startsWith("KronosSelectResult_") == true) {
            "mapped types were ${wrapper.mappedTypes}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expectGeneratedFunctionAlias(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
