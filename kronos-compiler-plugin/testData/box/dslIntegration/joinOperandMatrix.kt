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

// Verifies the table/selectable/raw JOIN operand matrix preserves AST shape and generated Selected types.

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_join_matrix_a")
data class JoinMatrixA(
    var id: Int? = null,
    var bId: Int? = null,
) : KPojo

@Table("tb_join_matrix_b")
data class JoinMatrixB(
    var id: Int? = null,
    var cId: Int? = null,
    var label: String? = null,
) : KPojo

@Table("tb_join_matrix_c")
data class JoinMatrixC(
    var id: Int? = null,
    var label: String? = null,
) : KPojo

fun box(): String {
    val derivedA = JoinMatrixA().select { [it.id, it.bId] }
    val derivedB = JoinMatrixB().select { [it.id, it.cId, it.label] }
    val derivedC = JoinMatrixC().select { [it.id, it.label] }
    val unionB = union(
        JoinMatrixB().select { [it.id, it.cId, it.label] },
        JoinMatrixB().select { [it.id, it.cId, it.label] },
    )
    val raw = JoinMatrixB().join(JoinMatrixC()) { b, c ->
        innerJoin { b.cId == c.id }
    }
    val rightRaw = JoinMatrixA().join(JoinMatrixB()) { a, b ->
        rightJoin { a.bId == b.id }
    }

    val selectableSelectable = derivedB.join(derivedC) { b, c ->
        leftJoin { b.cId == c.id }
            .select { [b.id, c.label.alias("cLabel")] }
    }
    val unionDerived = unionB.join(derivedC) { b, c ->
        leftJoin { b.cId == c.id }
            .select { [b.id, c.label.alias("cLabel")] }
    }
    val selectableRaw = derivedA.join(raw) { a, b, c ->
        leftJoin { a.bId == b.id }
            .select { [a.id, b.label.alias("bLabel"), c.label.alias("cLabel")] }
    }
    val rawSelectable = raw.join(derivedA) { b, c, a ->
        leftJoin { c.id == a.bId }
            .select { [b.id, c.label.alias("cLabel"), a.id.alias("aId")] }
    }
    val rawRaw = raw.join(rightRaw) { b, c, a, rightB ->
        leftJoin { c.id == a.bId }
            .select {
                [
                    b.id,
                    c.label.alias("cLabel"),
                    a.id.alias("aId"),
                    rightB.label.alias("rightBLabel"),
                ]
            }
    }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val selectableSelectableRow = selectableSelectable.first()
        val unionDerivedRow = unionDerived.first()
        val selectableRawRow = selectableRaw.first()
        val rawSelectableRow = rawSelectable.first()
        val rawRawRow = rawRaw.first()
        val values: List<Any?> = listOf(
            selectableSelectableRow.id,
            selectableSelectableRow.cLabel,
            unionDerivedRow.id,
            unionDerivedRow.cLabel,
            selectableRawRow.id,
            selectableRawRow.bLabel,
            selectableRawRow.cLabel,
            rawSelectableRow.id,
            rawSelectableRow.cLabel,
            rawSelectableRow.aId,
            rawRawRow.id,
            rawRawRow.cLabel,
            rawRawRow.aId,
            rawRawRow.rightBLabel,
        )
        return "Fail: generated values unexpectedly evaluated as $values"
    }

    val queries: List<KSelectable<out KPojo>> = listOf(
        JoinMatrixA().join(JoinMatrixB()) { a, b ->
            leftJoin { a.bId == b.id }.select { [a.id, b.label.alias("bLabel")] }
        },
        JoinMatrixA().join(derivedB) { a, b ->
            leftJoin { a.bId == b.id }.select { [a.id, b.label.alias("bLabel")] }
        },
        derivedB.join(JoinMatrixC()) { b, c ->
            leftJoin { b.cId == c.id }.select { [b.id, c.label.alias("cLabel")] }
        },
        JoinMatrixA().join(unionB) { a, b ->
            leftJoin { a.bId == b.id }.select { [a.id, b.label.alias("bLabel")] }
        },
        unionB.join(JoinMatrixC()) { b, c ->
            leftJoin { b.cId == c.id }.select { [b.id, c.label.alias("cLabel")] }
        },
        JoinMatrixA().join(raw) { a, b, c ->
            leftJoin { a.bId == b.id }
                .select { [a.id, b.label.alias("bLabel"), c.label.alias("cLabel")] }
        },
        raw.join(JoinMatrixA()) { b, c, a ->
            leftJoin { c.id == a.bId }
                .select { [b.id, c.label.alias("cLabel"), a.id.alias("aId")] }
        },
        selectableSelectable,
        unionDerived,
        selectableRaw,
        rawSelectable,
        rawRaw,
    )

    val shapes = queries.map { query ->
        val statement = query.toSqlQuery() as? SqlQuery.Select
            ?: return "Fail: query was ${query.toSqlQuery()}"
        statement.from.singleOrNull()?.shape()
    }
    val expected = listOf(
        "(tb_join_matrix_a Left tb_join_matrix_b)",
        "(tb_join_matrix_a Left subquery:select)",
        "(subquery:select Left tb_join_matrix_c)",
        "(tb_join_matrix_a Left subquery:set)",
        "(subquery:set Left tb_join_matrix_c)",
        "(tb_join_matrix_a Left (tb_join_matrix_b Inner tb_join_matrix_c))",
        "((tb_join_matrix_b Inner tb_join_matrix_c) Left tb_join_matrix_a)",
        "(subquery:select Left subquery:select)",
        "(subquery:set Left subquery:select)",
        "(subquery:select Left (tb_join_matrix_b Inner tb_join_matrix_c))",
        "((tb_join_matrix_b Inner tb_join_matrix_c) Left subquery:select)",
        "((tb_join_matrix_b Inner tb_join_matrix_c) Left (tb_join_matrix_a Right tb_join_matrix_b))",
    )
    return if (shapes == expected) "OK" else "Fail: operand shapes were $shapes"
}

private fun SqlTable.shape(): String = when (this) {
    is SqlTable.Ident -> name
    is SqlTable.Subquery -> "subquery:${query.kind()}"
    is SqlTable.Join -> "(${left.shape()} ${joinType.label()} ${right.shape()})"
    is SqlTable.Func -> "function:${name}"
    is SqlTable.Json -> "json"
    is SqlTable.Graph -> "graph:${name}"
}

private fun SqlJoinType.label(): String = when (this) {
    SqlJoinType.Inner -> "Inner"
    SqlJoinType.Left -> "Left"
    SqlJoinType.Right -> "Right"
    SqlJoinType.Full -> "Full"
    SqlJoinType.Cross -> "Cross"
    is SqlJoinType.UnsafeCustom -> "UnsafeCustom"
}

private fun SqlQuery.kind(): String = when (this) {
    is SqlQuery.Select -> "select"
    is SqlQuery.Set -> "set"
    is SqlQuery.With -> "with"
    is SqlQuery.Values -> "values"
}
