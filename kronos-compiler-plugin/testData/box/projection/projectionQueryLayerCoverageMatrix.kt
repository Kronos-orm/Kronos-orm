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

// Verifies generated projection types survive every selectable query layer.

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem

@Table("tb_projection_layer_matrix_user")
data class ProjectionLayerMatrixUser(
    @PrimaryKey
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_projection_layer_matrix_order")
data class ProjectionLayerMatrixOrder(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

fun box(): String {
    val projected = ProjectionLayerMatrixUser()
        .select { [it.id.alias("userId"), f.length(it.name).alias("nameLength")] }
    val explicitReceiver = ProjectionLayerMatrixUser()
        .select { source -> [source.id.alias("explicitId"), source.name] }
    val derived = projected.select { [it.userId, it.nameLength] }
    val paged = projected.page(pageIndex = 2, pageSize = 3)
    val pagedDerived = paged.select { [it.userId, it.nameLength] }
    val cursor = ProjectionLayerMatrixUser()
        .select { [it.id, it.name] }
        .orderBy { it.id.asc() }
        .cursor(pageSize = 2)
    val joined = ProjectionLayerMatrixUser().join(ProjectionLayerMatrixOrder()) { user, order ->
        innerJoin { user.id == order.userId }
            .select { [user.id.alias("userId"), order.amount.alias("amount")] }
    }
    val joinedDerived = joined.select { [it.userId, it.amount] }
    val joinedPage = joined.page(pageIndex = 2, pageSize = 3)
    val unioned = union(projected, projected)
    val unionDerived = unioned.select { [it.userId, it.nameLength] }
    val unionPage = unioned.limit(limit = 3, offset = 3)
    val withScalar = ProjectionLayerMatrixUser().select {
        [
            it.id,
            ProjectionLayerMatrixOrder()
                .select { order -> order.amount }
                .limit(1)
                .alias("latestAmount")
        ]
    }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val derivedRow = derived.first()
        val pagedRow = pagedDerived.firstOrNull()
        val cursorRows = cursor.toList()
        val typedRows = projected.toList<Map<String, Any?>>()
        val joinedRow = joinedDerived.first()
        val joinedPageRow = joinedPage.firstOrNull()
        val unionRow = unionDerived.first()
        val unionPageRow = unionPage.firstOrNull()
        val scalarRow = withScalar.first()
        val values: List<Any?> = listOf(
            derivedRow.userId,
            derivedRow.nameLength,
            pagedRow?.userId,
            cursorRows.records.firstOrNull()?.id,
            typedRows.firstOrNull(),
            joinedRow.userId,
            joinedPageRow?.userId,
            unionRow.nameLength,
            unionPageRow?.userId,
            scalarRow.latestAmount,
        )
        return "Fail: generated values were $values"
    }

    val statements = listOf(
        projected.toSqlQuery(),
        explicitReceiver.toSqlQuery(),
        derived.toSqlQuery(),
        paged.toSqlQuery(),
        pagedDerived.toSqlQuery(),
        cursor.toSqlQuery(),
        joined.toSqlQuery(),
        joinedDerived.toSqlQuery(),
        joinedPage.toSqlQuery(),
        unioned.toSqlQuery(),
        unionDerived.toSqlQuery(),
        unionPage.toSqlQuery(),
        withScalar.toSqlQuery(),
    )
    val outputNames = statements.map(SqlQuery::outputNames)
    return if (
        outputNames[0] == listOf("userId", "nameLength") &&
        outputNames[1] == listOf("explicitId", "name") &&
        outputNames[2] == listOf("userId", "nameLength") &&
        outputNames[3] == listOf("userId", "nameLength") &&
        outputNames[4] == listOf("userId", "nameLength") &&
        outputNames[5] == listOf("id", "name") &&
        outputNames[6] == listOf("userId", "amount") &&
        outputNames[7] == listOf("userId", "amount") &&
        outputNames[8] == listOf("userId", "amount") &&
        outputNames[9] == listOf("userId", "nameLength") &&
        outputNames[10] == listOf("userId", "nameLength") &&
        outputNames[11] == listOf("userId", "nameLength") &&
        outputNames[12] == listOf("id", "latestAmount")
    ) {
        "OK"
    } else {
        "Fail: projection layer outputs were $outputNames"
    }
}

private fun SqlQuery.outputNames(): List<String> = when (this) {
    is SqlQuery.Select -> select.mapNotNull { item ->
        (item as? SqlSelectItem.Expr)?.outputName()
    }
    is SqlQuery.Set -> left.outputNames()
    is SqlQuery.With -> query.outputNames()
    is SqlQuery.Values -> emptyList()
}

private fun SqlSelectItem.Expr.outputName(): String? =
    metadata?.outputName ?: alias ?: (expr as? com.kotlinorm.syntax.expr.SqlExpr.Column)?.columnName
