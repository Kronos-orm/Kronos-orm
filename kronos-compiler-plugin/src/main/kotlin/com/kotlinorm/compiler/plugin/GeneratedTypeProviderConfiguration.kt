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
import java.security.MessageDigest

/**
 * Compiler option carrying the stable module identity for generated metadata.
 */
internal const val GENERATED_PROVIDER_ID_OPTION_NAME = "generated-provider-id"
/**
 * Compiler option carrying the generated provider's fully qualified class name.
 */
internal const val GENERATED_PROVIDER_FQ_NAME_OPTION_NAME = "generated-provider-fq-name"

/**
 * Compiler option selecting independently emitted provider fragments.
 *
 * Each source file receives a stable provider name derived from the compilation identity and its
 * source identity. This lets Kotlin incremental compilation preserve contributions from unchanged
 * source files instead of overwriting one compilation-wide provider with a partial IR view.
 */
internal const val GENERATED_PROVIDER_FRAGMENTS_OPTION_NAME = "generated-provider-fragments"

/**
 * Compiler option used by Native compilations that transform Kronos code but
 * intentionally do not emit the JVM ServiceLoader provider.
 */
internal const val SKIP_GENERATED_PROVIDER_OPTION_NAME = "skip-generated-provider"

/** Compiler option carrying the common-source Native metadata entry function. */
internal const val NATIVE_PROVIDER_ENTRY_FQ_NAME_OPTION_NAME = "native-provider-entry-fq-name"

/** Compiler option carrying the stable identity for the Native metadata entry. */
internal const val NATIVE_PROVIDER_ID_OPTION_NAME = "native-provider-id"

/**
 * Compiler key storing [GENERATED_PROVIDER_ID_OPTION_NAME].
 */
internal val GeneratedProviderIdKey = CompilerConfigurationKey<String>(
    "stable id of the module-local Kronos generated type provider"
)

/**
 * Compiler key storing [GENERATED_PROVIDER_FQ_NAME_OPTION_NAME].
 */
internal val GeneratedProviderFqNameKey = CompilerConfigurationKey<String>(
    "fully qualified name of the module-local Kronos generated type provider"
)

/**
 * Compiler key enabling source-owned generated provider fragments.
 */
internal val GeneratedProviderFragmentsKey = CompilerConfigurationKey<Boolean>(
    "whether generated type providers are emitted as source-owned fragments"
)

/**
 * Compiler key recording an explicit request to skip JVM provider generation.
 */
internal val SkipGeneratedProviderKey = CompilerConfigurationKey<Boolean>(
    "whether this compilation intentionally skips JVM generated type provider emission"
)

internal val NativeProviderEntryFqNameKey = CompilerConfigurationKey<String>(
    "fully qualified common-source Native metadata entry function"
)

internal val NativeProviderIdKey = CompilerConfigurationKey<String>(
    "stable module identity for the Native metadata entry"
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

    /**
     * Creates a deterministic source-owned provider identity.
     *
     * The source identity is intentionally hashed before becoming part of the binary name. It can
     * contain an absolute path when the Kotlin compiler has no source-root information, but never
     * leaks that path into generated bytecode or service metadata. Renaming a source creates a
     * new fragment; Kotlin's output mapping removes the old source-owned class.
     */
    fun fragmentFor(sourceIdentity: String): GeneratedTypeProviderConfiguration {
        require(sourceIdentity.isNotBlank()) { "Generated provider source identity must not be blank" }
        val sourceHash = MessageDigest.getInstance("SHA-256")
            .digest(sourceIdentity.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte ->
                (byte.toInt() and HASH_BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')
            }
            .take(GENERATED_PROVIDER_SOURCE_HASH_LENGTH)
        return GeneratedTypeProviderConfiguration(
            id = "$id:$sourceHash",
            fqName = FqName("${fqName.asString()}_$sourceHash")
        )
    }
}

/**
 * Backend provider-generation policy derived from compiler options.
 *
 * FIR-only IDE loading may legitimately leave every option absent. A real IR
 * compilation must instead either provide a complete identity or explicitly
 * opt out for a Native target.
 */
internal sealed interface GeneratedTypeProviderMode {
    data class Enabled(
        val configuration: GeneratedTypeProviderConfiguration,
        val useSourceFragments: Boolean
    ) : GeneratedTypeProviderMode

    data object Disabled : GeneratedTypeProviderMode

    /** Native uses a source-visible common entry instead of ServiceLoader. */
    data class Native(
        val id: String,
        val entryFqName: FqName
    ) : GeneratedTypeProviderMode

    data object Missing : GeneratedTypeProviderMode
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
        ?: error("Missing compiler option '$GENERATED_PROVIDER_ID_OPTION_NAME'")
    val fqName = get(GeneratedProviderFqNameKey)
        ?: error("Missing compiler option '$GENERATED_PROVIDER_FQ_NAME_OPTION_NAME'")
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
    requireNotNull(id) { "Missing compiler option '$GENERATED_PROVIDER_ID_OPTION_NAME'" }
    requireNotNull(fqName) { "Missing compiler option '$GENERATED_PROVIDER_FQ_NAME_OPTION_NAME'" }
    return GeneratedTypeProviderConfiguration(id, FqName(fqName))
}

/**
 * Resolves the backend provider policy while preserving the all-or-none
 * identity invariant.
 *
 * @return [GeneratedTypeProviderMode.Missing] when IDE FIR-only loading has
 * no build options, [GeneratedTypeProviderMode.Disabled] only after an
 * explicit Native opt-out, otherwise the validated provider identity
 */
internal fun CompilerConfiguration.generatedTypeProviderMode(): GeneratedTypeProviderMode {
    val nativeEntry = get(NativeProviderEntryFqNameKey)
    val nativeId = get(NativeProviderIdKey)
    if (nativeEntry != null || nativeId != null) {
        require(nativeEntry != null) {
            "Missing compiler option '$NATIVE_PROVIDER_ENTRY_FQ_NAME_OPTION_NAME'"
        }
        require(nativeId != null) {
            "Missing compiler option '$NATIVE_PROVIDER_ID_OPTION_NAME'"
        }
        require(nativeId.isNotBlank()) { "Native generated provider id must not be blank" }
        require(get(GeneratedProviderIdKey) == null && get(GeneratedProviderFqNameKey) == null) {
            "Native metadata entry options cannot be combined with JVM generated provider options"
        }
        require(get(SkipGeneratedProviderKey) != true) {
            "Native metadata entry options cannot be combined with '$SKIP_GENERATED_PROVIDER_OPTION_NAME'"
        }
        return GeneratedTypeProviderMode.Native(nativeId, FqName(nativeEntry))
    }
    val provider = generatedTypeProviderConfigurationOrNull()
    if (get(SkipGeneratedProviderKey) == true) {
        require(provider == null) {
            "Compiler option '$SKIP_GENERATED_PROVIDER_OPTION_NAME' cannot be combined with generated provider options"
        }
        require(get(GeneratedProviderFragmentsKey) != true) {
            "Compiler option '$GENERATED_PROVIDER_FRAGMENTS_OPTION_NAME' cannot be combined with '$SKIP_GENERATED_PROVIDER_OPTION_NAME'"
        }
        return GeneratedTypeProviderMode.Disabled
    }
    return provider?.let {
        GeneratedTypeProviderMode.Enabled(it, get(GeneratedProviderFragmentsKey) == true)
    } ?: GeneratedTypeProviderMode.Missing
}

private const val HASH_BYTE_MASK = 0xff
private const val HEX_RADIX = 16
private const val GENERATED_PROVIDER_SOURCE_HASH_LENGTH = 16
