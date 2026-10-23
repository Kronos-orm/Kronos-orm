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

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

// Verifies dynamic KTypes stay dynamic while caller-supplied metadata survives direct tasks and typed terminals.

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.database.SqlExecutor
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.utils.StaticTypeRef
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class DynamicTargetWrapper : KronosDataSourceWrapper {
    override val url = "jdbc:dynamic-type-metadata"
    override val userName = ""
    override val dbType = DBType.Mysql
    var latest: KAtomicQueryTask? = null
    override fun toList(task: KAtomicQueryTask): List<Any?> { latest = task; return listOf("value") }
    override fun first(task: KAtomicQueryTask): Any? { latest = task; return "value" }
    override fun update(task: KAtomicActionTask): Int = 0
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
    override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? = null
}

fun runtimeType(): KType = typeOf<String>()

inline fun <reified T> genericTask() = KronosAtomicQueryTask(sql = "SELECT value", targetType = typeOf<T>())

inline fun <reified T> genericNestedTask() =
    KronosAtomicQueryTask(sql = "SELECT value", targetType = typeOf<List<T>>())

inline fun <reified T> genericQuery(wrapper: DynamicTargetWrapper): List<T> =
    with(SqlExecutor) { wrapper.queryList<T>("SELECT value") }

fun box(): String {
    val explicit = StaticTypeInfo(StaticTypeRef("kotlin.String", nullable = false))
    val direct = KronosAtomicQueryTask(sql = "SELECT value", targetType = typeOf<String>(), targetTypeInfo = explicit)
    if (direct.targetTypeInfo !== explicit) return "Fail: direct explicit metadata was ${direct.targetTypeInfo}"
    val dynamic = KronosAtomicQueryTask(sql = "SELECT value", targetType = runtimeType())
    if (dynamic.targetTypeInfo != null) return "Fail: dynamic direct metadata was ${dynamic.targetTypeInfo}"
    if (dynamic.targetType != typeOf<String>()) return "Fail: dynamic target was ${dynamic.targetType}"
    val generic = genericTask<String>()
    if (generic.targetTypeInfo != null) return "Fail: generic declaration captured metadata ${generic.targetTypeInfo}"

    val nestedGeneric = genericNestedTask<String>()
    if (nestedGeneric.targetTypeInfo != null) return "Fail: nested generic metadata was ${nestedGeneric.targetTypeInfo}"

    val wrapper = DynamicTargetWrapper()
    val genericRows = genericQuery<String>(wrapper)
    if (genericRows != listOf("value")) return "Fail: generic rows were $genericRows"
    if (wrapper.latest?.targetTypeInfo != null) return "Fail: generic terminal metadata was ${wrapper.latest?.targetTypeInfo}"
    with(SqlExecutor) {
        val dynamicRows = wrapper.queryList<Any>("SELECT value", targetType = runtimeType())
        if (dynamicRows != listOf("value")) return "Fail: dynamic rows were $dynamicRows"
        if (wrapper.latest?.targetTypeInfo != null) return "Fail: dynamic terminal metadata was ${wrapper.latest?.targetTypeInfo}"
        if (wrapper.latest?.targetType != typeOf<String>()) return "Fail: dynamic terminal target was ${wrapper.latest?.targetType}"
        val capturedType = runtimeType()
        wrapper.queryList<Any>("SELECT value", targetType = capturedType)
        if (wrapper.latest?.targetTypeInfo != null) return "Fail: captured terminal metadata was ${wrapper.latest?.targetTypeInfo}"
        val explicitRows = wrapper.queryList<String>("SELECT value", targetTypeInfo = explicit)
        if (explicitRows != listOf("value")) return "Fail: explicit rows were $explicitRows"
        if (wrapper.latest?.targetTypeInfo !== explicit) return "Fail: terminal replaced caller metadata ${wrapper.latest?.targetTypeInfo}"
        val staticRows = wrapper.queryList<Any>("SELECT value", targetType = typeOf<String>())
        if (staticRows != listOf("value")) return "Fail: static rows were $staticRows"
        if (wrapper.latest?.targetTypeInfo?.type != StaticTypeRef("kotlin.String", nullable = false)) {
            return "Fail: explicit static type metadata was ${wrapper.latest?.targetTypeInfo}"
        }
    }
    return "OK"
}
