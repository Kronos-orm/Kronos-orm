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

// Verifies explicit KPojo metadata overrides are excluded from whole-source projection fields.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.utils.Extensions.mapperTo
import kotlin.reflect.KClass

class MetadataOverrideProjectionSource(
    var id: Int? = null,
    var name: String? = null,
) : KPojo {
    @Ignore([IgnoreAction.ALL])
    override var __kClass: KClass<out KPojo> = KPojo::class
    @Ignore([IgnoreAction.ALL])
    override var __tableName: String = "runtime_projection_user"
    @Ignore([IgnoreAction.ALL])
    override var __tableComment: String = "runtime source"
    @Ignore([IgnoreAction.ALL])
    override var __columns: MutableList<Field> = mutableListOf(Field("id", "id"), Field("name", "name"))
    @Ignore([IgnoreAction.ALL])
    override var __tableIndexes: MutableList<KTableIndex> = mutableListOf(KTableIndex("idx_runtime_name", arrayOf("name")))
    @Ignore([IgnoreAction.ALL])
    override var __createTime: KronosCommonStrategy = Kronos.createTimeStrategy
    @Ignore([IgnoreAction.ALL])
    override var __updateTime: KronosCommonStrategy = Kronos.updateTimeStrategy
    @Ignore([IgnoreAction.ALL])
    override var __logicDelete: KronosCommonStrategy = Kronos.logicDeleteStrategy
    @Ignore([IgnoreAction.ALL])
    override var __optimisticLock: KronosCommonStrategy = Kronos.optimisticLockStrategy
}

@Table("tb_static_projection_metadata")
data class StaticProjectionMetadataSource(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

class MetadataOverrideProjectionWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:metadata-override-projection"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val mappedClasses = mutableListOf<KClass<*>>()

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        val kClass = task.targetType.classifier as? KClass<*> ?: return emptyList()
        mappedClasses.add(kClass)
        return [mapOf("id" to 5, "name" to "Ada").mapperTo(kClass as KClass<out KPojo>)]
    }

    override fun first(task: KAtomicQueryTask): Any? = null

    override fun update(task: KAtomicActionTask): Int = 0

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()

    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?
    ): Any? = null
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val source = MetadataOverrideProjectionSource()
    val clause = source.select { it }
    val sqlOutputNames = (clause.toSqlQuery() as SqlQuery.Select).selectedOutputNames()
    val wrapper = MetadataOverrideProjectionWrapper()
    val row = clause.toList(wrapper).singleOrNull()
    val generatedFieldNames = row?.__columns.orEmpty().map { it.name }
    val metadataNames = listOf(
        "__kClass",
        "__tableName",
        "__tableComment",
        "__columns",
        "__tableIndexes",
        "__createTime",
        "__updateTime",
        "__logicDelete",
        "__optimisticLock"
    )
    val leakedSqlNames = sqlOutputNames.filter { it in metadataNames }
    val leakedGeneratedNames = generatedFieldNames.filter { it in metadataNames }
    val staticSource = StaticProjectionMetadataSource()

    val failures = listOfNotNull(
        metadataProjectionExpect(sqlOutputNames == listOf("id", "name")) {
            "SQL output names were $sqlOutputNames"
        },
        metadataProjectionExpect(generatedFieldNames == listOf("id", "name")) {
            "generated projection fields were $generatedFieldNames"
        },
        metadataProjectionExpect(leakedSqlNames.isEmpty()) {
            "metadata SQL names leaked as $leakedSqlNames"
        },
        metadataProjectionExpect(leakedGeneratedNames.isEmpty()) {
            "metadata generated names leaked as $leakedGeneratedNames"
        },
        metadataProjectionExpect(row?.id == 5) {
            "mapped id was ${row?.id}"
        },
        metadataProjectionExpect(row?.name == "Ada") {
            "mapped name was ${row?.name}"
        },
        metadataProjectionExpect(staticSource.__kClass == StaticProjectionMetadataSource::class) {
            "static __kClass was ${staticSource.__kClass}"
        },
        metadataProjectionExpect(staticSource.__tableName == "tb_static_projection_metadata") {
            "static __tableName was ${staticSource.__tableName}"
        },
        metadataProjectionExpect(staticSource.__columns.map { it.name } == listOf("id", "name")) {
            "static __columns were ${staticSource.__columns.map { it.name }}"
        },
        metadataProjectionExpect(wrapper.mappedClasses.singleOrNull()?.simpleName?.startsWith("KronosSelectResult_") == true) {
            "mapped class was ${wrapper.mappedClasses.singleOrNull()}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun metadataProjectionExpect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun SqlQuery.Select.selectedOutputNames(): List<String> =
    select.mapNotNull { item ->
        val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
        expression.metadata?.outputName
            ?: expression.alias
            ?: (expression.expr as? SqlExpr.Column)?.columnName
    }
