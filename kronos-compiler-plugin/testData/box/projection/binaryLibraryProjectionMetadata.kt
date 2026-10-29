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

// Verifies binary library projections expose typed aliases, generic fields, contexts and factories.
// MODULE: library
// FILE: library.kt
package fixture

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

data class BinaryProjectionRow(
    var id: Int = 7,
    var name: String = "Ada",
    var tags: List<String?> = listOf("a", null),
) : KPojo

fun exportedProjection() = BinaryProjectionRow().select { [it.id, it.name.alias("displayName"), it.tags] }

// MODULE: main(library)
// FILE: application.kt
@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)
package fixture

import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.SelectClause
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.utils.initializeKronosMetadata
import kotlin.reflect.typeOf

inline fun <reified T : KPojo> materialize(query: SelectClause<*, T, *>, data: Map<String, Any?>): T =
    (Kronos.createKPojo(typeOf<T>()) as T).fromMapData<T>(data)

fun hasAliasOrder(query: SqlQuery.Select): Boolean =
    query.orderBy.any { it.ordering == SqlOrdering.Desc && (it.expr as? SqlExpr.Column)?.columnName == "displayName" } ||
        query.from.filterIsInstance<SqlTable.Subquery>().any { (it.query as? SqlQuery.Select)?.let(::hasAliasOrder) == true }

fun box(): String {
    initializeKronosMetadata()
    val query = exportedProjection().orderBy { it.displayName.desc() }
    val projected = materialize(query, mapOf("id" to 9, "displayName" to "Grace", "tags" to listOf("b", null)))
    val id: Int = projected.id
    val displayName: String = projected.displayName
    val tags: List<String?> = projected.tags
    if (id != 9 || displayName != "Grace" || tags != listOf("b", null)) {
        return "Fail: binary projection values were $id / $displayName / $tags"
    }
    if (!hasAliasOrder(query.toSqlQuery() as SqlQuery.Select)) return "Fail: binary context lost alias ordering"
    return "OK"
}
