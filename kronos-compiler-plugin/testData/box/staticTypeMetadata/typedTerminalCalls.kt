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

@file:OptIn(ExperimentalStdlibApi::class, com.kotlinorm.annotations.InternalKronosApi::class)

// Verifies typed terminal calls carry caller-generated metadata into the executed query task.

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.database.SqlExecutor
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.utils.StaticTypeRef
import kotlin.reflect.typeOf

data class TerminalMetadataUser(
    @PrimaryKey
    var id: Int? = null,
) : KPojo

data class TerminalMetadataOrder(
    var id: Int? = null,
    var userId: Int? = null,
) : KPojo

private class TerminalMetadataWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:terminal-metadata"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val calls = mutableListOf<KAtomicQueryTask>()

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        calls += task
        return listOf(TerminalMetadataUser(id = 1))
    }

    override fun first(task: KAtomicQueryTask): Any? {
        calls += task
        return if (task.targetType == typeOf<Int>()) 1 else TerminalMetadataUser(id = 1)
    }

    override fun update(task: KAtomicActionTask): Int = 0

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()

    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?,
    ): Any? = null
}

private fun joinedTerminalQuery() = TerminalMetadataUser().join(TerminalMetadataOrder()) { user, order ->
    innerJoin { user.id == order.userId }.select()
}

private fun TerminalMetadataWrapper.expectLatestUserTask(nullable: Boolean = false): String? {
    val task = calls.lastOrNull { it.targetType == if (nullable) typeOf<TerminalMetadataUser?>() else typeOf<TerminalMetadataUser>() }
        ?: return "Fail: no ${if (nullable) "nullable " else ""}user task was captured from $calls"
    val expected = StaticTypeRef("TerminalMetadataUser", nullable)
    return when {
        task.targetTypeInfo == null -> "Fail: user task metadata was null"
        task.targetTypeInfo?.type != expected -> "Fail: user task metadata was ${task.targetTypeInfo}"
        else -> null
    }
}

private fun TerminalMetadataWrapper.resetCalls() {
    calls.clear()
}

fun box(): String {
    val wrapper = TerminalMetadataWrapper()
    val failures = mutableListOf<String>()

    with(SqlExecutor) {
        wrapper.queryList<TerminalMetadataUser>("SELECT id FROM terminal_metadata_user")
    }
    wrapper.expectLatestUserTask()?.let(failures::add)

    wrapper.resetCalls()
    KronosQueryTask(
        KronosAtomicQueryTask(
            sql = "SELECT id FROM terminal_metadata_user",
            targetType = typeOf<Any?>(),
        )
    ).toList<TerminalMetadataUser>(wrapper)
    wrapper.expectLatestUserTask()?.let(failures::add)

    wrapper.resetCalls()
    TerminalMetadataUser().select().toList<TerminalMetadataUser>(wrapper)
    wrapper.expectLatestUserTask()?.let(failures::add)

    wrapper.resetCalls()
    union(
        TerminalMetadataUser().select(),
        TerminalMetadataUser().select(),
    ).toList<TerminalMetadataUser>(wrapper)
    wrapper.expectLatestUserTask()?.let(failures::add)

    wrapper.resetCalls()
    joinedTerminalQuery().toList<TerminalMetadataUser>(wrapper)
    wrapper.expectLatestUserTask()?.let(failures::add)

    wrapper.resetCalls()
    TerminalMetadataUser().select().page(pageIndex = 1, pageSize = 1).toList<TerminalMetadataUser>(wrapper)
    wrapper.expectLatestUserTask()?.let(failures::add)

    wrapper.resetCalls()
    TerminalMetadataUser().select().page(pageIndex = 1, pageSize = 1).withTotal()
        .toList<TerminalMetadataUser>(wrapper)
    wrapper.expectLatestUserTask()?.let(failures::add)

    wrapper.resetCalls()
    TerminalMetadataUser().select().orderBy { it.id.asc() }.cursor(pageSize = 1)
        .toList<TerminalMetadataUser>(wrapper)
    wrapper.expectLatestUserTask()?.let(failures::add)

    wrapper.resetCalls()
    TerminalMetadataUser().select().first<TerminalMetadataUser>(wrapper)
    wrapper.expectLatestUserTask()?.let(failures::add)

    wrapper.resetCalls()
    TerminalMetadataUser().select().firstOrNull<TerminalMetadataUser>(wrapper)
    wrapper.expectLatestUserTask(nullable = true)?.let(failures::add)

    return failures.firstOrNull() ?: "OK"
}
