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

// Verifies that legal binary operator select expressions become syntax binary projections.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_operator")
data class OperatorSelectUser(
    var id: Int? = null,
    var score: Int = 0,
) : KPojo

fun OperatorSelectUser.collectOperatorSelect(block: ToSelect<OperatorSelectUser, Any?>): List<SqlSelectItem> {
    val result = mutableListOf<SqlSelectItem>()
    afterSelect {
        block!!(it)
        result += selectItems
    }
    return result
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val items = OperatorSelectUser().collectOperatorSelect { [it.score + 10 + 20, it.score % 2] }
    val add = (items.getOrNull(0) as? SqlSelectItem.Expr)?.expr as? SqlExpr.Binary
    val nestedAdd = add?.left as? SqlExpr.Binary
    val mod = (items.getOrNull(1) as? SqlSelectItem.Expr)?.expr as? SqlExpr.Binary

    val failures = listOfNotNull(
        expect(items.size == 2) { "size was ${items.size}" },
        expect(add?.operator == com.kotlinorm.syntax.expr.SqlBinaryOperator.Plus) { "first operator was ${add?.operator}" },
        expect(nestedAdd?.operator == com.kotlinorm.syntax.expr.SqlBinaryOperator.Plus) { "nested operator was ${nestedAdd?.operator}" },
        expect((nestedAdd?.left as? SqlExpr.Column)?.columnName == "score") {
            "nested first field was ${nestedAdd?.left}"
        },
        expect(mod?.operator == com.kotlinorm.syntax.expr.SqlBinaryOperator.Mod) { "second operator was ${mod?.operator}" },
        expect((mod?.left as? SqlExpr.Column)?.columnName == "score") {
            "mod first field was ${mod?.left}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
