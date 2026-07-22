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

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

// Verifies function-local projection DTOs can be used by select(...).toList() mapping.

import com.kotlinorm.Kronos
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
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.Extensions.mapperTo
import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.KPojoFactory
import java.util.ServiceLoader
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Table("tb_local_projection_source")
data class LocalProjectionSource(
    var id: Int? = null,
    var name: String? = null,
    var ignored: String? = null,
) : KPojo

class LocalProjectionWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:local-projection"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql
    var mappedType: KType? = null

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        mappedType = task.targetType
        return [mapOf("id" to 11, "name" to "Local").mapperTo(task.targetType)]
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
    data class LocalProjectionRow(
        var id: Int? = null,
        var name: String? = null,
    ) : KPojo

    val capturedMarker = "captured"
    data class CapturedProjectionRow(
        var id: Int? = null,
        var marker: String = capturedMarker,
    ) : KPojo

    val wrapper = LocalProjectionWrapper()

    with(Kronos) {
        dataSource = { wrapper }
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val rows = LocalProjectionSource()
        .select<LocalProjectionSource, LocalProjectionRow>(typeOf<LocalProjectionRow>()) { [it.id, it.name] }
        .toList(wrapper)
    val row = rows.singleOrNull()

    val contributedOwnerIds = linkedSetOf<String>()
    ServiceLoader.load(GeneratedTypeProvider::class.java).forEach { provider ->
        provider.contributeTo(object : GeneratedTypeRegistrar {
            override fun registerKPojo(
                type: KType,
                ownerId: String,
                constructorSignature: String,
                factory: KPojoFactory,
            ) {
                contributedOwnerIds += ownerId
            }

            override fun registerEnum(
                type: KType,
                entryNames: List<String>,
                factory: EnumFactory,
            ) = Unit
        })
    }

    val failures = listOfNotNull(
        expect(wrapper.mappedType == typeOf<LocalProjectionRow>()) { "mapped type was ${wrapper.mappedType}" },
        expect(row is LocalProjectionRow) { "row type was ${row?.let { it::class }}" },
        expect(row?.id == 11) { "row id was ${row?.id}" },
        expect(row?.name == "Local") { "row name was ${row?.name}" },
        expect(contributedOwnerIds.any { it.endsWith(".LocalProjectionRow") }) {
            "zero-capture local owner was not contributed: $contributedOwnerIds"
        },
        expect(contributedOwnerIds.none { it.endsWith(".CapturedProjectionRow") }) {
            "captured local owner was contributed: $contributedOwnerIds"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
