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

// Verifies Iterable.any predicates preserve boolean grouping in outer and inner expressions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.render.toSql
import com.kotlinorm.types.ToFilter

@Table(name = "tb_iterable_any_logical")
data class IterableAnyLogicalUser(
    var id: Int? = null,
    var name: String? = null,
    var email: String? = null,
    var status: Int? = null,
    var active: Boolean? = null,
) : KPojo

fun iterableAnyLogicalWhere(
    user: IterableAnyLogicalUser,
    block: ToFilter<IterableAnyLogicalUser, Boolean?>,
): SqlExpr? {
    var result: SqlExpr? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = sqlExpr
    }
    return result
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = IterableAnyLogicalUser(name = "Ada", email = "ada@example.com", status = 1, active = true)
    val keywords = listOf("Ada", "Grace")

    val outerOr = iterableAnyLogicalWhere(user) {
        keywords.any { keyword -> it.name.contains(keyword) } || it.status == 1
    } as? SqlExpr.Binary
    val outerAnd = iterableAnyLogicalWhere(user) {
        it.active == true && keywords.any { keyword -> it.name.contains(keyword) }
    } as? SqlExpr.Binary
    val innerOr = iterableAnyLogicalWhere(user) {
        keywords.any { keyword -> it.name.contains(keyword) || it.email.contains(keyword) }
    } as? SqlExpr.Binary
    val negated = iterableAnyLogicalWhere(user) {
        !keywords.any { keyword -> it.name.contains(keyword) }
    } as? SqlExpr.Binary

    val outerOrAny = outerOr?.left as? SqlExpr.Binary
    val outerOrStatus = outerOr?.right as? SqlExpr.Binary
    val outerAndActive = outerAnd?.left as? SqlExpr.Binary
    val outerAndAny = outerAnd?.right as? SqlExpr.Binary
    val innerOrFirst = innerOr?.left as? SqlExpr.Binary
    val innerOrSecond = innerOr?.right as? SqlExpr.Binary
    val negatedFirst = negated?.left as? SqlExpr.Like
    val negatedSecond = negated?.right as? SqlExpr.Like
    val renderedOuterAnd = outerAnd?.toSql()

    return when {
        outerOr?.operator != SqlBinaryOperator.Or -> "Fail: outer OR was ${outerOr?.operator}"
        outerOrAny?.operator != SqlBinaryOperator.Or -> "Fail: outer OR any was ${outerOrAny?.operator}"
        outerOrStatus?.operator != SqlBinaryOperator.Equal -> "Fail: outer OR status was ${outerOrStatus?.operator}"
        outerAnd?.operator != SqlBinaryOperator.And -> "Fail: outer AND was ${outerAnd?.operator}"
        outerAndActive?.operator != SqlBinaryOperator.Equal -> "Fail: outer AND active was ${outerAndActive?.operator}"
        outerAndAny?.operator != SqlBinaryOperator.Or -> "Fail: outer AND any was ${outerAndAny?.operator}"
        innerOr?.operator != SqlBinaryOperator.Or -> "Fail: inner root was ${innerOr?.operator}"
        innerOrFirst?.operator != SqlBinaryOperator.Or -> "Fail: first inner predicate was ${innerOrFirst?.operator}"
        innerOrSecond?.operator != SqlBinaryOperator.Or -> "Fail: second inner predicate was ${innerOrSecond?.operator}"
        negated?.operator != SqlBinaryOperator.And -> "Fail: negated root was ${negated?.operator}"
        negatedFirst?.withNot != true -> "Fail: first negated predicate was ${negatedFirst?.withNot}"
        negatedSecond?.withNot != true -> "Fail: second negated predicate was ${negatedSecond?.withNot}"
        renderedOuterAnd != "\"tb_iterable_any_logical\".\"active\" = :active AND (\"tb_iterable_any_logical\".\"name\" LIKE :name ESCAPE '\\' OR \"tb_iterable_any_logical\".\"name\" LIKE :name@1 ESCAPE '\\')" ->
            "Fail: rendered outer AND was $renderedOuterAnd"
        else -> "OK"
    }
}
