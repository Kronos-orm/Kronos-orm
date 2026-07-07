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

import com.intellij.psi.PsiElement
import com.kotlinorm.compiler.fir.KronosIdeProjectionField
import com.kotlinorm.compiler.fir.KronosIdeProjectionModel
import com.kotlinorm.compiler.fir.KronosProjectionIdeBridge
import com.kotlinorm.compiler.utils.GeneratedProjectionPackageFqName
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSpiExtensionPoint
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtension
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionFile
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionNavigationTargetsProvider
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement

/**
 * Provides a readable declaration view for FIR-generated Kronos projection classes.
 *
 * The compiler plugin remains the semantic owner of `KronosSelectResult_*` and
 * `KronosSelectContext_*`. This resolve extension mirrors the current shape so
 * IDEA can show a data-class declaration for navigation, quick definition, and
 * scope inspection instead of an opaque generated symbol.
 */
@OptIn(KaExperimentalApi::class, KaSpiExtensionPoint::class)
class KronosProjectionDeclarationViewResolveExtensionProvider : KaResolveExtensionProvider() {
    override fun provideExtensionsFor(module: KaModule): List<KaResolveExtension> {
        return listOf(KronosProjectionDeclarationViewResolveExtension())
    }
}

@OptIn(KaExperimentalApi::class, KaSpiExtensionPoint::class)
private class KronosProjectionDeclarationViewResolveExtension : KaResolveExtension() {
    override fun getKtFiles(): List<KaResolveExtensionFile> =
        KronosProjectionIdeBridge.read()
            .takeIf { it.isNotEmpty() }
            ?.let { listOf(KronosProjectionDeclarationViewFile(it)) }
            .orEmpty()

    override fun getContainedPackages(): Set<FqName> =
        setOf(GeneratedProjectionPackageFqName)
}

@OptIn(KaExperimentalApi::class, KaSpiExtensionPoint::class)
private class KronosProjectionDeclarationViewFile(
    private val models: List<KronosIdeProjectionModel>,
) : KaResolveExtensionFile() {
    override fun getFileName(): String = "KronosProjectionDeclarations.kt"
    override fun getFilePackageName(): FqName = GeneratedProjectionPackageFqName
    override fun getTopLevelCallableNames(): Set<Name> = emptySet()
    override fun getTopLevelClassifierNames(): Set<Name> =
        models.flatMap { listOf(it.name, it.contextName) }
            .mapTo(linkedSetOf(), Name::identifier)

    override fun buildFileText(): String = buildString {
        appendLine("package ${GeneratedProjectionPackageFqName.asString()}")
        appendLine()
        models.forEach { model ->
            appendProjectionClass(model.name, model.fields)
            appendLine()
            appendProjectionClass(model.contextName, model.contextFields)
            appendLine()
        }
    }

    override fun createNavigationTargetsProvider(): KaResolveExtensionNavigationTargetsProvider =
        object : KaResolveExtensionNavigationTargetsProvider() {
            override fun KaSession.getNavigationTargets(element: KtElement): Collection<PsiElement> =
                listOf(element)
        }

    private fun StringBuilder.appendProjectionClass(name: String, fields: List<KronosIdeProjectionField>) {
        append("data class ")
        append(name)
        appendLine("(")
        fields.forEach { field ->
            append("    var ")
            append(field.name.asKotlinIdentifier())
            append(": ")
            append(field.type.asRenderableType())
            appendLine(" = null,")
        }
        appendLine(") : com.kotlinorm.interfaces.KPojo")
    }
}
