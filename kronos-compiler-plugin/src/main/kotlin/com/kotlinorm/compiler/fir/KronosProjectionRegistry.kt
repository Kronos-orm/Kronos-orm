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
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
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
    private val statesBySession = WeakHashMap<FirSession, WeakReference<KronosProjectionSessionState>>()

    /**
     * Registers a projection model produced while refining a select call.
     */
    fun register(session: FirSession, model: KronosProjectionModel) {
        val models = requireNotNull(state(session)).models
        models[model.classId] = model
        models[model.contextClassId] = model
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
        val state = state(session) ?: return
        val models = state.models
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
            state.members.remove(model.symbol)
            state.members.remove(model.contextSymbol)
        }
    }

    /**
     * Looks up a projection model by its generated class id.
     */
    fun find(session: FirSession, classId: ClassId): KronosProjectionModel? =
        state(session)?.models?.get(classId)

    /**
     * Looks up a projection model after FIR session information is no longer available.
     */
    fun findAny(classId: ClassId): KronosProjectionModel? =
        synchronized(sessionLock) {
            statesBySession.values.asSequence().mapNotNull { it.get()?.models?.get(classId) }.firstOrNull()
        }

    /**
     * Returns all generated top-level projection class ids known to this compilation.
     */
    fun allTopLevelClassIds(session: FirSession): Set<ClassId> =
        state(session)?.models?.keys?.toSet().orEmpty()

    /**
     * Stores the FIR declaration generator registered for the current session.
     */
    @OptIn(SessionConfiguration::class)
    fun registerDeclarationGenerator(session: FirSession, generator: FirDeclarationGenerationExtension) {
        synchronized(sessionLock) {
            val state = statesBySession[session]?.get() ?: KronosProjectionSessionState().also {
                session.register(KronosProjectionSessionState::class, it)
                statesBySession[session] = WeakReference(it)
            }
            state.generator = generator
        }
    }

    /**
     * Returns the FIR declaration generator registered for the current session.
     */
    fun declarationGenerator(session: FirSession): FirDeclarationGenerationExtension? =
        state(session)?.generator

    internal fun memberCache(session: FirSession): ConcurrentHashMap<FirRegularClassSymbol, KronosProjectionGeneratedMembers> =
        requireNotNull(state(session)).members

    private fun state(session: FirSession): KronosProjectionSessionState? =
        synchronized(sessionLock) { statesBySession[session]?.get() }

}

/** The session owns all FIR graphs; the backend lookup index must not keep retired sessions alive. */
private class KronosProjectionSessionState : FirSessionComponent {
    val models = ConcurrentHashMap<ClassId, KronosProjectionModel>()
    val members = ConcurrentHashMap<FirRegularClassSymbol, KronosProjectionGeneratedMembers>()
    var generator: FirDeclarationGenerationExtension? = null
}

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
