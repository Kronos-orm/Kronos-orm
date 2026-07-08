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

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Verifies the IntelliJ Platform fixture can start for Kronos IDEA plugin tests.
 */
class KronosIdeaPlatformSmokeTest : BasePlatformTestCase() {
    /**
     * Ensures a Kotlin file can be opened by the platform fixture.
     */
    fun testKotlinFileCanBeConfiguredInFixture() {
        myFixture.configureByText("Smoke.kt", "fun smoke(): String = \"OK\"")

        assertEquals("Smoke.kt", myFixture.file.name)
        assertEquals("fun smoke(): String = \"OK\"", myFixture.file.text)
    }

    /**
     * Ensures plugin.xml exposes the bundled FIR compiler plugin provider used for generated projections.
     */
    fun testPluginXmlRegistersKronosFirCompilerPluginProvider() {
        val pluginXml = Files.readString(Paths.get("build/resources/test/META-INF/plugin.xml"))
            .replace("\r\n", "\n")

        assertTrue(pluginXml.contains("<id>com.kotlinorm.kronos-idea-plugin</id>"))
        assertTrue(pluginXml.contains("<name>Kronos-ORM</name>"))
        assertTrue(pluginXml.contains("<b>Kronos IDEA plugin</b> brings Kronos compiler-plugin information into IntelliJ IDEA."))
        assertTrue(pluginXml.contains("href=\"https://www.kotlinorm.com/\""))
        assertTrue(pluginXml.contains("href=\"https://www.kotlinorm.com/#/documentation/en/resources/idea-plugin\""))
        assertTrue(pluginXml.contains("assets/idea-plugin/kronos-idea-projection-completion.png"))
        assertTrue(pluginXml.contains("assets/idea-plugin/kronos-idea-projection-context-docs.png"))
        assertTrue(pluginXml.contains("assets/idea-plugin/kronos-idea-code-generator.png"))
        assertTrue(pluginXml.contains("assets/idea-plugin/kronos-idea-projection-docs.png"))
        assertTrue(pluginXml.contains("kronos-idea-projection-completion.png\" width=\"640\""))
        assertTrue(pluginXml.contains("kronos-idea-projection-context-docs.png\" width=\"640\""))
        assertTrue(pluginXml.contains("kronos-idea-projection-docs.png\" width=\"640\""))
        assertTrue(pluginXml.contains("kronos-idea-code-generator.png\" width=\"640\""))
        assertFalse(pluginXml.contains("width=\"320\""))
        assertTrue(pluginXml.contains("implementation=\"com.kotlinorm.idea.KronosBundledFirCompilerPluginProvider\""))
        assertTrue(pluginXml.contains("implementation=\"com.kotlinorm.idea.KronosFirCompilerPluginConfigurationForIdeProvider\""))
        assertTrue(pluginXml.contains("implementation=\"com.kotlinorm.idea.KronosProjectionDeclarationViewResolveExtensionProvider\""))
        assertTrue(pluginXml.contains("factoryClass=\"com.kotlinorm.plugin.idea.MainWinFactory\""))
        assertTrue(pluginXml.contains("displayName=\"Kronos ORM Setting\""))
    }

    /**
     * Ensures the optional projection probe uses the current IntelliJ startup API.
     */
    fun testProjectionProbeUsesProjectActivityApi() {
        val probeSource = Files.readString(
            Paths.get("src/main/kotlin/com/kotlinorm/idea/KronosProjectionProbeStartupActivity.kt")
        )

        assertTrue(probeSource.contains("ProjectActivity"))
        assertTrue(probeSource.contains("override suspend fun execute(project: Project)"))
        assertFalse(probeSource.contains("import com.intellij.openapi.startup.StartupActivity"))
        assertFalse(probeSource.contains("runActivity(project: Project)"))
    }

    /**
     * Ensures generated projection semantics do not fall back to a default virtual Kotlin file.
     */
    fun testPluginXmlRegistersProjectionDeclarationViewWithoutOldFallback() {
        val pluginXml = Files.readString(Paths.get("src/main/resources/META-INF/plugin.xml"))
        assertTrue(pluginXml.contains("kaResolveExtensionProvider"))
        assertTrue(pluginXml.contains("KronosProjectionDeclarationViewResolveExtensionProvider"))
        assertTrue(pluginXml.contains("platform.backend.documentation.targetProvider"))
        assertTrue(pluginXml.contains("KronosProjectionDocumentationTargetProvider"))
        assertTrue(pluginXml.contains("platform.backend.documentation.psiTargetProvider"))
        assertTrue(pluginXml.contains("KronosProjectionPsiDocumentationTargetProvider"))
        assertTrue(pluginXml.contains("platform.backend.documentation.linkHandler"))
        assertTrue(pluginXml.contains("KronosProjectionDocumentationLinkHandler"))
        assertFalse(pluginXml.contains("KronosProjectionResolveExtensionProvider"))
        assertFalse(pluginXml.contains("KronosGeneratedProjections.kt"))

        val provider = Files.readString(
            Paths.get("src/main/kotlin/com/kotlinorm/idea/KronosProjectionDeclarationViewResolveExtensionProvider.kt")
        )
        assertTrue(provider.contains("data class "))
        assertTrue(provider.contains("model.name"))
        assertTrue(provider.contains("model.contextName"))
        assertTrue(provider.contains("fields"))
        assertTrue(provider.contains("contextFields"))

        val documentationProvider = Files.readString(
            Paths.get("src/main/kotlin/com/kotlinorm/idea/KronosProjectionDocumentationTargetProvider.kt")
        )
        assertTrue(documentationProvider.contains("DocumentationTargetProvider"))
        assertTrue(documentationProvider.contains("PsiDocumentationTargetProvider"))
        assertTrue(documentationProvider.contains("getReferencedName() != \"it\""))
        assertTrue(documentationProvider.contains("KronosSelectContext_"))
        assertTrue(documentationProvider.contains("data class "))
        assertTrue(documentationProvider.contains("kronos-projection://"))
        assertFalse(documentationProvider.contains("psi_element://"))
    }

    /**
     * Ensures the tool window no longer opens the old config-file/project-root placeholder.
     */
    fun testMainPanelDoesNotUseConfigPopupOrProjectRootBrowser() {
        val mainPanel = Files.readString(Paths.get("src/main/kotlin/com/kotlinorm/plugin/idea/MainPanel.kt"))
        assertFalse(mainPanel.contains("JOptionPane"))
        assertFalse(mainPanel.contains("ProjectRootManager"))
        assertFalse(mainPanel.contains("configJsonFile"))
        assertTrue(mainPanel.contains("Code Generator"))
        assertTrue(mainPanel.contains("Templates"))
    }
}
