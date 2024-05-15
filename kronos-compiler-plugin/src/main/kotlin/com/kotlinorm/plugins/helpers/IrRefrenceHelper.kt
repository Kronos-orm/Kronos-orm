package com.kotlinorm.plugins.helpers

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

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

internal fun IrPluginContext.referenceClass(
    classId: String
) = referenceClass(ClassId.fromString(classId.replace(".", "/")))