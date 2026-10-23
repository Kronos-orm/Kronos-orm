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

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.utils

import com.kotlinorm.annotations.InternalKronosApi
import com.kotlinorm.exceptions.ConflictingEnumMetadata
import com.kotlinorm.exceptions.ConflictingGeneratedKPojoFactory
import com.kotlinorm.exceptions.ConflictingGeneratedProvider
import kotlin.reflect.KType

/**
 * Resolves a generated enum name without reflection.
 *
 * Returning `null` means that the name is not one of the generated entries.
 */
@InternalKronosApi
fun interface EnumFactory {
    /**
     * Returns an enum entry for [name], or `null` when no generated entry matches.
     *
     * @param name generated enum entry name selected from a persisted name or ordinal
     * @return generated enum constant, or `null` for an unknown name
     */
    fun create(name: String): Enum<*>?
}

/**
 * Collects module-generated KPojo factories, enum metadata, and static type
 * descriptors for one runtime snapshot. Static descriptor keys retain the
 * complete compiler-generated type structure and ignore only root nullability.
 */
@InternalKronosApi
interface GeneratedTypeRegistrar {
    /**
     * Contributes one generated KPojo factory keyed by complete [type].
     *
     * @param type exact concrete KPojo type generated in this module
     * @param ownerId stable generated owner identity used in conflict detection
     * @param constructorSignature stable description of the generated constructor path
     * @param factory non-reflective fresh-instance factory
     * @throws com.kotlinorm.exceptions.UnsupportedType when [type] is not supported
     */
    fun registerKPojo(
        type: KType,
        ownerId: String,
        constructorSignature: String,
        factory: KPojoFactory
    )

    /**
     * Contributes generated enum names and a non-reflective decoder for [type].
     *
     * @param type exact non-generic enum type
     * @param entryNames generated names in declaration order
     * @param factory non-reflective name-to-entry lookup
     * @throws IllegalArgumentException when the type or names are invalid
     */
    fun registerEnum(type: KType, entryNames: List<String>, factory: EnumFactory)

    /**
     * Contributes a compiler-generated descriptor for one complete static type.
     *
     * The default keeps existing provider implementations source compatible;
     * generated providers override this through their emitted registration call.
     * Registrations distinguish generic arguments, projections, and nested
     * nullability while treating only the descriptor root as nullable-insensitive.
     */
    fun registerTypeInfo(typeInfo: StaticTypeInfo) = Unit
}

/**
 * Supplies one module's generated KPojo, enum, and static-type metadata to the
 * Kronos runtime. JVM discovers providers lazily through ServiceLoader; Native
 * applications install them through an explicit compiler-bound initialization call.
 */
@InternalKronosApi
interface GeneratedTypeProvider {
    /**
     * Returns the stable module-unique identity used to detect conflicting
     * provider content.
     */
    val id: String

    /**
     * Contributes generated metadata while a process-wide snapshot is built.
     *
     * @param registrar snapshot-local collector that must not be retained
     */
    fun contributeTo(registrar: GeneratedTypeRegistrar)
}

/**
 * Generated construction metadata for one exact KPojo type.
 *
 * @property type complete contributed KPojo type
 * @property ownerId stable generated owner identity
 * @property constructorSignature stable generated construction description
 * @property factory non-reflective fresh-instance factory
 */
internal data class GeneratedFactoryEntry(
    val type: KType,
    val ownerId: String,
    val constructorSignature: String,
    val factory: KPojoFactory
)

/**
 * Generated declaration-order metadata and decoder for one exact enum type.
 *
 * @property type complete contributed enum type
 * @property entryNames enum names in declaration order
 * @property factory non-reflective name-to-entry lookup
 */
internal data class GeneratedEnumMetadata(
    val type: KType,
    val entryNames: List<String>,
    val factory: EnumFactory
)

/**
 * Conflict-checked, immutable view of all generated type contributions visible
 * to one runtime load. Lookups normalize only top-level nullability.
 */
