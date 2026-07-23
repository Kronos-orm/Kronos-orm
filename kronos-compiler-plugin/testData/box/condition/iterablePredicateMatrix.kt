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

// Verifies Iterable.any/all/none predicates preserve SQL tree operators and leaf negation under outer !.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.types.ToFilter

@Table(name = "tb_iterable_predicate_matrix")
data class IterablePredicateMatrixUser(
    var id: Int? = null,
    var name: String? = null,
    var active: Boolean? = null,
    var status: Int? = null,
) : KPojo

fun iterablePredicateMatrixWhere(
    user: IterablePredicateMatrixUser,
    block: ToFilter<IterablePredicateMatrixUser, Boolean?>,
): SqlExpr? {
    var result: SqlExpr? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = sqlExpr
    }
    return result
}

data class IterablePredicateShape(
    val operator: SqlBinaryOperator?,
    val leafNegations: List<Boolean?>,
)

fun iterablePredicateShape(expr: SqlExpr?): IterablePredicateShape {
    val root = expr as? SqlExpr.Binary
    return IterablePredicateShape(
        root?.operator,
        listOf(root?.left as? SqlExpr.Like, root?.right as? SqlExpr.Like).map { it?.withNot },
    )
}

fun expectIterablePredicateMatrix(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = IterablePredicateMatrixUser(id = 1, name = "Ada", active = true, status = 1)
    val values = listOf("Ada", "Grace")
    var receiverEvaluations = 0
    fun nextValues(): List<String> {
        receiverEvaluations += 1
        return values
    }

    val any = iterablePredicateMatrixWhere(user) { values.any { value -> it.name.contains(value) } }
    val negatedAny = iterablePredicateMatrixWhere(user) { !(values.any { value -> it.name.contains(value) }) }
    val all = iterablePredicateMatrixWhere(user) { nextValues().all { value -> it.name.contains(value) } }
    val negatedAll = iterablePredicateMatrixWhere(user) { !(values.all { value -> it.name.contains(value) }) }
    val none = iterablePredicateMatrixWhere(user) { values.none { value -> it.name.contains(value) } }
    val negatedNone = iterablePredicateMatrixWhere(user) { !(values.none { value -> it.name.contains(value) }) }
    val combined = iterablePredicateMatrixWhere(user) {
        it.active == true && values.none { value -> it.name.contains(value) }
    } as? SqlExpr.Binary

    val combinedLeft = combined?.left as? SqlExpr.Binary
    val combinedRight = combined?.right
    val failures = listOfNotNull(
        expectIterablePredicateMatrix(
            iterablePredicateShape(any) == IterablePredicateShape(SqlBinaryOperator.Or, listOf(false, false))
        ) { "any shape was ${iterablePredicateShape(any)}" },
        expectIterablePredicateMatrix(
            iterablePredicateShape(negatedAny) == IterablePredicateShape(SqlBinaryOperator.And, listOf(true, true))
        ) { "negated any shape was ${iterablePredicateShape(negatedAny)}" },
        expectIterablePredicateMatrix(
            iterablePredicateShape(all) == IterablePredicateShape(SqlBinaryOperator.And, listOf(false, false))
        ) { "all shape was ${iterablePredicateShape(all)}" },
        expectIterablePredicateMatrix(
            iterablePredicateShape(negatedAll) == IterablePredicateShape(SqlBinaryOperator.Or, listOf(true, true))
        ) { "negated all shape was ${iterablePredicateShape(negatedAll)}" },
        expectIterablePredicateMatrix(
            iterablePredicateShape(none) == IterablePredicateShape(SqlBinaryOperator.And, listOf(true, true))
        ) { "none shape was ${iterablePredicateShape(none)}" },
        expectIterablePredicateMatrix(
            iterablePredicateShape(negatedNone) == IterablePredicateShape(SqlBinaryOperator.Or, listOf(false, false))
        ) { "negated none shape was ${iterablePredicateShape(negatedNone)}" },
        expectIterablePredicateMatrix(receiverEvaluations == 1) {
            "Iterable receiver was evaluated $receiverEvaluations times"
        },
        expectIterablePredicateMatrix(combined?.operator == SqlBinaryOperator.And) {
            "combined root was ${combined?.operator}"
        },
        expectIterablePredicateMatrix(combinedLeft?.operator == SqlBinaryOperator.Equal) {
            "combined left operator was ${combinedLeft?.operator}"
        },
        expectIterablePredicateMatrix(
            iterablePredicateShape(combinedRight) == IterablePredicateShape(SqlBinaryOperator.And, listOf(true, true))
        ) { "combined none shape was ${iterablePredicateShape(combinedRight)}" },
    )

    return failures.firstOrNull() ?: "OK"
}
