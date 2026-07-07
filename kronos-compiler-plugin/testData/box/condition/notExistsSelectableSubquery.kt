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

// Verifies `!exists(query)` lowers to syntax NOT EXISTS predicate.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.types.ToFilter

@Table(name = "tb_not_exists_user")
data class NotExistsUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table(name = "tb_not_exists_order")
data class NotExistsOrder(
    var id: Int? = null,
    var userId: Int? = null,
) : KPojo

fun notExistsWhere(user: NotExistsUser, block: ToFilter<NotExistsUser, Boolean?>): SqlExpr? {
    var result: SqlExpr? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = sqlExpr
    }
    return result
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val condition = notExistsWhere(NotExistsUser(id = 7)) {
        !exists(NotExistsOrder().select { order -> order.userId })
    } as? SqlExpr.ExistsPredicate

    val failures = listOfNotNull(
        expect(condition?.withNot == true) { "exists not flag was ${condition?.withNot}" },
        expect(condition?.query is SqlQuery.Select) { "query was ${condition?.query?.let { it::class.qualifiedName }}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
