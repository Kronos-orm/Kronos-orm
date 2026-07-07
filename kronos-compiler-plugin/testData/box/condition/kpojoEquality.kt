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

// Verifies no-argument KPojo equality expands to equality SqlExpr nodes for every column property.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_kpojo_equality")
data class KPojoEqualityUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo

data class CapturedKPojoEquality(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun kpojoEqualityWhere(user: KPojoEqualityUser, block: ToFilter<KPojoEqualityUser, Boolean?>): CapturedKPojoEquality {
    var result: CapturedKPojoEquality? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedKPojoEquality(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedKPojoEquality(null, emptyMap())
}

fun kpojoEqualityLeaves(expr: SqlExpr?): List<SqlExpr.Binary> {
    val binary = expr as? SqlExpr.Binary ?: return emptyList()
    return if (binary.operator == SqlBinaryOperator.And) {
        kpojoEqualityLeaves(binary.left) + kpojoEqualityLeaves(binary.right)
    } else {
        listOf(binary)
    }
}

fun kpojoEqualityParameter(actual: CapturedKPojoEquality, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun expectKPojo(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = KPojoEqualityUser(id = 1, name = "Ada", age = 36)
    val equal = kpojoEqualityWhere(user) { it.eq }
    val leaves = kpojoEqualityLeaves(equal.expr)
    val equalNames = leaves.mapNotNull { (it.left as? SqlExpr.Column)?.columnName }.toSet()
    val equalValues = leaves.associate { leaf ->
        ((leaf.left as? SqlExpr.Column)?.columnName ?: "") to kpojoEqualityParameter(equal, leaf.right)
    }

    val failures = listOfNotNull(
        expectKPojo(leaves.all { it.operator == SqlBinaryOperator.Equal }) { "operators were ${leaves.map { it.operator }}" },
        expectKPojo(equalNames == setOf("id", "name", "age")) { "equal fields were $equalNames" },
        expectKPojo(equalValues["id"] == 1) { "id value was ${equalValues["id"]}" },
        expectKPojo(equalValues["name"] == "Ada") { "name value was ${equalValues["name"]}" },
        expectKPojo(equalValues["age"] == 36) { "age value was ${equalValues["age"]}" },
    )

    return failures.firstOrNull() ?: "OK"
}
