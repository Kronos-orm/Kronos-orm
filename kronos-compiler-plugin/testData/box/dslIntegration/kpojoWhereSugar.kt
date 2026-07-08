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

// Verifies KPojo.where delegates to select().where() and queryList() returns Source rows.

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
import com.kotlinorm.orm.select.where
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import kotlin.reflect.KClass

@Table("tb_where_sugar_user")
data class WhereSugarUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

class WhereSugarWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:where-sugar"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    var mappedClass: KClass<*>? = null

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> {
        mappedClass = kClass
        return listOf(WhereSugarUser(id = 7, name = "Ada"))
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
    val wrapper = WhereSugarWrapper()

    with(Kronos) {
        dataSource = { wrapper }
    }

    val statement = WhereSugarUser()
        .where { it.id == 7 }
        .toSqlQuery() as SqlQuery.Select
    val rows: List<WhereSugarUser> = WhereSugarUser()
        .where { it.id == 7 }
        .queryList(wrapper)
    val row = rows.singleOrNull()
    val derived = WhereSugarUser()
        .where { it.id == 7 }
        .select { [it.id] }
        .where { it.name == "Ada" }
        .toSqlQuery() as SqlQuery.Select
    val inner = (derived.from.singleOrNull() as? SqlTable.Subquery)?.query as? SqlQuery.Select

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val selected = WhereSugarUser()
            .where { it.id == 7 }
            .select { [it.id] }
            .queryOne()
        val id: Int? = selected.id
        return "Fail: selected projection unexpectedly evaluated as $id"
    }

    return when {
        statement.select.size != 2 -> "Fail: select size was ${statement.select.size}"
        wrapper.mappedClass != WhereSugarUser::class -> "Fail: mapped class was ${wrapper.mappedClass}"
        row?.name != "Ada" -> "Fail: row was $row"
        inner == null -> "Fail: derived source was ${derived.from}"
        derived.select.mapNotNull { ((it as? SqlSelectItem.Expr)?.expr as? SqlExpr.Column)?.columnName } != listOf("id") ->
            "Fail: derived select list was ${derived.select}"
        derived.where !is SqlExpr.Binary -> "Fail: derived where was ${derived.where}"
        inner.select.mapNotNull { ((it as? SqlSelectItem.Expr)?.expr as? SqlExpr.Column)?.columnName } != listOf("id", "name") ->
            "Fail: inner select list was ${inner.select}"
        (derived.select.singleOrNull() as? SqlSelectItem.Expr)?.expr !is SqlExpr.Column ->
            "Fail: selected item was ${derived.select.singleOrNull()}"
        else -> "OK"
    }
}
