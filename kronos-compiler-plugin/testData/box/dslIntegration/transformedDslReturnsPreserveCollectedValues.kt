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

// Verifies all side-effect DSL lowerings return without evaluating source getters again.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.beans.dsl.KTableForInsertSelect
import com.kotlinorm.beans.dsl.KTableForReference
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.database.SqlManager
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.types.ToInsertSelect
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSort

var loweredDslFieldReads = 0

@Table("dsl_return_user")
class DslReturnUser(var id: Int? = null) : KPojo {
    var name: String? = null
        get() { loweredDslFieldReads++; return field }
}

@Table("dsl_return_target")
data class DslReturnTarget(@PrimaryKey var id: Int? = null, var name: String? = null) : KPojo

class DslReturnCapture {
    var reads = 0
    val value: String get() { reads++; return "captured" }
    fun read(): String = value
}

fun box(): String {
    Kronos.fieldNamingStrategy = Kronos.lineHumpNamingStrategy
    Kronos.tableNamingStrategy = Kronos.lineHumpNamingStrategy
    val user = DslReturnUser()
    val id = user.__columns.single { it.name == "id" }
    val name = user.__columns.single { it.name == "name" }
    val select: ToSelect<DslReturnUser, Any?> = { it.name }
    val selectUnit: ToSelect<DslReturnUser, Unit> = { [it.id, it.name] }
    val selectTable = KTableForSelect<DslReturnUser>()
    if (select!!(selectTable, user) != null) return "select Any? return"
    if (selectTable.fields != listOf(name)) return "select fields ${selectTable.fields}"
    val selectUnitTable = KTableForSelect<DslReturnUser>()
    if (selectUnit!!(selectUnitTable, user) != Unit) return "select Unit return"
    if (selectUnitTable.fields != listOf(id, name)) return "select Unit fields ${selectUnitTable.fields}"

    val ordinaryCapture = DslReturnCapture()
    val ordinaryUnit: ToSelect<DslReturnUser, Unit> = { ordinaryCapture.read() }
    val ordinaryTable = KTableForSelect<DslReturnUser>()
    if (ordinaryUnit!!(ordinaryTable, user) != Unit) return "Fail: ordinary Unit return"
    if (ordinaryCapture.reads != 1 || ordinaryTable.fields.isNotEmpty()) {
        return "Fail: ordinary Unit callback reads ${ordinaryCapture.reads}, fields ${ordinaryTable.fields}"
    }
    var skipFields = true
    var nestedReturns = 0
    val earlyUnit: ToSelect<DslReturnUser, Unit> = selectFields@{
        run { nestedReturns++; return@run }
        if (skipFields) return@selectFields
        [it.id]
    }
    val earlyTable = KTableForSelect<DslReturnUser>()
    earlyUnit!!(earlyTable, user)
    if (earlyTable.fields.isNotEmpty()) return "Fail: early return collected ${earlyTable.fields}"
    skipFields = false
    earlyUnit(earlyTable, user)
    if (earlyTable.fields != listOf(id) || nestedReturns != 2) {
        return "Fail: Unit return targets produced ${earlyTable.fields}, nested calls $nestedReturns"
    }

    val sort: ToSort<DslReturnUser, Any?> = { it.name.desc() }
    val sortUnit: ToSort<DslReturnUser, Unit> = { [it.id, it.name.desc()] }
    val sortTable = KTableForSort<DslReturnUser>()
    if (sort!!(sortTable, user) != null) return "sort Any? return"
    val sorted = sortTable.sortedItems.map {
        val item = it as KTableForSort.SortItem.FieldItem
        item.field to item.ordering
    }
    if (sorted != listOf(name to SqlOrdering.Desc)) return "sort fields $sorted"
    val sortUnitTable = KTableForSort<DslReturnUser>()
    if (sortUnit!!(sortUnitTable, user) != Unit) return "sort Unit return"
    val sortedUnit = sortUnitTable.sortedItems.map {
        val item = it as KTableForSort.SortItem.FieldItem
        item.field to item.ordering
    }
    if (sortedUnit != listOf(id to SqlOrdering.Asc, name to SqlOrdering.Desc)) return "sort Unit fields $sortedUnit"

    val reference: ToReference<DslReturnUser, Any?> = { it::name }
    val referenceUnit: ToReference<DslReturnUser, Unit> = { [it::id, it::name] }
    val referenceTable = KTableForReference<DslReturnUser>()
    if (reference!!(referenceTable, user) != null) return "reference Any? return"
    if (referenceTable.fields != listOf(name)) return "reference fields ${referenceTable.fields}"
    val referenceUnitTable = KTableForReference<DslReturnUser>()
    if (referenceUnit!!(referenceUnitTable, user) != Unit) return "reference Unit return"
    if (referenceUnitTable.fields != listOf(id, name)) return "reference Unit fields ${referenceUnitTable.fields}"

    val capture = DslReturnCapture()
    val insert: ToInsertSelect<DslReturnUser, Any?> = { capture.read() }
    val insertUnit: ToInsertSelect<DslReturnUser, Unit> = { [it.id, capture.read()] }
    val insertTable = KTableForInsertSelect<DslReturnUser>()
    if (insert!!(insertTable, user) != null) return "insert Any? return"
    if (insertTable.values != listOf("captured")) return "insert values ${insertTable.values}"
    if (capture.reads != 1) return "insert Any? getter reads ${capture.reads}"
    val insertUnitTable = KTableForInsertSelect<DslReturnUser>()
    if (insertUnit!!(insertUnitTable, user) != Unit) return "insert Unit return"
    if (insertUnitTable.values != listOf(id, "captured")) return "insert Unit values ${insertUnitTable.values}"
    if (capture.reads != 2) return "insert Unit getter reads ${capture.reads}"
    val delegatedCapture = DslReturnCapture()
    val delegate: ToInsertSelect<DslReturnUser, String> = { delegatedCapture.read() }
    val forwardingUnit: ToInsertSelect<DslReturnUser, Unit> = { delegate!!(this, it) }
    val forwardingTable = KTableForInsertSelect<DslReturnUser>()
    forwardingUnit!!(forwardingTable, user)
    if (forwardingTable.values != listOf("captured") || delegatedCapture.reads != 1) {
        return "Fail: delegated Unit values ${forwardingTable.values}, reads ${delegatedCapture.reads}"
    }
    var ordinaryInvokeCalls = 0
    val ordinaryFunction: (KTableForInsertSelect<DslReturnUser>, DslReturnUser) -> String = { _, _ ->
        ordinaryInvokeCalls++
        "ordinary"
    }
    val ordinaryInsertUnit: ToInsertSelect<DslReturnUser, Unit> = { ordinaryFunction(this, it) }
    val ordinaryInsertTable = KTableForInsertSelect<DslReturnUser>()
    ordinaryInsertUnit!!(ordinaryInsertTable, user)
    if (ordinaryInsertTable.values != listOf("ordinary") || ordinaryInvokeCalls != 1) {
        return "Fail: ordinary invoke values ${ordinaryInsertTable.values}, calls $ordinaryInvokeCalls"
    }
    if (loweredDslFieldReads != 0) return "source getters were evaluated $loweredDslFieldReads times"

    return verifyRenderedDsl(user, capture)
}

