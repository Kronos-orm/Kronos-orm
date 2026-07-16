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

// Verifies generated and derived projection fields retain source @Serialize metadata.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.createInstance
import kotlin.reflect.KClass

@Table("tb_serialized_projection_metadata")
data class SerializedProjectionMetadataUser(
    var id: Int? = null,
    @Serialize
    var tags: List<String?>? = null,
) : KPojo

class SerializedProjectionMetadataWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:serialized-projection-metadata"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    val mappedClasses = mutableListOf<KClass<out KPojo>>()

    @Suppress("UNCHECKED_CAST")
    override fun toList(task: KAtomicQueryTask): List<Any?> {
        mappedClasses += task.targetType.classifier as KClass<out KPojo>
        return emptyList()
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

    val projected = SerializedProjectionMetadataUser().select { [it.id, it.tags] }
    val aliased = SerializedProjectionMetadataUser().select { [it.id, it.tags.alias("labels")] }
    val derived = projected.select()
    val wrapper = SerializedProjectionMetadataWrapper()

    projected.toList(wrapper)
    aliased.toList(wrapper)
    derived.toList(wrapper)

    val projectedField = wrapper.mappedClasses[0].projectionField("tags")
    val aliasedField = wrapper.mappedClasses[1].projectionField("labels")
    val derivedField = wrapper.mappedClasses[2].projectionField("tags")

    return when {
        !projectedField.serializable -> "Fail: projected tags serializable was false"
        !aliasedField.serializable -> "Fail: aliased labels serializable was false"
        !derivedField.serializable -> "Fail: derived tags serializable was false"
        else -> "OK"
    }
}

fun KClass<out KPojo>.projectionField(name: String) =
    createInstance().__columns.single { it.name == name }
