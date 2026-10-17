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

package com.kotlinorm.utils

/**
 * Receives compiler-generated static type descriptors from one module.
 *
 * This contract intentionally uses [StaticTypeRef] instead of platform reflection
 * types so an application can install generated metadata on every Kotlin target.
 */
interface StaticTypeMetadataRegistrar {
    /**
     * Registers one compiler-generated descriptor.
     */
    fun registerTypeInfo(typeInfo: StaticTypeInfo)
}

/**
 * Supplies the generated static type descriptors owned by one module fragment.
 *
 * Applications install every reachable provider before the first metadata lookup.
 * The compiler/Gradle integration is responsible for creating an explicit Native
 * aggregation entry; this common contract does not discover providers itself.
 */
interface StaticTypeMetadataProvider {
    /**
     * Stable identity used to deduplicate equivalent generated contributions.
     */
    val id: String

    /**
     * Adds this provider's generated descriptors to [registrar].
     */
    fun contributeTo(registrar: StaticTypeMetadataRegistrar)
}

/**
 * Immutable, conflict-checked static metadata available after registry freeze.
 */
class StaticTypeMetadataSnapshot internal constructor(
    private val typeInfos: Map<StaticTypeRef, StaticTypeInfo>
) {
    /**
     * Returns metadata for [type], treating only its outer nullability as equivalent.
     */
    fun typeInfo(type: StaticTypeRef): StaticTypeInfo? =
        typeInfos[type.withoutTopLevelNullability()]?.copyForRegistry()
}

/**
 * Explicit generated-metadata registry for common and Native runtimes.
 *
 * Registration remains open until [snapshot] is called. That first lookup builds
 * one deterministic snapshot and closes registration permanently, so application
 * startup cannot accidentally change type metadata during query execution.
 */
class StaticTypeMetadataRegistry {
    private val lock = StaticTypeMetadataLock()
    private val contributions = mutableListOf<StaticTypeProviderContribution>()
    private var frozenSnapshot: StaticTypeMetadataSnapshot? = null
    private var state = StaticTypeMetadataRegistryState.OPEN

    /**
     * Adds a generated provider before the first [snapshot] call.
     *
     * Equivalent provider ids are allowed and are deduplicated while building the
     * snapshot. Conflicting content is reported by [snapshot] with the provider id.
     */
    fun register(provider: StaticTypeMetadataProvider) {
        lock.withLock {
            check(state == StaticTypeMetadataRegistryState.OPEN) {
                "Static type metadata registry is frozen; register all providers before the first lookup."
            }
        }
        val providerId = provider.id
        require(providerId.isNotBlank()) { "Static type metadata provider id must not be blank." }
        val contribution = provider.collectContribution(providerId)
        lock.withLock {
            check(state == StaticTypeMetadataRegistryState.OPEN) {
                "Static type metadata registry is frozen; register all providers before the first lookup."
            }
            contributions += contribution
        }
    }

    /**
     * Installs one compiler-generated module contribution.
     *
     * Native modules expose this operation through a generated common-source
     * entry. Keeping the provider id and descriptors in one call lets the
     * compiler-generated entry remain a plain source-visible function while
     * preserving the same duplicate and freeze checks as [register].
     */
    fun registerGeneratedTypeMetadata(
        providerId: String,
        typeInfos: List<StaticTypeInfo>
    ) {
        register(object : StaticTypeMetadataProvider {
            override val id: String = providerId

            override fun contributeTo(registrar: StaticTypeMetadataRegistrar) {
                typeInfos.forEach(registrar::registerTypeInfo)
            }
        })
    }

    /**
     * Returns the deterministic metadata snapshot and permanently closes registration.
     */
    fun snapshot(): StaticTypeMetadataSnapshot = lock.withLock {
        frozenSnapshot ?: run {
            check(state == StaticTypeMetadataRegistryState.OPEN) {
                "Static type metadata registry is already building its first snapshot."
            }
            state = StaticTypeMetadataRegistryState.FREEZING
            try {
                buildSnapshot(contributions).also {
                    frozenSnapshot = it
                    state = StaticTypeMetadataRegistryState.FROZEN
                }
            } catch (cause: Throwable) {
                state = StaticTypeMetadataRegistryState.OPEN
                throw cause
            }
        }
    }

