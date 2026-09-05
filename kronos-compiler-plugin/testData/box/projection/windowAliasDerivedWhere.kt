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

// Verifies a generated window alias remains FIR-visible in a derived query where receiver.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_projection_window_alias_derived")
data class ProjectionWindowAliasDerivedRow(
    var id: Int? = null,
    var age: Long? = null,
    var createdAt: Long? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val ranked = ProjectionWindowAliasDerivedRow()
        .select {
            [
                it.id,
                it.age,
                f.rowNumber()
                    .over {
                        partitionBy(it.age)
                        orderBy(it.createdAt.desc())
                    }
                    .alias("rn")
            ]
        }

    val statement = ranked
        .select()
        .where { it.rn == 1 }
        .toSqlQuery() as SqlQuery.Select

    val source = statement.from.singleOrNull() as? SqlTable.Subquery
        ?: return "Fail: outer source was ${statement.from}"
    val inner = source.query as? SqlQuery.Select
        ?: return "Fail: inner query was ${source.query}"
    val where = statement.where as? SqlExpr.Binary
        ?: return "Fail: outer where was ${statement.where}"
    val whereColumn = where.left as? SqlExpr.Column
        ?: return "Fail: outer where left was ${where.left}"
    val whereParameter = (where.right as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named
        ?: return "Fail: outer where right was ${where.right}"
    val innerAliases = inner.select.map { (it as? SqlSelectItem.Expr)?.metadata?.outputName }

    return when {
        "rn" !in innerAliases -> "Fail: inner aliases were $innerAliases"
        where.operator != SqlBinaryOperator.Equal -> "Fail: outer where operator was ${where.operator}"
        whereColumn.tableName != "q" -> "Fail: outer where table was ${whereColumn.tableName}"
        whereColumn.columnName != "rn" -> "Fail: outer where column was ${whereColumn.columnName}"
        whereParameter.name != "rn" -> "Fail: outer where parameter was $whereParameter"
        else -> "OK"
    }
}
