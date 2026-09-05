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

// Verifies implicit source receivers and shadowed JOIN lambda names keep lexical ownership.

import com.kotlinorm.annotations.Table
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem

@Table("tb_projection_receiver_scope_user")
data class ProjectionReceiverScopeUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_projection_receiver_scope_order")
data class ProjectionReceiverScopeOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

fun box(): String {
    val source = ProjectionReceiverScopeUser()
    val implicitOuterReceiver = with(source) {
        select { id }
    }
    val explicitOuterReceiver = source.run sourceScope@ {
        select { this@sourceScope.name }
    }
    val shadowedJoinReceiver = source.join(ProjectionReceiverScopeOrder()) { user, row ->
        innerJoin { user.id == row.userId }
            .select { row -> [row.name.alias("userName"), user.id.alias("userId")] }
    }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val implicitRow = implicitOuterReceiver.first()
        val explicitRow = explicitOuterReceiver.first()
        val joinedRow = shadowedJoinReceiver.first()
        val implicitId: Int? = implicitRow.id
        val explicitName: String? = explicitRow.name
        val joinedName: String? = joinedRow.userName
        val joinedId: Int? = joinedRow.userId
        return "Fail: generated values were $implicitId/$explicitName/$joinedName/$joinedId"
    }

    val implicitStatement = implicitOuterReceiver.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val explicitStatement = explicitOuterReceiver.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val joinedStatement = shadowedJoinReceiver.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select

    val failures = listOfNotNull(
        expect(implicitStatement.outputNames() == listOf("id")) {
            "implicit outputs were ${implicitStatement.outputNames()}"
        },
        expect(explicitStatement.outputNames() == listOf("name")) {
            "explicit outputs were ${explicitStatement.outputNames()}"
        },
        expect(joinedStatement.outputNames() == listOf("userName", "userId")) {
            "JOIN outputs were ${joinedStatement.outputNames()}"
        },
        expect(joinedStatement.selectedColumns() == listOf(
            "tb_projection_receiver_scope_user.name",
            "tb_projection_receiver_scope_user.id",
        )) {
            "JOIN columns were ${joinedStatement.selectedColumns()}"
        },
    )
    return failures.firstOrNull() ?: "OK"
}

private fun SqlQuery.Select.outputNames(): List<String> = select.mapNotNull { item ->
    val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
    expression.metadata?.outputName
        ?: expression.alias
        ?: (expression.expr as? SqlExpr.Column)?.columnName
}

private fun SqlQuery.Select.selectedColumns(): List<String> = select.mapNotNull { item ->
    val column = (item as? SqlSelectItem.Expr)?.expr as? SqlExpr.Column ?: return@mapNotNull null
    listOfNotNull(column.tableName, column.columnName).joinToString(".")
}

private inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
