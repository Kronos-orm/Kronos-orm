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

package com.kotlinorm.idea

import com.kotlinorm.compiler.fir.KronosIdeProjectionField
import com.kotlinorm.compiler.fir.KronosIdeProjectionModel
import com.kotlinorm.compiler.utils.GeneratedProjectionPackageFqName

internal fun String.asKotlinIdentifier(): String =
    if (matches(KotlinIdentifierRegex) && this !in KotlinKeywords) this else "`$this`"

internal fun String.asRenderableType(): String =
    takeIf { it.isNotBlank() } ?: "kotlin.Any?"

internal data class KronosProjectionClassDeclaration(
    val name: String,
    val fields: List<KronosIdeProjectionField>,
    val conflictingShapeCount: Int,
)

internal fun projectionClassDeclarations(
    models: List<KronosIdeProjectionModel>,
): List<KronosProjectionClassDeclaration> =
    models
        .flatMap { model ->
            listOf(
                KronosProjectionClassDeclaration(model.name, model.fields, 1),
                KronosProjectionClassDeclaration(model.contextName, model.contextFields, 1),
            )
        }
        .groupBy { it.name }
        .toSortedMap()
        .map { (name, candidates) ->
            val shapes = candidates
                .distinctBy { it.fields.projectionShapeKey() }
                .sortedBy { it.fields.projectionShapeKey() }
            shapes.first().copy(name = name, conflictingShapeCount = shapes.size)
        }

internal fun List<KronosIdeProjectionModel>.canonicalProjectionClassDeclaration(
    className: String,
): KronosProjectionClassDeclaration? =
    projectionClassDeclarations(this).firstOrNull { declaration -> declaration.name == className }

internal fun renderProjectionDeclarationFile(models: List<KronosIdeProjectionModel>): String {
    val declarations = projectionClassDeclarations(models)
    return buildString {
        appendLine("package ${GeneratedProjectionPackageFqName.asString()}")
        declarations.forEach { declaration ->
            appendLine()
            if (declaration.conflictingShapeCount > 1) {
                append("// Kronos projection shape conflict for ")
                append(declaration.name)
                append(": selected [")
                append(declaration.fields.projectionShapeDescription())
                append("], ignored ")
                append(declaration.conflictingShapeCount - 1)
                appendLine(" alternative(s).")
            }
            appendLine(renderProjectionClass(declaration.name, declaration.fields))
        }
    }
}

internal fun renderProjectionClass(name: String, fields: List<KronosIdeProjectionField>): String =
    buildString {
        append("data class ")
        append(name.asKotlinIdentifier())
        appendLine("(")
        fields.forEach { field ->
            val type = field.type.asRenderableType()
            append("    var ")
            append(field.name.asKotlinIdentifier())
            append(": ")
            append(type)
            if (type.trim().endsWith("?")) {
                append(" = null")
            }
            appendLine(",")
        }
        append(") : com.kotlinorm.interfaces.KPojo")
    }

private fun List<KronosIdeProjectionField>.projectionShapeKey(): String =
    joinToString("\u0000") { field -> "${field.name}\u0001${field.type.asRenderableType()}" }

private fun List<KronosIdeProjectionField>.projectionShapeDescription(): String =
    joinToString { field -> "${field.name}: ${field.type.asRenderableType()}" }

private val KotlinIdentifierRegex = Regex("[A-Za-z_][A-Za-z0-9_]*")

private val KotlinKeywords = setOf(
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "interface",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while",
)
