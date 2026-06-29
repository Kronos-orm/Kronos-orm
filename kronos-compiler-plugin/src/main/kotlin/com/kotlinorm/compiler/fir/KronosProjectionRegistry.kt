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
import org.jetbrains.kotlin.name.ClassId
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds generated projection models shared across FIR callbacks during one compilation.
 */
object KronosProjectionRegistry {
    private val projectionsByClassId = ConcurrentHashMap<ClassId, KronosProjectionModel>()
    private val declarationGeneratorsBySession = ConcurrentHashMap<FirSession, FirDeclarationGenerationExtension>()
    private val topLevelClassIds = ConcurrentHashMap.newKeySet<ClassId>()

    /**
     * Registers a projection model produced while refining a select call.
     */
    fun register(model: KronosProjectionModel) {
        projectionsByClassId[model.classId] = model
        topLevelClassIds += model.classId
    }

    /**
     * Looks up a projection model by its generated class id.
     */
    fun find(classId: ClassId): KronosProjectionModel? = projectionsByClassId[classId]

    /**
     * Returns all generated top-level projection class ids known to this compilation.
     */
    fun allTopLevelClassIds(): Set<ClassId> =
        topLevelClassIds

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