    private fun buildSnapshot(
        registeredContributions: List<StaticTypeProviderContribution>
    ): StaticTypeMetadataSnapshot {
        val contributionsByProvider = linkedMapOf<String, StaticTypeProviderContribution>()
        registeredContributions.forEach { contribution ->
            val existing = contributionsByProvider[contribution.providerId]
            when {
                existing == null -> contributionsByProvider[contribution.providerId] = contribution
                existing.summary != contribution.summary -> {
                    throw IllegalArgumentException(
                        "Static type metadata provider '${contribution.providerId}' contributed conflicting metadata."
                    )
                }
            }
        }

        val typeInfos = linkedMapOf<StaticTypeRef, StaticTypeInfo>()
        contributionsByProvider.values.sortedBy(StaticTypeProviderContribution::providerId).forEach { contribution ->
            contribution.typeInfos.forEach { (type, typeInfo) ->
                val existing = typeInfos[type]
                when {
                    existing == null -> typeInfos[type] = typeInfo
                    existing != typeInfo -> {
                        throw IllegalArgumentException(
                            "Conflicting generated static type metadata was registered for " +
                                type.structuralSignature()
                        )
                    }
                }
            }
        }
        return StaticTypeMetadataSnapshot(typeInfos.toMap())
    }
}

private enum class StaticTypeMetadataRegistryState {
    OPEN,
    FREEZING,
    FROZEN
}

private data class StaticTypeProviderContribution(
    val providerId: String,
    val typeInfos: Map<StaticTypeRef, StaticTypeInfo>
) {
    val summary: List<String> = typeInfos.entries
        .sortedBy { (type, _) -> type.structuralSignature() }
        .map { (type, typeInfo) ->
            "TYPE|${type.structuralSignature()}|${typeInfo.structuralSignature()}"
        }
}

private fun StaticTypeMetadataProvider.collectContribution(
    providerId: String
): StaticTypeProviderContribution {
    val typeInfos = linkedMapOf<StaticTypeRef, StaticTypeInfo>()
    contributeTo(object : StaticTypeMetadataRegistrar {
        override fun registerTypeInfo(typeInfo: StaticTypeInfo) {
            val normalized = typeInfo.normalizedForRegistry()
            val existing = typeInfos[normalized.type]
            when {
                existing == null -> typeInfos[normalized.type] = normalized
                existing != normalized -> {
                    throw IllegalArgumentException(
                        "Conflicting generated static type metadata was registered for " +
                            normalized.type.structuralSignature()
                    )
                }
            }
        }
    })
    return StaticTypeProviderContribution(providerId, typeInfos.toMap())
}

private fun StaticTypeInfo.normalizedForRegistry(): StaticTypeInfo = StaticTypeInfo(
    type = type.copyForRegistry().withoutTopLevelNullability(),
    directSupertypes = directSupertypes.map { it.copyForRegistry().withoutTopLevelNullability() },
    containerKind = containerKind,
    elementType = elementType?.copyForRegistry(),
    isKPojo = isKPojo,
    enumKey = enumKey,
    declarationVariances = declarationVariances.toList()
)

private fun StaticTypeInfo.copyForRegistry(): StaticTypeInfo = StaticTypeInfo(
    type = type.copyForRegistry(),
    directSupertypes = directSupertypes.map(StaticTypeRef::copyForRegistry),
    containerKind = containerKind,
    elementType = elementType?.copyForRegistry(),
    isKPojo = isKPojo,
    enumKey = enumKey,
    declarationVariances = declarationVariances.toList()
)

private fun StaticTypeRef.copyForRegistry(): StaticTypeRef = StaticTypeRef(
    key = key,
    nullable = nullable,
    arguments = arguments.map { argument ->
        StaticTypeArgument(argument.variance, argument.type?.copyForRegistry())
    }
)
