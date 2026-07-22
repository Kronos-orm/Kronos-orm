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

// Verifies an explicitly opted-in alias replacement keeps the selected expression type through mapping and Context.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UnsafeProjectionOverride
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

@Table("tb_projection_selected_override_type")
data class SelectedAliasOverrideUser(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

class SelectedAliasOverrideWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:selected-alias-override-type"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        return listOf(mapOf("id" to 3, "username" to 5).mapperTo(task.targetType))
    }

    override fun first(task: KAtomicQueryTask): Any? = null
    override fun update(task: KAtomicActionTask): Int = 0
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?,
    ): Any? = null
}

@OptIn(UnsafeProjectionOverride::class)
fun box(): String {
    val wrapper = SelectedAliasOverrideWrapper()
    Kronos.dataSource = { wrapper }

    val clause = SelectedAliasOverrideUser()
        .select { [it.id, f.length(it.username).alias("username")] }
        .orderBy { it.username.desc() }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val selectedLength: Int? = clause.first(wrapper).username
        return "Fail: selected alias type unexpectedly resolved as $selectedLength"
    }
    val rows = clause.toList(wrapper)
    val row = rows.singleOrNull()

    val failures = listOfNotNull(
        expect(row?.id == 3) { "id was ${row?.id}" },
        expect(row?.username == 5) { "username was ${row?.username}" },
        expect(row?.__columns?.map { it.name } == listOf("id", "username")) {
            "columns were ${row?.__columns}"
        },
    )
    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
