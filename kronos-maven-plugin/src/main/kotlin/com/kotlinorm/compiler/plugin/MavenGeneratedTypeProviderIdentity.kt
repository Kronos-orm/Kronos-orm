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

package com.kotlinorm.compiler.plugin

import java.security.MessageDigest

internal data class MavenGeneratedTypeProviderIdentity(
    val id: String,
    val fqName: String
) {
    val compilerOptions: Map<String, String>
        get() = linkedMapOf(
            GeneratedProviderIdOptionName to id,
            GeneratedProviderFqNameOptionName to fqName
        )

    val serviceContent: String
        get() = "$fqName\n"
}

internal const val GeneratedProviderIdOptionName = "generated-provider-id"
internal const val GeneratedProviderFqNameOptionName = "generated-provider-fq-name"

internal fun mavenGeneratedTypeProviderIdentity(moduleCoordinate: String): MavenGeneratedTypeProviderIdentity {
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(moduleCoordinate.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        .take(16)
    return MavenGeneratedTypeProviderIdentity(
        id = "$moduleCoordinate#$hash",
        fqName = "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_$hash"
    )
}
