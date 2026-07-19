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

// Verifies deterministic duplicate names across SQL labels, typed/Map mapping, derived select, and union.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UnsafeProjectionOverride
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.utils.Extensions.mapperTo
import kotlin.reflect.KClass

@Table("tb_projection_duplicate_names")
data class DuplicateProjectionUser(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

private val duplicateRow = linkedMapOf<String, Any?>(
    "id" to 7,
    "id_2" to 9,
    "id_1" to "reserved",
)

class DuplicateProjectionWrapper(
    private val row: Map<String, Any?> = duplicateRow,
) : KronosDataSourceWrapper {
    override val url: String = "jdbc:duplicate-projection-output-names"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql

    override fun toList(task: KAtomicQueryTask): List<Any?> =
        listOf(mapResult(task))

    override fun first(task: KAtomicQueryTask): Any? = mapResult(task)

    private fun mapResult(task: KAtomicQueryTask): Any {
        val classifier = task.targetType.classifier
        if (classifier == Map::class) return row
        val kClass = classifier as? KClass<*> ?: return row
        @Suppress("UNCHECKED_CAST")
        return row.mapperTo(kClass as KClass<out KPojo>)
    }

    override fun update(task: KAtomicActionTask): Int = 0
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?,
    ): Any? = null
}

@OptIn(UnsafeProjectionOverride::class)
private fun verifyPrimaryDuplicateNames(wrapper: DuplicateProjectionWrapper): String? {
    val firstBranch = DuplicateProjectionUser().select {
        [it.id, it.id, it.username.alias("id_1")]
    }
    val fields = firstBranch.toSqlQuery().selectOutputNames()
    val typed = firstBranch.toList(wrapper).single()
    val mapList = firstBranch.toMapList(wrapper).single()
    val map = firstBranch.toMap(wrapper)
    val mapOrNull = firstBranch.toMapOrNull(wrapper)

    val derived = firstBranch.select { [it.id, it.id_2, it.id_1] }
    val derivedStatement = derived.toSqlQuery() as? SqlQuery.Select
    val derivedSourceNames = (derivedStatement?.from?.single() as? com.kotlinorm.syntax.table.SqlTable.Subquery)
        ?.query
        ?.selectOutputNames()
    val derivedNames = derivedStatement?.selectOutputNames()

    return listOfNotNull(
        expect(fields == listOf("id", "id_2", "id_1")) { "selected names were $fields" },
        expect(typed.id == 7) { "typed id was ${typed.id}" },
        expect(typed.id_2 == 9) { "typed id_2 was ${typed.id_2}" },
        expect(typed.id_1 == "reserved") { "typed id_1 was ${typed.id_1}" },
        expect(typed.__columns.map { it.name } == listOf("id", "id_2", "id_1")) {
            "typed columns were ${typed.__columns.map { it.name }}"
        },
        expect(mapList == duplicateRow) { "map list row was $mapList" },
        expect(map == duplicateRow) { "map row was $map" },
        expect(mapOrNull == duplicateRow) { "nullable map row was $mapOrNull" },
        expect(derivedSourceNames == fields) { "derived source names were $derivedSourceNames" },
        expect(derivedNames == fields) { "derived selected names were $derivedNames" },
    ).firstOrNull()
}

@OptIn(UnsafeProjectionOverride::class)
private fun verifyMixedSourceAndExpressionAliases(): String? {
    val mixedRow = linkedMapOf<String, Any?>(
        "username" to "source",
        "username_1" to 6,
    )
    val mixed = DuplicateProjectionUser().select {
        [it.username, f.length(it.username).alias("username")]
    }
    val mixedTyped = mixed.toList(DuplicateProjectionWrapper(mixedRow)).single()

    return listOfNotNull(
        expect(mixedTyped.username == "source") { "mixed username was ${mixedTyped.username}" },
        expect(mixedTyped.username_1 == 6) { "mixed username_1 was ${mixedTyped.username_1}" },
        expect(mixedTyped.__columns.map { it.name } == listOf("username", "username_1")) {
            "mixed columns were ${mixedTyped.__columns.map { it.name }}"
        },
    ).firstOrNull()
}

@OptIn(UnsafeProjectionOverride::class)
private fun verifyRepeatedExplicitAliases(): String? {
    val sameAliasRow = linkedMapOf<String, Any?>(
        "value" to 6,
        "value_1" to "source",
    )
    val sameAlias = DuplicateProjectionUser().select {
        [f.length(it.username).alias("value"), it.username.alias("value")]
    }
    val sameAliasTyped = sameAlias.toList(DuplicateProjectionWrapper(sameAliasRow)).single()

    return listOfNotNull(
        expect(sameAliasTyped.value == 6) { "same-alias value was ${sameAliasTyped.value}" },
        expect(sameAliasTyped.value_1 == "source") {
            "same-alias value_1 was ${sameAliasTyped.value_1}"
        },
        expect(sameAliasTyped.__columns.map { it.name } == listOf("value", "value_1")) {
            "same-alias columns were ${sameAliasTyped.__columns.map { it.name }}"
        },
    ).firstOrNull()
}

@OptIn(UnsafeProjectionOverride::class)
private fun verifyUnionOutputNames(wrapper: DuplicateProjectionWrapper): String? {
    val firstBranch = DuplicateProjectionUser().select {
        [it.id, it.id, it.username.alias("id_1")]
    }
    val secondBranch = DuplicateProjectionUser().select {
        [it.id, it.id.alias("otherId"), it.username.alias("otherName")]
    }
    val unionQuery = union(firstBranch, secondBranch)
    val unionTyped = unionQuery.toList(wrapper).single()
    val unionStatement = unionQuery.toSqlQuery()
    val unionFirstNames = (unionStatement as? SqlQuery.Set)?.left?.selectOutputNames()
    val unionSecondNames = (unionStatement as? SqlQuery.Set)?.right?.selectOutputNames()

    return listOfNotNull(
        expect(unionTyped.id == 7 && unionTyped.id_2 == 9 && unionTyped.id_1 == "reserved") {
            "union typed row was $unionTyped"
        },
        expect(unionFirstNames == listOf("id", "id_2", "id_1")) {
            "union first names were $unionFirstNames"
        },
        expect(unionSecondNames == listOf("id", "otherId", "otherName")) {
            "union second names were $unionSecondNames"
        },
    ).firstOrNull()
}

fun box(): String {
    val wrapper = DuplicateProjectionWrapper()
    Kronos.dataSource = { wrapper }
    val failures = listOfNotNull(
        verifyPrimaryDuplicateNames(wrapper),
        verifyMixedSourceAndExpressionAliases(),
        verifyRepeatedExplicitAliases(),
        verifyUnionOutputNames(wrapper),
    )
    return failures.firstOrNull() ?: "OK"
}

private fun SqlQuery.selectOutputNames(): List<String> =
    when (this) {
        is SqlQuery.Select -> select.mapNotNull { item ->
            val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
            expression.metadata?.outputName
                ?: expression.alias
                ?: (expression.expr as? SqlExpr.Column)?.columnName
        }
        is SqlQuery.Set -> left.selectOutputNames()
        is SqlQuery.With -> query.selectOutputNames()
        is SqlQuery.Values -> emptyList()
    }

private inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
