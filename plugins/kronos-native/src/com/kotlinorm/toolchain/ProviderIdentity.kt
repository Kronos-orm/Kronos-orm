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

package com.kotlinorm.toolchain

import java.security.MessageDigest

internal const val MODULE_COORDINATE_OPTION_NAME = "module-coordinate"
private const val HASH_BYTE_MASK = 0xff
private const val HEX_RADIX = 16
private const val GENERATED_PROVIDER_HASH_LENGTH = 16

internal data class GradleGeneratedTypeProviderIdentity(
    val id: String,
    val fqName: String
) {
    private val hash: String
        get() = fqName.substringAfterLast('_')

    val nativeEntryFqName: String
        get() = "com.kotlinorm.generated.native.KronosGeneratedTypeMetadata_$hash"
}

internal fun gradleGeneratedTypeProviderIdentity(moduleCoordinate: String): GradleGeneratedTypeProviderIdentity {
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(moduleCoordinate.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and HASH_BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')
        }
        .take(GENERATED_PROVIDER_HASH_LENGTH)
    return GradleGeneratedTypeProviderIdentity(
        id = "$moduleCoordinate#$hash",
        fqName = "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_$hash"
    )
}
