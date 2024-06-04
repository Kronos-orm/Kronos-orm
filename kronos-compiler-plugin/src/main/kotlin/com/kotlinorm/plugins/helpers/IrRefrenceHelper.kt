package com.kotlinorm.plugins.helpers

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
internal fun IrPluginContext.referenceFunctions(
    packageName: String,
    functionName: String,
    className: String? = null
) = referenceFunctions(
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
internal fun IrPluginContext.referenceClass(
    classId: String
) = referenceClass(ClassId.fromString(classId.replace(".", "/")))