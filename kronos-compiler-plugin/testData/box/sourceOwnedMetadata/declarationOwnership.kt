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

// MODULE: library
// FILE: status.kt
package source.library

enum class Status { READY, DONE }
object Envelope { enum class NestedStatus { NESTED } }

// MODULE: main(library)
// FILE: entity.kt
package source.consumer

import com.kotlinorm.interfaces.KPojo
import source.library.Status

interface GenericOwner<out T>
data class Record(var status: Status = Status.READY) : KPojo, GenericOwner<Status>

// FILE: query.kt
@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)
package source.consumer

import com.kotlinorm.Kronos
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.ResultColumnMetadata
import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.KPojoFactory
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.utils.StaticTypeVariance
import com.kotlinorm.utils.decodeDatabaseValue
import com.kotlinorm.utils.initializeKronosMetadata
import source.library.Status
import source.library.Envelope
import java.util.ServiceLoader
import kotlin.reflect.KType
import kotlin.reflect.typeOf

val statusQuery = KronosAtomicQueryTask("SELECT status", targetType = typeOf<Status?>())
val nestedQuery = KronosAtomicQueryTask("SELECT nested", targetType = typeOf<Envelope.NestedStatus?>())

fun box(): String {
    val providers = ServiceLoader.load(GeneratedTypeProvider::class.java)
        .filter { it.id.startsWith("kronos-test:") }
    if (providers.size != 3) return "Fail: source-owned providers were ${providers.map { it.id }}"
    val enumOwners = mutableListOf<String>()
    val nestedEnumOwners = mutableListOf<String>()
    val pojoOwners = mutableListOf<String>()
    val staticTypes = mutableListOf<StaticTypeInfo>()
    providers.forEach { provider -> provider.contributeTo(object : GeneratedTypeRegistrar {
        override fun registerKPojo(type: KType, ownerId: String, constructorSignature: String, factory: KPojoFactory) {
            pojoOwners += ownerId
        }
        override fun registerEnum(type: KType, entryNames: List<String>, factory: EnumFactory) {
            if (type == typeOf<Status>()) {
                enumOwners += provider.id
                if (entryNames != listOf("READY", "DONE")) error("Incomplete declaration enum factory")
            }
            if (type == typeOf<Envelope.NestedStatus>()) nestedEnumOwners += provider.id
        }
        override fun registerTypeInfo(typeInfo: StaticTypeInfo) { staticTypes += typeInfo }
    }) }
    if (enumOwners.size != 1 || !enumOwners.single().startsWith("kronos-test:library#")) {
        return "Fail: dependency enum was reconstructed by consumer: $enumOwners"
    }
    if (nestedEnumOwners != enumOwners) return "Fail: nested declaration enum ownership lost: $nestedEnumOwners"
    if (pojoOwners != listOf("source.consumer.Record")) return "Fail: concrete factories were $pojoOwners"
    val generic = staticTypes.singleOrNull { it.type.key == "source.consumer.GenericOwner" }
        ?: return "Fail: generic interface metadata missing"
    if (generic.declarationVariances != listOf(StaticTypeVariance.OUT)) return "Fail: variance lost: $generic"
    if (generic.isKPojo || generic.type.arguments.singleOrNull()?.type?.key != "source.library.Status") {
        return "Fail: generic non-KPojo supertype was not substituted: $generic"
    }
    initializeKronosMetadata()
    if (Kronos.createKPojo(typeOf<Record>()) !is Record) return "Fail: source-owned factory unavailable"
    val decoded = decodeDatabaseValue("READY", ResultColumnMetadata(typeOf<Status?>(), typeInfo = statusQuery.targetTypeInfo))
    val nested = decodeDatabaseValue("NESTED", ResultColumnMetadata(typeOf<Envelope.NestedStatus?>(), typeInfo = nestedQuery.targetTypeInfo))
    return if (decoded == Status.READY && nested == Envelope.NestedStatus.NESTED &&
        statusQuery.targetTypeInfo?.type?.nullable == true) "OK"
    else "Fail: nullable dependency enum decoding failed: $decoded"
}
