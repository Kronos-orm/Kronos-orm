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
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds generated projection models shared across FIR callbacks during one compilation.
 */
object KronosProjectionRegistry {
    private val projectionsBySession = ConcurrentHashMap<FirSession, ConcurrentHashMap<ClassId, KronosProjectionModel>>()
    private val declarationGeneratorsBySession = ConcurrentHashMap<FirSession, FirDeclarationGenerationExtension>()

    /**
     * Registers a projection model produced while refining a select call.
     */
    fun register(session: FirSession, model: KronosProjectionModel) {
        projectionsBySession.computeIfAbsent(session) { ConcurrentHashMap() }.also { models ->
            models[model.classId] = model
            models[model.contextClassId] = model
        }
    }

    /**
     * Refines fields that were first seen as receiver-less alias literals before
     * FIR resolved the aliased scalar query call.
     */
    fun refineAliasFieldType(session: FirSession, alias: String, type: ConeKotlinType) {
        val models = projectionsBySession[session] ?: return
        val aliasName = Name.identifier(alias)
        val updated = models.values.distinctBy { it.classId }.mapNotNull { model ->
            val fields = model.fields.refineAlias(aliasName, type)
            val contextFields = model.contextFields.refineAlias(aliasName, type)
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
    }

    /**
     * Looks up a projection model by its generated class id.
     */
    fun find(session: FirSession, classId: ClassId): KronosProjectionModel? =
        projectionsBySession[session]?.get(classId)

    /**
     * Looks up a projection model after FIR session information is no longer available.
     */
    fun findAny(classId: ClassId): KronosProjectionModel? =
        projectionsBySession.values.asSequence().mapNotNull { it[classId] }.firstOrNull()

    /**
     * Returns all generated top-level projection class ids known to this compilation.
     */
    fun allTopLevelClassIds(session: FirSession): Set<ClassId> =
        projectionsBySession[session]?.keys?.toSet().orEmpty()

    /**
     * Stores the FIR declaration generator registered for the current session.
     */
    fun registerDeclarationGenerator(session: FirSession, generator: FirDeclarationGenerationExtension) {
        declarationGeneratorsBySession[session] = generator
    }

    /**
     * Returns the FIR declaration generator registered for the current session.
     */
    fun declarationGenerator(session: FirSession): FirDeclarationGenerationExtension? = declarationGeneratorsBySession[session]
}

private fun List<KronosProjectionField>.refineAlias(
    alias: Name,
    type: ConeKotlinType
): List<KronosProjectionField> {
    var changed = false
    val refined = map { field ->
        if (field.name == alias && field.type != type) {
            changed = true
            field.copy(type = type, signature = "${field.signature}:refined:${type}")
        } else {
            field
        }
    }
    return if (changed) refined else this
}
