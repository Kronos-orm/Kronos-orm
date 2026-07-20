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

// Verifies join insert-select value lambdas expose the join query's generated Selected receiver.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem

@Table("tb_join_insert_user")
data class JoinInsertUser(
    @PrimaryKey
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_join_insert_order")
data class JoinInsertOrder(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_join_insert_target")
data class JoinInsertTarget(
    @PrimaryKey
    var id: Int? = null,
    var userName: String? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val source = JoinInsertUser()
        .join(JoinInsertOrder()) { user, order ->
            leftJoin { user.id == order.userId }
                .select { [user.id, user.name.alias("userName")] }
        }

    val insert = source.insert<JoinInsertTarget> {
        val userName: String? = it.userName
        if (userName == "unreachable") error("unreachable")
        [it.id, it.userName, null]
    }

    val statement = insert.toSqlStatement(wrapper = CompilerTestDataSourceWrapper)
    val sourceStatement = (statement.mode as? SqlInsertMode.Subquery)?.query as? SqlQuery.Select
        ?: return "Fail: source was ${statement.mode}"
    val rewritten = sourceStatement.select

    val failures = listOfNotNull(
        expect(statement.columns.map { it.last } == listOf("id", "user_name", "status")) {
            "target columns were ${statement.columns.map { it.last }}"
        },
        expect(rewritten.size == 3) { "rewritten select size was ${rewritten.size}" },
        expect(rewritten.expressionColumn(0) == "id") { "first source expression was ${rewritten.getOrNull(0)}" },
        expect(rewritten.expressionColumn(1) == "name") { "second source expression was ${rewritten.getOrNull(1)}" },
    )

    return failures.firstOrNull() ?: "OK"
}

fun List<SqlSelectItem>.expressionColumn(index: Int): String? {
    return ((getOrNull(index) as? SqlSelectItem.Expr)?.expr as? SqlExpr.Column)?.columnName
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
