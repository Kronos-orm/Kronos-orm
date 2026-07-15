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
import com.kotlinorm.utils.KStack
import com.kotlinorm.utils.LinkedHashSet as KronosLinkedHashSet
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
    val excludedDirectWithAlias = source.select { [it - it.id, it.id.alias("directIid")] }
    val excludedCollectionLiteralWithAlias = source.select { [it - [it.id, it.status], it.id.alias("iid")] }
    val excludedListWithAlias = source.select { [it - listOf(it.id, it.status), it.id.alias("listIid")] }
    val excludedArrayWithAlias = source.select { [it - arrayOf(it.id, it.status), it.id.alias("arrayIid")] }
    val excludedMutableListWithAlias =
        source.select { [it - mutableListOf(it.id, it.status), it.id.alias("mutableIid")] }
    val excludedSetWithAlias = source.select { [it - setOf(it.id, it.status), it.id.alias("setIid")] }
    val excludedLinkedHashSetOfWithAlias =
        source.select { [it - KronosLinkedHashSet.of(it.id, it.status), it.id.alias("linkedSetIid")] }
    val excludedStackOfWithAlias = source.select { [it - KStack.of(it.id, it.status), it.id.alias("stackIid")] }
    val chainedMinusWithAlias = source.select { [it - it.id - it.name, it.name.alias("uname")] }
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
        val directAliasRow = excludedDirectWithAlias.first()
        val directAliasName: String? = directAliasRow.name
        val directAliasStatus: Int? = directAliasRow.status
        val directAliasIid: Int? = directAliasRow.directIid
        val collectionAliasRow = excludedCollectionLiteralWithAlias.first()
        val collectionAliasName: String? = collectionAliasRow.name
        val collectionAliasIid: Int? = collectionAliasRow.iid
        val listAliasRow = excludedListWithAlias.first()
        val listAliasName: String? = listAliasRow.name
        val listAliasIid: Int? = listAliasRow.listIid
        val arrayAliasRow = excludedArrayWithAlias.first()
        val arrayAliasName: String? = arrayAliasRow.name
        val arrayAliasIid: Int? = arrayAliasRow.arrayIid
        val mutableAliasRow = excludedMutableListWithAlias.first()
        val mutableAliasName: String? = mutableAliasRow.name
        val mutableAliasIid: Int? = mutableAliasRow.mutableIid
        val setAliasRow = excludedSetWithAlias.first()
        val setAliasName: String? = setAliasRow.name
        val setAliasIid: Int? = setAliasRow.setIid
        val linkedSetAliasRow = excludedLinkedHashSetOfWithAlias.first()
        val linkedSetAliasName: String? = linkedSetAliasRow.name
        val linkedSetAliasIid: Int? = linkedSetAliasRow.linkedSetIid
        val stackAliasRow = excludedStackOfWithAlias.first()
        val stackAliasName: String? = stackAliasRow.name
        val stackAliasIid: Int? = stackAliasRow.stackIid
        val chainedMinusAliasRow = chainedMinusWithAlias.first()
        val chainedMinusStatus: Int? = chainedMinusAliasRow.status
        val chainedMinusUname: String? = chainedMinusAliasRow.uname
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
            "$collectionExcludedName/$listExcludedName/$directAliasName/$directAliasStatus/$directAliasIid/" +
            "$collectionAliasName/$collectionAliasIid/" +
            "$listAliasName/$listAliasIid/$arrayAliasName/$arrayAliasIid/$mutableAliasName/$mutableAliasIid/" +
            "$setAliasName/$setAliasIid/$linkedSetAliasName/$linkedSetAliasIid/$stackAliasName/$stackAliasIid/" +
            "$chainedMinusStatus/$chainedMinusUname/" +
            "$namedAllId/$namedAllName/$namedAllStatus/$namedExcludedName/" +
            "$selectedIt/$previousId/$shorterLength/$excludedNonColumnId/$excludedNonColumnName"
    }

    val identityClauses = listOf(
        "allDirect" to allDirect,
        "allLiteral" to allLiteral,
        "namedAll" to namedAll,
    )
    val projectionClauses = listOf(
        "allWithAlias" to allWithAlias,
        "excludedDirect" to excludedDirect,
        "excludedLiteral" to excludedLiteral,
        "excludedCollectionLiteral" to excludedCollectionLiteral,
        "excludedList" to excludedList,
        "excludedDirectWithAlias" to excludedDirectWithAlias,
        "excludedCollectionLiteralWithAlias" to excludedCollectionLiteralWithAlias,
        "excludedListWithAlias" to excludedListWithAlias,
        "excludedArrayWithAlias" to excludedArrayWithAlias,
        "excludedMutableListWithAlias" to excludedMutableListWithAlias,
        "excludedSetWithAlias" to excludedSetWithAlias,
        "excludedLinkedHashSetOfWithAlias" to excludedLinkedHashSetOfWithAlias,
        "excludedStackOfWithAlias" to excludedStackOfWithAlias,
        "chainedMinusWithAlias" to chainedMinusWithAlias,
        "namedExcluded" to namedExcluded,
        "propertyNamedIt" to propertyNamedIt,
        "arithmeticMinus" to arithmeticMinus,
        "functionMinus" to functionMinus,
        "excludedNonColumn" to excludedNonColumn
    )
    val clauses = identityClauses + projectionClauses
    val outputNames = clauses.associate { (name, clause) ->
        name to (clause.toSqlQuery() as SqlQuery.Select).selectedOutputNames()
    }
    val wrapper = KPojoExpansionProjectionWrapper()
    projectionClauses.forEach { (_, clause) -> clause.toList(wrapper) }

    val failures = listOfNotNull(
        expansionExpectOutput(outputNames, "allDirect", listOf("id", "name", "status", "it", "tags")),
        expansionExpectOutput(outputNames, "allLiteral", listOf("id", "name", "status", "it", "tags")),
        expansionExpectOutput(outputNames, "allWithAlias", listOf("id", "name", "status", "it", "tags", "xxx")),
        expansionExpectOutput(outputNames, "excludedDirect", listOf("name", "status", "it", "tags")),
        expansionExpectOutput(outputNames, "excludedLiteral", listOf("name", "status", "it", "tags")),
        expansionExpectOutput(outputNames, "excludedCollectionLiteral", listOf("name", "it", "tags")),
        expansionExpectOutput(outputNames, "excludedList", listOf("name", "it", "tags")),
        expansionExpectOutput(outputNames, "excludedDirectWithAlias", listOf("name", "status", "it", "tags", "directIid")),
        expansionExpectOutput(outputNames, "excludedCollectionLiteralWithAlias", listOf("name", "it", "tags", "iid")),
        expansionExpectOutput(outputNames, "excludedListWithAlias", listOf("name", "it", "tags", "listIid")),
        expansionExpectOutput(outputNames, "excludedArrayWithAlias", listOf("name", "it", "tags", "arrayIid")),
        expansionExpectOutput(outputNames, "excludedMutableListWithAlias", listOf("name", "it", "tags", "mutableIid")),
        expansionExpectOutput(outputNames, "excludedSetWithAlias", listOf("name", "it", "tags", "setIid")),
        expansionExpectOutput(outputNames, "excludedLinkedHashSetOfWithAlias", listOf("name", "it", "tags", "linkedSetIid")),
        expansionExpectOutput(outputNames, "excludedStackOfWithAlias", listOf("name", "it", "tags", "stackIid")),
        expansionExpectOutput(outputNames, "chainedMinusWithAlias", listOf("status", "it", "tags", "uname")),
        expansionExpectOutput(outputNames, "namedAll", listOf("id", "name", "status", "it", "tags")),
        expansionExpectOutput(outputNames, "namedExcluded", listOf("name", "it", "tags")),
        expansionExpectOutput(outputNames, "propertyNamedIt", listOf("it")),
        expansionExpectOutput(outputNames, "arithmeticMinus", listOf("previousId")),
        expansionExpectOutput(outputNames, "functionMinus", listOf("shorterLength")),
        expansionExpectOutput(outputNames, "excludedNonColumn", listOf("id", "name", "status", "it", "tags")),
        expansionExpect(wrapper.mappedClasses.size == projectionClauses.size) {
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

fun expansionExpectOutput(
    outputNames: Map<String, List<String>>,
    caseName: String,
    expected: List<String>
): String? =
    expansionExpect(outputNames[caseName] == expected) {
        "$caseName columns were ${outputNames[caseName]}"
    }

fun SqlQuery.Select.selectedOutputNames(): List<String> =
    select.mapNotNull { item ->
        val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
        expression.metadata?.outputName
            ?: expression.alias
            ?: (expression.expr as? SqlExpr.Column)?.columnName
    }
