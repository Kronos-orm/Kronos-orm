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

package com.kotlinorm.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds generated projection models shared across FIR callbacks during one compilation.
 */
object KronosProjectionRegistry {
    private val sessionLock = Any()
    private val projectionsBySession = WeakHashMap<FirSession, ConcurrentHashMap<ClassId, KronosProjectionModel>>()
    private val declarationGeneratorsBySession = WeakHashMap<FirSession, WeakReference<FirDeclarationGenerationExtension>>()
    private val ideGenerationTokensBySession = WeakHashMap<FirSession, String>()

    /**
     * Registers a projection model produced while refining a select call.
     */
    fun register(session: FirSession, model: KronosProjectionModel) {
        val models = synchronized(sessionLock) {
            projectionsBySession.getOrPut(session) { ConcurrentHashMap() }
        }
        models[model.classId] = model
        models[model.contextClassId] = model
        publishIdeSnapshot(session, models)
    }

    /**
     * Refines fields that were first seen as receiver-less alias literals before
     * FIR resolved the aliased scalar query call.
     */
    fun refineAliasFieldType(
        session: FirSession,
        alias: String,
        occurrence: ProjectionAliasOccurrence,
        type: ConeKotlinType
    ) {
        val models = synchronized(sessionLock) { projectionsBySession[session] } ?: return
        val aliasName = Name.identifier(alias)
        val updated = models.values.distinctBy { it.classId }.mapNotNull { model ->
            val fields = model.fields.refineAlias(aliasName, occurrence, type)
            val contextFields = model.contextFields.refineAlias(aliasName, occurrence, type)
            if (fields === model.fields && contextFields === model.contextFields) {
                null
            } else {
                model.copy(fields = fields, contextFields = contextFields)
            }
        }
        updated.forEach { model ->
            models[model.classId] = model
            models[model.contextClassId] = model
            KronosProjectionDeclarationGenerationExtension.invalidateMemberCache(model)
        }
        if (updated.isNotEmpty()) {
            publishIdeSnapshot(session, models)
        }
    }

    /**
     * Looks up a projection model by its generated class id.
     */
    fun find(session: FirSession, classId: ClassId): KronosProjectionModel? =
        synchronized(sessionLock) { projectionsBySession[session] }?.get(classId)

    /**
     * Looks up a projection model after FIR session information is no longer available.
     */
    fun findAny(classId: ClassId): KronosProjectionModel? =
        synchronized(sessionLock) {
            projectionsBySession.values.asSequence().mapNotNull { it[classId] }.firstOrNull()
        }

    /**
     * Returns all generated top-level projection class ids known to this compilation.
     */
    fun allTopLevelClassIds(session: FirSession): Set<ClassId> =
        synchronized(sessionLock) { projectionsBySession[session] }?.keys?.toSet().orEmpty()

    /**
     * Returns a best-effort snapshot of all known projection models. IDEA integration
     * uses this to provide generated source backing for FIR synthetic projection classes.
     */
    fun allModelsSnapshot(): List<KronosProjectionModel> =
        synchronized(sessionLock) {
            projectionsBySession.values
                .flatMap { it.values }
                .distinctBy { it.classId }
        }

    /**
     * Stores the FIR declaration generator registered for the current session.
     */
    fun registerDeclarationGenerator(session: FirSession, generator: FirDeclarationGenerationExtension) {
        val generationToken = KronosProjectionIdeBridge.beginModuleSession(session.ideModuleName())
        synchronized(sessionLock) {
            declarationGeneratorsBySession[session] = WeakReference(generator)
            ideGenerationTokensBySession[session] = generationToken
        }
    }

    /**
     * Returns the FIR declaration generator registered for the current session.
     */
    fun declarationGenerator(session: FirSession): FirDeclarationGenerationExtension? =
        synchronized(sessionLock) { declarationGeneratorsBySession[session]?.get() }

    private fun publishIdeSnapshot(
        session: FirSession,
        models: Map<ClassId, KronosProjectionModel>
    ) {
        val snapshot = models.values
            .distinctBy { it.classId }
            .sortedBy { it.classId.asFqNameString() }
        val generationToken = synchronized(sessionLock) { ideGenerationTokensBySession[session] } ?: return
        KronosProjectionIdeBridge.publishModule(session.ideModuleName(), generationToken, snapshot)
    }
}

private fun FirSession.ideModuleName(): String =
    moduleData.stableModuleName ?: moduleData.name.asString()

private fun List<KronosProjectionField>.refineAlias(
    alias: Name,
    occurrence: ProjectionAliasOccurrence,
    type: ConeKotlinType
): List<KronosProjectionField> {
    val matchingCandidates = indices.filter { index ->
        this[index].requestedName == alias && this[index].aliasOccurrence?.overlaps(occurrence) == true
    }
    val narrowestMatchingRange = matchingCandidates.minOfOrNull { index ->
        this[index].aliasOccurrence?.rangeSize ?: Int.MAX_VALUE
    }
    val candidateIndexes = matchingCandidates
        .filter { index -> this[index].aliasOccurrence?.rangeSize == narrowestMatchingRange }
        .toSet()
    if (candidateIndexes.isEmpty()) return this

    var changed = false
    val refined = mapIndexed { index, field ->
        if (index in candidateIndexes && field.type != type) {
            changed = true
            field.copy(type = type, signature = "${field.signature}:refined:${type}")
        } else {
            field
        }
    }
    return if (changed) refined else this
}
