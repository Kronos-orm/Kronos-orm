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

// Verifies contains-style string conditions and array membership lower to SqlExpr nodes.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter
import com.kotlinorm.utils.TransformerSafeValue
import kotlin.reflect.typeOf

@Table(name = "tb_contains_membership")
data class ContainsMembershipUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

data class CapturedContainsMembership(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun containsMembershipWhere(
    user: ContainsMembershipUser,
    block: ToFilter<ContainsMembershipUser, Boolean?>,
): CapturedContainsMembership {
    var result: CapturedContainsMembership? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedContainsMembership(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedContainsMembership(null, emptyMap())
}

fun containsParameterValue(actual: CapturedContainsMembership, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun arrayParameterList(actual: CapturedContainsMembership): List<*>? =
    (actual.parameters["idList"] as? Array<*>)?.toList()

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = ContainsMembershipUser(id = 2, name = "Ada")
    val contains = containsMembershipWhere(user) { it.name.contains("d%_\\") }
    val containsExpr = contains.expr as? SqlExpr.Like
    val containsColumn = containsExpr?.expr as? SqlExpr.Column
    val negatedContains = containsMembershipWhere(user) { !it.name.contains("d%_\\") }
    val negatedContainsExpr = negatedContains.expr as? SqlExpr.Like
    val negatedContainsColumn = negatedContainsExpr?.expr as? SqlExpr.Column
    val ids = arrayOf<Int?>(1, 2, 3)
    val capturedMembership = containsMembershipWhere(user) { it.id in ids }
    val membership = capturedMembership.expr as? SqlExpr.In
    val membershipColumn = membership?.expr as? SqlExpr.Column
    val membershipItems = (membership?.`in` as? SqlInRightOperand.Values)?.items.orEmpty()
    val capturedNegatedMembership = containsMembershipWhere(user) { it.id !in ids }
    val negatedMembership = capturedNegatedMembership.expr as? SqlExpr.In
    val negatedMembershipColumn = negatedMembership?.expr as? SqlExpr.Column
    val negatedMembershipItems = (negatedMembership?.`in` as? SqlInRightOperand.Values)?.items.orEmpty()

    return when {
        containsColumn?.columnName != "name" -> "Fail: contains field was ${containsColumn?.columnName}"
        containsExpr?.withNot == true -> "Fail: contains should not be negated"
        containsExpr?.escape != SqlExpr.StringLiteral("\\") -> "Fail: contains escape was ${containsExpr?.escape}"
        containsParameterValue(contains, containsExpr?.pattern) != TransformerSafeValue("%d\\%\\_\\\\%", typeOf<String>()) ->
            "Fail: contains value was ${containsParameterValue(contains, containsExpr?.pattern)}"
        negatedContainsColumn?.columnName != "name" -> "Fail: negated contains field was ${negatedContainsColumn?.columnName}"
        negatedContainsExpr?.withNot != true -> "Fail: negated contains should be negated"
        negatedContainsExpr?.escape != SqlExpr.StringLiteral("\\") ->
            "Fail: negated contains escape was ${negatedContainsExpr?.escape}"
        containsParameterValue(negatedContains, negatedContainsExpr?.pattern) != TransformerSafeValue("%d\\%\\_\\\\%", typeOf<String>()) ->
            "Fail: negated contains value was ${containsParameterValue(negatedContains, negatedContainsExpr?.pattern)}"
        membershipColumn?.columnName != "id" -> "Fail: membership field was ${membershipColumn?.columnName}"
        membership?.withNot == true -> "Fail: membership should not be negated"
        membershipItems != listOf(SqlExpr.Parameter(SqlParameter.Named("idList"), expandAsList = true)) ->
            "Fail: membership values were $membershipItems"
        arrayParameterList(capturedMembership) != listOf(1, 2, 3) ->
            "Fail: membership parameters were ${capturedMembership.parameters}"
        negatedMembershipColumn?.columnName != "id" -> "Fail: negated membership field was ${negatedMembershipColumn?.columnName}"
        negatedMembership?.withNot != true -> "Fail: negated membership should be negated"
        negatedMembershipItems != listOf(SqlExpr.Parameter(SqlParameter.Named("idList"), expandAsList = true)) ->
            "Fail: negated membership values were $negatedMembershipItems"
        arrayParameterList(capturedNegatedMembership) != listOf(1, 2, 3) ->
            "Fail: negated membership parameters were ${capturedNegatedMembership.parameters}"
        else -> "OK"
    }
}
