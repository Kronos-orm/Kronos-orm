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

// Verifies explicit in/out and star generic projections retain their selected property types.

import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem

interface ProjectionProducer<out T>
interface ProjectionConsumer<in T>

@Table("tb_projection_generic_variance")
data class ProjectionGenericVarianceRow(
    var id: Int? = null,
    @Serialize
    var produced: ProjectionProducer<out String>? = null,
    @Serialize
    var consumed: ProjectionConsumer<in String>? = null,
    @Serialize
    var unknown: ProjectionProducer<*>? = null,
) : KPojo

fun box(): String {
    val clause = ProjectionGenericVarianceRow()
        .select {
            [
                it.produced.alias("producedValue"),
                it.consumed.alias("consumedValue"),
                it.unknown.alias("unknownValue"),
            ]
        }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val row = clause.first()
        val produced: ProjectionProducer<out String>? = row.producedValue
        val consumed: ProjectionConsumer<in String>? = row.consumedValue
        val unknown: ProjectionProducer<*>? = row.unknownValue
        return "Fail: generated values were $produced/$consumed/$unknown"
    }

    val statement = clause.toSqlQuery() as SqlQuery.Select
    val names = statement.select.mapNotNull { item ->
        (item as? SqlSelectItem.Expr)?.metadata?.outputName
    }
    return if (names == listOf("producedValue", "consumedValue", "unknownValue")) {
        "OK"
    } else {
        "Fail: projection names were $names"
    }
}
