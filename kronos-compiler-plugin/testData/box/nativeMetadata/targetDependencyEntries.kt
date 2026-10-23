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

// Verifies target-only dependencies join the existing common graph once before metadata freezes.
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

data class TargetDependencyPojo(var id: Int = 0) : KPojo
private var calls = ""

@Suppress("ClassNaming")
private class installNativeMetadataProvider : GeneratedTypeProvider {
    override val id = "source-placeholder"
    override fun contributeTo(registrar: GeneratedTypeRegistrar) {}
}

fun installNativeMetadata(registry: GeneratedTypeMetadataRegistry) {
    registry.register(installNativeMetadataProvider())
}

fun installCommonDependency(registry: GeneratedTypeMetadataRegistry) { calls += "common;" }
fun installTargetDependency(registry: GeneratedTypeMetadataRegistry) { calls += "target;" }

fun installNativeMetadataDependencies(registry: GeneratedTypeMetadataRegistry = generatedTypeMetadataRegistry) {
    installCommonDependency(registry)
    installNativeMetadata(registry)
}

fun box(): String {
    initializeKronosMetadata()
    if (calls != "target;common;") return "Fail: dependency calls were $calls"
    val instance = Kronos.createKPojo(typeOf<TargetDependencyPojo>()) as TargetDependencyPojo
    if (instance.id != 0) return "Fail: application factory returned $instance"
    return "OK"
}
