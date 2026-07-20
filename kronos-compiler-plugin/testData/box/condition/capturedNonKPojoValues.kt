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

// Verifies ordinary Kotlin object properties remain runtime values in condition lowering.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table("tb_captured_non_kpojo_user")
data class CapturedNonKPojoUser(
    var id: Int? = null,
    var name: String? = null,
    var score: Int? = null,
    var status: Int? = null,
    var value: Int? = null,
) : KPojo

@Table("tb_captured_non_kpojo_order")
data class CapturedNonKPojoOrder(
    var id: Int? = null,
    var userId: Int? = null,
) : KPojo

@Table("tb_nested_value_outer")
data class CapturedNestedValueOuter(var value: CapturedNestedValueInner? = null) : KPojo

@Table("tb_nested_value_inner")
data class CapturedNestedValueInner(var value: Int? = null) : KPojo

class CapturedPlainContext(
    val ownerId: Int?,
    val name: String?,
    val score: Int?,
)

data class CapturedDataContext(val ownerId: Int?, val name: String?)

object CapturedObjectContext {
    val ownerId: Int? get() = 19
    val name: String? get() = "Object"
}

class CapturedStaticContext(val marker: Int = 0) {
    companion object {
        val companionOwnerId: Int? get() = 23
        val companionName: String? get() = "Companion"

        @JvmStatic
        val staticOwnerId: Int? get() = 29

        @JvmStatic
        val staticName: String? get() = "Static"
    }
}

val capturedTopLevelOwnerId: Int? get() = 31
val capturedTopLevelName: String? get() = "TopLevel"

data class CapturedNonKPojoCondition(
    val expression: SqlExpr?,
    val parameters: Map<String, Any?>,
)

fun captureNonKPojoCondition(
    user: CapturedNonKPojoUser,
    block: ToFilter<CapturedNonKPojoUser, Boolean?>,
): CapturedNonKPojoCondition {
    var captured: CapturedNonKPojoCondition? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        captured = CapturedNonKPojoCondition(sqlExpr, parameterValues.toMap())
    }
    return captured ?: CapturedNonKPojoCondition(null, emptyMap())
}

fun captureNestedValueCondition(
    outer: CapturedNestedValueOuter,
    block: ToFilter<CapturedNestedValueOuter, Boolean?>,
): CapturedNonKPojoCondition {
    var captured: CapturedNonKPojoCondition? = null
    outer.afterFilter {
        sourceValues = outer.toDataMap()
        block!!(it)
        captured = CapturedNonKPojoCondition(sqlExpr, parameterValues.toMap())
    }
    return captured ?: CapturedNonKPojoCondition(null, emptyMap())
}

fun expectedCapturedNonKPojoValue(fieldName: String, value: Any?): CapturedNonKPojoCondition =
    CapturedNonKPojoCondition(
        SqlExpr.Binary(
            SqlExpr.Column("tb_captured_non_kpojo_user", fieldName),
            SqlBinaryOperator.Equal,
            SqlExpr.Parameter(SqlParameter.Named(fieldName)),
        ),
        mapOf(fieldName to value),
    )

