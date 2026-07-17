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

// Verifies no-value behavior, dynamic gates, boolean fallbacks, run conditions, and negated OR lowering.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_no_value_matrix")
data class NoValueMatrixUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo

data class CapturedNoValueMatrix(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun noValueMatrixWhere(
    user: NoValueMatrixUser,
    block: ToFilter<NoValueMatrixUser, Boolean?>,
): CapturedNoValueMatrix {
    var result: CapturedNoValueMatrix? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedNoValueMatrix(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedNoValueMatrix(null, emptyMap())
}

fun noValueMatrixParameter(actual: CapturedNoValueMatrix, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun expectNoValueMatrix(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = NoValueMatrixUser(id = 1, name = "Ada", age = 36)
    val nullName = user.copy(name = null)
    val missingName: String? = null
    val presentName: String? = "Ada"

    val runWrapped = noValueMatrixWhere(user) { run { it.age > 18 } }
    val runExpr = runWrapped.expr as? SqlExpr.Binary

    val negatedOr = noValueMatrixWhere(user) { !(it.age > 18 || it.name == "Ada") }
    val negatedOrRoot = negatedOr.expr as? SqlExpr.Binary
    val negatedLeft = negatedOrRoot?.left as? SqlExpr.Binary
    val negatedRight = negatedOrRoot?.right as? SqlExpr.Binary

    val takeIfTrue = noValueMatrixWhere(user) { (it.id == 1).takeIf(true) }
    val takeIfExpr = takeIfTrue.expr as? SqlExpr.Binary

    val takeIfFalse = noValueMatrixWhere(user) { (it.name == "Ada").takeIf(false) }
    val takeUnlessFalse = noValueMatrixWhere(user) { (it.age > 18).takeUnless(false) }
    val takeUnlessExpr = takeUnlessFalse.expr as? SqlExpr.Binary
    val takeUnlessTrue = noValueMatrixWhere(user) { (it.name == "Ada").takeUnless(true) }
    val explicitFalse = noValueMatrixWhere(nullName) {
        if (missingName != null) { it.name == missingName } else { false.asSql() }
    }
    val explicitTrue = noValueMatrixWhere(nullName) {
        if (missingName != null) { it.name == missingName } else { true.asSql() }
    }
    val explicitJudgeNull = noValueMatrixWhere(nullName) {
        if (missingName != null) { it.name == missingName } else { it.name.isNull }
    }
    val explicitPresent = noValueMatrixWhere(nullName) {
        if (presentName != null) { it.name == presentName } else { false.asSql() }
    }
    val defaultNoValue = noValueMatrixWhere(nullName) { it.name.eq }

    val judgeNullExpr = explicitJudgeNull.expr as? SqlExpr.Binary
    val judgeNullColumn = judgeNullExpr?.left as? SqlExpr.Column
    val presentExpr = explicitPresent.expr as? SqlExpr.Binary
    val failures = listOfNotNull(
        expectNoValueMatrix((runExpr?.left as? SqlExpr.Column)?.columnName == "age") {
            "run field was ${runExpr?.left}"
        },
        expectNoValueMatrix(runExpr?.operator == SqlBinaryOperator.GreaterThan) {
            "run operator was ${runExpr?.operator}"
        },
        expectNoValueMatrix(noValueMatrixParameter(runWrapped, runExpr?.right) == 18) {
            "run value was ${noValueMatrixParameter(runWrapped, runExpr?.right)}"
        },
        expectNoValueMatrix(negatedOrRoot?.operator == SqlBinaryOperator.And) {
            "negated OR root was ${negatedOrRoot?.operator}"
        },
        expectNoValueMatrix(negatedLeft?.operator == SqlBinaryOperator.LessThanEqual) {
            "negated left operator was ${negatedLeft?.operator}"
        },
        expectNoValueMatrix(negatedRight?.operator == SqlBinaryOperator.NotEqual) {
            "negated right operator was ${negatedRight?.operator}"
        },
        expectNoValueMatrix((takeIfExpr?.left as? SqlExpr.Column)?.columnName == "id") {
            "takeIf field was ${takeIfExpr?.left}"
        },
        expectNoValueMatrix(takeIfExpr?.operator == SqlBinaryOperator.Equal) {
            "takeIf operator was ${takeIfExpr?.operator}"
        },
        expectNoValueMatrix(takeIfFalse.expr == null) {
            "takeIf false condition was ${takeIfFalse.expr}"
        },
        expectNoValueMatrix((takeUnlessExpr?.left as? SqlExpr.Column)?.columnName == "age") {
            "takeUnless field was ${takeUnlessExpr?.left}"
        },
        expectNoValueMatrix(takeUnlessExpr?.operator == SqlBinaryOperator.GreaterThan) {
            "takeUnless operator was ${takeUnlessExpr?.operator}"
        },
        expectNoValueMatrix(takeUnlessTrue.expr == null) {
            "takeUnless true condition was ${takeUnlessTrue.expr}"
        },
        expectNoValueMatrix((explicitTrue.expr as? SqlExpr.BooleanLiteral)?.boolean == true) {
            "explicit true fallback was ${explicitTrue.expr}"
        },
        expectNoValueMatrix((explicitFalse.expr as? SqlExpr.BooleanLiteral)?.boolean == false) {
            "explicit false fallback was ${explicitFalse.expr}"
        },
        expectNoValueMatrix((presentExpr?.left as? SqlExpr.Column)?.columnName == "name") {
            "explicit present field was ${presentExpr?.left}"
        },
        expectNoValueMatrix(noValueMatrixParameter(explicitPresent, presentExpr?.right) == "Ada") {
            "explicit present value was ${noValueMatrixParameter(explicitPresent, presentExpr?.right)}"
        },
        expectNoValueMatrix(judgeNullColumn?.columnName == "name") {
            "judgeNull field was ${judgeNullExpr?.left}"
        },
        expectNoValueMatrix(judgeNullExpr?.operator == SqlBinaryOperator.Is(false)) {
            "judgeNull operator was ${judgeNullExpr?.operator}"
        },
        expectNoValueMatrix(judgeNullExpr?.right == SqlExpr.NullLiteral) {
            "judgeNull right was ${judgeNullExpr?.right}"
        },
        expectNoValueMatrix(defaultNoValue.expr == null) {
            "default no-value condition was ${defaultNoValue.expr}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}
