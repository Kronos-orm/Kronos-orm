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

// Verifies collection literal, listOf, and arrayOf select projection forms preserve fields, aliases, functions, and raw SQL.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_projection_collection_variants")
data class ProjectionCollectionVariantUser(
    var id: Int? = null,
    var name: String? = null,
    var score: Int? = null,
) : KPojo

data class ProjectionCollectionVariantCapture(
    val fields: List<Field>,
    val items: List<SqlSelectItem>,
)

fun ProjectionCollectionVariantUser.collectProjectionVariants(
    block: ToSelect<ProjectionCollectionVariantUser, Any?>
): ProjectionCollectionVariantCapture {
    var result: ProjectionCollectionVariantCapture? = null
    afterSelect {
        block!!(it)
        result = ProjectionCollectionVariantCapture(fields.toList(), selectItems.toList())
    }
    return result ?: error("select block did not run")
}

fun selectExprAt(items: List<SqlSelectItem>, index: Int): SqlSelectItem.Expr? =
    items.getOrNull(index) as? SqlSelectItem.Expr

fun expectProjectionCollectionVariant(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val source = ProjectionCollectionVariantUser()
    val literal = source.collectProjectionVariants {
        [it.id.alias("uid"), f.length(it.name).alias("nameLength"), "count(*)".alias("total")]
    }
    val list = source.collectProjectionVariants {
        listOf<Any?>(it.score, f.length(it.name).alias("listLength"), "now()")
    }
    val array = source.collectProjectionVariants {
        arrayOf<Any?>(it.name.alias("arrayName"), "1".alias("one"), f.length(it.name))
    }

    val literalFunction = selectExprAt(literal.items, 0)
    val literalRaw = selectExprAt(literal.items, 1)
    val listFunction = selectExprAt(list.items, 0)
    val listRaw = selectExprAt(list.items, 1)
    val arrayRaw = selectExprAt(array.items, 0)
    val arrayFunction = selectExprAt(array.items, 1)

    val failures = listOfNotNull(
        expectProjectionCollectionVariant(literal.fields.map { it.name } == listOf("uid")) {
            "literal fields were ${literal.fields.map { it.name }}"
        },
        expectProjectionCollectionVariant((literalFunction?.expr as? SqlExpr.Function)?.name?.last == "LENGTH") {
            "literal function was ${literalFunction?.expr}"
        },
        expectProjectionCollectionVariant(literalFunction?.alias == "nameLength") {
            "literal function alias was ${literalFunction?.alias}"
        },
        expectProjectionCollectionVariant((literalRaw?.expr as? SqlExpr.UnsafeRaw)?.sql == "count(*)") {
            "literal raw expr was ${literalRaw?.expr}"
        },
        expectProjectionCollectionVariant(literalRaw?.alias == "total") { "literal raw alias was ${literalRaw?.alias}" },
        expectProjectionCollectionVariant(list.fields.map { it.name } == listOf("score")) {
            "list fields were ${list.fields.map { it.name }}"
        },
        expectProjectionCollectionVariant((listFunction?.expr as? SqlExpr.Function)?.name?.last == "LENGTH") {
            "list function was ${listFunction?.expr}"
        },
        expectProjectionCollectionVariant(listFunction?.alias == "listLength") {
            "list function alias was ${listFunction?.alias}"
        },
        expectProjectionCollectionVariant((listRaw?.expr as? SqlExpr.UnsafeRaw)?.sql == "now()") {
            "list raw expr was ${listRaw?.expr}"
        },
        expectProjectionCollectionVariant(array.fields.map { it.name } == listOf("arrayName")) {
            "array fields were ${array.fields.map { it.name }}"
        },
        expectProjectionCollectionVariant((arrayRaw?.expr as? SqlExpr.NumberLiteral)?.number == "1") {
            "array raw expr was ${arrayRaw?.expr}"
        },
        expectProjectionCollectionVariant(arrayRaw?.alias == "one") { "array raw alias was ${arrayRaw?.alias}" },
        expectProjectionCollectionVariant((arrayFunction?.expr as? SqlExpr.Function)?.name?.last == "LENGTH") {
            "array function was ${arrayFunction?.expr}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

