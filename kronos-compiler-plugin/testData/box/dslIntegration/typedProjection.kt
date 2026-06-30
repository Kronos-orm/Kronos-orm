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

// Verifies that typed select projection keeps the source receiver and maps query results to the projection DTO.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import kotlin.reflect.KClass

@Table(name = "tb_projection_source")
data class ProjectionSource(
    var id: Int? = null,
    var name: String? = null,
    var ignored: String? = null,
) : KPojo

data class ProjectionRow(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

data class ProjectionMappingCall(
    val kClass: KClass<*>,
    val isKPojo: Boolean,
    val superTypes: List<String>,
)

class ProjectionRecordingWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:projection"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    var objectCall: ProjectionMappingCall? = null

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> = emptyList()

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> = emptyList()

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? = null

    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): Any? {
        objectCall = ProjectionMappingCall(kClass, isKPojo, superTypes)
        return ProjectionRow(7, "Projected")
    }

    override fun update(task: KAtomicActionTask): Int = 0

    override fun batchUpdate(task: com.kotlinorm.beans.task.KronosAtomicBatchTask): IntArray = intArrayOf()

    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?
    ): Any? = null
}

fun buildProjection(user: ProjectionSource): com.kotlinorm.orm.select.SelectClause<ProjectionSource, ProjectionRow, ProjectionSource> {
    return user.select(ProjectionRow::class) {
        it.ignored
        [it.id, it.name]
    }
}

fun box(): String {
    val wrapper = ProjectionRecordingWrapper()

    Kronos.init {
        dataSource = { wrapper }
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val clause = buildProjection(ProjectionSource())
    val statement = clause.toStatement(wrapper)
    val row: ProjectionRow? = clause.queryOneOrNull(wrapper)
    val call = wrapper.objectCall

    val failures = listOfNotNull(
        expect(statement.selectList.size == 2) { "select size was ${statement.selectList.size}" },
        expect(row?.name == "Projected") { "projection row was $row" },
        expect(call?.kClass == ProjectionRow::class) { "mapping kClass was ${call?.kClass}" },
        expect(call?.isKPojo == true) { "mapping isKPojo was ${call?.isKPojo}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
