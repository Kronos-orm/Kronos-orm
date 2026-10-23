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

// Verifies PSI-backed generated alias, window, and generic fields survive FIR-to-IR and runtime mapping.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.utils.Extensions.mapperTo

@Table("tb_psi_projection")
data class PsiProjectionPerson(
    var id: Int? = null,
    @Serialize var tags: List<List<String?>>? = null,
) : KPojo

class PsiProjectionWrapper : KronosDataSourceWrapper {
    override val url = "jdbc:psi-projection"
    override val userName = ""
    override val dbType = DBType.Mysql

    override fun first(task: KAtomicQueryTask): Any = mapOf(
        "uid" to 7, "rn" to 2, "labels" to listOf(listOf("Ada", null)),
    ).mapperTo(task.targetType)

    override fun toList(task: KAtomicQueryTask): List<Any?> = emptyList()
    override fun update(task: KAtomicActionTask): Int = 0
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?,
    ): Any? = null
}

fun box(): String {
    val wrapper = PsiProjectionWrapper()
    val previous = Kronos.dataSource
    Kronos.dataSource = { wrapper }
    try {
        val query = PsiProjectionPerson().select {
            [
                it.id.alias("uid"),
                it.tags.alias("labels"),
                f.rowNumber().over { orderBy(it.id.asc()) }.alias("rn"),
            ]
        }.orderBy { [it.uid.asc(), it.rn.desc()] }
        val row = query.first(wrapper)
        val uid: Int? = row.uid
        val rank: Int? = row.rn
        val labels: List<List<String?>>? = row.labels
        if (uid != 7 || rank != 2 || labels != listOf(listOf("Ada", null))) {
            return "Fail: mapped uid=$uid/rn=$rank/labels=$labels"
        }
        val statement = query.toSqlQuery() as SqlQuery.Select
        val names = statement.select.map { (it as SqlSelectItem.Expr).metadata?.outputName }
        if (names != listOf("uid", "labels", "rn")) return "Fail: selected fields were $names"
        val ordered = statement.orderBy.map { (it.expr as? SqlExpr.Column)?.columnName }
        if (ordered != listOf("uid", "rn")) return "Fail: ordered columns were $ordered"
        return "OK"
    } finally {
        Kronos.dataSource = previous
    }
}
