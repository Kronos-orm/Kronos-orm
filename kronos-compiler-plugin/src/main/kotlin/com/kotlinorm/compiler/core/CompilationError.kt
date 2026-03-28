/**
 * Copyright 2022-2025 kronos-orm
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

package com.kotlinorm.compiler.core

import org.jetbrains.kotlin.ir.IrElement

/**
 * Represents a compilation error in the Kronos compiler plugin
 *
 * @property element The IR element where the error occurred
 * @property errorMessage The error message
 * @property suggestion Optional suggestion for fixing the error
 */
data class CompilationError(
    val element: IrElement,
    val errorMessage: String,
    val suggestion: String? = null
) : Exception(errorMessage)
