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

// Verifies scalar subquery alias receiver type variants refine generated projection types.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.utils.Extensions.mapperTo
import kotlin.reflect.KClass

@Table("tb_projection_scalar_variant_user")
data class ProjectionScalarVariantUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_projection_scalar_variant_order")
data class ProjectionScalarVariantOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

class ProjectionScalarVariantWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:projection-scalar-variant"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val mappedClasses = mutableListOf<KClass<*>>()

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>,
    ): List<Any> {
        mappedClasses += kClass
        return listOf(row(1).mapperTo(kClass as KClass<out KPojo>))
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? = null

    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>,
    ): Any {
        mappedClasses += kClass
        return row(mappedClasses.size).mapperTo(kClass as KClass<out KPojo>)
    }

    override fun update(task: KAtomicActionTask): Int = 0

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()

    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?,
    ): Any? = null
}

fun row(id: Int): Map<String, Any> =
    mapOf("id" to id, "lastAmount" to (100 + id), "latestAmount" to (200 + id), "listAmount" to (300 + id))

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }
    val wrapper = ProjectionScalarVariantWrapper()

    val direct = ProjectionScalarVariantUser()
        .select {
            ProjectionScalarVariantOrder()
                .select { order -> order.amount }
                .limit(1)
                .alias("lastAmount")
        }
    val collection = ProjectionScalarVariantUser()
        .select {
            [
                it.id,
                ProjectionScalarVariantOrder()
                    .select { order -> order.amount }
                    .limit(1)
                    .alias("latestAmount")
            ]
        }
    val list = ProjectionScalarVariantUser()
        .select {
            listOf(
                it.id,
                ProjectionScalarVariantOrder()
                    .select { order -> order.amount }
                    .limit(1)
                    .alias("listAmount")
            )
        }

    val directRow = direct.queryOne(wrapper)
    val collectionRow = collection.queryOne(wrapper)
    val listRow = list.queryOne(wrapper)

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val directValue: Int? = directRow.lastAmount
        val collectionValue: Int? = collectionRow.latestAmount
        val listValue: Int? = listRow.listAmount
        return "Fail: unreachable values $directValue $collectionValue $listValue"
    }

    val directStatement = direct.toSqlQuery() as SqlQuery.Select
    val collectionStatement = collection.toSqlQuery() as SqlQuery.Select
    val listStatement = list.toSqlQuery() as SqlQuery.Select

    val failures = listOfNotNull(
        expect(directRow.kronosColumns().map { it.name } == listOf("lastAmount")) {
            "direct fields were ${directRow.kronosColumns().map { it.name }}"
        },
        expect(collectionRow.kronosColumns().map { it.name }.toSet() == setOf("id", "latestAmount")) {
            "collection fields were ${collectionRow.kronosColumns().map { it.name }}"
        },
        expect(listRow.kronosColumns().map { it.name }.toSet() == setOf("id", "listAmount")) {
            "list fields were ${listRow.kronosColumns().map { it.name }}"
        },
        expect(directStatement.hasScalarAlias("lastAmount")) { "direct aliases were ${directStatement.aliases()}" },
        expect(collectionStatement.hasScalarAlias("latestAmount")) { "collection aliases were ${collectionStatement.aliases()}" },
        expect(listStatement.hasScalarAlias("listAmount")) { "list aliases were ${listStatement.aliases()}" },
        expect(wrapper.mappedClasses.size == 3) { "mapped classes were ${wrapper.mappedClasses}" },
        expect(wrapper.mappedClasses.all { it.simpleName?.startsWith("KronosSelectResult_") == true }) {
            "mapped classes were ${wrapper.mappedClasses}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun SqlQuery.Select.hasScalarAlias(alias: String): Boolean {
    return allSelects().any { statement ->
        statement.select.any { item ->
            item is SqlSelectItem.Expr &&
                item.alias == alias &&
                item.expr is SqlExpr.Subquery &&
                item.metadata?.outputName == alias
        }
    }
}

fun SqlQuery.Select.aliases(): List<String?> =
    allSelects().flatMap { statement -> statement.select.map { (it as? SqlSelectItem.Expr)?.alias } }

fun SqlQuery.Select.allSelects(): List<SqlQuery.Select> {
    val nested = ((from.singleOrNull() as? SqlTable.Subquery)?.query as? SqlQuery.Select)?.allSelects().orEmpty()
    return listOf(this) + nested
}
