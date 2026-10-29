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

// Verifies binary target entries receive the shared registry exactly once before common initialization.
// MODULE: library
// FILE: targetDependency.kt
@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)
package fixture

import com.kotlinorm.utils.GeneratedTypeMetadataRegistry
import com.kotlinorm.utils.generatedTypeMetadataRegistry

var dependencyCalls = ""

// This entry is available only through the compiled dependency's symbol provider.
fun installTargetDependency(registry: GeneratedTypeMetadataRegistry) {
    check(registry === generatedTypeMetadataRegistry) { "Binary dependency received a different registry" }
    dependencyCalls += "target;"
}

// MODULE: main(library)
// FILE: application.kt
@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)
package fixture

import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.GeneratedTypeMetadataRegistry
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.generatedTypeMetadataRegistry
import com.kotlinorm.utils.initializeKronosMetadata
import kotlin.reflect.typeOf

data class BinaryDependencyApplicationPojo(var id: Int = 0) : KPojo

@Suppress("ClassNaming")
private class installNativeMetadataProvider : GeneratedTypeProvider {
    override val id = "source-placeholder"
    override fun contributeTo(registrar: GeneratedTypeRegistrar) {}
}

fun installNativeMetadata(registry: GeneratedTypeMetadataRegistry) {
    registry.register(installNativeMetadataProvider())
}

fun installCommonDependency(registry: GeneratedTypeMetadataRegistry) {
    check(registry === generatedTypeMetadataRegistry) { "Common dependency received a different registry" }
    dependencyCalls += "common;"
}

fun installNativeMetadataDependencies(registry: GeneratedTypeMetadataRegistry = generatedTypeMetadataRegistry) {
    installCommonDependency(registry)
    installNativeMetadata(registry)
}

fun box(): String {
    initializeKronosMetadata()
    if (dependencyCalls != "target;common;") return "Fail: dependency calls were $dependencyCalls"
    val instance = Kronos.createKPojo(typeOf<BinaryDependencyApplicationPojo>()) as BinaryDependencyApplicationPojo
    if (instance.id != 0) return "Fail: application factory returned $instance"
    return "OK"
}
