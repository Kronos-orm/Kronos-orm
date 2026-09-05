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

// Verifies that @KronosFunction changes the SQL function identifier lowered by the compiler plugin.

import com.kotlinorm.annotations.KronosFunction
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.functions.FunctionHandler
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_kronos_function")
data class KronosFunctionAnnotationUser(
    var id: Int? = null,
    var payload: String? = null,
) : KPojo

@KronosFunction("json_extract")
fun FunctionHandler.jsonExtract(payload: String?, path: String?): String? = null

fun KronosFunctionAnnotationUser.collectSelectItems(
    block: ToSelect<KronosFunctionAnnotationUser, Any?>
): List<SqlSelectItem> {
    val result = mutableListOf<SqlSelectItem>()
    afterSelect {
        block!!(it)
        result += selectItems
    }
    return result
}

fun box(): String {
    val items = KronosFunctionAnnotationUser()
        .collectSelectItems { [f.jsonExtract(it.payload, "$.name").alias("nameValue")] }
    val item = items.singleOrNull() as? SqlSelectItem.Expr
    val function = item?.expr as? SqlExpr.Function

    return when {
        item == null -> "Fail: item was ${items.singleOrNull()}"
        item.alias != "nameValue" -> "Fail: alias was ${item.alias}"
        function == null -> "Fail: expression was ${item.expr}"
        function.name.last != "json_extract" -> "Fail: function name was ${function.name.last}"
        function.args.size != 2 -> "Fail: args size was ${function.args.size}"
        else -> "OK"
    }
}
