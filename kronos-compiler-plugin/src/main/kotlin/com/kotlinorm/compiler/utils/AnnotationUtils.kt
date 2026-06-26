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

package com.kotlinorm.compiler.utils

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.FqName

/**
 * Annotation utility functions for Kronos compiler plugin
 *
 * Provides helper functions for reading and processing annotations on IR elements
 */

/**
 * Extension property to get value arguments from IrFunctionAccessExpression
 * Uses the arguments property which is the stable K2 API
 * 
 * This follows the same pattern as IrMemberAccessExpressionHelper from the old plugin
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
private val IrFunctionAccessExpression.valueArgs: List<IrExpression?>
    get() = arguments.filterIndexed { index, _ ->
        symbol.owner.parameters[index].kind == IrParameterKind.Regular
    }

/**
 * Finds an annotation by its fully qualified name
 *
 * @param fqName The fully qualified name of the annotation to find
 * @return The IrConstructorCall representing the annotation, or null if not found
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrAnnotationContainer.findAnnotation(fqName: FqName): IrConstructorCall? {
    return annotations.find { 
        it.symbol.owner.returnType.getClass()?.fqNameWhenAvailable == fqName 
    }
}

/**
 * Gets the value argument from an annotation at the specified index
 *
 * @param annotation The annotation to read from
 * @param index The parameter index (default: 0)
 * @return The IrExpression representing the value, or null if not found
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun getAnnotationValueArgument(annotation: IrConstructorCall?, index: Int = 0): IrExpression? {
    if (annotation == null) return null
    return annotation.valueArgs.getOrNull(index)
}

/**
 * Gets the value of a string parameter from an annotation
 *
 * @param annotation The annotation to read from
 * @param index The parameter index (default: 0)
 * @return The string value, or null if not found or not a string
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun getAnnotationStringValue(annotation: IrConstructorCall?, index: Int = 0): String? {
    val arg = getAnnotationValueArgument(annotation, index) ?: return null
    return when (arg) {
        is IrConstImpl -> arg.value as? String
        else -> null
    }
}

/**
 * Gets the value of an integer parameter from an annotation
 *
 * @param annotation The annotation to read from
 * @param index The parameter index (default: 0)
 * @return The integer value, or null if not found or not an integer
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun getAnnotationIntValue(annotation: IrConstructorCall?, index: Int = 0): Int? {
    val arg = getAnnotationValueArgument(annotation, index) ?: return null
    return when (arg) {
        is IrConstImpl -> arg.value as? Int
        else -> null
    }
}

/**
 * Gets the value of a boolean parameter from an annotation
 *
 * @param annotation The annotation to read from
 * @param index The parameter index (default: 0)
 * @return The boolean value, or null if not found or not a boolean
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun getAnnotationBooleanValue(annotation: IrConstructorCall?, index: Int = 0): Boolean? {
    val arg = getAnnotationValueArgument(annotation, index) ?: return null
    return when (arg) {
        is IrConstImpl -> arg.value as? Boolean
        else -> null
    }
}

/**
 * Gets the @Column annotation's name parameter
 *
 * @return The column name, or null if annotation not present or no name specified
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.getColumnName(): String? {
    val annotation = findAnnotation(ColumnAnnotationFqName) ?: return null
    return getAnnotationStringValue(annotation, 0)
}

/**
 * Gets the @ColumnType annotation's type parameter
 *
 * @return The column type name, or null if annotation not present
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.getColumnTypeName(): String? {
    val annotation = findAnnotation(ColumnTypeAnnotationFqName) ?: return null
    // The type parameter is an enum value, we need to extract its name
    val typeArg = annotation.valueArgs.getOrNull(0) ?: return null
    // For enum values, we can get the name from the IrGetEnumValue expression
    return typeArg.toString() // This will need refinement based on actual IR structure
}

/**
 * Gets the @ColumnType annotation's length parameter
 *
 * @return The column length, or null if not specified
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.getColumnTypeLength(): Int? {
    val annotation = findAnnotation(ColumnTypeAnnotationFqName) ?: return null
    return getAnnotationIntValue(annotation, 1)
}

/**
 * Gets the @ColumnType annotation's scale parameter
 *
 * @return The column scale, or null if not specified
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.getColumnTypeScale(): Int? {
    val annotation = findAnnotation(ColumnTypeAnnotationFqName) ?: return null
    return getAnnotationIntValue(annotation, 2)
}

/**
 * Gets the @DateTimeFormat annotation's format parameter
 *
 * @return The date/time format string, or null if annotation not present
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.getDateTimeFormat(): String? {
    val annotation = findAnnotation(DateTimeFormatAnnotationFqName) ?: return null
    return getAnnotationStringValue(annotation, 0)
}

/**
 * Gets the @Default annotation's value parameter
 *
 * @return The default value as a string, or null if annotation not present
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.getDefaultValue(): String? {
    val annotation = findAnnotation(DefaultValueAnnotationFqName) ?: return null
    return getAnnotationStringValue(annotation, 0)
}

/**
 * Checks if the property has @PrimaryKey annotation with identity=true
 *
 * @return true if the property is an identity primary key
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.isIdentityPrimaryKey(): Boolean {
    val annotation = findAnnotation(PrimaryKeyAnnotationFqName) ?: return false
    return getAnnotationBooleanValue(annotation, 0) ?: false
}

/**
 * Checks if the property has @PrimaryKey annotation with uuid=true
 *
 * @return true if the property is a UUID primary key
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.isUuidPrimaryKey(): Boolean {
    val annotation = findAnnotation(PrimaryKeyAnnotationFqName) ?: return false
    return getAnnotationBooleanValue(annotation, 1) ?: false
}

/**
 * Checks if the property has @Necessary annotation (required field)
 *
 * @return true if the property is marked as necessary
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.isNecessary(): Boolean {
    return findAnnotation(NecessaryAnnotationFqName) != null
}

/**
 * Gets the @Table annotation's name parameter from a class
 *
 * @return The table name, or null if annotation not present or no name specified
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrClass.getTableName(): String? {
    val annotation = findAnnotation(TableAnnotationFqName) ?: return null
    return getAnnotationStringValue(annotation, 0)
}

/**
 * Collects all annotations from a property into a structured format
 *
 * @return A map of annotation FQN to IrConstructorCall
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.collectAnnotations(): Map<FqName, IrConstructorCall> {
    val result = mutableMapOf<FqName, IrConstructorCall>()
    
    annotations.forEach { annotation ->
        val fqName = annotation.symbol.owner.returnType.getClass()?.fqNameWhenAvailable
        if (fqName != null) {
            result[fqName] = annotation
        }
    }
    
    return result
}

/**
 * Checks if a property should be ignored based on @Ignore annotation
 *
 * The @Ignore annotation can have an action parameter that specifies when to ignore:
 * - "all": ignore in all operations
 * - "select": ignore in select operations
 * - "insert": ignore in insert operations
 * - "update": ignore in update operations
 * - "to_map": ignore when converting to map
 *
 * @param action The action to check (e.g., "select", "insert", "update", "to_map", "all")
 * @return true if the property should be ignored for the specified action
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.shouldIgnore(action: String = "all"): Boolean {
    val annotation = findAnnotation(IgnoreAnnotationFqName) ?: return false
    
    // If no action specified in annotation, ignore for all actions
    val ignoreAction = getAnnotationStringValue(annotation, 0) ?: return true
    
    // Check if the action matches
    return ignoreAction == "all" || ignoreAction == action
}
