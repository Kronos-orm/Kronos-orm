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

// Verifies portable avgDouble lowers into AVG with its declared alias.
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.avgDouble
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBuiltinFunction
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToSelect

data class AverageReading(var score: Double? = null) : KPojo

fun AverageReading.averageItems(block: ToSelect<AverageReading, Any?>): List<SqlSelectItem> {
    val result = mutableListOf<SqlSelectItem>()
    afterSelect { block!!(it); result += selectItems }
    return result
}

fun box(): String {
    val item = AverageReading().averageItems { [f.avgDouble(it.score).alias("averageScore")] }.single()
    val function = (item as? SqlSelectItem.Expr)?.expr as? SqlExpr.Function
    return when {
        function?.name?.last != "AVG" -> "Fail: AVG name was ${function?.name}"
        function.builtinFunction != SqlBuiltinFunction.Average -> "Fail: builtin was ${function.builtinFunction}"
        (item as SqlSelectItem.Expr).alias != "averageScore" -> "Fail: alias was ${item.alias}"
        else -> "OK"
    }
}
