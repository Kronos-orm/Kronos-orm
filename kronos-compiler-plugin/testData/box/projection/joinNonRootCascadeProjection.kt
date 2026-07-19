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

// Verifies a cascade projected from a non-root JOIN source keeps its hidden local key and loads the relation.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.join.join
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.utils.Extensions.mapperTo
import kotlin.reflect.KClass

@Table("tb_join_cascade_root")
data class JoinCascadeRoot(
    val id: Int? = null,
) : KPojo

@Table("tb_join_cascade_profile")
data class JoinCascadeProfile(
    val id: Int? = null,
    val label: String? = null,
) : KPojo

@Table("tb_join_cascade_owner")
data class JoinCascadeOwner(
    val id: Int? = null,
    val rootId: Int? = null,
    val profileId: Int? = null,
    @Cascade(["profileId"], ["id"])
    val profile: JoinCascadeProfile? = null,
) : KPojo

class JoinNonRootCascadeWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:join-non-root-cascade-projection"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    var mainQueryCount: Int = 0
    var cascadeFirstCount: Int = 0
    var mainSql: String? = null

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        mainQueryCount++
        mainSql = task.sql
        val kClass = task.targetType.classifier as? KClass<*> ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        return [mapOf("profileId" to 7).mapperTo(kClass as KClass<out KPojo>)]
    }

    override fun first(task: KAtomicQueryTask): Any? {
        cascadeFirstCount++
        return JoinCascadeProfile(id = 7, label = "profile-7")
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
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val query = JoinCascadeRoot().join(JoinCascadeOwner()) { root, owner ->
        innerJoin { root.id == owner.rootId }
            .select { owner.profile }
    }
    val wrapper = JoinNonRootCascadeWrapper()
    val statement = query.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val rows = query.toList(wrapper)
    val row = rows.singleOrNull()
    val fields = row?.toDataMap().orEmpty()

    val failures = listOfNotNull(
        expect(statement.select.filterIsInstance<SqlSelectItem.Expr>().map { it.expr }
            .filterIsInstance<SqlExpr.Column>().map { it.columnName } == listOf("profile_id")) {
            "JOIN SQL columns were ${statement.select}"
        },
        expect(statement.select.filterIsInstance<SqlSelectItem.Expr>().mapNotNull {
            it.metadata?.outputName ?: it.alias
        } == listOf("profileId")) {
            "JOIN SQL output labels were ${statement.select}"
        },
        expect(wrapper.mainSql?.contains("profile_id") == true &&
            wrapper.mainSql?.contains(".profile ") != true) {
            "main SQL was ${wrapper.mainSql}"
        },
        expect(rows.size == 1) { "row count was ${rows.size}" },
        expect(fields.keys == linkedSetOf("profile", "profileId")) { "Selected fields were ${fields.keys}" },
        expect(fields["profileId"] == 7) { "hidden profileId was ${fields["profileId"]}" },
        expect(row?.__columns?.map { it.name } == listOf("profile", "profileId")) {
            "Selected columns were ${row?.__columns?.map { it.name }}"
        },
        expect(wrapper.mainQueryCount == 1) { "main query count was ${wrapper.mainQueryCount}" },
        expect(wrapper.cascadeFirstCount == 1) { "cascade first count was ${wrapper.cascadeFirstCount}" },
        expect(row?.profile == JoinCascadeProfile(id = 7, label = "profile-7")) {
            "cascade profile was ${row?.profile}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

private inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