fun expectedCapturedNonKPojoFunction(value: String): CapturedNonKPojoCondition =
    CapturedNonKPojoCondition(
        SqlExpr.Binary(
            SqlExpr.Function(
                name = SqlIdentifier.of("LENGTH"),
                args = listOf(SqlExpr.StringLiteral(value)),
            ),
            SqlBinaryOperator.Equal,
            SqlExpr.Column("tb_captured_non_kpojo_user", "score"),
        ),
        emptyMap(),
    )

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = CapturedNonKPojoUser()
    val context = CapturedPlainContext(ownerId = 17, name = "Plain", score = 91)
    val dataContext = CapturedDataContext(ownerId = 13, name = "Data")
    val probe = CapturedNonKPojoUser(value = 41)
    val expectedId = expectedCapturedNonKPojoValue("id", 17)
    val direct = captureNonKPojoCondition(user) { it.id == context.ownerId }
    val reversed = captureNonKPojoCondition(user) { context.ownerId == it.id }
    val valueField = captureNonKPojoCondition(user) { it.value == 5 }
    val explicitValue = captureNonKPojoCondition(user) { it.value == probe.value.value }
    val nestedSource = CapturedNestedValueOuter()
    val nestedProbe = CapturedNestedValueOuter(CapturedNestedValueInner(value = 47))
    val nestedValueField = captureNestedValueCondition(nestedSource) { it.value?.value == 5 }
    val nestedExplicitValue = captureNestedValueCondition(nestedSource) {
        it.value?.value == nestedProbe.value?.value.value
    }
    val expectedNestedValue = CapturedNonKPojoCondition(
        SqlExpr.Binary(
            SqlExpr.Column("tb_nested_value_inner", "value"),
            SqlBinaryOperator.Equal,
            SqlExpr.Parameter(SqlParameter.Named("value")),
        ),
        mapOf("value" to 47),
    )
    val valueCases = listOf(
        "data class" to (
            captureNonKPojoCondition(user) { it.id == dataContext.ownerId } to
                expectedCapturedNonKPojoValue("id", 13)
            ),
        "object" to (
            captureNonKPojoCondition(user) { it.id == CapturedObjectContext.ownerId } to
                expectedCapturedNonKPojoValue("id", 19)
            ),
        "companion" to (
            captureNonKPojoCondition(user) { it.id == CapturedStaticContext.companionOwnerId } to
                expectedCapturedNonKPojoValue("id", 23)
            ),
        "jvm static" to (
            captureNonKPojoCondition(user) { it.id == CapturedStaticContext.staticOwnerId } to
                expectedCapturedNonKPojoValue("id", 29)
            ),
        "top level" to (
            captureNonKPojoCondition(user) { it.id == capturedTopLevelOwnerId } to
                expectedCapturedNonKPojoValue("id", 31)
            ),
    )
    val functionCases = listOf(
        "plain class" to (
            captureNonKPojoCondition(user) { it.score == f.length(context.name) } to
                expectedCapturedNonKPojoFunction("Plain")
            ),
        "data class" to (
            captureNonKPojoCondition(user) { it.score == f.length(dataContext.name) } to
                expectedCapturedNonKPojoFunction("Data")
            ),
        "object" to (
            captureNonKPojoCondition(user) { it.score == f.length(CapturedObjectContext.name) } to
                expectedCapturedNonKPojoFunction("Object")
            ),
        "companion" to (
            captureNonKPojoCondition(user) { it.score == f.length(CapturedStaticContext.companionName) } to
                expectedCapturedNonKPojoFunction("Companion")
            ),
        "jvm static" to (
            captureNonKPojoCondition(user) { it.score == f.length(CapturedStaticContext.staticName) } to
                expectedCapturedNonKPojoFunction("Static")
            ),
        "top level" to (
            captureNonKPojoCondition(user) { it.score == f.length(capturedTopLevelName) } to
                expectedCapturedNonKPojoFunction("TopLevel")
            ),
    )

    val whereParams = user.select()
        .where { it.id == context.ownerId }
        .build(CompilerTestDataSourceWrapper)
        .atomicTask.paramMap
    val havingParams = user.select()
        .having { it.score == context.score }
        .build(CompilerTestDataSourceWrapper)
        .atomicTask.paramMap
    val joinParams = user.join(CapturedNonKPojoOrder()) { source, order ->
        leftJoin { source.id == context.ownerId }
            .select { source.id }
    }.build(CompilerTestDataSourceWrapper).atomicTask.paramMap
    val (_, updateParams) = user.update()
        .set { it.status = 2 }
        .where { it.id == context.ownerId }
        .build(CompilerTestDataSourceWrapper)
    val (_, deleteParams) = user.delete()
        .logic(false)
        .where { it.id == context.ownerId }
        .build(CompilerTestDataSourceWrapper)

    return when {
        direct != expectedId -> "Fail: direct was $direct"
        reversed != expectedId -> "Fail: reversed was $reversed"
        valueField != expectedCapturedNonKPojoValue("value", 5) -> "Fail: value field was $valueField"
        explicitValue != expectedCapturedNonKPojoValue("value", 41) -> "Fail: explicit value was $explicitValue"
        nestedValueField != expectedNestedValue.copy(parameters = mapOf("value" to 5)) ->
            "Fail: nested value field was $nestedValueField"
        nestedExplicitValue != expectedNestedValue -> "Fail: nested explicit value was $nestedExplicitValue"
        valueCases.any { it.second.first != it.second.second } -> "Fail: value cases were $valueCases"
        functionCases.any { it.second.first != it.second.second } -> "Fail: function cases were $functionCases"
        whereParams != mapOf("id" to 17) -> "Fail: where params were $whereParams"
        havingParams != mapOf("score" to 91) -> "Fail: having params were $havingParams"
        joinParams != mapOf("id" to 17) -> "Fail: join params were $joinParams"
        updateParams != mapOf("statusNew" to 2, "id" to 17) -> "Fail: update params were $updateParams"
        deleteParams != mapOf("id" to 17) -> "Fail: delete params were $deleteParams"
        else -> "OK"
    }
}
