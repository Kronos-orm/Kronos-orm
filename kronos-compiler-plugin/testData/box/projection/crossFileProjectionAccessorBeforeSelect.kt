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

// Verifies projection getters visited in a caller file retain their originating select's metadata and runtime mapping.

// FILE: caller.kt
fun box(): String {
    val wrapper = CrossFileProjectionWrapper()
    val row = crossFileSelection().first(wrapper)
    val renamed: String? = row.renamed
    val id: Int? = row.id
    if (renamed != "Ada" || id != 7) return "Fail: projected values were $id/$renamed"
    val names = row.__columns.map { it.name }
    if (names != listOf("id", "renamed")) return "Fail: projected columns were $names"
    val data = row.toDataMap()
    if (data != mapOf("id" to 7, "renamed" to "Ada")) return "Fail: projected mapping was $data"
    if (row.__tableName != "cross_file_person") return "Fail: table was ${row.__tableName}"
    return "OK"
}

// FILE: selection.kt
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("cross_file_person")
data class CrossFilePerson(var id: Int? = null, var name: String? = null) : KPojo

fun crossFileSelection() = CrossFilePerson().select { [it.id, it.name.alias("renamed")] }

// FILE: wrapper.kt
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.Extensions.mapperTo

class CrossFileProjectionWrapper : KronosDataSourceWrapper {
    override val url = "jdbc:cross-file-projection"
    override val userName = ""
    override val dbType = DBType.Mysql
    override fun first(task: KAtomicQueryTask): Any? =
        mapOf("id" to 7, "renamed" to "Ada").mapperTo(task.targetType)
    override fun toList(task: KAtomicQueryTask): List<Any?> = emptyList()
    override fun update(task: KAtomicActionTask): Int = 0
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
    override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? = null
}
