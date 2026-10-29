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

// Verifies a missing target metadata entry is rejected during compilation before Native linking.
@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)
package fixture

import com.kotlinorm.utils.GeneratedTypeMetadataRegistry
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.generatedTypeMetadataRegistry
import com.kotlinorm.utils.initializeKronosMetadata

@Suppress("ClassNaming")
private class installNativeMetadataProvider : GeneratedTypeProvider {
    override val id = "source-placeholder"
    override fun contributeTo(registrar: GeneratedTypeRegistrar) {}
}

fun installNativeMetadata(registry: GeneratedTypeMetadataRegistry) {
    registry.register(installNativeMetadataProvider())
}

fun installCommonDependency(registry: GeneratedTypeMetadataRegistry) {
    check(registry === generatedTypeMetadataRegistry)
}

fun installNativeMetadataDependencies(registry: GeneratedTypeMetadataRegistry = generatedTypeMetadataRegistry) {
    installCommonDependency(registry)
    installNativeMetadata(registry)
}

fun box(): String {
    initializeKronosMetadata()
    return "Unreachable when a target dependency entry is missing"
}
