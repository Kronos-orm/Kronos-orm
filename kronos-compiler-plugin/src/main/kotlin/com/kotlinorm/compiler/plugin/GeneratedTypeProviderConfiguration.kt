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

import com.kotlinorm.compiler.utils.GeneratedFactoryPackageFqName
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.name.FqName

/**
 * Compiler option carrying the stable module identity for generated metadata.
 */
internal const val GeneratedProviderIdOptionName = "generated-provider-id"
/**
 * Compiler option carrying the generated provider's fully qualified class name.
 */
internal const val GeneratedProviderFqNameOptionName = "generated-provider-fq-name"

/**
 * Compiler key storing [GeneratedProviderIdOptionName].
 */
internal val GeneratedProviderIdKey = CompilerConfigurationKey<String>(
    "stable id of the module-local Kronos generated type provider"
)

/**
 * Compiler key storing [GeneratedProviderFqNameOptionName].
 */
internal val GeneratedProviderFqNameKey = CompilerConfigurationKey<String>(
    "fully qualified name of the module-local Kronos generated type provider"
)

/**
 * Validated identity for the module-local generated type provider.
 *
 * The fully qualified name must place the provider directly in the generated factory package,
 * and its short name must be a legal Kotlin identifier. The stable [id] identifies provider
 * content at runtime and must be unique for the compilation module.
 *
 * @property id non-blank, stable module identity used for duplicate-provider detection
 * @property fqName fully qualified class name emitted by the IR generator
 * @throws IllegalArgumentException when either identity invariant is violated
 */
internal data class GeneratedTypeProviderConfiguration(
    val id: String,
    val fqName: FqName
) {
    init {
        require(id.isNotBlank()) { "Generated type provider id must not be blank" }
        require(!fqName.isRoot) { "Generated type provider fq-name must not be blank" }
        require(fqName.parent() == GeneratedFactoryPackageFqName) {
            "Generated type provider must be in ${GeneratedFactoryPackageFqName.asString()}: ${fqName.asString()}"
        }
        require(fqName.shortName().asString().matches(KotlinIdentifier)) {
            "Generated type provider class name must be a valid Kotlin identifier: ${fqName.shortName()}"
        }
    }
}

private val KotlinIdentifier = Regex("[A-Za-z_][A-Za-z0-9_]*")

/**
 * Reads and validates both generated-provider options for callers that require provider generation.
 *
 * @return validated provider configuration
 * @throws IllegalStateException when either compiler option is absent
 * @throws IllegalArgumentException when a supplied identity is invalid
 */
internal fun CompilerConfiguration.generatedTypeProviderConfiguration(): GeneratedTypeProviderConfiguration {
    val id = get(GeneratedProviderIdKey)
        ?: error("Missing compiler option '$GeneratedProviderIdOptionName'")
    val fqName = get(GeneratedProviderFqNameKey)
        ?: error("Missing compiler option '$GeneratedProviderFqNameOptionName'")
    return GeneratedTypeProviderConfiguration(id, FqName(fqName))
}

/**
 * Reads generated-provider options when this compilation is configured to emit a provider.
 *
 * @return `null` when neither option is present, otherwise the validated configuration
 * @throws IllegalArgumentException when only one option is present or an identity is invalid
 */
internal fun CompilerConfiguration.generatedTypeProviderConfigurationOrNull(): GeneratedTypeProviderConfiguration? {
    val id = get(GeneratedProviderIdKey)
    val fqName = get(GeneratedProviderFqNameKey)
    if (id == null && fqName == null) return null
    requireNotNull(id) { "Missing compiler option '$GeneratedProviderIdOptionName'" }
    requireNotNull(fqName) { "Missing compiler option '$GeneratedProviderFqNameOptionName'" }
    return GeneratedTypeProviderConfiguration(id, FqName(fqName))
}
