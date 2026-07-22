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

// Verifies PostgreSQL FunctionHandler.any remains a SQL function rather than an Iterable.any predicate.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
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

    val actual = iterableAnyCallableBoundaryWhere(IterableAnyCallableBoundaryUser(id = 1)) {
        it.id == f.any(listOf(1, 2))
    } as? SqlExpr.Binary
    val operands = listOfNotNull(actual?.left, actual?.right)
    val column = operands.filterIsInstance<SqlExpr.Column>().singleOrNull()
    val function = operands.filterIsInstance<SqlExpr.Function>().singleOrNull()

    return when {
        actual?.operator != SqlBinaryOperator.Equal -> "Fail: operator was ${actual?.operator}"
        column?.columnName != "id" -> "Fail: field was $operands"
        function?.name?.last != "ANY" -> "Fail: function was $operands"
        else -> "OK"
    }
}
