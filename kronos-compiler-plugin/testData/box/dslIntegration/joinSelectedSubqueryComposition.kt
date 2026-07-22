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

// Verifies generated JOIN Selected queries compose as scalar and predicate subqueries.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlJoinCondition
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.utils.Extensions.mapperTo
import kotlin.reflect.KClass
import kotlin.reflect.KType

@Table("tb_join_subquery_user")
data class JoinSubqueryUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_join_subquery_order")
data class JoinSubqueryOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

class JoinSelectedSubqueryWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:join-selected-subquery"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val mappedTypes = mutableListOf<KType>()

    override fun toList(task: KAtomicQueryTask): List<Any?> = listOfNotNull(first(task))

    override fun first(task: KAtomicQueryTask): Any? {
        mappedTypes += task.targetType
        return mapOf(
            "id" to 3,
            "userId" to 17,
            "joinedUserId" to 17,
        ).mapperTo(task.targetType)
    }

    override fun update(task: KAtomicActionTask): Int = 0

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()

    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?,
    ): Any? = null
}

private fun joinedUserIds() = JoinSubqueryOrder()
    .join(JoinSubqueryUser()) { order, user ->
        innerJoin { order.userId == user.id }
            .select { [order.userId] }
    }
    .where { it.amount > 10 }

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val wrapper = JoinSelectedSubqueryWrapper()
    val joinedRow = joinedUserIds().first(wrapper)
    val joinedValue: Int? = joinedRow.userId

    val scalarClause = JoinSubqueryUser().select {
        [
            it.id,
            joinedUserIds().limit(1).alias("joinedUserId"),
        ]
    }
    val scalarRow = scalarClause.first(wrapper)
    val scalarValue: Int? = scalarRow.joinedUserId
    val scalarStatement = scalarClause.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val scalarItem = scalarStatement.select.singleOrNull { item ->
        (item as? SqlSelectItem.Expr)?.metadata?.outputName == "joinedUserId"
    } as? SqlSelectItem.Expr
    val scalarSubquery = (scalarItem?.expr as? SqlExpr.Subquery)?.query as? SqlQuery.Select
    val scalarJoin = scalarSubquery?.from?.singleOrNull() as? SqlTable.Join

    val predicateStatement = JoinSubqueryUser()
        .select()
        .where { it.id in joinedUserIds() }
        .toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val predicate = predicateStatement.where as? SqlExpr.In
    val predicateSubquery = (predicate?.`in` as? SqlInRightOperand.Subquery)?.query as? SqlQuery.Select
    val predicateJoin = predicateSubquery?.from?.singleOrNull() as? SqlTable.Join

    val failures = listOfNotNull(
        expect(joinedValue == 17) { "JOIN Selected value was $joinedValue" },
        expect(scalarValue == 17) { "scalar Selected value was $scalarValue" },
        expect(wrapper.mappedTypes.size == 2) { "mapped types were ${wrapper.mappedTypes}" },
        expect(wrapper.mappedTypes.all { !it.isMarkedNullable && it.arguments.isEmpty() }) {
            "mapped types were ${wrapper.mappedTypes}"
        },
        expect(wrapper.mappedTypes.all {
            (it.classifier as? KClass<*>)?.simpleName?.startsWith("KronosSelectResult_") == true
        }) {
            "mapped types were ${wrapper.mappedTypes}"
        },
        expect(scalarItem?.alias == "joinedUserId") { "scalar item was $scalarItem" },
        expect(scalarSubquery?.outputNames() == listOf("userId")) {
            "scalar subquery output was ${scalarSubquery?.outputNames()}"
        },
        expect(scalarJoin?.condition is SqlJoinCondition.On) { "scalar JOIN was $scalarJoin" },
        expect(scalarSubquery?.where is SqlExpr.Binary) { "scalar subquery where was ${scalarSubquery?.where}" },
        expect((predicate?.expr as? SqlExpr.Column)?.columnName == "id") {
            "predicate left expression was ${predicate?.expr}"
        },
        expect(predicateSubquery?.outputNames() == listOf("userId")) {
            "predicate subquery output was ${predicateSubquery?.outputNames()}"
        },
        expect(predicateJoin?.condition is SqlJoinCondition.On) { "predicate JOIN was $predicateJoin" },
        expect(predicateSubquery?.where is SqlExpr.Binary) {
            "predicate subquery where was ${predicateSubquery?.where}"
        },
    )
    return failures.firstOrNull() ?: "OK"
}

private fun SqlQuery.Select.outputNames(): List<String> = select.mapNotNull { item ->
    val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
    expression.metadata?.outputName
        ?: expression.alias
        ?: (expression.expr as? SqlExpr.Column)?.columnName
}

private inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
