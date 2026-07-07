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

package com.kotlinorm.compiler.fir

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import com.intellij.util.diff.FlyweightCapableTreeStructure
import java.lang.reflect.Proxy
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KronosProjectionIdeBridgeTest {
    @Test
    fun `read decodes current payloads and ignores malformed or old lines`() = withCleanIdeBridgeProperties {
        val current = encodedLine(
            "main",
            "KronosSelectResult_current",
            encodedFields("id" to "kotlin.Int?", "name" to "kotlin.String?"),
            "KronosSelectScope_current",
            encodedFields("id" to "kotlin.Int?", "total" to "kotlin.Long?"),
        )
        val duplicate = current
        val oldPayloadWithoutModule = encodedLine(
            "KronosSelectResult_legacy",
            encodedFields("uid" to "kotlin.Int?"),
            "KronosSelectScope_legacy",
            encodedFields("uid" to "kotlin.Int?"),
        )
        System.setProperty(
            ProjectionPayloadProperty,
            listOf(current, "malformed", duplicate, oldPayloadWithoutModule).joinToString("\n")
        )

        val models = KronosProjectionIdeBridge.read()

        assertEquals(1, models.size)
        assertEquals(
            KronosIdeProjectionModel(
                moduleName = "main",
                name = "KronosSelectResult_current",
                fields = listOf(
                    KronosIdeProjectionField("id", "kotlin.Int?"),
                    KronosIdeProjectionField("name", "kotlin.String?"),
                ),
                contextName = "KronosSelectScope_current",
                contextFields = listOf(
                    KronosIdeProjectionField("id", "kotlin.Int?"),
                    KronosIdeProjectionField("total", "kotlin.Long?"),
                ),
            ),
            models[0],
        )
    }

    @Test
    fun `ide active state and last publish summary read system properties`() = withCleanIdeBridgeProperties {
        assertFalse(KronosProjectionIdeBridge.isIdeActive())
        assertFalse(KronosProjectionIdeBridge.isResolveExtensionFallbackEnabled())
        assertEquals(emptyList(), KronosProjectionIdeBridge.read())

        KronosProjectionIdeBridge.markIdeActive()
        assertTrue(KronosProjectionIdeBridge.isIdeActive())

        System.setProperty(ResolveExtensionFallbackProperty, "true")
        assertTrue(KronosProjectionIdeBridge.isResolveExtensionFallbackEnabled())

        System.setProperty(ProjectionCountProperty, "3")
        System.setProperty(ProjectionPayloadSizeProperty, "128")
        assertEquals("count=3, payloadSize=128", KronosProjectionIdeBridge.lastPublishSummary())
    }

    @Test
    fun `read handles missing blank and malformed field payloads`() = withCleanIdeBridgeProperties {
        assertEquals(emptyList(), KronosProjectionIdeBridge.read())

        System.setProperty(ProjectionPayloadProperty, "\n\n")
        assertEquals(emptyList(), KronosProjectionIdeBridge.read())

        val malformedFields = encodedLine(
            "main",
            "KronosSelectResult_bad",
            listOf("missingSeparator".encodeUrlBase64(), encodedField("", "kotlin.Int?")).joinToString(","),
            "KronosSelectScope_bad",
            encodedField("valid", "kotlin.String?"),
        )
        System.setProperty(ProjectionPayloadProperty, "\n$malformedFields\n")

        val model = KronosProjectionIdeBridge.read().single()
        assertEquals("KronosSelectResult_bad", model.name)
        assertEquals(emptyList(), model.fields)
        assertEquals(listOf(KronosIdeProjectionField("valid", "kotlin.String?")), model.contextFields)
    }

    @Test
    fun `ide active also recognizes idea platform properties and summary defaults`() = withCleanIdeBridgeProperties {
        assertEquals("count=0, payloadSize=0", KronosProjectionIdeBridge.lastPublishSummary())

        System.setProperty("idea.paths.selector", "KronosTest")
        assertTrue(KronosProjectionIdeBridge.isIdeActive())

        System.clearProperty("idea.paths.selector")
        System.setProperty("idea.platform.prefix", "Idea")
        assertTrue(KronosProjectionIdeBridge.isIdeActive())
    }

    @Test
    fun `resolve extension fallback reads system property`() = withCleanIdeBridgeProperties {
        assertFalse(KronosProjectionIdeBridge.isResolveExtensionFallbackEnabled())

        System.setProperty(ResolveExtensionFallbackProperty, "true")
        assertTrue(KronosProjectionIdeBridge.isResolveExtensionFallbackEnabled())

        System.setProperty(ResolveExtensionFallbackProperty, "false")
        assertFalse(KronosProjectionIdeBridge.isResolveExtensionFallbackEnabled())
    }

    @Test
    fun `generated projection declarations only use stable Kotlin fake source for ide member psi`() {
        val source = Files.readString(
            Paths.get("src/main/kotlin/com/kotlinorm/compiler/fir/KronosProjectionDeclarationGenerationExtension.kt")
        )

        assertTrue(source.contains("KtFakeSourceElementKind.PropertyFromParameter"))
        assertTrue(source.contains("fakeElement"))
        assertTrue(source.contains("Class.forName(name)"))
        assertTrue(source.contains("classOrNull(\"org.jetbrains.kotlin.KtFakePsiSourceElement\")"))
        assertFalse(source.contains("import org.jetbrains.kotlin.KtFakePsiSourceElement"))
        assertFalse(source.contains("import org.jetbrains.kotlin.KtPsiSourceElement"))
        assertFalse(source.contains("PluginGenerated"))
        assertFalse(source.contains("DataClassGeneratedMembers"))
        assertFalse(source.contains("SyntheticCall"))
        assertTrue(source.contains("sourceDeclaration"))
        assertTrue(source.contains("projectionOriginForIde"))
    }

    @Test
    fun `generated projection fake source avoids direct psi abi descriptors`() {
        val classLoader = KronosProjectionGeneratedDeclarationKey::class.java.classLoader
        val constantPoolText = listOf(
            "com/kotlinorm/compiler/fir/KronosProjectionDeclarationGenerationExtensionKt.class",
            "com/kotlinorm/compiler/fir/KronosSourceElementCompat.class",
        ).joinToString("\n") { resource ->
            val bytecode = checkNotNull(classLoader.getResourceAsStream(resource)).use { it.readBytes() }
            String(bytecode, Charsets.ISO_8859_1)
        }

        assertFalse(constantPoolText.contains("org/jetbrains/kotlin/KtPsiSourceElement"))
        assertFalse(constantPoolText.contains("org/jetbrains/kotlin/KtFakePsiSourceElement"))
        assertFalse(constantPoolText.contains("org/jetbrains/kotlin/com/intellij/psi/PsiElement"))
        assertFalse(constantPoolText.contains("com/intellij/psi/PsiElement"))
    }

    @Test
    fun `generated projection fake source compat handles reflective psi shapes`() {
        val compatClass = Class.forName("com.kotlinorm.compiler.fir.KronosSourceElementCompat")
        val compat = compatClass.getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null)
        val fakePsiSourceElement = compatClass
            .getDeclaredMethod("fakePsiSourceElement", KtSourceElement::class.java)
            .apply { isAccessible = true }
        val psiReflectively = compatClass
            .getDeclaredMethod("psiReflectively", KtSourceElement::class.java)
            .apply { isAccessible = true }
        val classOrNull = compatClass
            .getDeclaredMethod("classOrNull", String::class.java)
            .apply { isAccessible = true }
        val psiSourceElement = KtRealPsiSourceElement(fakePsiElement())

        assertNull(fakePsiSourceElement.invoke(compat, fakeLightSourceElement()))
        assertNull(psiReflectively.invoke(compat, fakeLightSourceElement()))
        assertTrue(PsiElement::class.java.isInstance(psiReflectively.invoke(compat, psiSourceElement)))
        assertEquals(
            "org.jetbrains.kotlin.KtFakePsiSourceElement",
            (classOrNull.invoke(compat, "org.jetbrains.kotlin.KtFakePsiSourceElement") as Class<*>).name
        )
        assertNull(classOrNull.invoke(compat, "org.jetbrains.kotlin.DoesNotExist"))

        val wrapped = fakePsiSourceElement.invoke(compat, psiSourceElement)

        assertNotNull(wrapped)
        assertEquals("org.jetbrains.kotlin.KtFakePsiSourceElement", wrapped::class.qualifiedName)
    }

    private fun withCleanIdeBridgeProperties(block: () -> Unit) {
        synchronized(IdeBridgePropertyLock) {
            val keys = listOf(
                ProjectionPayloadProperty,
                ProjectionActiveProperty,
                ProjectionCountProperty,
                ProjectionPayloadSizeProperty,
                ResolveExtensionFallbackProperty,
                "idea.paths.selector",
                "idea.platform.prefix",
            )
            val oldValues = keys.associateWith { System.getProperty(it) }
            try {
                keys.forEach { System.clearProperty(it) }
                block()
            } finally {
                keys.forEach { key ->
                    val value = oldValues[key]
                    if (value == null) System.clearProperty(key) else System.setProperty(key, value)
                }
            }
        }
    }
}

