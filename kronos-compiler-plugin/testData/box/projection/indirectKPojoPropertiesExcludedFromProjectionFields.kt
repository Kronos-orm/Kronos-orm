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

// Verifies indirect KPojo properties are excluded from whole-source projection fields.

import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem

interface IndirectProjectionKPojo : KPojo

data class IndirectProjectionRelation(
    var id: Int? = null,
) : IndirectProjectionKPojo

data class IndirectProjectionSource(
    var id: Int? = null,
    var name: String? = null,
    var relation: IndirectProjectionRelation? = null,
    var relations: List<IndirectProjectionRelation> = emptyList(),
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val clause = IndirectProjectionSource().select { [it, it.id.alias("sourceId")] }
    val outputNames = (clause.toSqlQuery() as SqlQuery.Select).indirectProjectionOutputNames()
    val expected = listOf("id", "name", "sourceId")

    return if (outputNames == expected) "OK" else "Fail: projection fields were $outputNames"
}

fun SqlQuery.Select.indirectProjectionOutputNames(): List<String> =
    select.mapNotNull { item ->
        val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
        expression.metadata?.outputName
            ?: expression.alias
            ?: (expression.expr as? SqlExpr.Column)?.columnName
    }
