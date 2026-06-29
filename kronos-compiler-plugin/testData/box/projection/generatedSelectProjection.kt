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

// Verifies that bare select projections refine queryList() to a generated projection type.

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.orm.select.select
import kotlin.reflect.KClass

data class GeneratedProjectionUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

class GeneratedProjectionWrapper : KronosDataSourceWrapper {
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
        return emptyList()
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
    val user = GeneratedProjectionUser()
    val wrapper = GeneratedProjectionWrapper()

    val idRows = user.select { it.id }.queryList(wrapper)
    idRows.firstOrNull()?.id

    val rows = user.select { [it.id, it.name] }.queryList(wrapper)
    rows.firstOrNull()?.name

    val failures = listOfNotNull(
        expect(wrapper.mappedClasses.size == 2) { "mapped class count was ${wrapper.mappedClasses.size}" },
        expect(wrapper.mappedClasses.all { it != GeneratedProjectionUser::class }) {
            "projection used source class ${GeneratedProjectionUser::class}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
