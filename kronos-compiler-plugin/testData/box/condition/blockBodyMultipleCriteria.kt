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

// Verifies block-wrapped condition expressions lower into a single AND tree.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_block_body")
data class BlockBodyConditionUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
    var status: Int? = null,
) : KPojo

data class CapturedBlockBodyCondition(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun blockBodyWhere(user: BlockBodyConditionUser, block: ToFilter<BlockBodyConditionUser, Boolean?>): CapturedBlockBodyCondition {
    var result: CapturedBlockBodyCondition? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedBlockBodyCondition(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedBlockBodyCondition(null, emptyMap())
}

fun flattenBinary(expr: SqlExpr?, operator: SqlBinaryOperator): List<SqlExpr.Binary> {
    val binary = expr as? SqlExpr.Binary ?: return emptyList()
    return if (binary.operator == operator) {
        flattenBinary(binary.left, operator) + flattenBinary(binary.right, operator)
    } else {
        listOf(binary)
    }
}

fun blockParameter(actual: CapturedBlockBodyCondition, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun expectBlock(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = BlockBodyConditionUser(id = 1, name = "Ada", age = 36, status = 2)
    val captured = blockBodyWhere(user) {
        run { it.age > 18 } && run { it.name == "Ada" } && (it.id == 1 || it.status < 3)
    }

    val andLeaves = flattenBinary(captured.expr, SqlBinaryOperator.And)
    val age = andLeaves.getOrNull(0)
    val name = andLeaves.getOrNull(1)
    val or = andLeaves.getOrNull(2)
    val orLeaves = flattenBinary(or, SqlBinaryOperator.Or)
    val idEquals = orLeaves.getOrNull(0)
    val statusLessThan = orLeaves.getOrNull(1)

    val failures = listOfNotNull(
        expectBlock(andLeaves.size == 3) { "AND leaves were $andLeaves" },
        expectBlock(((age?.left as? SqlExpr.Column)?.columnName) == "age") { "age field was ${age?.left}" },
        expectBlock(age?.operator == SqlBinaryOperator.GreaterThan) { "age operator was ${age?.operator}" },
        expectBlock(blockParameter(captured, age?.right) == 18) { "age value was ${blockParameter(captured, age?.right)}" },
        expectBlock(((name?.left as? SqlExpr.Column)?.columnName) == "name") { "name field was ${name?.left}" },
        expectBlock(name?.operator == SqlBinaryOperator.Equal) { "name operator was ${name?.operator}" },
        expectBlock(blockParameter(captured, name?.right) == "Ada") { "name value was ${blockParameter(captured, name?.right)}" },
        expectBlock(or?.operator == SqlBinaryOperator.Or) { "OR root was ${or?.operator}" },
        expectBlock(((idEquals?.left as? SqlExpr.Column)?.columnName) == "id") { "id field was ${idEquals?.left}" },
        expectBlock(idEquals?.operator == SqlBinaryOperator.Equal) { "id operator was ${idEquals?.operator}" },
        expectBlock(blockParameter(captured, idEquals?.right) == 1) { "id value was ${blockParameter(captured, idEquals?.right)}" },
        expectBlock(((statusLessThan?.left as? SqlExpr.Column)?.columnName) == "status") {
            "status field was ${statusLessThan?.left}"
        },
        expectBlock(statusLessThan?.operator == SqlBinaryOperator.LessThan) {
            "status operator was ${statusLessThan?.operator}"
        },
        expectBlock(blockParameter(captured, statusLessThan?.right) == 3) {
            "status value was ${blockParameter(captured, statusLessThan?.right)}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}
