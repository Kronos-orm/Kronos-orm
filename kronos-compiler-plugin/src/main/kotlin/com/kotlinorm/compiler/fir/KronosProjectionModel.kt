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

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * Carries the generated top-level projection class shape for one refined select call.
 */
data class KronosProjectionModel(
    val classId: ClassId,
    val name: Name,
    val symbol: FirRegularClassSymbol,
    val contextClassId: ClassId,
    val contextName: Name,
    val contextSymbol: FirRegularClassSymbol,
    val sourceType: ConeKotlinType,
    val fields: List<KronosProjectionField>,
    val contextFields: List<KronosProjectionField>,
    val anchor: KtSourceElement,
)

/**
 * Describes one generated projection field and its resolved Kotlin type.
 */
data class KronosProjectionField(
    val name: Name,
    val type: ConeKotlinType,
    val source: KtSourceElement?,
    val sourceName: Name = name,
    val signature: String = name.asString(),
)
