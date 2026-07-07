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

import com.intellij.model.Pointer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.LinkResolveResult
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.kotlinorm.compiler.fir.KronosIdeProjectionField
import com.kotlinorm.compiler.fir.KronosIdeProjectionModel
import com.kotlinorm.compiler.fir.KronosProjectionIdeBridge
import com.kotlinorm.compiler.utils.GeneratedProjectionPackageFqName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.expressionType
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression

/**
 * Shows the generated projection/context shape when users hover the implicit `it` receiver.
 *
 * The type still comes from the compiler plugin. This provider only renders the already
 * published shape in quick documentation so the generated Context/Result is inspectable.
 */
class KronosProjectionDocumentationTargetProvider : DocumentationTargetProvider, PsiDocumentationTargetProvider {
    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        return documentationTargets(file.findElementAt(offset), offset)
    }

    override fun documentationTargets(element: PsiElement, originalElement: PsiElement?): List<DocumentationTarget> {
        return documentationTargets(originalElement ?: element, originalElement?.textOffset ?: element.textOffset)
    }

    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        return documentationTargets(element, originalElement).firstOrNull()
    }

    private fun documentationTargets(element: PsiElement?, offset: Int): List<DocumentationTarget> {
        val property = element
            ?.parents()
            ?.filterIsInstance<KtProperty>()
            ?.firstOrNull { it.nameIdentifier?.textRange?.contains(offset) == true }
        if (property != null) {
            val projectionType = property.initializer?.kronosProjectionType() ?: return emptyList()
            val model = projectionType.findModel() ?: return emptyList()
            val fields = if (projectionType.className == model.contextName) model.contextFields else model.fields
            return listOf(
                KronosProjectionDocumentationTarget(
                    property,
                    property.name ?: "it",
                    projectionType.subjectType,
                    projectionType.className,
                    fields
                )
            )
        }

        val reference = element
            ?.parents()
            ?.filterIsInstance<KtNameReferenceExpression>()
            ?.firstOrNull()
            ?: return emptyList()

        if (!reference.isProjectionDocumentationCandidate()) {
            return emptyList()
        }

        if (reference.getReferencedName() != "it") {
            val fieldTargets = reference.projectionFieldDocumentationTargets()
            if (fieldTargets.isNotEmpty()) return fieldTargets
        }

        val expression = reference.parent as? KtExpression ?: reference
        val expressionProjectionType = expression.kronosProjectionType() ?: expression.parentExpressionProjectionType()
        if (expressionProjectionType != null && reference.getReferencedName() != "it") {
            val model = expressionProjectionType.findModel() ?: return emptyList()
            val fields = if (expressionProjectionType.className == model.contextName) model.contextFields else model.fields
            return listOf(
                KronosProjectionDocumentationTarget(
                    reference,
                    reference.getReferencedName(),
                    expressionProjectionType.subjectType,
                    expressionProjectionType.className,
                    fields
                )
            )
        }

        if (reference.getReferencedName() != "it") {
            return reference.projectionFieldDocumentationTargets()
        }

        val projectionType = reference.kronosProjectionType() ?: return reference.projectionFieldDocumentationTargets()
        val model = projectionType.findModel() ?: return emptyList()
        val fields = if (projectionType.className == model.contextName) model.contextFields else model.fields
        return listOf(
            KronosProjectionDocumentationTarget(
                reference,
                "it",
                projectionType.subjectType,
                projectionType.className,
                fields
            )
        )
    }

    private fun KtNameReferenceExpression.projectionFieldDocumentationTargets(): List<DocumentationTarget> {
        val fieldName = getReferencedName()
        val qualified = parent
        val receiver = when (qualified) {
            is KtDotQualifiedExpression -> {
                if (qualified.selectorExpression != this) return emptyList()
                qualified.receiverExpression
            }
            is KtSafeQualifiedExpression -> {
                if (qualified.selectorExpression != this) return emptyList()
                qualified.receiverExpression
            }
            else -> return emptyList()
        }
        val projectionType = receiver.kronosProjectionType() ?: return emptyList()
        val model = projectionType.findModel() ?: return emptyList()
        val fields = if (projectionType.className == model.contextName) model.contextFields else model.fields
        val field = fields.firstOrNull { it.name == fieldName } ?: return emptyList()
        return listOf(KronosProjectionFieldDocumentationTarget(this, projectionType.className, field))
    }
}

