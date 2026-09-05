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

// Verifies nested generic source aliases retain their generated Kotlin type.

import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UnsafeProjectionOverride
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem

@Table("tb_projection_generic_source_alias")
data class GenericSourceAliasProjectionUser(
    var id: Int? = null,
    @Serialize
    var matrix: List<List<String>>? = null,
) : KPojo

@OptIn(UnsafeProjectionOverride::class)
fun box(): String {
    val clause = GenericSourceAliasProjectionUser()
        .select { [it.matrix.alias("matrix")] }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val selectedMatrix: List<List<String>>? = clause.first().matrix
        return "Fail: selected matrix was $selectedMatrix"
    }

    val statement = clause.toSqlQuery() as SqlQuery.Select
    val aliases = statement.select.mapNotNull { item ->
        val expression = item as? SqlSelectItem.Expr
        expression?.metadata?.outputName ?: expression?.alias
    }
    return if (aliases == listOf("matrix")) {
        "OK"
    } else {
        "Fail: selected aliases were $aliases"
    }
}
