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

// Verifies advanced condition helpers are lowered into syntax SqlExpr nodes.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter
import com.kotlinorm.utils.TransformerSafeValue
import kotlin.reflect.typeOf

@Table(name = "tb_condition_advanced")
data class AdvancedConditionUser(
    var id: Int? = null,
    @Column("user_name")
    var name: String? = null,
    var age: Int? = null,
    var status: String? = null,
) : KPojo

data class CapturedAdvancedCondition(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun advancedWhere(user: AdvancedConditionUser, block: ToFilter<AdvancedConditionUser, Boolean?>): CapturedAdvancedCondition {
    var result: CapturedAdvancedCondition? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedAdvancedCondition(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedAdvancedCondition(null, emptyMap())
}

fun advancedParameterValue(actual: CapturedAdvancedCondition, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = AdvancedConditionUser(name = "Ada", age = 36, status = "^A.*")

    val between = advancedWhere(user) { it.age between 18..40 }.expr as? SqlExpr.Between
    val betweenColumn = between?.expr as? SqlExpr.Column
    if (betweenColumn?.columnName != "age") return "Fail: between field was ${betweenColumn?.columnName}"
    if ((between?.start as? SqlExpr.NumberLiteral)?.number != "18") return "Fail: between start was ${between?.start}"
    if ((between?.end as? SqlExpr.NumberLiteral)?.number != "40") return "Fail: between end was ${between?.end}"

    val regexp = advancedWhere(user) { it.status.regexp }
    val regexpExpr = regexp.expr as? SqlExpr.Binary
    val regexpColumn = regexpExpr?.left as? SqlExpr.Column
    if (regexpColumn?.columnName != "status") return "Fail: regexp field was ${regexpColumn?.columnName}"
    if (regexpExpr?.operator != SqlBinaryOperator.Regexp) return "Fail: regexp operator was ${regexpExpr?.operator}"
    if (advancedParameterValue(regexp, regexpExpr?.right) != "^A.*") {
        return "Fail: regexp value was ${advancedParameterValue(regexp, regexpExpr?.right)}"
    }

    val omitted = advancedWhere(user.copy(name = null)) { it.name.eq.takeIf(false) }
    if (omitted.expr != null) return "Fail: omitted condition was ${omitted.expr}"

    val customColumn = advancedWhere(user) { it.name.startsWith("A%_\\") }
    val startsWith = customColumn.expr as? SqlExpr.Like
    val startsWithColumn = startsWith?.expr as? SqlExpr.Column
    if (startsWithColumn?.columnName != "user_name") return "Fail: columnName was ${startsWithColumn?.columnName}"
    if (startsWith?.escape != SqlExpr.StringLiteral("\\")) return "Fail: startsWith escape was ${startsWith?.escape}"
    if (advancedParameterValue(customColumn, startsWith?.pattern) != TransformerSafeValue("A\\%\\_\\\\%", typeOf<String>())) {
        return "Fail: startsWith value was ${advancedParameterValue(customColumn, startsWith?.pattern)}"
    }

    return "OK"
}
