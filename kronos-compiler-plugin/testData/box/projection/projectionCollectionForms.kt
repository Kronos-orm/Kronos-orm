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

// Verifies supported collection constructors feed generated Selected and Context receivers.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import kotlin.reflect.KClass

@Table("tb_projection_collection_forms")
data class ProjectionCollectionFormsUser(
    var id: Int? = null,
    var username: String? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val arrayClause = ProjectionCollectionFormsUser()
        .select { arrayOf<Any?>(it, it.id.alias("arrayId")) }
        .orderBy { it.arrayId.desc() }
    val mutableListClause = ProjectionCollectionFormsUser()
        .select { mutableListOf<Any?>(it, it.id.alias("mutableId")) }
        .orderBy { it.mutableId.asc() }
    val setClause = ProjectionCollectionFormsUser()
        .select { setOf<Any?>(it, it.id.alias("setId")) }
        .orderBy { it.setId.desc() }
    val singleLiteralClause = ProjectionCollectionFormsUser()
        .select { [it.id.alias("singleId")] }
        .orderBy { it.singleId.asc() }
    val multiLiteralClause = ProjectionCollectionFormsUser()
        .select { [it, it.id.alias("literalId")] }
        .orderBy { it.literalId.desc() }
    val listClause = ProjectionCollectionFormsUser()
        .select { listOf<Any?>(it, it.id.alias("listId")) }
        .orderBy { it.listId.asc() }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val arraySelected = arrayClause.first()
        val arraySourceId: Int? = arraySelected.id
        val arrayUsername: String? = arraySelected.username
        val arrayStatus: Int? = arraySelected.status
        val arrayId: Int? = arraySelected.arrayId
        val mutableSelected = mutableListClause.first()
        val mutableSourceId: Int? = mutableSelected.id
        val mutableUsername: String? = mutableSelected.username
        val mutableSourceStatus: Int? = mutableSelected.status
        val mutableId: Int? = mutableSelected.mutableId
        val setSelected = setClause.first()
        val setSourceId: Int? = setSelected.id
        val setUsername: String? = setSelected.username
        val setStatus: Int? = setSelected.status
        val setId: Int? = setSelected.setId
        val singleLiteralSelected = singleLiteralClause.first()
        val singleId: Int? = singleLiteralSelected.singleId
        val multiLiteralSelected = multiLiteralClause.first()
        val literalSourceId: Int? = multiLiteralSelected.id
        val literalUsername: String? = multiLiteralSelected.username
        val literalStatus: Int? = multiLiteralSelected.status
        val literalId: Int? = multiLiteralSelected.literalId
        val listSelected = listClause.first()
        val listSourceId: Int? = listSelected.id
        val listUsername: String? = listSelected.username
        val listStatus: Int? = listSelected.status
        val listId: Int? = listSelected.listId
        return "Fail: selected values unexpectedly evaluated as " +
            "$arraySourceId/$arrayUsername/$arrayStatus/$arrayId/" +
            "$mutableSourceId/$mutableUsername/$mutableSourceStatus/$mutableId/" +
            "$setSourceId/$setUsername/$setStatus/$setId/$singleId/" +
            "$literalSourceId/$literalUsername/$literalStatus/$literalId/" +
            "$listSourceId/$listUsername/$listStatus/$listId"
    }

    val arrayStatement = arrayClause.toSqlQuery() as SqlQuery.Select
    val mutableListStatement = mutableListClause.toSqlQuery() as SqlQuery.Select
    val setStatement = setClause.toSqlQuery() as SqlQuery.Select
    val singleLiteralStatement = singleLiteralClause.toSqlQuery() as SqlQuery.Select
    val multiLiteralStatement = multiLiteralClause.toSqlQuery() as SqlQuery.Select
    val listStatement = listClause.toSqlQuery() as SqlQuery.Select

    val failures = listOfNotNull(
        expect(arrayStatement.selectAliases() == listOf("id", "username", "status", "arrayId")) {
            "array aliases were ${arrayStatement.selectAliases()}"
        },
        expect(arrayStatement.hasOrderByColumn("arrayId", SqlOrdering.Desc)) {
            "array order by was ${arrayStatement.allOrderBy()}"
        },
        expect(mutableListStatement.selectAliases() == listOf("id", "username", "status", "mutableId")) {
            "mutable list aliases were ${mutableListStatement.selectAliases()}"
        },
        expect(mutableListStatement.hasOrderByColumn("mutableId", SqlOrdering.Asc)) {
            "mutable list order by was ${mutableListStatement.allOrderBy()}"
        },
        expect(setStatement.selectAliases() == listOf("id", "username", "status", "setId")) {
            "set aliases were ${setStatement.selectAliases()}"
        },
        expect(setStatement.hasOrderByColumn("setId", SqlOrdering.Desc)) {
            "set order by was ${setStatement.allOrderBy()}"
        },
        expect(singleLiteralStatement.selectAliases() == listOf("singleId")) {
            "single literal aliases were ${singleLiteralStatement.selectAliases()}"
        },
        expect(singleLiteralStatement.hasOrderByColumn("singleId", SqlOrdering.Asc)) {
            "single literal order by was ${singleLiteralStatement.allOrderBy()}"
        },
        expect(multiLiteralStatement.selectAliases() == listOf("id", "username", "status", "literalId")) {
            "multi literal aliases were ${multiLiteralStatement.selectAliases()}"
        },
        expect(multiLiteralStatement.hasOrderByColumn("literalId", SqlOrdering.Desc)) {
            "multi literal order by was ${multiLiteralStatement.allOrderBy()}"
        },
        expect(listStatement.selectAliases() == listOf("id", "username", "status", "listId")) {
            "list aliases were ${listStatement.selectAliases()}"
        },
        expect(listStatement.hasOrderByColumn("listId", SqlOrdering.Asc)) {
            "list order by was ${listStatement.allOrderBy()}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun SqlQuery.Select.selectAliases(): List<String?> {
    return allSelectStatements().last().select.map { item ->
        val expr = item as? SqlSelectItem.Expr
        expr?.metadata?.outputName ?: expr?.alias
    }
}

fun SqlQuery.Select.hasOrderByColumn(name: String, direction: SqlOrdering): Boolean {
    return allOrderBy().any { item ->
        item.ordering == direction &&
            item.expr.containsExpression(SqlExpr.Column::class) { it.columnName == name }
    }
}

fun SqlQuery.Select.allOrderBy(): List<SqlOrderingItem> =
    allSelectStatements().flatMap { it.orderBy }

fun SqlQuery.Select.allSelectStatements(): List<SqlQuery.Select> {
    val nested = ((from.singleOrNull() as? SqlTable.Subquery)?.query as? SqlQuery.Select)?.allSelectStatements().orEmpty()
    return listOf(this) + nested
}

fun <T : Any> Any?.containsExpression(type: KClass<T>, match: (T) -> Boolean): Boolean {
    return when (this) {
        null -> false
        else -> {
            @Suppress("UNCHECKED_CAST")
            val currentMatches = type.isInstance(this) && match(this as T)
            currentMatches || when (this) {
                is SqlExpr.Binary -> left.containsExpression(type, match) || right.containsExpression(type, match)
                else -> false
            }
        }
    }
}
