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

// Verifies that a selectable aliased inside a select list is collected as a scalar subquery select item.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToSelect

@Table(name = "tb_scalar_select_user")
data class ScalarSelectUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table(name = "tb_scalar_select_order")
data class ScalarSelectOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

data class SelectCapture(
    val fields: List<Field>,
    val selectItems: List<SqlSelectItem>,
)

fun ScalarSelectUser.collectSelectItems(block: ToSelect<ScalarSelectUser, Any?>): SelectCapture {
    var capture = SelectCapture(emptyList(), emptyList())
    afterSelect {
        block!!(it)
        capture = SelectCapture(fields.toList(), selectItems.toList())
    }
    return capture
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val capture = ScalarSelectUser().collectSelectItems {
        [
            it.id,
            ScalarSelectOrder()
                .select { order -> order.amount }
                .limit(1)
                .alias("lastAmount"),
            it.name
        ]
    }
    val scalarItem = capture.selectItems.singleOrNull() as? SqlSelectItem.Expr

    val failures = listOfNotNull(
        expect(capture.fields.map { it.name } == listOf("id", "name")) {
            "fields were ${capture.fields.map { it.name }}"
        },
        expect(scalarItem != null) { "scalar item was ${capture.selectItems.singleOrNull()?.let { it::class.qualifiedName }}" },
        expect(scalarItem?.alias == "lastAmount") { "alias was ${scalarItem?.alias}" },
        expect(scalarItem?.expr is SqlExpr.Subquery) {
            "expression was ${scalarItem?.expr?.let { it::class.qualifiedName }}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
