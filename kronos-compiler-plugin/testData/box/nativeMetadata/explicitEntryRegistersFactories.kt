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

// Verifies that the explicit Native entry installs full factories and rejects registrations after lookup.
@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package fixture

import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.GeneratedTypeMetadataRegistry
import com.kotlinorm.utils.generatedTypeMetadataRegistry
import com.kotlinorm.utils.initializeKronosMetadata
import kotlin.reflect.typeOf

data class NativeEntryPojo(var id: Int = 0) : KPojo

// Matches the configured source entry prefix used by the Native generator.
@Suppress("ClassNaming")
private class installNativeMetadataProvider : com.kotlinorm.utils.GeneratedTypeProvider {
    override val id = "source-placeholder"
    override fun contributeTo(registrar: com.kotlinorm.utils.GeneratedTypeRegistrar) {}
}

fun installNativeMetadata(registry: GeneratedTypeMetadataRegistry) {
    registry.register(installNativeMetadataProvider())
}

fun installNativeMetadataDependencies(
    registry: GeneratedTypeMetadataRegistry = generatedTypeMetadataRegistry
) { installNativeMetadata(registry) }

fun box(): String {
    if (installNativeMetadataProvider().id == "source-placeholder") {
        return "Fail: compiler did not install the configured provider identity"
    }
    initializeKronosMetadata()
    installNativeMetadata(generatedTypeMetadataRegistry)
    val first = Kronos.createKPojo(typeOf<NativeEntryPojo>()) as NativeEntryPojo
    val second = Kronos.createKPojo(typeOf<NativeEntryPojo>()) as NativeEntryPojo
    if (first.id != 0 || first === second) {
        return "Fail: generated factory did not return fresh default instances: $first / $second"
    }
    return try {
        installNativeMetadata(generatedTypeMetadataRegistry)
        "Fail: generated registration was allowed after first factory lookup"
    } catch (expected: IllegalStateException) {
        if (expected.message != "Generated type metadata registry is frozen; register all providers before the first lookup.") {
            "Fail: unexpected late registration failure: ${expected.message}"
        } else "OK"
    }
}