private val IdeBridgePropertyLock = Any()

private const val ProjectionPayloadProperty = "com.kotlinorm.kronos.ide.projections"
private const val ProjectionActiveProperty = "com.kotlinorm.kronos.ide.active"
private const val ResolveExtensionFallbackProperty = "com.kotlinorm.kronos.ide.resolveExtensionFallback"
private const val ProjectionCountProperty = "com.kotlinorm.kronos.ide.projections.count"
private const val ProjectionPayloadSizeProperty = "com.kotlinorm.kronos.ide.projections.payloadSize"

private fun encodedLine(vararg parts: String): String =
    parts.joinToString("|") { it.encodeUrlBase64() }

private fun encodedFields(vararg fields: Pair<String, String>): String =
    fields.joinToString(",") { (name, type) -> encodedField(name, type) }

private fun encodedField(name: String, type: String): String =
    "${name.encodeUrlBase64()}:${type.encodeUrlBase64()}"

private fun String.encodeUrlBase64(): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(StandardCharsets.UTF_8))

private fun fakeLightSourceElement(): KtLightSourceElement =
    KtLightSourceElement(
        fakeLighterAstNode(),
        0,
        0,
        fakeTreeStructure(),
        KtRealSourceElementKind
    )

private fun fakePsiElement(): PsiElement =
    Proxy.newProxyInstance(
        PsiElement::class.java.classLoader,
        arrayOf(PsiElement::class.java)
    ) { proxy, method, args ->
        when (method.name) {
            "toString" -> "fakePsiElement"
            "hashCode" -> 1
            "equals" -> proxy === args?.singleOrNull()
            else -> null
        }
    } as PsiElement

private fun fakeLighterAstNode(): LighterASTNode =
    Proxy.newProxyInstance(
        LighterASTNode::class.java.classLoader,
        arrayOf(LighterASTNode::class.java)
    ) { _, _, _ -> null } as LighterASTNode

private fun fakeTreeStructure(): FlyweightCapableTreeStructure<LighterASTNode> {
    @Suppress("UNCHECKED_CAST")
    return Proxy.newProxyInstance(
        FlyweightCapableTreeStructure::class.java.classLoader,
        arrayOf(FlyweightCapableTreeStructure::class.java)
    ) { _, _, _ -> null } as FlyweightCapableTreeStructure<LighterASTNode>
}
