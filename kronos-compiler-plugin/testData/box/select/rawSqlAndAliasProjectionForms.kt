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

// Verifies raw SQL and raw SQL alias projection forms are lowered into syntax select items.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_raw_sql_forms")
data class RawSqlSelectUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

data class RawSqlSelectCapture(
    val fields: List<Field>,
    val items: List<SqlSelectItem>
)

fun RawSqlSelectUser.collectRawSqlItems(block: ToSelect<RawSqlSelectUser, Any?>): RawSqlSelectCapture {
    val fields = mutableListOf<Field>()
    val items = mutableListOf<SqlSelectItem>()
    afterSelect {
        block!!(it)
        fields += this.fields
        items += selectItems
    }
    return RawSqlSelectCapture(fields, items)
}

fun rawSqlAt(items: List<SqlSelectItem>, index: Int): SqlSelectItem.Expr? =
    items.getOrNull(index) as? SqlSelectItem.Expr

fun expectRawSqlSelect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val listCapture = RawSqlSelectUser().collectRawSqlItems {
        listOf(
            "count(*)".alias("total"),
            "now()",
            it.id
        )
    }
    val arrayCapture = RawSqlSelectUser().collectRawSqlItems {
        arrayOf("1".alias("one"), it.name)
    }
    val total = rawSqlAt(listCapture.items, 0)
    val now = rawSqlAt(listCapture.items, 1)
    val one = rawSqlAt(arrayCapture.items, 0)

    val failures = listOfNotNull(
        expectRawSqlSelect(listCapture.fields.singleOrNull()?.columnName == "id") {
            "list fields were ${listCapture.fields.map { it.columnName }}"
        },
        expectRawSqlSelect(arrayCapture.fields.singleOrNull()?.columnName == "name") {
            "array fields were ${arrayCapture.fields.map { it.columnName }}"
        },
        expectRawSqlSelect(listCapture.items.size == 2) { "list item size was ${listCapture.items.size}" },
        expectRawSqlSelect(arrayCapture.items.size == 1) { "array item size was ${arrayCapture.items.size}" },
        expectRawSqlSelect((total?.expr as? SqlExpr.UnsafeRaw)?.sql == "count(*)") { "total expr was ${total?.expr}" },
        expectRawSqlSelect(total?.alias == "total") { "total alias was ${total?.alias}" },
        expectRawSqlSelect((now?.expr as? SqlExpr.UnsafeRaw)?.sql == "now()") { "now expr was ${now?.expr}" },
        expectRawSqlSelect(now?.alias == null) { "now alias was ${now?.alias}" },
        expectRawSqlSelect((one?.expr as? SqlExpr.NumberLiteral)?.number == "1") { "one expr was ${one?.expr}" },
        expectRawSqlSelect(one?.alias == "one") { "one alias was ${one?.alias}" },
    )

    return failures.firstOrNull() ?: "OK"
}
