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

// Verifies insert-select target filtering is reflected in the generated insert statement.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.statement.SqlDmlStatement

@Table("tb_insert_filter_child")
data class InsertFilterChild(
    var id: Int? = null,
    var label: String? = null,
) : KPojo

@Table("tb_insert_filter_source")
data class InsertFilterSource(
    var userId: Int? = null,
    var payload: String? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_insert_filter_target")
data class InsertFilterTarget(
    @PrimaryKey(identity = true)
    @Serialize
    var id: Int? = null,
    var userId: Int? = null,
    @Serialize
    var payload: String? = null,
    var status: Int? = null,
    @Ignore
    var ignored: String? = null,
    @Cascade(["userId"], ["id"])
    var child: InsertFilterChild? = null,
    var nested: InsertFilterChild? = null,
    var children: List<InsertFilterChild> = emptyList(),
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val statement = InsertFilterSource()
        .select { [it.userId, it.payload, it.status] }
        .insert<InsertFilterTarget> { [it.userId, it.payload, it.status] }
        .toSqlStatement()

    val failures = listOfNotNull(
        expect(statement.table.name == "tb_insert_filter_target") { "target table was ${statement.table}" },
        expect(statement.targetColumns() == listOf("user_id", "payload", "status")) {
            "target columns were ${statement.targetColumns()}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

fun SqlDmlStatement.Insert.targetColumns(): List<String> = columns.map { it.last }

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