private fun KtNameReferenceExpression.isProjectionDocumentationCandidate(): Boolean {
    val name = getReferencedName()
    if (name == "it") {
        return projectionType() != null
    }

    val qualified = parent
    val receiver = when (qualified) {
        is KtDotQualifiedExpression -> qualified.receiverExpression.takeIf { qualified.selectorExpression == this }
        is KtSafeQualifiedExpression -> qualified.receiverExpression.takeIf { qualified.selectorExpression == this }
        else -> null
    }
    return receiver != null || name.startsWith("KronosSelectResult_") || name.startsWith("KronosSelectContext_")
}

private class KronosProjectionDocumentationTarget(
    element: PsiElement,
    private val subjectName: String,
    private val subjectType: String,
    private val className: String,
    private val fields: List<KronosIdeProjectionField>,
) : DocumentationTarget {
    private val pointer = SmartPointerManager.createPointer(element)

    override fun createPointer(): Pointer<out DocumentationTarget> =
        Pointer.delegatingPointer(pointer) { element ->
            KronosProjectionDocumentationTarget(element, subjectName, subjectType, className, fields)
        }

    override fun computePresentation(): TargetPresentation =
        TargetPresentation.builder("$subjectName: $subjectType")
            .containerText(GeneratedProjectionPackageFqName.asString())
            .presentation()

    override fun computeDocumentationHint(): String =
        "$subjectName: $subjectType"

    override fun computeDocumentation(): DocumentationResult =
        DocumentationResult.documentation(buildDocumentationHtml())

    private fun buildDocumentationHtml(): String = buildString {
        append("<div class='definition'><pre>")
        append(subjectName.escapeHtml())
        append(": ")
        append(subjectType.linkProjectionTypeNames())
        append("</pre></div>")
        append("<div class='content'><pre>")
        append(renderProjectionClass(className, fields).toHighlightedKotlinHtml())
        append("</pre></div>")
    }

    private fun projectionTypeLink(name: String): String {
        val fqName = "${GeneratedProjectionPackageFqName.asString()}.$name"
        return "<a href=\"$ProjectionDocScheme$fqName\">${name.escapeHtml()}</a>"
    }
}

class KronosProjectionDocumentationLinkHandler : DocumentationLinkHandler {
    override fun resolveLink(target: DocumentationTarget, url: String): LinkResolveResult? {
        val fqName = url.removePrefix(ProjectionDocScheme).takeIf { it != url } ?: return null
        val className = fqName.substringAfterLast('.')
        val model = KronosProjectionIdeBridge.read()
            .firstOrNull { it.name == className || it.contextName == className }
            ?: return null
        val fields = if (model.contextName == className) model.contextFields else model.fields
        return LinkResolveResult.resolvedTarget(
            KronosProjectionClassDocumentationTarget(className, fields)
        )
    }
}

private class KronosProjectionClassDocumentationTarget(
    private val className: String,
    private val fields: List<KronosIdeProjectionField>,
) : DocumentationTarget {
    override fun createPointer(): Pointer<out DocumentationTarget> =
        Pointer.hardPointer(this)

    override fun computePresentation(): TargetPresentation =
        TargetPresentation.builder(className)
            .containerText(GeneratedProjectionPackageFqName.asString())
            .presentation()

    override fun computeDocumentationHint(): String =
        "${GeneratedProjectionPackageFqName.asString()}.$className"

    override fun computeDocumentation(): DocumentationResult =
        DocumentationResult.documentation(
            "<div class='definition'><pre>${renderProjectionClass(className, fields).toHighlightedKotlinHtml()}</pre></div>"
        )
}

private class KronosProjectionFieldDocumentationTarget(
    element: KtNameReferenceExpression,
    private val className: String,
    private val field: KronosIdeProjectionField,
) : DocumentationTarget {
    private val pointer = SmartPointerManager.createPointer(element)

    override fun createPointer(): Pointer<out DocumentationTarget> =
        Pointer.delegatingPointer(pointer) { element ->
            KronosProjectionFieldDocumentationTarget(element, className, field)
        }

    override fun computePresentation(): TargetPresentation =
        TargetPresentation.builder("${field.name}: ${field.type.asRenderableType()}")
            .containerText(className)
            .presentation()

    override fun computeDocumentationHint(): String =
        "${field.name}: ${field.type.asRenderableType()}"

    override fun computeDocumentation(): DocumentationResult =
        DocumentationResult.documentation(
            buildString {
                append("<div class='definition'><pre>")
                append("var ")
                append(field.name.asKotlinIdentifier().escapeHtml())
                append(": ")
                append(field.type.asRenderableType().escapeHtml())
                append("</pre></div>")
                append("<div class='content'>Declared in ")
                append(projectionTypeLink(className))
                append("</div>")
            }
        )

    private fun projectionTypeLink(name: String): String {
        val fqName = "${GeneratedProjectionPackageFqName.asString()}.$name"
        return "<a href=\"$ProjectionDocScheme$fqName\">${name.escapeHtml()}</a>"
    }
}

