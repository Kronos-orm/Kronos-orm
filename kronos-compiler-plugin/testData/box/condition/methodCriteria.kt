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

// Verifies method-style condition helpers are lowered into syntax SqlExpr nodes.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter
import com.kotlinorm.utils.TransformerSafeValue
import kotlin.reflect.typeOf

@Table(name = "tb_method_criteria")
data class MethodCriteriaUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
    var deletedAt: String? = null,
) : KPojo

data class CapturedMethodCondition(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun methodWhere(user: MethodCriteriaUser, block: ToFilter<MethodCriteriaUser, Boolean?>): CapturedMethodCondition {
    var result: CapturedMethodCondition? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedMethodCondition(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedMethodCondition(null, emptyMap())
}

fun methodParameterValue(actual: CapturedMethodCondition, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun expectMethod(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = MethodCriteriaUser(id = 1, name = "Ada", age = 36)
    val like = methodWhere(user) { it.name.like("A%") }
    val likeExpr = like.expr as? SqlExpr.Like
    val likeColumn = likeExpr?.expr as? SqlExpr.Column
    val notBetween = methodWhere(user) { it.age notBetween 1..17 }.expr as? SqlExpr.Between
    val notBetweenColumn = notBetween?.expr as? SqlExpr.Column
    val negatedNotBetween = methodWhere(user) { !(it.age notBetween 1..17) }.expr as? SqlExpr.Between
    val negatedNotLike = methodWhere(user) { !(it.name notLike "A%") }.expr as? SqlExpr.Like
    val negatedNotRegexp = methodWhere(user) { !(it.name notRegexp "^A.*") }.expr as? SqlExpr.Binary
    val isNull = methodWhere(user) { it.deletedAt.isNull }.expr as? SqlExpr.Binary
    val isNullColumn = isNull?.left as? SqlExpr.Column
    val notNull = methodWhere(user) { it.name.notNull }.expr as? SqlExpr.Binary
    val notNullColumn = notNull?.left as? SqlExpr.Column
    val sql = methodWhere(user) { "age > 18".asSql() }.expr as? SqlExpr.UnsafeRaw

    val failures = listOfNotNull(
        expectMethod(likeColumn?.columnName == "name") { "like field was ${likeColumn?.columnName}" },
        expectMethod(methodParameterValue(like, likeExpr?.pattern) == TransformerSafeValue("A%", typeOf<String>())) {
            "like value was ${methodParameterValue(like, likeExpr?.pattern)}"
        },
        expectMethod(notBetweenColumn?.columnName == "age") { "notBetween field was ${notBetweenColumn?.columnName}" },
        expectMethod(notBetween?.withNot == true) { "notBetween not was ${notBetween?.withNot}" },
        expectMethod((notBetween?.start as? SqlExpr.NumberLiteral)?.number == "1") { "notBetween start was ${notBetween?.start}" },
        expectMethod((notBetween?.end as? SqlExpr.NumberLiteral)?.number == "17") { "notBetween end was ${notBetween?.end}" },
        expectMethod(negatedNotBetween?.withNot == false) { "negatedNotBetween not was ${negatedNotBetween?.withNot}" },
        expectMethod(negatedNotLike?.withNot == false) { "negatedNotLike not was ${negatedNotLike?.withNot}" },
        expectMethod(negatedNotRegexp?.operator == SqlBinaryOperator.Regexp) {
            "negatedNotRegexp operator was ${negatedNotRegexp?.operator}"
        },
        expectMethod(isNullColumn?.columnName == "deleted_at") { "isNull field was ${isNullColumn?.columnName}" },
        expectMethod(isNull?.operator == SqlBinaryOperator.Is(false)) { "isNull operator was ${isNull?.operator}" },
        expectMethod(isNull?.right == SqlExpr.NullLiteral) { "isNull right was ${isNull?.right}" },
        expectMethod(notNullColumn?.columnName == "name") { "notNull field was ${notNullColumn?.columnName}" },
        expectMethod(notNull?.operator == SqlBinaryOperator.Is(true)) { "notNull operator was ${notNull?.operator}" },
        expectMethod(notNull?.right == SqlExpr.NullLiteral) { "notNull right was ${notNull?.right}" },
        expectMethod(sql?.sql == "age > 18") { "asSql value was ${sql?.sql}" },
    )

    return failures.firstOrNull() ?: "OK"
}
