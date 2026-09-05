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

// Verifies single-source select forms keep Source as Selected and Context instead of generating projection classes.

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.SelectClause
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.statement.SqlQuery

@Table("tb_identity_projection_user")
data class IdentityProjectionUser(
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    val direct: SelectClause<IdentityProjectionUser, IdentityProjectionUser, IdentityProjectionUser> =
        IdentityProjectionUser().select { it }.orderBy { it.id.desc() }
    val literal: SelectClause<IdentityProjectionUser, IdentityProjectionUser, IdentityProjectionUser> =
        IdentityProjectionUser().select { [it] }.orderBy { it.name.asc() }
    val named: SelectClause<IdentityProjectionUser, IdentityProjectionUser, IdentityProjectionUser> =
        IdentityProjectionUser().select { row -> row }.orderBy { row -> row.id.asc() }
    val list: SelectClause<IdentityProjectionUser, IdentityProjectionUser, IdentityProjectionUser> =
        IdentityProjectionUser().select { listOf<Any?>(it) }.orderBy { it.status.desc() }
    val mutableList: SelectClause<IdentityProjectionUser, IdentityProjectionUser, IdentityProjectionUser> =
        IdentityProjectionUser().select { mutableListOf<Any?>(it) }.orderBy { it.id.asc() }
    val array: SelectClause<IdentityProjectionUser, IdentityProjectionUser, IdentityProjectionUser> =
        IdentityProjectionUser().select { arrayOf<Any?>(it) }.orderBy { it.name.desc() }
    val set: SelectClause<IdentityProjectionUser, IdentityProjectionUser, IdentityProjectionUser> =
        IdentityProjectionUser().select { setOf<Any?>(it) }.orderBy { it.status.asc() }
    val expandedWithAlias = IdentityProjectionUser().select { [it, it.id.alias("uid")] }
    val propertyProjection = IdentityProjectionUser().select { it.id.alias("onlyId") }
    val namedOuterIt: SelectClause<IdentityProjectionUser, IdentityProjectionUser, IdentityProjectionUser> = run {
        val it = IdentityProjectionUser()
        IdentityProjectionUser().select { row -> it }
    }
    val sourceColumns = listOf("id", "name", "status")

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val namedOuterItRow = namedOuterIt.first()
        val namedOuterItId: Int? = namedOuterItRow.id
        val namedOuterItName: String? = namedOuterItRow.name
        val namedOuterItStatus: Int? = namedOuterItRow.status
    }

    val identityClauses = listOf(direct, literal, named, list, mutableList, array, set, namedOuterIt)
    val failures = listOfNotNull(
        expect(identityClauses.all { (it.toSqlQuery() as SqlQuery.Select).selectAliases() == sourceColumns }) {
            "identity aliases were ${identityClauses.map { (it.toSqlQuery() as SqlQuery.Select).selectAliases() }}"
        },
        expect((expandedWithAlias.toSqlQuery() as SqlQuery.Select).selectAliases() == sourceColumns + "uid") {
            "expanded-with-alias aliases were ${(expandedWithAlias.toSqlQuery() as SqlQuery.Select).selectAliases()}"
        },
        expect((propertyProjection.toSqlQuery() as SqlQuery.Select).selectAliases() == listOf("onlyId")) {
            "property projection aliases were ${(propertyProjection.toSqlQuery() as SqlQuery.Select).selectAliases()}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

fun SqlQuery.Select.selectAliases(): List<String?> =
    select.map { item ->
        val expr = item as? com.kotlinorm.syntax.statement.SqlSelectItem.Expr
        expr?.metadata?.outputName ?: expr?.alias
    }

inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