private data class KronosProjectionType(
    val classId: ClassId,
    val subjectType: String,
) {
    val className: String = classId.shortClassName.asString()
}

private fun KtExpression.kronosProjectionType(): KronosProjectionType? =
    projectionType()
        ?: projectionTypeFromLocalInitializer()
        ?: projectionTypeFromQualifiedReceiver()

private fun KtExpression.projectionType(): KronosProjectionType? =
    runCatching {
        analyze(this) {
            expressionType?.toProjectionType()
        }
    }.getOrNull()

private fun KtExpression.projectionTypeFromLocalInitializer(): KronosProjectionType? {
    val reference = this as? KtNameReferenceExpression ?: return null
    val property = reference.references
        .asSequence()
        .mapNotNull { it.resolve() }
        .mapNotNull { resolved -> resolved.parents().filterIsInstance<KtProperty>().firstOrNull() }
        .firstOrNull()
        ?: return null
    return property.initializer?.kronosProjectionType()
}

private fun KtExpression.projectionTypeFromQualifiedReceiver(): KronosProjectionType? {
    val qualified = this as? KtQualifiedExpression ?: return null
    val selector = qualified.selectorExpression
    val selectorName = (selector as? KtNameReferenceExpression)?.getReferencedName()
        ?: (selector as? KtCallExpression)
            ?.calleeExpression
            ?.let { it as? KtNameReferenceExpression }
            ?.getReferencedName()
    if (selectorName != "firstOrNull" && selectorName != "singleOrNull" && selectorName != "first") return null
    val receiverType = qualified.receiverExpression.kronosProjectionType() ?: return null
    val nullable = selectorName == "firstOrNull" || selectorName == "singleOrNull"
    val subjectType = receiverType.classId.asFqNameString() + if (nullable) "?" else ""
    return KronosProjectionType(receiverType.classId, subjectType)
}

private fun KtExpression.parentExpressionProjectionType(): KronosProjectionType? {
    val expression = parents().filterIsInstance<KtExpression>().drop(1).firstOrNull() ?: return null
    return expression.kronosProjectionType()
}

private fun KaType.toProjectionType(): KronosProjectionType? {
    val type = this as? KaClassType ?: return null
    val directClassId = type.classId.takeIf { it.isKronosProjectionClassId() }
    if (directClassId != null) return KronosProjectionType(directClassId, renderProjectionTypeText())
    val argumentClassId = type.typeArguments.firstOrNull()
        ?.type
        ?.let { it as? KaClassType }
        ?.classId
        ?.takeIf { it.isKronosProjectionClassId() }
        ?: return null
    return KronosProjectionType(argumentClassId, renderProjectionTypeText())
}

private fun KaType.renderProjectionTypeText(): String {
    val classType = this as? KaClassType ?: return toString()
    val args = classType.typeArguments
        .mapNotNull { it.type?.renderProjectionTypeText() }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(prefix = "<", postfix = ">")
        .orEmpty()
    return classType.classId.asFqNameString() + args + if (toString().trim().endsWith("?")) "?" else ""
}

private fun ClassId.isKronosProjectionClassId(): Boolean {
    if (packageFqName != GeneratedProjectionPackageFqName) return false
    val className = shortClassName.asString()
    return className.startsWith("KronosSelectContext_") || className.startsWith("KronosSelectResult_")
}

private fun KronosProjectionType.findModel(): KronosIdeProjectionModel? =
    KronosProjectionIdeBridge.read().firstOrNull { it.name == className || it.contextName == className }

private fun renderProjectionClass(name: String, fields: List<KronosIdeProjectionField>): String {
    val constructor = FunSpec.constructorBuilder()
    val properties = mutableListOf<PropertySpec>()
    fields.forEach { field ->
        val parameterName = field.name.asKotlinIdentifier()
        val type = field.type.asRenderableType().toKotlinPoetTypeName()
        constructor.addParameter(
            ParameterSpec.builder(parameterName, type)
                .defaultValue("null")
                .build()
        )
        properties += PropertySpec.builder(parameterName, type)
            .mutable(true)
            .initializer(parameterName)
            .build()
    }
    return TypeSpec.classBuilder(name)
        .addModifiers(KModifier.DATA)
        .primaryConstructor(constructor.build())
        .addProperties(properties)
        .addSuperinterface(ClassName("com.kotlinorm.interfaces", "KPojo"))
        .build()
        .toString()
}

