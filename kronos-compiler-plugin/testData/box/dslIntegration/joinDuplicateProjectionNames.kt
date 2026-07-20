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

// Verifies opted-in join projections use id/id_1 consistently in SQL and generated Selected.

import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UnsafeProjectionOverride
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.join.join
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.utils.Extensions.mapperTo
import kotlin.reflect.KClass

@Table("tb_join_duplicate_user")
data class JoinDuplicateUser(
    var id: Int? = null,
    var companyId: Int? = null,
) : KPojo

@Table("tb_join_duplicate_company")
data class JoinDuplicateCompany(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

private val joinDuplicateRow = linkedMapOf<String, Any?>(
    "id" to 7,
    "id_1" to 9,
)

class JoinDuplicateWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:join-duplicate-projection"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql

    override fun toList(task: KAtomicQueryTask): List<Any?> = listOf(mapResult(task))

    override fun first(task: KAtomicQueryTask): Any? = mapResult(task)

    private fun mapResult(task: KAtomicQueryTask): Any {
        val classifier = task.targetType.classifier
        if (classifier == Map::class) return joinDuplicateRow
        val kClass = classifier as? KClass<*> ?: return joinDuplicateRow
        @Suppress("UNCHECKED_CAST")
        return joinDuplicateRow.mapperTo(kClass as KClass<out KPojo>)
    }

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
    val wrapper = JoinDuplicateWrapper()
    val clause = JoinDuplicateUser().join(JoinDuplicateCompany()) { user, company ->
        leftJoin { user.companyId == company.id }
            .select { [user.id, company.id] }
    }
    val statement = clause.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val names = statement.select.mapNotNull { item ->
        val expression = item as? SqlSelectItem.Expr
        expression?.metadata?.outputName ?: expression?.alias
    }
    val typed = clause.toList(wrapper).singleOrNull()
    val map = clause.toMapList(wrapper).singleOrNull()
    val failures = listOfNotNull(
        expect(names == listOf("id", "id_1")) { "output names were $names" },
        expect(typed?.id == 7) { "typed id was ${typed?.id}" },
        expect(typed?.id_1 == 9) { "typed id_1 was ${typed?.id_1}" },
        expect(typed?.__columns?.map { it.name } == listOf("id", "id_1")) {
            "typed columns were ${typed?.__columns?.map { it.name }}"
        },
        expect(map == joinDuplicateRow) { "map row was $map" },
    )
    return failures.firstOrNull() ?: "OK"
}

private inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
