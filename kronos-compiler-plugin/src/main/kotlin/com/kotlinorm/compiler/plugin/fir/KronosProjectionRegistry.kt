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

package com.kotlinorm.compiler.plugin.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import java.util.concurrent.ConcurrentHashMap

// Holds projection models shared across FIR callbacks during one compilation session.
object KronosProjectionRegistry {
    private val projectionsByClassId = ConcurrentHashMap<ClassId, KronosProjectionModel>()
    private val projectionsBySymbol = ConcurrentHashMap<FirRegularClassSymbol, KronosProjectionModel>()
    private val declarationGeneratorsBySession = ConcurrentHashMap<FirSession, FirDeclarationGenerationExtension>()

    fun register(model: KronosProjectionModel) {
        projectionsByClassId[model.classId] = model
        projectionsBySymbol[model.symbol] = model
    }

    fun find(classId: ClassId): KronosProjectionModel? = projectionsByClassId[classId]

    fun find(symbol: FirRegularClassSymbol): KronosProjectionModel? = projectionsBySymbol[symbol]

    fun findNested(owner: ClassId, name: org.jetbrains.kotlin.name.Name): KronosProjectionModel? {
        return projectionsByClassId[owner.createNestedClassId(name)]
    }

    fun nestedNames(owner: ClassId): Set<org.jetbrains.kotlin.name.Name> {
        return projectionsByClassId.keys
            .filter { it.outerClassId == owner }
            .mapTo(mutableSetOf()) { it.shortClassName }
    }

    fun allClassIds(): Set<ClassId> = projectionsByClassId.keys

    fun registerDeclarationGenerator(session: FirSession, generator: FirDeclarationGenerationExtension) {
        declarationGeneratorsBySession[session] = generator
    }

    fun declarationGenerator(session: FirSession): FirDeclarationGenerationExtension? = declarationGeneratorsBySession[session]
}
