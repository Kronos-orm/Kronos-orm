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

// Verifies insert-select values treat `limit(1) as T` as a scalar subquery type hint.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem

@Table("tb_insert_scalar_hint_source")
data class InsertScalarHintSource(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_insert_scalar_hint_lookup")
data class InsertScalarHintLookup(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_insert_scalar_hint_target")
data class InsertScalarHintTarget(
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

    val params = mutableMapOf<String, Any?>()
    val source = InsertScalarHintSource()
        .select {
            [
                it.id,
                it.userId.alias("archivedUserId"),
                it.status.alias("archivedStatus")
            ]
        }
        .where { it.status == 9 }

    val insert = source.insert<InsertScalarHintTarget> {
        [
            it.id,
            (InsertScalarHintLookup()
                .select { lookup -> lookup.userId }
                .where { lookup -> lookup.status == 5 }
                .limit(1) as Int?),
            it.archivedStatus
        ]
    }

    val statement = insert.toSqlStatement(parameterValues = params)
    val sourceStatement = (statement.mode as? SqlInsertMode.Subquery)?.query as? SqlQuery.Select
    val scalar = sourceStatement?.scalarExpressionAt(1)

    val failures = listOfNotNull(
        expect(statement.targetColumns() == listOf("id", "user_id", "status")) {
            "target columns were ${statement.targetColumns()}"
        },
        expect(scalar is SqlExpr.Subquery) {
            "scalar was ${scalar?.let { it::class.qualifiedName }}"
        },
        expect(9 in params.values && 5 in params.values) { "params were $params" },
    )

    return failures.firstOrNull() ?: "OK"
}

fun SqlDmlStatement.Insert.targetColumns(): List<String> = columns.map { it.last }

fun SqlQuery.Select.scalarExpressionAt(index: Int) =
    (select.getOrNull(index) as? SqlSelectItem.Expr)?.expr

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