private fun String.linkProjectionTypeNames(): String {
    val packageName = GeneratedProjectionPackageFqName.asString()
    return Regex("""\bKronosSelect(?:Result|Context)_[A-Za-z0-9_]+\b|$packageName\.KronosSelect(?:Result|Context)_[A-Za-z0-9_]+""")
        .replace(escapeHtml()) { match ->
            val escaped = match.value.escapeHtml()
            val className = match.value.substringAfterLast('.')
            val fqName = if (match.value.contains('.')) match.value else "$packageName.$className"
            "<a href=\"$ProjectionDocScheme$fqName\">$escaped</a>"
        }
}

private fun String.toHighlightedKotlinHtml(): String {
    val keywords = setOf(
        "data", "class", "var", "val", "fun", "interface", "object", "null",
        "true", "false", "is", "as", "in", "out", "return", "package", "import"
    )
    val builder = StringBuilder()
    var index = 0
    while (index < length) {
        val char = this[index]
        when {
            char.isLetter() || char == '_' -> {
                val start = index
                index++
                while (index < length && (this[index].isLetterOrDigit() || this[index] == '_')) index++
                val token = substring(start, index).escapeHtml()
                when {
                    token in keywords -> builder.append("<span style=\"color:#0033b3;font-weight:bold;\">")
                        .append(token)
                        .append("</span>")
                    token.firstOrNull()?.isUpperCase() == true -> builder.append("<span style=\"color:#00627a;\">")
                        .append(token)
                        .append("</span>")
                    else -> builder.append(token)
                }
            }
            char.isDigit() -> {
                val start = index
                index++
                while (index < length && this[index].isDigit()) index++
                builder.append("<span style=\"color:#1750eb;\">")
                    .append(substring(start, index).escapeHtml())
                    .append("</span>")
            }
            char == '"' -> {
                val start = index
                index++
                while (index < length) {
                    val current = this[index++]
                    if (current == '"' && this.getOrNull(index - 2) != '\\') break
                }
                builder.append("<span style=\"color:#067d17;\">")
                    .append(substring(start, index).escapeHtml())
                    .append("</span>")
            }
            else -> {
                builder.append(char.toString().escapeHtml())
                index++
            }
        }
    }
    return builder.toString()
}

private fun String.toKotlinPoetTypeName(): TypeName =
    KotlinTypeNameParser(this).parseTypeName()

private fun PsiElement.parents(): Sequence<PsiElement> = generateSequence(this) { it.parent }

private fun String.escapeHtml(): String = StringUtil.escapeXmlEntities(this)

private const val ProjectionDocScheme = "kronos-projection://"

private class KotlinTypeNameParser(
    private val source: String,
) {
    private var index = 0

    fun parseTypeName(): TypeName {
        val type = parseType()
        skipWhitespace()
        return type
    }

    private fun parseType(): TypeName {
        skipWhitespace()
        val rawName = readQualifiedName().ifBlank { "kotlin.Any" }
        val typeArguments = if (peek() == '<') {
            index++
            mutableListOf<TypeName>().apply {
                do {
                    add(parseType())
                    skipWhitespace()
                    val next = peek()
                    if (next == ',') index++
                } while (next == ',')
            }.also {
                if (peek() == '>') index++
            }
        } else {
            emptyList()
        }
        skipWhitespace()
        val nullable = if (peek() == '?') {
            index++
            true
        } else {
            false
        }
        val className = rawName.toKotlinPoetClassName()
        return if (typeArguments.isEmpty()) {
            className.copy(nullable = nullable)
        } else {
            className.parameterizedBy(typeArguments).copy(nullable = nullable)
        }
    }

    private fun readQualifiedName(): String {
        val start = index
        while (index < source.length) {
            val char = source[index]
            if (char.isLetterOrDigit() || char == '_' || char == '.') {
                index++
            } else {
                break
            }
        }
        return source.substring(start, index)
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) index++
    }

    private fun peek(): Char? = source.getOrNull(index)
}

private fun String.toKotlinPoetClassName(): ClassName {
    val fqName = takeIf { it.contains('.') } ?: "kotlin.$this"
    val packageName = fqName.substringBeforeLast('.', missingDelimiterValue = "")
    val simpleNames = fqName.substringAfterLast('.').split('.')
    return ClassName(packageName, simpleNames)
}
