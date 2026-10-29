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

// Verifies Native-mode projection classes use a real source anchor and retain factories, aliases, and decoding.

// FILE: projectionMetadataAnchor.kt
package com.kotlinorm.generated.projection

@Suppress("ClassNaming")
internal object installNativeMetadataProjectionMetadataAnchor

// FILE: nativeEntry.kt
@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)
package fixture

import com.kotlinorm.utils.GeneratedTypeMetadataRegistry
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.generatedTypeMetadataRegistry

@Suppress("ClassNaming")
private class installNativeMetadataProvider : GeneratedTypeProvider {
    override val id = "source-placeholder"
    override fun contributeTo(registrar: GeneratedTypeRegistrar) {}
}

fun installNativeMetadata(registry: GeneratedTypeMetadataRegistry) {
    registry.register(installNativeMetadataProvider())
}

fun installNativeMetadataDependencies(
    registry: GeneratedTypeMetadataRegistry = generatedTypeMetadataRegistry
) { installNativeMetadata(registry) }

// FILE: projectionQuery.kt
@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)
package fixture

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.filter
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.utils.Extensions.safeMapperTo
import com.kotlinorm.utils.initializeKronosMetadata
import kotlin.reflect.KClass

@Table("native_projection_source")
data class NativeProjectionSource(var id: Int? = null, var name: String? = null) : KPojo

class NativeProjectionWrapper : KronosDataSourceWrapper {
    override val url = "native-projection:test"
    override val userName = ""
    override val dbType = DBType.Mysql
    var mappedTask: KAtomicQueryTask? = null

    override fun first(task: KAtomicQueryTask): Any {
        mappedTask = task
        return mapOf("uid" to 7L, "displayName" to "Native projection").safeMapperTo(task.targetType)
    }

    override fun toList(task: KAtomicQueryTask): List<Any?> = error("Unexpected list query")
    override fun update(task: KAtomicActionTask): Int = error("Unexpected update")
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = error("Unexpected batch update")
    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?
    ): Any? = error("Unexpected transaction")
}

fun box(): String {
    initializeKronosMetadata()
    val selected = NativeProjectionSource()
        .select { [it.id.alias("uid"), it.name.alias("displayName")] }
        .filter { it.uid == 7 }
    val statement = selected.toSqlQuery() as SqlQuery.Select
    val source = statement.from.singleOrNull() as? SqlTable.Subquery
        ?: return "Fail: projection source was ${statement.from}"
    val inner = source.query as? SqlQuery.Select
        ?: return "Fail: inner query was ${source.query}"
    val outputNames = inner.select.map { (it as? SqlSelectItem.Expr)?.metadata?.outputName }
    if (outputNames != listOf("uid", "displayName")) return "Fail: aliases were $outputNames"
    val predicate = statement.where as? SqlExpr.Binary
        ?: return "Fail: projection predicate was ${statement.where}"
    val column = predicate.left as? SqlExpr.Column
        ?: return "Fail: projection predicate column was ${predicate.left}"
    if (column.columnName != "uid" || predicate.operator != SqlBinaryOperator.Equal) {
        return "Fail: aliased predicate was $predicate"
    }

    val wrapper = NativeProjectionWrapper()
    val row = selected.first(wrapper)
    if (row.uid != 7 || row.displayName != "Native projection") {
        return "Fail: generated projection decoded to ${row.toDataMap()}"
    }
    val fields = row.__columns.map { it.name }
    if (fields != outputNames) return "Fail: generated field metadata was $fields"
    val task = wrapper.mappedTask ?: return "Fail: typed query did not execute"
    val className = (task.targetType.classifier as? KClass<*>)?.qualifiedName
    if (className?.startsWith("com.kotlinorm.generated.projection.KronosSelectResult_") != true) {
        return "Fail: projection target type was ${task.targetType}"
    }
    if (task.paramMap.values.toList() != listOf(7)) return "Fail: filter parameters were ${task.paramMap}"
    return "OK"
}