internal class GeneratedTypeMetadataSnapshot internal constructor(
    private val kPojoFactories: Map<KTypeKey, GeneratedFactoryEntry>,
    private val enums: Map<KTypeKey, GeneratedEnumMetadata>,
    private val staticTypeInfos: Map<StaticTypeRef, StaticTypeInfo>
) {
    /**
     * Returns generated construction metadata for an exact supported KPojo type.
     *
     * @param type exact KPojo type, with root nullability normalized
     * @return matching generated metadata, or `null` when absent
     */
    internal fun kPojoFactory(type: KType): GeneratedFactoryEntry? = kPojoFactories[KTypeKey.from(type, ignoreTopLevelNullability = true)]

    /**
     * Returns generated enum metadata for an exact concrete enum type.
     *
     * @param type exact enum type, with root nullability normalized
     * @return matching generated metadata, or `null` when absent
     */
    internal fun enumMetadata(type: KType): GeneratedEnumMetadata? =
        enums[KTypeKey.from(type, ignoreTopLevelNullability = true)]?.let { it.copy(entryNames = it.entryNames.toList()) }

    /**
     * Returns generated static metadata for an exact complete type reference.
     *
     * Only the root nullable marker is normalized. Every nested generic argument,
     * projection, and nested nullable marker remains part of the lookup key.
     */
    internal fun typeInfo(type: StaticTypeRef): StaticTypeInfo? =
        staticTypeInfos[type.withoutTopLevelNullability()]?.copyForGeneratedRegistry()
}

private data class ProviderContribution(
    val providerId: String,
    val kPojoFactories: Map<KTypeKey, GeneratedFactoryEntry>,
    val enums: Map<KTypeKey, GeneratedEnumMetadata>,
    val staticTypeInfos: Map<StaticTypeRef, StaticTypeInfo>
) {
    val summary: List<String> = buildList {
        kPojoFactories.entries
            .sortedBy { it.key.stableSignature() }
            .forEach { (type, entry) ->
                add("KPOJO|${type.stableSignature()}|${entry.ownerId}|${entry.constructorSignature}")
            }
        enums.entries
            .sortedBy { it.key.stableSignature() }
            .forEach { (type, metadata) ->
                add("ENUM|${type.stableSignature()}|${metadata.entryNames.joinToString("\u0000")}")
            }
        staticTypeInfos.entries
            .sortedBy { it.key.structuralSignature() }
            .forEach { (type, metadata) ->
                add("TYPE|${type.structuralSignature()}|${metadata.structuralSignature()}")
            }
    }
}

/** Explicit Native registration boundary, frozen by the first runtime lookup. */
@InternalKronosApi
class GeneratedTypeMetadataRegistry {
    private val lock = StaticTypeMetadataLock()
    private val providers = mutableListOf<GeneratedTypeProvider>()
    private var frozen: GeneratedTypeMetadataSnapshot? = null
    private var freezing = false

    /** Registers a provider before the first lookup. Duplicate content is checked when freezing. */
    fun register(provider: GeneratedTypeProvider) {
        // Capture callbacks outside the state lock. Mutations or escaped callbacks cannot alter the snapshot.
        lock.withLock { requireOpen() }
        val id = provider.id
        require(id.isNotBlank()) { "Generated type provider id must not be blank" }
        val contribution = provider.collectContribution(id)
        val captured = object : GeneratedTypeProvider {
            override val id = contribution.providerId
            override fun contributeTo(registrar: GeneratedTypeRegistrar) {
                contribution.kPojoFactories.values.forEach {
                    registrar.registerKPojo(it.type, it.ownerId, it.constructorSignature, it.factory)
                }
                contribution.enums.values.forEach { registrar.registerEnum(it.type, it.entryNames, it.factory) }
                contribution.staticTypeInfos.values.forEach(registrar::registerTypeInfo)
            }
        }
        lock.withLock {
            requireOpen()
            providers += captured
        }
    }

