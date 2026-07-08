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

// Verifies TypeParameterFixer injects explicit non-KPojo metadata for scalar typed query APIs.

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import kotlin.reflect.KClass

data class NonKPojoQueryTypeCall(
    val kind: String,
    val kClassName: String?,
    val isKPojo: Boolean,
    val hasKPojoSupertype: Boolean,
)

class NonKPojoRecordingWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:non-kpojo-query-type"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val calls = mutableListOf<NonKPojoQueryTypeCall>()

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> {
        calls += NonKPojoQueryTypeCall("list", kClass.qualifiedName, isKPojo, "com.kotlinorm.interfaces.KPojo" in superTypes)
        return listOf("Ada")
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? = null

    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): Any? {
        calls += NonKPojoQueryTypeCall("object", kClass.qualifiedName, isKPojo, "com.kotlinorm.interfaces.KPojo" in superTypes)
        return if (kClass == Int::class) 7 else 8L
    }

    override fun update(task: KAtomicActionTask): Int = 0
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
    override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? = null
}

fun box(): String {
    val wrapper = NonKPojoRecordingWrapper()
    val task = KronosQueryTask(KronosAtomicQueryTask("select 1", emptyMap()))

    val names: List<String> = task.queryList<String>(wrapper)
    val count: Int = task.queryOne<Int>(wrapper)
    val total: Long? = task.queryOneOrNull<Long>(wrapper)

    val failures = listOfNotNull(
        expect(names == listOf("Ada")) { "names were $names" },
        expect(count == 7) { "count was $count" },
        expect(total == 8L) { "total was $total" },
        expect(
            wrapper.calls == listOf(
                NonKPojoQueryTypeCall("list", "kotlin.String", isKPojo = false, hasKPojoSupertype = false),
                NonKPojoQueryTypeCall("object", "kotlin.Int", isKPojo = false, hasKPojoSupertype = false),
                NonKPojoQueryTypeCall("object", "kotlin.Long", isKPojo = false, hasKPojoSupertype = false),
            )
        ) { "calls were ${wrapper.calls}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
