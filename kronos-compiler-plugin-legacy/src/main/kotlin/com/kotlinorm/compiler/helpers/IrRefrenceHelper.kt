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

package com.kotlinorm.compiler.helpers

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Retrieves a reference to a function using its package name, function name, and optional class name.
 *
 * @param packageName The fully qualified name of the package containing the function.
 * @param functionName The name of the function.
 * @param className The fully qualified name of the class containing the function, if applicable. Defaults to null.
 * @return The reference to the function, or null if the function is not found.
 */
context(context: IrPluginContext)
internal fun referenceFunctions(
    packageName: String,
    functionName: String,
    className: String? = null
) = context.referenceFunctions(
    CallableId(
        FqName(packageName),
        className?.let { FqName(it) },
        Name.identifier(functionName)
    )
)

/**
 * Retrieves a reference to a class using its fully qualified name.
 *
 * @param classId The fully qualified name of the class, with periods replaced by slashes.
 * @return The reference to the class, or null if the class is not found.
 */
context(context: IrPluginContext)
internal fun referenceClass(
    classId: String
) = context.referenceClass(ClassId.fromString(classId.replace(".", "/")))