private fun verifyRenderedDsl(user: DslReturnUser, capture: DslReturnCapture): String {
    val wrapper = CompilerTestDataSourceWrapper
    val query = user.select { [it.id, it.name] }.orderBy { it.name.desc() }.build(wrapper).atomicTask
    val expectedSelect = "SELECT `id`, `name` FROM `dsl_return_user` ORDER BY `name` DESC"
    if (query.sql != expectedSelect) return "SELECT SQL ${query.sql}"
    if (query.paramMap != emptyMap<String, Any?>()) return "SELECT parameters ${query.paramMap}"
    val parameters = linkedMapOf<String, Any?>()
    val statement = user.select().insert<DslReturnTarget> { [it.id, capture.read()] }
        .toSqlStatement(parameters, wrapper)
    val rendered = SqlManager.renderStatement(wrapper, statement, parameters)
    val expectedInsert = "INSERT INTO `dsl_return_target` (`id`, `name`) SELECT `id`, :name FROM `dsl_return_user`"
    if (rendered.sql != expectedInsert) return "INSERT SQL ${rendered.sql}"
    if (rendered.parameters != mapOf("name" to "captured")) return "INSERT parameters ${rendered.parameters}"
    if (capture.reads != 3) return "SQL getter reads ${capture.reads}"
    return "OK"
}
