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

// Verifies indirect KPojo interfaces stay field-free while concrete implementations receive bodies and factories.

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.KPojoFactory
import java.security.MessageDigest
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface IndirectKPojo : KPojo

data class IndirectEntity(var id: Int? = null) : IndirectKPojo

fun box(): String {
    val kronosBackingFields = IndirectKPojo::class.java.declaredFields
        .map { it.name }
        .filter { it in KronosBackingFieldNames }
    if (kronosBackingFields.isNotEmpty()) return "Fail: interface Kronos fields were $kronosBackingFields"

    val entity = IndirectEntity(7)
    if (entity.__kType != typeOf<IndirectEntity>()) return "Fail: KType was ${entity.__kType}"
    val columnNames = entity.__columns.map { it.name }
    if ("id" !in columnNames) return "Fail: columns were $columnNames"
    if (entity.toDataMap()["id"] != 7) return "Fail: mapped id was ${entity.toDataMap()["id"]}"

    val provider = generatedProvider()
    var interfaceContributed = false
    var entityFactory: KPojoFactory? = null
    provider.contributeTo(object : GeneratedTypeRegistrar {
        override fun registerKPojo(
            type: KType,
            ownerId: String,
            constructorSignature: String,
            factory: KPojoFactory
        ) {
            if (type == typeOf<IndirectKPojo>()) interfaceContributed = true
            if (type == typeOf<IndirectEntity>()) entityFactory = factory
        }

        override fun registerEnum(type: KType, entryNames: List<String>, factory: EnumFactory) = Unit
    })

    val created = entityFactory?.create(typeOf<IndirectEntity>())
    return when {
        interfaceContributed -> "Fail: interface received a factory"
        created !is IndirectEntity -> "Fail: concrete factory returned $created"
        else -> "OK"
    }
}

private val KronosBackingFieldNames = setOf(
    "__kType",
    "__tableName",
    "__tableComment",
    "__columns",
    "__tableIndexes",
    "__createTime",
    "__updateTime",
    "__logicDelete",
    "__optimisticLock",
)

private fun generatedProvider(): GeneratedTypeProvider {
    val moduleCoordinate = "kronos-test:main"
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(moduleCoordinate.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        .take(16)
    val fqName = "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_$hash"
    return Class.forName(fqName).getDeclaredConstructor().newInstance() as GeneratedTypeProvider
}
