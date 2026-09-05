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

// Verifies raw nested JOIN operands retain distinct left/right AST shapes and generated Selected fields.

import com.kotlinorm.annotations.Table
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_nested_join_a")
data class NestedJoinA(
    var id: Int? = null,
    var bId: Int? = null,
) : KPojo

@Table("tb_nested_join_b")
data class NestedJoinB(
    var id: Int? = null,
    var aId: Int? = null,
    var cId: Int? = null,
    var label: String? = null,
) : KPojo

@Table("tb_nested_join_c")
data class NestedJoinC(
    var id: Int? = null,
    var label: String? = null,
) : KPojo

fun box(): String {
    val rightRaw = NestedJoinB().join(NestedJoinC()) { b, c ->
        innerJoin { b.cId == c.id }
    }
    val rightNested = NestedJoinA().join(rightRaw) { a, b, c ->
        leftJoin { a.bId == b.id }
            .select { [a.id, b.label.alias("bLabel"), c.label.alias("cLabel")] }
    }

    val leftRaw = NestedJoinA().join(NestedJoinB()) { a, b ->
        innerJoin { a.bId == b.id }
    }
    val leftNested = leftRaw.join(NestedJoinC()) { a, b, c ->
        rightJoin { b.cId == c.id }
            .select { [a.id, b.label.alias("bLabel"), c.label.alias("cLabel")] }
    }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val row = rightNested.first()
        val id: Int? = row.id
        val bLabel: String? = row.bLabel
        val cLabel: String? = row.cLabel
        return "Fail: selected values unexpectedly evaluated as $id/$bLabel/$cLabel"
    }

    val rightRoot = (rightNested.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select)
        .from.singleOrNull() as? SqlTable.Join
    val leftRoot = (leftNested.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select)
        .from.singleOrNull() as? SqlTable.Join
    val failures = listOfNotNull(
        expect(rightRoot != null) { "right-nested root was not a JOIN" },
        expect(rightRoot?.joinType == SqlJoinType.Left) { "right root type was ${rightRoot?.joinType}" },
        expect(rightRoot?.left is SqlTable.Ident) { "right root left was ${rightRoot?.left}" },
        expect(rightRoot?.right is SqlTable.Join) { "right root right was ${rightRoot?.right}" },
        expect(leftRoot != null) { "left-nested root was not a JOIN" },
        expect(leftRoot?.joinType == SqlJoinType.Right) { "left root type was ${leftRoot?.joinType}" },
        expect(leftRoot?.left is SqlTable.Join) { "left root left was ${leftRoot?.left}" },
        expect(leftRoot?.right is SqlTable.Ident) { "left root right was ${leftRoot?.right}" },
    )
    return failures.firstOrNull() ?: "OK"
}

private inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
