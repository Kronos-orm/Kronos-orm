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

// Verifies insert-select value lowering for fields, functions, and operators.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem

@Table("tb_insert_value_matrix_source")
data class InsertValueMatrixSource(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var name: String? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_insert_value_matrix_target")
data class InsertValueMatrixTarget(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var nameLength: Int? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val source = InsertValueMatrixSource()
        .select {
            [
                it.id,
                it.userId.alias("archivedUserId"),
                it.name.alias("archivedName"),
                it.status.alias("archivedStatus")
            ]
        }

    val insert = source.insert<InsertValueMatrixTarget> {
        [
            it.id,
            it.archivedUserId,
            f.length(it.archivedName),
            it.archivedStatus + 1
        ]
    }

    val statement = insert.toSqlStatement()
    val sourceStatement = (statement.mode as? SqlInsertMode.Subquery)?.query as? SqlQuery.Select
        ?: return "Fail: source was ${statement.mode}"
    val rewritten = sourceStatement.select

    val userIdColumn = rewritten.exprAt(1) as? SqlExpr.Column
    val lengthExpr = rewritten.exprAt(2) as? SqlExpr.Function
    val plusExpr = rewritten.exprAt(3) as? SqlExpr.Binary

    val failures = listOfNotNull(
        expect(statement.targetColumns() == listOf("id", "user_id", "name_length", "status")) {
            "target columns were ${statement.targetColumns()}"
        },
        expect(rewritten.size == 4) { "rewritten size was ${rewritten.size}" },
        expect((rewritten.exprAt(0) as? SqlExpr.Column)?.columnName == "id") {
            "id expr was ${rewritten.exprAt(0)}"
        },
        expect(userIdColumn?.columnName == "user_id") { "user id expr was $userIdColumn" },
        expect(lengthExpr?.name?.last == "LENGTH") { "length expr was $lengthExpr" },
        expect((lengthExpr?.args?.singleOrNull() as? SqlExpr.Column)?.columnName == "name") {
            "length args were ${lengthExpr?.args}"
        },
        expect(plusExpr?.operator == SqlBinaryOperator.Plus) { "plus expr was $plusExpr" },
        expect((plusExpr?.left as? SqlExpr.Column)?.columnName == "status") {
            "plus left was ${plusExpr?.left}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

fun SqlDmlStatement.Insert.targetColumns(): List<String> = columns.map { it.last }

fun List<SqlSelectItem>.exprAt(index: Int): SqlExpr? =
    (getOrNull(index) as? SqlSelectItem.Expr)?.expr

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