    private fun requireOpen() {
        check(frozen == null && !freezing) {
            "Generated type metadata registry is frozen; register all providers before the first lookup."
        }
    }

    /** Validates the complete graph and permanently closes registration. */
    fun freeze() { snapshot() }

    internal fun snapshot(): GeneratedTypeMetadataSnapshot = lock.withLock {
        frozen ?: run {
            check(!freezing) { "Generated type metadata registry is already building its first snapshot." }
            freezing = true
            try {
                loadGeneratedTypeMetadata(providers).also { frozen = it }
            } finally {
                freezing = false
            }
        }
    }
}

/** Process-wide Native initialization target. Register every generated module before running queries. */
@InternalKronosApi
val generatedTypeMetadataRegistry: GeneratedTypeMetadataRegistry = GeneratedTypeMetadataRegistry()

/**
 * Installs the current application's complete generated dependency graph.
 *
 * Call this once during startup before querying or decoding values on Native.
 * The compiler plugin binds the explicit call to the application's generated
 * registration entry. JVM eagerly loads its usual ServiceLoader providers.
 */
fun initializeKronosMetadata() = initializePlatformGeneratedMetadata()

internal expect fun initializePlatformGeneratedMetadata()

internal expect fun discoverGeneratedTypeProviders(): Iterable<GeneratedTypeProvider>

private val platformGeneratedMetadata by lazy {
    discoverGeneratedTypeProviders().forEach(generatedTypeMetadataRegistry::register)
    generatedTypeMetadataRegistry
}

/** Returns the process-wide conflict-checked generated metadata snapshot. */
internal fun generatedTypeMetadata(): GeneratedTypeMetadataSnapshot = platformGeneratedMetadata.snapshot()

/**
 * Builds a deterministic generated metadata snapshot from explicit providers.
 *
 * Providers with the same id may repeat only equivalent semantic
 * contributions. KPojo ownership and enum names are then checked across ids.
 *
 * @param providers providers to collect, in arbitrary discovery order
 * @return immutable conflict-checked metadata snapshot
 * @throws ConflictingGeneratedProvider when duplicate ids differ
 * @throws ConflictingGeneratedKPojoFactory when exact KPojo metadata conflicts
 * @throws ConflictingEnumMetadata when exact enum names conflict
 * @throws IllegalStateException when complete static type metadata conflicts
 */
internal fun loadGeneratedTypeMetadata(
    providers: Iterable<GeneratedTypeProvider>
): GeneratedTypeMetadataSnapshot {
    val contributionsByProvider = linkedMapOf<String, ProviderContribution>()
    providers.forEach { provider ->
        val providerId = provider.id
        require(providerId.isNotBlank()) { "Generated type provider id must not be blank" }
        val contribution = provider.collectContribution(providerId)
        val existing = contributionsByProvider[providerId]
        when {
            existing == null -> contributionsByProvider[providerId] = contribution
            existing.summary != contribution.summary -> throw ConflictingGeneratedProvider(providerId)
        }
    }

    val kPojoFactories = linkedMapOf<KTypeKey, GeneratedFactoryEntry>()
    val enums = linkedMapOf<KTypeKey, GeneratedEnumMetadata>()
    val staticTypeInfos = linkedMapOf<StaticTypeRef, StaticTypeInfo>()
    contributionsByProvider.values.sortedBy { it.providerId }.forEach { contribution ->
        contribution.kPojoFactories.forEach { (type, entry) ->
            val existing = kPojoFactories[type]
            when {
                existing == null -> kPojoFactories[type] = entry
                existing.ownerId != entry.ownerId ||
                    existing.constructorSignature != entry.constructorSignature ->
                    throw ConflictingGeneratedKPojoFactory(existing.type)
            }
        }
        contribution.enums.forEach { (type, metadata) ->
            val existing = enums[type]
            when {
                existing == null -> enums[type] = metadata
                existing.entryNames != metadata.entryNames -> throw ConflictingEnumMetadata(existing.type)
            }
        }
        contribution.staticTypeInfos.forEach { (type, metadata) ->
            val existing = staticTypeInfos[type]
            when {
                existing == null -> staticTypeInfos[type] = metadata
                existing != metadata -> throw conflictingStaticTypeMetadata(type)
            }
        }
    }
    return GeneratedTypeMetadataSnapshot(kPojoFactories.toMap(), enums.toMap(), staticTypeInfos.toMap())
}

