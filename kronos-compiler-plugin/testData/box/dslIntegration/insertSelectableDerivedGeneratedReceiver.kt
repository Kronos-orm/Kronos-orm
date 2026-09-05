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

// Verifies insert<Target> value lambdas use a derived query's generated Selected receiver.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_insert_derived_source")
data class InsertDerivedSource(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_insert_derived_target")
data class InsertDerivedTarget(
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
    val base = InsertDerivedSource()
        .select {
            [
                it.id,
                it.userId.alias("archivedUserId"),
                it.status.alias("archivedStatus")
            ]
        }
        .where { it.status == 7 }

    val derived = base
        .select {
            [
                it.id,
                it.archivedUserId,
                it.archivedStatus
            ]
        }
        .where { it.archivedStatus == 8 }

    val insert = derived.insert<InsertDerivedTarget> {
        val userId: Int? = it.archivedUserId
        val status: Int? = it.archivedStatus
        if (userId == -1 || status == -1) error("unreachable")
        [
            it.id,
            it.archivedUserId,
            it.archivedStatus + 1
        ]
    }

    val statement = insert.toSqlStatement(parameterValues = params)
    val sourceStatement = (statement.mode as? SqlInsertMode.Subquery)?.query as? SqlQuery.Select
    val rewritten = sourceStatement?.select.orEmpty()
    val sourceFrom = sourceStatement?.from

    val failures = listOfNotNull(
        expect(statement.targetColumns() == listOf("id", "user_id", "status")) {
            "target columns were ${statement.targetColumns()}"
        },
        expect(sourceFrom?.singleOrNull() is SqlTable.Subquery) { "source from was $sourceFrom" },
        expect(7 in params.values && 8 in params.values) { "params were $params" },
        expect(rewritten.size == 3) { "rewritten select size was ${rewritten.size}" },
        expect(rewritten.expressionColumn(0) == "id") { "first source expression was ${rewritten.getOrNull(0)}" },
        expect(rewritten.expressionColumn(1) == "archivedUserId") {
            "second source expression was ${rewritten.getOrNull(1)}"
        },
        expect(rewritten.expressionOperator(2) == SqlBinaryOperator.Plus) { "third source expression was ${rewritten.getOrNull(2)}" },
    )

    return failures.firstOrNull() ?: "OK"
}

fun SqlDmlStatement.Insert.targetColumns(): List<String> = columns.map { it.last }

fun List<SqlSelectItem>.expressionColumn(index: Int): String? {
    return ((getOrNull(index) as? SqlSelectItem.Expr)?.expr as? SqlExpr.Column)?.columnName
}

fun List<SqlSelectItem>.expressionOperator(index: Int): SqlBinaryOperator? {
    return ((getOrNull(index) as? SqlSelectItem.Expr)?.expr as? SqlExpr.Binary)?.operator
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
