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

// Verifies whole-source and source-minus selects generate projection rows matching runtime columns.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import kotlin.reflect.KClass

@Table("tb_kpojo_expansion_projection")
data class KPojoExpansionProjectionUser(
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
    var it: String? = null,
    @Ignore
    var ignored: String? = null,
    @Serialize
    var tags: List<String>? = null,
    var child: KPojoExpansionProjectionChild? = null,
    var children: List<KPojoExpansionProjectionChild> = emptyList(),
    @Cascade(["id"], ["id"])
    var cascadeChild: KPojoExpansionProjectionChild? = null,
) : KPojo

data class KPojoExpansionProjectionChild(
    var id: Int? = null,
) : KPojo

class KPojoExpansionProjectionWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:kpojo-expansion-projection"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val mappedClasses = mutableListOf<KClass<*>>()

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        (task.targetType.classifier as? KClass<*>)?.let(mappedClasses::add)
        return emptyList()
    }

    override fun first(task: KAtomicQueryTask): Any? = null

    override fun update(task: KAtomicActionTask): Int = 0

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()

    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?
    ): Any? = null
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val source = KPojoExpansionProjectionUser()
    val allDirect = source.select { it }
    val allLiteral = source.select { [it] }
    val allWithAlias = source.select { [it, it.id.alias("xxx")] }
    val excludedDirect = source.select { it - it.id }
    val excludedLiteral = source.select { [it - it.id] }
    val excludedCollectionLiteral = source.select { it - [it.id, it.status] }
    val excludedList = source.select { it - listOf(it.id, it.status) }
    val namedAll = source.select { row -> row }
    val namedExcluded = source.select { row -> row - [row.id, row.status] }
    val propertyNamedIt = source.select { it.it }
    val arithmeticMinus = source.select { (it.id - 1).alias("previousId") }
    val functionMinus = source.select { (f.length(it.name) - 1).alias("shorterLength") }
    val excludedNonColumn = source.select { it - KPojoExpansionProjectionUser::ignored }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val directAllRow = allDirect.first()
        val directAllId: Int? = directAllRow.id
        val directAllName: String? = directAllRow.name
        val directAllStatus: Int? = directAllRow.status
        val directAllIt: String? = directAllRow.it
        val directAllTags: List<String>? = directAllRow.tags
        val literalAllRow = allLiteral.first()
        val literalAllId: Int? = literalAllRow.id
        val literalAllName: String? = literalAllRow.name
        val literalAllStatus: Int? = literalAllRow.status
        val literalAllIt: String? = literalAllRow.it
        val literalAllTags: List<String>? = literalAllRow.tags
        val allWithAliasRow = allWithAlias.first()
        val expandedId: Int? = allWithAliasRow.id
        val expandedName: String? = allWithAliasRow.name
        val expandedStatus: Int? = allWithAliasRow.status
        val aliasedId: Int? = allWithAliasRow.xxx
        val directExcludedRow = excludedDirect.first()
        val directExcludedName: String? = directExcludedRow.name
        val directExcludedStatus: Int? = directExcludedRow.status
        val literalExcludedRow = excludedLiteral.first()
        val literalExcludedName: String? = literalExcludedRow.name
        val literalExcludedStatus: Int? = literalExcludedRow.status
        val collectionExcludedRow = excludedCollectionLiteral.first()
        val collectionExcludedName: String? = collectionExcludedRow.name
        val listExcludedRow = excludedList.first()
        val listExcludedName: String? = listExcludedRow.name
        val namedAllRow = namedAll.first()
        val namedAllId: Int? = namedAllRow.id
        val namedAllName: String? = namedAllRow.name
        val namedAllStatus: Int? = namedAllRow.status
        val namedExcludedRow = namedExcluded.first()
        val namedExcludedName: String? = namedExcludedRow.name
        val propertyNamedItRow = propertyNamedIt.first()
        val selectedIt: String? = propertyNamedItRow.it
        val arithmeticMinusRow = arithmeticMinus.first()
        val previousId: Number? = arithmeticMinusRow.previousId
        val functionMinusRow = functionMinus.first()
        val shorterLength: Number? = functionMinusRow.shorterLength
        val excludedNonColumnRow = excludedNonColumn.first()
        val excludedNonColumnId: Int? = excludedNonColumnRow.id
        val excludedNonColumnName: String? = excludedNonColumnRow.name
        return "Fail: selected values unexpectedly evaluated as " +
            "$directAllId/$directAllName/$directAllStatus/$directAllIt/$directAllTags/" +
            "$literalAllId/$literalAllName/$literalAllStatus/$literalAllIt/$literalAllTags/" +
            "$expandedId/$expandedName/$expandedStatus/$aliasedId/" +
            "$directExcludedName/$directExcludedStatus/$literalExcludedName/$literalExcludedStatus/" +
            "$collectionExcludedName/$listExcludedName/$namedAllId/$namedAllName/$namedAllStatus/$namedExcludedName/" +
            "$selectedIt/$previousId/$shorterLength/$excludedNonColumnId/$excludedNonColumnName"
    }

    val clauses = listOf(
        allDirect,
        allLiteral,
        allWithAlias,
        excludedDirect,
        excludedLiteral,
        excludedCollectionLiteral,
        excludedList,
        namedAll,
        namedExcluded,
        propertyNamedIt,
        arithmeticMinus,
        functionMinus,
        excludedNonColumn
    )
    val outputNames = clauses.map { clause ->
        (clause.toSqlQuery() as SqlQuery.Select).selectedOutputNames()
    }
    val wrapper = KPojoExpansionProjectionWrapper()
    clauses.forEach { it.toList(wrapper) }

    val failures = listOfNotNull(
        expansionExpect(outputNames[0] == listOf("id", "name", "status", "it", "tags")) {
            "direct all columns were ${outputNames[0]}"
        },
        expansionExpect(outputNames[1] == listOf("id", "name", "status", "it", "tags")) {
            "literal all columns were ${outputNames[1]}"
        },
        expansionExpect(outputNames[2] == listOf("id", "name", "status", "it", "tags", "xxx")) {
            "all-with-alias columns were ${outputNames[2]}"
        },
        expansionExpect(outputNames[3] == listOf("name", "status", "it", "tags")) {
            "direct excluded columns were ${outputNames[3]}"
        },
        expansionExpect(outputNames[4] == listOf("name", "status", "it", "tags")) {
            "literal excluded columns were ${outputNames[4]}"
        },
        expansionExpect(outputNames[5] == listOf("name", "it", "tags")) {
            "collection-literal excluded columns were ${outputNames[5]}"
        },
        expansionExpect(outputNames[6] == listOf("name", "it", "tags")) {
            "list excluded columns were ${outputNames[6]}"
        },
        expansionExpect(outputNames[7] == listOf("id", "name", "status", "it", "tags")) {
            "named source columns were ${outputNames[7]}"
        },
        expansionExpect(outputNames[8] == listOf("name", "it", "tags")) {
            "named excluded columns were ${outputNames[8]}"
        },
        expansionExpect(outputNames[9] == listOf("it")) {
            "property named it columns were ${outputNames[9]}"
        },
        expansionExpect(outputNames[10] == listOf("previousId")) {
            "arithmetic-minus columns were ${outputNames[10]}"
        },
        expansionExpect(outputNames[11] == listOf("shorterLength")) {
            "function-minus columns were ${outputNames[11]}"
        },
        expansionExpect(outputNames[12] == listOf("id", "name", "status", "it", "tags")) {
            "non-column exclusion columns were ${outputNames[12]}"
        },
        expansionExpect(wrapper.mappedClasses.size == clauses.size) {
            "mapped classes were ${wrapper.mappedClasses}"
        },
        expansionExpect(wrapper.mappedClasses.all { it.simpleName?.startsWith("KronosSelectResult_") == true }) {
            "non-projection mapping target was ${wrapper.mappedClasses}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expansionExpect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun SqlQuery.Select.selectedOutputNames(): List<String> =
    select.mapNotNull { item ->
        val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
        expression.metadata?.outputName
            ?: expression.alias
            ?: (expression.expr as? SqlExpr.Column)?.columnName
    }
