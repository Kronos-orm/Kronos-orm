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

// Verifies union insert-select value lambdas expose the union source's generated Selected receiver.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem

@Table("tb_union_insert_source")
data class UnionInsertSource(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_union_insert_target")
data class UnionInsertTarget(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val source = union(
        UnionInsertSource().select { [it.id, it.userId.alias("uid"), it.status] },
        UnionInsertSource().select { [it.id, it.userId.alias("uid"), it.status] }
    )

    val insert = source.insert<UnionInsertTarget> {
        val uid: Int? = it.uid
        if (uid == -1) error("unreachable")
        [it.id, it.uid, null]
    }

    val statement = insert.toSqlStatement()
    val unionStatement = (statement.mode as? SqlInsertMode.Subquery)?.query as? SqlQuery.Set
        ?: return "Fail: source was ${statement.mode}"
    val rewritten = unionStatement.selectBranches().map { it.select }

    val failures = listOfNotNull(
        expect(statement.columns.map { it.last } == listOf("id", "user_id", "status")) {
            "target columns were ${statement.columns.map { it.last }}"
        },
        expect(rewritten.size == 2) { "union branch count was ${rewritten.size}" },
        expect(rewritten.all { it.size == 3 }) { "rewritten branch sizes were ${rewritten.map { it.size }}" },
        expect(rewritten.all { it.expressionColumn(0) == "id" }) { "first expressions were $rewritten" },
        expect(rewritten.all { it.expressionColumn(1) == "user_id" }) { "second expressions were $rewritten" },
    )

    return failures.firstOrNull() ?: "OK"
}

fun SqlQuery.Set.selectBranches(): List<SqlQuery.Select> =
    left.selectBranches() + right.selectBranches()

fun SqlQuery.selectBranches(): List<SqlQuery.Select> =
    when (this) {
        is SqlQuery.Select -> listOf(this)
        is SqlQuery.Set -> selectBranches()
        is SqlQuery.With -> query.selectBranches()
        is SqlQuery.Values -> emptyList()
    }

fun List<SqlSelectItem>.expressionColumn(index: Int): String? {
    return ((getOrNull(index) as? SqlSelectItem.Expr)?.expr as? SqlExpr.Column)?.columnName
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
