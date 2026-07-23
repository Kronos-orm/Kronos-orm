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

// Verifies empty and all-no-value Iterable predicates retain BooleanLiteral(false), including update and delete WHERE clauses.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.update.update
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.types.ToFilter

@Table(name = "tb_iterable_predicate_empty_safety")
data class IterablePredicateEmptySafetyUser(
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
) : KPojo

fun iterablePredicateEmptySafetyWhere(
    user: IterablePredicateEmptySafetyUser,
    block: ToFilter<IterablePredicateEmptySafetyUser, Boolean?>,
): SqlExpr? {
    var result: SqlExpr? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = sqlExpr
    }
    return result
}

fun iterablePredicateIsFalse(expr: SqlExpr?): Boolean =
    (expr as? SqlExpr.BooleanLiteral)?.boolean == false

fun expectIterablePredicateEmptySafety(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = IterablePredicateEmptySafetyUser(id = 1, name = "Ada", status = 1)
    val empty: List<String> = emptyList()
    val noValues: List<String?> = listOf(null, null)
    val falseCases = listOf(
        "empty any" to iterablePredicateEmptySafetyWhere(user) { empty.any { value -> it.name.contains(value) } },
        "empty all" to iterablePredicateEmptySafetyWhere(user) { empty.all { value -> it.name.contains(value) } },
        "empty none" to iterablePredicateEmptySafetyWhere(user) { empty.none { value -> it.name.contains(value) } },
        "negated empty any" to iterablePredicateEmptySafetyWhere(user) { !(empty.any { value -> it.name.contains(value) }) },
        "negated empty all" to iterablePredicateEmptySafetyWhere(user) { !(empty.all { value -> it.name.contains(value) }) },
        "negated empty none" to iterablePredicateEmptySafetyWhere(user) { !(empty.none { value -> it.name.contains(value) }) },
        "no-value any" to iterablePredicateEmptySafetyWhere(user) { noValues.any { value -> it.name like value } },
        "no-value all" to iterablePredicateEmptySafetyWhere(user) { noValues.all { value -> it.name like value } },
        "no-value none" to iterablePredicateEmptySafetyWhere(user) { noValues.none { value -> it.name like value } },
        "negated no-value any" to iterablePredicateEmptySafetyWhere(user) { !(noValues.any { value -> it.name like value }) },
        "negated no-value all" to iterablePredicateEmptySafetyWhere(user) { !(noValues.all { value -> it.name like value }) },
        "negated no-value none" to iterablePredicateEmptySafetyWhere(user) { !(noValues.none { value -> it.name like value }) },
    )

    val (_, _, updateTasks) = user.update()
        .set { it.status = 2 }
        .where { empty.all { value -> it.name.contains(value) } }
        .build(CompilerTestDataSourceWrapper)
    val updateWhere = (updateTasks.single().statement as? SqlDmlStatement.Update)?.where
    val (_, _, deleteTasks) = user.delete()
        .logic(false)
        .where { empty.none { value -> it.name.contains(value) } }
        .build(CompilerTestDataSourceWrapper)
    val deleteWhere = (deleteTasks.single().statement as? SqlDmlStatement.Delete)?.where

    val invalidCase = falseCases.firstOrNull { (_, expr) -> !iterablePredicateIsFalse(expr) }
    val failures = listOfNotNull(
        expectIterablePredicateEmptySafety(invalidCase == null) {
            "${invalidCase?.first} produced ${invalidCase?.second}"
        },
        expectIterablePredicateEmptySafety(iterablePredicateIsFalse(updateWhere)) {
            "update WHERE was $updateWhere"
        },
        expectIterablePredicateEmptySafety(iterablePredicateIsFalse(deleteWhere)) {
            "delete WHERE was $deleteWhere"
        },
    )

    return failures.firstOrNull() ?: "OK"
}
