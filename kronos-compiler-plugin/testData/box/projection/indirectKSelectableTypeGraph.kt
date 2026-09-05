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

// Verifies two generic KSelectable bounds work as projection and scalar-subquery receivers.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.SelectClause
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import kotlin.reflect.typeOf

@Table("tb_indirect_selectable_user")
data class IndirectSelectableUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

data class IndirectSelectableScalarRow(
    var id: Int? = null,
    var lastId: Int? = null,
) : KPojo

private fun <Base, S> generatedProjection(source: S)
    where Base : KSelectable<IndirectSelectableUser>, S : Base =
        source.select { [it.id, it.name.alias("renamed")] }

private fun <Base, S> scalarProjection(source: S)
    where Base : KSelectable<IndirectSelectableUser>, S : Base =
        IndirectSelectableUser().select<IndirectSelectableUser, IndirectSelectableScalarRow>(
            typeOf<IndirectSelectableScalarRow>()
        ) {
            [it.id, source.alias("lastId")]
        }

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val fullSource = IndirectSelectableUser().select()
    val scalarSource = IndirectSelectableUser().select<IndirectSelectableUser, IndirectSelectableUser>(
        typeOf<IndirectSelectableUser>()
    ) { it.id }
    val generated = generatedProjection<
        KSelectable<IndirectSelectableUser>,
        SelectClause<IndirectSelectableUser, IndirectSelectableUser, IndirectSelectableUser>
    >(fullSource)
    val scalar = scalarProjection<
        KSelectable<IndirectSelectableUser>,
        SelectClause<IndirectSelectableUser, IndirectSelectableUser, IndirectSelectableUser>
    >(scalarSource)

    val generatedStatement = generated.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val scalarStatement = scalar.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val scalarItems = scalarStatement.select.filterIsInstance<SqlSelectItem.Expr>()
    val failures = listOfNotNull(
        expect(generatedStatement.outputNames() == listOf("id", "renamed")) {
            "generated outputs were ${generatedStatement.outputNames()}"
        },
        expect(scalarStatement.outputNames() == listOf("id", "lastId")) {
            "scalar outputs were ${scalarStatement.outputNames()}"
        },
        expect(scalarItems.map { it.expr is SqlExpr.Subquery } == listOf(false, true)) {
            "scalar expression kinds were ${scalarItems.map { it.expr::class.qualifiedName }}"
        },
    )
    return failures.firstOrNull() ?: "OK"
}

private fun SqlQuery.Select.outputNames(): List<String> = select.mapNotNull { item ->
    val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
    expression.metadata?.outputName
        ?: expression.alias
        ?: (expression.expr as? SqlExpr.Column)?.columnName
}

private inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