private fun GeneratedTypeProvider.collectContribution(providerId: String): ProviderContribution {
    val kPojoFactories = linkedMapOf<KTypeKey, GeneratedFactoryEntry>()
    val enums = linkedMapOf<KTypeKey, GeneratedEnumMetadata>()
    val staticTypeInfos = linkedMapOf<StaticTypeRef, StaticTypeInfo>()
    contributeTo(object : GeneratedTypeRegistrar {
        override fun registerKPojo(
            type: KType,
            ownerId: String,
            constructorSignature: String,
            factory: KPojoFactory
        ) {
            registerPlatformGeneratedType(type, isKPojo = true, isEnum = false)
            val key = type.normalizedKPojoType()
            val entry = GeneratedFactoryEntry(type, ownerId, constructorSignature, factory)
            val existing = kPojoFactories[key]
            when {
                existing == null -> kPojoFactories[key] = entry
                existing.ownerId != ownerId || existing.constructorSignature != constructorSignature ->
                    throw ConflictingGeneratedKPojoFactory(existing.type)
            }
        }

        override fun registerEnum(type: KType, entryNames: List<String>, factory: EnumFactory) {
            registerPlatformGeneratedType(type, isKPojo = false, isEnum = true)
            val key = type.normalizedEnumType()
            val names = entryNames.toList()
            require(names.size == names.distinct().size) { "Enum metadata for $key contains duplicate names" }
            val metadata = GeneratedEnumMetadata(type, names, factory)
            val existing = enums[key]
            when {
                existing == null -> enums[key] = metadata
                existing.entryNames != names -> throw ConflictingEnumMetadata(existing.type)
            }
        }

        override fun registerTypeInfo(typeInfo: StaticTypeInfo) {
            val normalized = typeInfo.normalizedForRegistry()
            val key = normalized.type
            val existing = staticTypeInfos[key]
            when {
                existing == null -> staticTypeInfos[key] = normalized
                existing != normalized -> throw conflictingStaticTypeMetadata(key)
            }
        }
    })
    return ProviderContribution(providerId, kPojoFactories.toMap(), enums.toMap(), staticTypeInfos.toMap())
}

private fun StaticTypeInfo.copyForGeneratedRegistry(): StaticTypeInfo = copy(
    type = type.copyForGeneratedRegistry(),
    directSupertypes = directSupertypes.map { it.copyForGeneratedRegistry() },
    elementType = elementType?.copyForGeneratedRegistry(),
    declarationVariances = declarationVariances.toList()
)

private fun StaticTypeInfo.normalizedForRegistry(): StaticTypeInfo = copy(
    type = type.copyForGeneratedRegistry().withoutTopLevelNullability(),
    directSupertypes = directSupertypes.map { it.copyForGeneratedRegistry().withoutTopLevelNullability() },
    elementType = elementType?.copyForGeneratedRegistry(),
    declarationVariances = declarationVariances.toList()
)

private fun StaticTypeRef.copyForGeneratedRegistry(): StaticTypeRef = copy(
    arguments = arguments.map { it.copy(type = it.type?.copyForGeneratedRegistry()) }
)

private fun conflictingStaticTypeMetadata(type: StaticTypeRef): Nothing =
    error("Conflicting generated static type metadata was registered for ${type.structuralSignature()}")
