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

// Verifies that TypeParameterFixer injects KPojo metadata into typed KronosQueryTask calls.

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.database.SqlHandler
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import kotlin.reflect.KClass

@Table(name = "tb_query_type")
data class QueryTypeUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

data class QueryTypeCall(
    val kind: String,
    val kClass: KClass<*>,
    val isKPojo: Boolean,
    val superTypes: List<String>,
)

class RecordingWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:empty"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val calls = mutableListOf<QueryTypeCall>()

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> {
        calls += QueryTypeCall("list", kClass, isKPojo, superTypes)
        return when (kClass) {
            QueryTypeUser::class -> listOf(QueryTypeUser(1, "Ada"))
            String::class -> listOf("plain-list")
            else -> emptyList()
        }
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? = null

    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): Any? {
        calls += QueryTypeCall("object", kClass, isKPojo, superTypes)
        return when (kClass) {
            QueryTypeUser::class -> QueryTypeUser(2, "Grace")
            String::class -> "plain-object"
            else -> null
        }
    }

    override fun update(task: KAtomicActionTask): Int = 0

    override fun batchUpdate(task: com.kotlinorm.beans.task.KronosAtomicBatchTask): IntArray = intArrayOf()

    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?
    ): Any? = null
}

fun box(): String {
    val wrapper = RecordingWrapper()
    val directUsers: List<QueryTypeUser> = KronosQueryTask(KronosAtomicQueryTask("select 1", emptyMap()))
        .queryList<QueryTypeUser>(wrapper)
    val one: QueryTypeUser? = KronosQueryTask(KronosAtomicQueryTask("select 1", emptyMap()))
        .queryOneOrNull<QueryTypeUser>(wrapper)
    val selectedUsers: List<QueryTypeUser> = QueryTypeUser()
        .select()
        .queryList<QueryTypeUser>(wrapper)
    val directOne: QueryTypeUser = KronosQueryTask(KronosAtomicQueryTask("select 1", emptyMap()))
        .queryOne<QueryTypeUser>(wrapper)
    val selectedOneOrNull: QueryTypeUser? = QueryTypeUser()
        .select()
        .queryOneOrNull<QueryTypeUser>(wrapper)
    val plainList: List<String> = KronosQueryTask(KronosAtomicQueryTask("select 1", emptyMap()))
        .queryList<String>(wrapper)
    val plainOne: String = KronosQueryTask(KronosAtomicQueryTask("select 1", emptyMap()))
        .queryOne<String>(wrapper)
    val plainOneOrNull: String? = KronosQueryTask(KronosAtomicQueryTask("select 1", emptyMap()))
        .queryOneOrNull<String>(wrapper)
    val handlerUsers: List<QueryTypeUser>
    val handlerOne: QueryTypeUser
    val handlerOneOrNull: QueryTypeUser?
    with(SqlHandler) {
        handlerUsers = wrapper.queryList<QueryTypeUser>("select 1")
        handlerOne = wrapper.queryOne<QueryTypeUser>("select 1")
        handlerOneOrNull = wrapper.queryOneOrNull<QueryTypeUser>("select 1")
    }

    val failures = listOfNotNull(
        expect(directUsers.singleOrNull()?.name == "Ada") { "list result was $directUsers" },
        expect(one?.name == "Grace") { "object result was $one" },
        expect(selectedUsers.singleOrNull()?.name == "Ada") { "selected list result was $selectedUsers" },
        expect(directOne.name == "Grace") { "direct one result was $directOne" },
        expect(selectedOneOrNull?.name == "Grace") { "selected oneOrNull result was $selectedOneOrNull" },
        expect(plainList.singleOrNull() == "plain-list") { "plain list result was $plainList" },
        expect(plainOne == "plain-object") { "plain one result was $plainOne" },
        expect(plainOneOrNull == "plain-object") { "plain oneOrNull result was $plainOneOrNull" },
        expect(handlerUsers.singleOrNull()?.name == "Ada") { "handler list result was $handlerUsers" },
        expect(handlerOne.name == "Grace") { "handler one result was $handlerOne" },
        expect(handlerOneOrNull?.name == "Grace") { "handler oneOrNull result was $handlerOneOrNull" },
        expect(wrapper.calls.size == 11) { "call count was ${wrapper.calls.size}" },
        expectInjectedCall(wrapper.calls.getOrNull(0), "list"),
        expectInjectedCall(wrapper.calls.getOrNull(1), "object"),
        expectInjectedCall(wrapper.calls.getOrNull(2), "list"),
        expectInjectedCall(wrapper.calls.getOrNull(3), "object"),
        expectInjectedCall(wrapper.calls.getOrNull(4), "object"),
        expectPlainCall(wrapper.calls.getOrNull(5), "list"),
        expectPlainCall(wrapper.calls.getOrNull(6), "object"),
        expectPlainCall(wrapper.calls.getOrNull(7), "object"),
        expectInjectedCall(wrapper.calls.getOrNull(8), "list"),
        expectInjectedCall(wrapper.calls.getOrNull(9), "object"),
        expectInjectedCall(wrapper.calls.getOrNull(10), "object"),
    )

    return failures.firstOrNull() ?: "OK"
}

fun expectInjectedCall(call: QueryTypeCall?, kind: String): String? {
    return when {
        call == null -> "Fail: missing $kind call"
        call.kind != kind -> "Fail: expected $kind call but was $call"
        call.kClass != QueryTypeUser::class -> "Fail: expected $kind kClass QueryTypeUser but was $call"
        !call.isKPojo -> "Fail: expected $kind KPojo metadata but was $call"
        "com.kotlinorm.interfaces.KPojo" !in call.superTypes -> "Fail: expected $kind KPojo supertype but was $call"
        else -> null
    }
}

fun expectPlainCall(call: QueryTypeCall?, kind: String): String? {
    return when {
        call == null -> "Fail: missing plain $kind call"
        call.kind != kind -> "Fail: expected plain $kind call but was $call"
        call.kClass != String::class -> "Fail: expected plain $kind kClass String but was $call"
        call.isKPojo -> "Fail: expected plain $kind non-KPojo metadata but was $call"
        "com.kotlinorm.interfaces.KPojo" in call.superTypes -> "Fail: unexpected plain $kind KPojo supertype in $call"
        else -> null
    }
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
