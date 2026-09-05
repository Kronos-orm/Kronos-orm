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

// Verifies mutableListOf and setOf select projections preserve mixed field and expression items.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_mutable_set_variants")
data class MutableSetProjectionUser(
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
) : KPojo

data class MutableSetProjectionCapture(
    val fields: List<Field>,
    val items: List<SqlSelectItem>,
)

fun MutableSetProjectionUser.collectMutableSetProjection(
    block: ToSelect<MutableSetProjectionUser, Any?>
): MutableSetProjectionCapture {
    var result: MutableSetProjectionCapture? = null
    afterSelect {
        block!!(it)
        result = MutableSetProjectionCapture(fields.toList(), selectItems.toList())
    }
    return result ?: error("select block did not run")
}

fun mutableSetExprAt(items: List<SqlSelectItem>, index: Int): SqlSelectItem.Expr? =
    items.getOrNull(index) as? SqlSelectItem.Expr

fun expectMutableSetProjection(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val source = MutableSetProjectionUser()
    val mutableList = source.collectMutableSetProjection {
        mutableListOf<Any?>(it.id.alias("uid"), f.length(it.name).alias("nameLength"), "current_date")
    }
    val set = source.collectMutableSetProjection {
        setOf<Any?>(it.status, "count(*)".alias("total"))
    }

    val mutableFunction = mutableSetExprAt(mutableList.items, 0)
    val mutableRaw = mutableSetExprAt(mutableList.items, 1)
    val setRaw = mutableSetExprAt(set.items, 0)

    val failures = listOfNotNull(
        expectMutableSetProjection(mutableList.fields.map { it.name } == listOf("uid")) {
            "mutable fields were ${mutableList.fields.map { it.name }}"
        },
        expectMutableSetProjection((mutableFunction?.expr as? SqlExpr.Function)?.name?.last == "LENGTH") {
            "mutable function was ${mutableFunction?.expr}"
        },
        expectMutableSetProjection(mutableFunction?.alias == "nameLength") {
            "mutable function alias was ${mutableFunction?.alias}"
        },
        expectMutableSetProjection((mutableRaw?.expr as? SqlExpr.UnsafeRaw)?.sql == "current_date") {
            "mutable raw expr was ${mutableRaw?.expr}"
        },
        expectMutableSetProjection(set.fields.map { it.name } == listOf("status")) {
            "set fields were ${set.fields.map { it.name }}"
        },
        expectMutableSetProjection((setRaw?.expr as? SqlExpr.UnsafeRaw)?.sql == "count(*)") {
            "set raw expr was ${setRaw?.expr}"
        },
        expectMutableSetProjection(setRaw?.alias == "total") { "set raw alias was ${setRaw?.alias}" },
    )

    return failures.firstOrNull() ?: "OK"
}
