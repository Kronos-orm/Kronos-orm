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

// Verifies literal null predicates and relationship safe-call field chains lower to SQL expressions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_director")
data class NullSafeDirector(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table(name = "tb_condition_movie")
data class NullSafeMovie(
    var id: Int? = null,
    var directorId: Int? = null,
    @Cascade(["directorId"], ["id"])
    val director: NullSafeDirector? = null,
    var title: String? = null,
) : KPojo

data class NullSafeConditionCapture(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>,
)

fun nullSafeWhere(
    movie: NullSafeMovie,
    block: ToFilter<NullSafeMovie, Boolean?>,
): NullSafeConditionCapture {
    var result: NullSafeConditionCapture? = null
    movie.afterFilter {
        sourceValues = movie.toDataMap()
        block!!(it)
        result = NullSafeConditionCapture(sqlExpr, parameterValues.toMap())
    }
    return result ?: NullSafeConditionCapture(null, emptyMap())
}

fun expectNullPredicate(
    label: String,
    actual: NullSafeConditionCapture,
    column: String,
    withNot: Boolean,
): String? {
    val binary = actual.expr as? SqlExpr.Binary
    val left = binary?.left as? SqlExpr.Column
    return when {
        binary == null -> "Fail: $label expr was ${actual.expr}"
        left?.columnName != column -> "Fail: $label left column was ${left?.columnName}"
        binary.operator != SqlBinaryOperator.Is(withNot = withNot) -> "Fail: $label operator was ${binary.operator}"
        binary.right != SqlExpr.NullLiteral -> "Fail: $label right was ${binary.right}"
        actual.parameters.isNotEmpty() -> "Fail: $label parameters were ${actual.parameters}"
        else -> null
    }
}

fun expectColumnComparison(
    label: String,
    actual: NullSafeConditionCapture,
    leftColumn: String,
    rightColumn: String,
): String? {
    val binary = actual.expr as? SqlExpr.Binary
    val left = binary?.left as? SqlExpr.Column
    val right = binary?.right as? SqlExpr.Column
    return when {
        binary == null -> "Fail: $label expr was ${actual.expr}"
        left?.columnName != leftColumn -> "Fail: $label left column was ${left?.columnName}"
        binary.operator != SqlBinaryOperator.Equal -> "Fail: $label operator was ${binary.operator}"
        right?.columnName != rightColumn -> "Fail: $label right column was ${right?.columnName}"
        actual.parameters.isNotEmpty() -> "Fail: $label parameters were ${actual.parameters}"
        else -> null
    }
}

fun expectLiteralNull(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val movie = NullSafeMovie(id = 1, directorId = 10, title = "Solaris")
    val title: String? = null

    val literalNull = nullSafeWhere(movie) { it.title == null }
    val literalNotNull = nullSafeWhere(movie) { it.title != null }
    val reversedLiteralNull = nullSafeWhere(movie) { null == it.title }
    val reversedLiteralNotNull = nullSafeWhere(movie) { null != it.title }
    val dynamicNull = nullSafeWhere(movie) { it.title == title }
    val unsafeCascade = nullSafeWhere(movie) { it.directorId == it.director!!.id }
    val safeCascade = nullSafeWhere(movie) { it.directorId == it.director?.id }
    val notWrappedSafeCascade = nullSafeWhere(movie) { !(it.directorId != it.director?.id) }
    val methodNotWrappedSafeCascade = nullSafeWhere(movie) { (it.directorId != it.director?.id).not() }
    val ifGuardCascade = nullSafeWhere(movie) {
        it.directorId == if (it.director == null) null else it.director.id
    }
    val whenGuardCascade = nullSafeWhere(movie) {
        it.directorId == when {
            it.director == null -> null
            else -> it.director.id
        }
    }
    val ifNotNullGuardCascade = nullSafeWhere(movie) {
        it.directorId == if (it.director != null) it.director.id else null
    }
    val whenNotNullGuardCascade = nullSafeWhere(movie) {
        it.directorId == when {
            it.director != null -> it.director.id
            else -> null
        }
    }
    val reversedIfGuardCascade = nullSafeWhere(movie) {
        it.directorId == if (null == it.director) null else it.director.id
    }
    val reversedWhenGuardCascade = nullSafeWhere(movie) {
        it.directorId == when {
            null == it.director -> null
            else -> it.director.id
        }
    }
    val reversedIfNotNullGuardCascade = nullSafeWhere(movie) {
        it.directorId == if (null != it.director) it.director.id else null
    }
    val reversedWhenNotNullGuardCascade = nullSafeWhere(movie) {
        it.directorId == when {
            null != it.director -> it.director.id
            else -> null
        }
    }
    val notGuardedIfCascade = nullSafeWhere(movie) {
        it.directorId == if (!(it.director == null)) it.director.id else null
    }
    val methodNotGuardedIfCascade = nullSafeWhere(movie) {
        it.directorId == if ((it.director == null).not()) it.director.id else null
    }
    val negatedNotNullGuardCascade = nullSafeWhere(movie) {
        it.directorId == if (!(it.director != null)) null else it.director.id
    }
    val methodNegatedNotNullGuardCascade = nullSafeWhere(movie) {
        it.directorId == if ((it.director != null).not()) null else it.director.id
    }
    val castGuardedIfCascade = nullSafeWhere(movie) {
        it.directorId == if ((it.director as NullSafeDirector?) != null) (it.director as NullSafeDirector).id else null
    }

    val failures = listOfNotNull(
        expectNullPredicate("literalNull", literalNull, "title", withNot = false),
        expectNullPredicate("literalNotNull", literalNotNull, "title", withNot = true),
        expectNullPredicate("reversedLiteralNull", reversedLiteralNull, "title", withNot = false),
        expectNullPredicate("reversedLiteralNotNull", reversedLiteralNotNull, "title", withNot = true),
        expectLiteralNull(dynamicNull.expr == null) { "dynamicNull expr was ${dynamicNull.expr}" },
        expectColumnComparison("unsafeCascade", unsafeCascade, "director_id", "id"),
        expectColumnComparison("safeCascade", safeCascade, "director_id", "id"),
        expectColumnComparison("notWrappedSafeCascade", notWrappedSafeCascade, "director_id", "id"),
        expectColumnComparison("methodNotWrappedSafeCascade", methodNotWrappedSafeCascade, "director_id", "id"),
        expectColumnComparison("ifGuardCascade", ifGuardCascade, "director_id", "id"),
        expectColumnComparison("whenGuardCascade", whenGuardCascade, "director_id", "id"),
        expectColumnComparison("ifNotNullGuardCascade", ifNotNullGuardCascade, "director_id", "id"),
        expectColumnComparison("whenNotNullGuardCascade", whenNotNullGuardCascade, "director_id", "id"),
        expectColumnComparison("reversedIfGuardCascade", reversedIfGuardCascade, "director_id", "id"),
        expectColumnComparison("reversedWhenGuardCascade", reversedWhenGuardCascade, "director_id", "id"),
        expectColumnComparison("reversedIfNotNullGuardCascade", reversedIfNotNullGuardCascade, "director_id", "id"),
        expectColumnComparison("reversedWhenNotNullGuardCascade", reversedWhenNotNullGuardCascade, "director_id", "id"),
        expectColumnComparison("notGuardedIfCascade", notGuardedIfCascade, "director_id", "id"),
        expectColumnComparison("methodNotGuardedIfCascade", methodNotGuardedIfCascade, "director_id", "id"),
        expectColumnComparison("negatedNotNullGuardCascade", negatedNotNullGuardCascade, "director_id", "id"),
        expectColumnComparison("methodNegatedNotNullGuardCascade", methodNegatedNotNullGuardCascade, "director_id", "id"),
        expectColumnComparison("castGuardedIfCascade", castGuardedIfCascade, "director_id", "id"),
    )

    return failures.firstOrNull() ?: "OK"
}