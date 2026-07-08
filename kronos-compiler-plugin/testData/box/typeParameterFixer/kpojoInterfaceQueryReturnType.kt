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

// Verifies TypeParameterFixer treats KPojo itself as KPojo metadata.

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import kotlin.reflect.KClass

data class KPojoInterfaceQueryTypeCall(
    val kind: String,
    val kClassName: String?,
    val isKPojo: Boolean,
    val hasKPojoSupertype: Boolean,
)

class KPojoInterfaceRecordingWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:kpojo-interface-query-type"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val calls = mutableListOf<KPojoInterfaceQueryTypeCall>()

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> {
        calls += KPojoInterfaceQueryTypeCall("list", kClass.qualifiedName, isKPojo, "com.kotlinorm.interfaces.KPojo" in superTypes)
        return emptyList()
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? = null
    override fun forObject(task: KAtomicQueryTask, kClass: KClass<*>, isKPojo: Boolean, superTypes: List<String>): Any? = null
    override fun update(task: KAtomicActionTask): Int = 0
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
    override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? = null
}

fun box(): String {
    val wrapper = KPojoInterfaceRecordingWrapper()
    val rows: List<KPojo> = KronosQueryTask(KronosAtomicQueryTask("select 1", emptyMap()))
        .queryList<KPojo>(wrapper)

    val failures = listOfNotNull(
        expect(rows == emptyList<KPojo>()) { "rows were $rows" },
        expect(
            wrapper.calls == listOf(
                KPojoInterfaceQueryTypeCall("list", "com.kotlinorm.interfaces.KPojo", isKPojo = true, hasKPojoSupertype = true),
            )
        ) { "calls were ${wrapper.calls}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
