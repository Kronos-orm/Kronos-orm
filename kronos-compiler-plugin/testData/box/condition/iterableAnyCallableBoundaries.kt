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

// Verifies PostgreSQL FunctionHandler calls and no-lambda Iterable overloads stay outside Iterable predicate lowering.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.functions.bundled.exts.PostgresFunctions.all
import com.kotlinorm.functions.bundled.exts.PostgresFunctions.any
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.types.ToFilter

@Table(name = "tb_iterable_any_callable_boundary")
data class IterableAnyCallableBoundaryUser(
    var id: Int? = null,
) : KPojo

fun iterableAnyCallableBoundaryWhere(
    user: IterableAnyCallableBoundaryUser,
    block: ToFilter<IterableAnyCallableBoundaryUser, Boolean?>,
): SqlExpr? {
    var result: SqlExpr? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = sqlExpr
    }
    return result
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = IterableAnyCallableBoundaryUser(id = 1)
    val anyActual = iterableAnyCallableBoundaryWhere(user) {
        it.id == f.any(listOf(1, 2))
    }
    val allActual = iterableAnyCallableBoundaryWhere(user) {
        it.id == f.all(listOf(1, 2))
    }
    val noLambdaAny = iterableAnyCallableBoundaryWhere(user) { emptyList<Int>().any() }
    val noLambdaNone = iterableAnyCallableBoundaryWhere(user) { emptyList<Int>().none() }

    val anyOperands = callableBoundaryOperands(anyActual)
    val allOperands = callableBoundaryOperands(allActual)

    val failures = listOfNotNull(
        expectCallableBoundary(anyOperands.operator == SqlBinaryOperator.Equal) {
            "ANY operator was ${anyOperands.operator}"
        },
        expectCallableBoundary(anyOperands.column?.columnName == "id") {
            "ANY field was ${anyOperands.column}"
        },
        expectCallableBoundary(anyOperands.function?.name?.last == "ANY") {
            "ANY function was ${anyOperands.function}"
        },
        expectCallableBoundary(allOperands.operator == SqlBinaryOperator.Equal) {
            "ALL operator was ${allOperands.operator}"
        },
        expectCallableBoundary(allOperands.column?.columnName == "id") {
            "ALL field was ${allOperands.column}"
        },
        expectCallableBoundary(allOperands.function?.name?.last == "ALL") {
            "ALL function was ${allOperands.function}"
        },
        expectCallableBoundary(noLambdaAny == null) {
            "Iterable.any() without a lambda produced $noLambdaAny"
        },
        expectCallableBoundary(noLambdaNone == null) {
            "Iterable.none() without a lambda produced $noLambdaNone"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

data class CallableBoundaryOperands(
    val operator: SqlBinaryOperator?,
    val column: SqlExpr.Column?,
    val function: SqlExpr.Function?,
)

fun callableBoundaryOperands(expr: SqlExpr?): CallableBoundaryOperands {
    val binary = expr as? SqlExpr.Binary
    val operands = listOfNotNull(binary?.left, binary?.right)
    return CallableBoundaryOperands(
        binary?.operator,
        operands.filterIsInstance<SqlExpr.Column>().singleOrNull(),
        operands.filterIsInstance<SqlExpr.Function>().singleOrNull(),
    )
}

fun expectCallableBoundary(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
