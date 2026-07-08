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

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.project.DumbService
import com.intellij.platform.backend.documentation.DocumentationData
import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReference
import com.kotlinorm.compiler.fir.KronosProjectionIdeBridge
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.expressionType
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Optional real-IDE probe for projection analysis/navigation debugging.
 *
 * Enable with:
 * -Dcom.kotlinorm.kronos.ide.probe=true
 * -Dcom.kotlinorm.kronos.ide.probe.file=kronos-testing/src/test/kotlin/.../ProjectionIdeProbeSample.kt
 * -Dcom.kotlinorm.kronos.ide.probe.targets="it.rn.asc()#rn;projectionIdeProbeRows()#projectionIdeProbeRows"
 */
class KronosProjectionProbeStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (System.getProperty(ProbeEnabledProperty) != "true" && System.getenv(ProbeEnabledEnv) != "true") return

        Thread {
            Thread.sleep(probeDelayMs())
            DumbService.getInstance(project).runWhenSmart {
                Thread {
                    runProbe(project)
                }.apply {
                    name = "Kronos projection IDE smart probe"
                    isDaemon = true
                    start()
                }
            }
        }.apply {
            name = "Kronos projection IDE probe"
            isDaemon = true
            start()
        }
    }

    private fun runProbe(project: Project) {
        try {
            ReadAction.run<RuntimeException> {
                val basePath = project.basePath ?: return@run
                val relativePath = System.getProperty(ProbeFileProperty) ?: DefaultProbeFile
                val targets = probeTargets()
                LOG.info("Kronos probe: starting; projectionBridge=${KronosProjectionIdeBridge.lastPublishSummary()}")
                val file = LocalFileSystem.getInstance().refreshAndFindFileByPath("$basePath/$relativePath")
                    ?: return@run LOG.warn("Kronos probe: file not found: $basePath/$relativePath")
                val psiFile = PsiManager.getInstance(project).findFile(file)
                    ?: return@run LOG.warn("Kronos probe: PSI file not found: ${file.path}")
                FileDocumentManager.getInstance().getDocument(file)?.let { document ->
                    targets.forEach { target ->
                        val offset = document.text.indexOf(target.text)
                        if (offset < 0) {
                            LOG.warn("Kronos probe: text not found in ${file.path}: ${target.text}")
                            return@forEach
                        }
                        val nameOffset = target.text.indexOf(target.name).coerceAtLeast(0)
                        LOG.info("Kronos probe: file=${file.path}, targetText=${target.text}, targetName=${target.name}, offset=$offset")
                        val element = psiFile.findElementAt(offset + nameOffset)
                        logDocumentationTargets(psiFile, offset + nameOffset)
                        val reference = element?.parents()
                            ?.filterIsInstance<KtNameReferenceExpression>()
                            ?.firstOrNull { it.getReferencedName() == target.name }
                        if (reference == null) {
                            LOG.warn("Kronos probe: ${target.name} reference not found at offset in ${file.path}")
                            return@forEach
                        }
                        logReference(reference)
                        (reference.parent as? KtExpression)?.let { expression ->
                            LOG.info("Kronos probe: parent expression '${expression.text}' at ${expression.textRange}")
                            logExpressionType(expression)
                        }
                        (reference.parent?.parent as? KtExpression)?.let { expression ->
                            LOG.info("Kronos probe: grandparent expression '${expression.text}' at ${expression.textRange}")
                            logExpressionType(expression)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            LOG.error("Kronos probe failed", e)
        }
    }

    private fun probeDelayMs(): Long =
        System.getProperty(ProbeDelayProperty)?.toLongOrNull()
            ?: System.getenv(ProbeDelayEnv)?.toLongOrNull()
            ?: 30000L

    private fun probeTargets(): List<ProbeTarget> {
        val raw = System.getProperty(ProbeTargetsProperty)
            ?: System.getProperty(ProbeTextProperty)?.let { "$it#id" }
            ?: DefaultProbeTargets
        return raw.split(';')
            .mapNotNull { item ->
                val text = item.substringBefore('#').trim()
                val name = item.substringAfter('#', missingDelimiterValue = "").trim()
                if (text.isEmpty() || name.isEmpty()) null else ProbeTarget(text, name)
            }
    }

    private fun PsiElement.parents(): Sequence<PsiElement> = generateSequence(this) { it.parent }

    private fun logReference(reference: KtNameReferenceExpression) {
        LOG.info("Kronos probe: reference '${reference.text}' at ${reference.textRange}")
        runCatching {
            reference.references.forEach { psiReference ->
                val resolved = when (psiReference) {
                    is PsiPolyVariantReference -> psiReference.multiResolve(false).joinToString { result ->
                        result.element.describePsi()
                    }
                    else -> psiReference.resolve().describePsi()
                }
                LOG.info("Kronos probe: psiReference=${psiReference.javaClass.name}, resolved=[$resolved]")
            }
        }.onFailure {
            LOG.error("Kronos probe: PSI resolve failed for '${reference.text}'", it)
        }

        logExpressionType(reference)
    }

    private fun logExpressionType(expression: KtExpression) {
        runCatching {
            analyze(expression) {
                val type = expression.expressionType
                val typeText = when (type) {
                    is KaClassType -> "${type::class.java.name}(rendered=${type.renderProbeType()}, classId=${type.classId}, symbol=${type.symbol})"
                    null -> "null"
                    else -> "${type::class.java.name}($type)"
                }
                LOG.info("Kronos probe: analysisType '${expression.text}' = $typeText")
            }
        }.onFailure {
            LOG.error("Kronos probe: Analysis API failed for '${expression.text}'", it)
        }
    }

    private fun KaType.renderProbeType(): String {
        val classType = this as? KaClassType ?: return toString()
        val args = classType.typeArguments
            .mapNotNull { it.type?.renderProbeType() }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "<", postfix = ">")
            .orEmpty()
        return classType.classId.asFqNameString() + args
    }

    private fun logDocumentationTargets(psiFile: com.intellij.psi.PsiFile, offset: Int) {
        runCatching {
            DocumentationTargetProvider.EP_NAME.extensionList
                .filter { it.javaClass.name.contains("KronosProjection") }
                .forEach { provider ->
                    val targets = provider.documentationTargets(psiFile, offset)
                    LOG.info("Kronos probe: documentationProvider=${provider.javaClass.name}, targetCount=${targets.size}")
                    targets.forEach { target ->
                        val presentation = target.computePresentation()
                        val documentation = target.computeDocumentation()
                        val html = (documentation as? DocumentationData)?.html
                            ?.take(400)
                            ?.replace('\n', ' ')
                        LOG.info(
                            "Kronos probe: documentationTarget presentation=${presentation.presentableText}, " +
                                "container=${presentation.containerText}, hint=${target.computeDocumentationHint()}, html=$html"
                        )
                        val link = Regex("kronos-projection://[^\"]+").find(html.orEmpty())?.value
                        if (link != null) {
                            DocumentationLinkHandler.EP_NAME.extensionList
                                .filter { it.javaClass.name.contains("KronosProjection") }
                                .forEach { handler ->
                                    val resolved = handler.resolveLink(target, link)
                                    val resolvedPresentation = resolved?.javaClass?.name
                                    LOG.info(
                                        "Kronos probe: documentationLinkHandler=${handler.javaClass.name}, " +
                                            "link=$link, resolved=$resolvedPresentation"
                                    )
                                }
                        }
                    }
                }
        }.onFailure {
            LOG.error("Kronos probe: documentation target failed at offset=$offset", it)
        }
    }

    private fun PsiElement?.describePsi(): String =
        if (this == null) {
            "null"
        } else {
            "${javaClass.name}(file=${containingFile?.name}, text='${text.take(80).replace('\n', ' ')}')"
        }

    private companion object {
        const val ProbeEnabledProperty = "com.kotlinorm.kronos.ide.probe"
        const val ProbeEnabledEnv = "KRONOS_IDE_PROBE"
        const val ProbeDelayProperty = "com.kotlinorm.kronos.ide.probe.delayMs"
        const val ProbeDelayEnv = "KRONOS_IDE_PROBE_DELAY_MS"
        const val ProbeFileProperty = "com.kotlinorm.kronos.ide.probe.file"
        const val ProbeTextProperty = "com.kotlinorm.kronos.ide.probe.text"
        const val ProbeTargetsProperty = "com.kotlinorm.kronos.ide.probe.targets"
        const val DefaultProbeFile =
            "kronos-testing/src/test/kotlin/com/kotlinorm/integration/suites/ProjectionIdeProbeSample.kt"
        const val DefaultProbeTargets =
            "it.rn.asc()#it;" +
                "it.rn.asc()#rn;" +
                "projectionIdeProbeRows().firstOrNull()?.rn#rn;" +
                "projectionIdeProbeRows().firstOrNull()#projectionIdeProbeRows;" +
                "val rows =#rows;" +
                "val row =#row;" +
                "row?.rn#row;" +
                "row?.rn#rn"
        val LOG = logger<KronosProjectionProbeStartupActivity>()
    }
}

private data class ProbeTarget(
    val text: String,
    val name: String,
)
