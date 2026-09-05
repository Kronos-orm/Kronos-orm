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

import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionIn
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ProjectionKind
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.name.StandardClassIds
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KronosProjectionIdeBridgeTest {
    @Test
    fun `module replacement removes stale models and retains other modules`() = withCleanIdeBridgeProperties {
        replaceIdeModuleSnapshot(
            "main",
            listOf(
                ideModel("main", "Result_old", "Context_old", "old" to "kotlin.Int?"),
                ideModel("main", "Result_removed", "Context_removed", "removed" to "kotlin.String?"),
            )
        )
        replaceIdeModuleSnapshot(
            "feature",
            listOf(ideModel("feature", "Result_feature", "Context_feature", "feature" to "kotlin.Long?"))
        )

        replaceIdeModuleSnapshot(
            "main",
            listOf(ideModel("ignored", "Result_current", "Context_current", "current" to "kotlin.Boolean?"))
        )

        assertEquals(
            listOf("feature:Result_feature:Context_feature", "main:Result_current:Context_current"),
            KronosProjectionIdeBridge.read().map { "${it.moduleName}:${it.name}:${it.contextName}" }
        )
        assertEquals("count=2, payloadSize=${System.getProperty(ProjectionPayloadProperty).length}", KronosProjectionIdeBridge.lastPublishSummary())
    }

    @Test
    fun `same module and result name keeps the latest model`() = withCleanIdeBridgeProperties {
        replaceIdeModuleSnapshot(
            "main",
            listOf(ideModel("main", "Result_shared", "Context_first", "value" to "kotlin.Int?"))
        )
        replaceIdeModuleSnapshot(
            "main",
            listOf(ideModel("main", "Result_shared", "Context_latest", "value" to "kotlin.String?"))
        )

        val model = KronosProjectionIdeBridge.read().single()
        assertEquals("Context_latest", model.contextName)
        assertEquals(listOf(KronosIdeProjectionField("value", "kotlin.String?")), model.fields)
        assertEquals("count=1, payloadSize=${System.getProperty(ProjectionPayloadProperty).length}", KronosProjectionIdeBridge.lastPublishSummary())
    }

    @Test
    fun `empty module snapshot clears only that module`() = withCleanIdeBridgeProperties {
        replaceIdeModuleSnapshot(
            "main",
            listOf(ideModel("main", "Result_main", "Context_main", "id" to "kotlin.Int?"))
        )
        replaceIdeModuleSnapshot(
            "feature",
            listOf(ideModel("feature", "Result_feature", "Context_feature", "id" to "kotlin.Long?"))
        )

        replaceIdeModuleSnapshot("main", emptyList())

        assertEquals(listOf("feature:Result_feature"), KronosProjectionIdeBridge.read().map { "${it.moduleName}:${it.name}" })
        replaceIdeModuleSnapshot("feature", emptyList())
        assertEquals(emptyList(), KronosProjectionIdeBridge.read())
        assertEquals("count=0, payloadSize=0", KronosProjectionIdeBridge.lastPublishSummary())
        assertEquals(null, System.getProperty(ProjectionPayloadProperty))
    }

    @Test
    fun `beginning a new module session clears stale models and retains other modules`() =
        withCleanIdeBridgeProperties {
            replaceIdeModuleSnapshot(
                "main",
                listOf(ideModel("main", "Result_stale", "Context_stale", "id" to "kotlin.Int?"))
            )
            replaceIdeModuleSnapshot(
                "feature",
                listOf(ideModel("feature", "Result_feature", "Context_feature", "id" to "kotlin.Long?"))
            )

            KronosProjectionIdeBridge.beginModuleSession("main")

            assertEquals(
                listOf("feature:Result_feature"),
                KronosProjectionIdeBridge.read().map { "${it.moduleName}:${it.name}" }
            )
        }

    @Test
    fun `shared owner token rejects a stale bridge state after a new classloader generation`() =
        withCleanIdeBridgeProperties {
            val firstBridgeToken = KronosProjectionIdeBridge.beginModuleSession("main")
            KronosProjectionIdeBridge.publishModuleSnapshot(
                "main",
                firstBridgeToken,
                listOf(ideModel("main", "Result_first", "Context_first", "id" to "kotlin.Int?"))
            )

            val secondBridgeToken = KronosProjectionIdeBridge.beginModuleSession("main")
            assertFalse(firstBridgeToken == secondBridgeToken)
            KronosProjectionIdeBridge.publishModuleSnapshot(
                "main",
                firstBridgeToken,
                listOf(ideModel("main", "Result_stale", "Context_stale", "id" to "kotlin.String?"))
            )

            assertEquals(emptyList(), KronosProjectionIdeBridge.read())
            assertEquals("count=0, payloadSize=0", KronosProjectionIdeBridge.lastPublishSummary())
            KronosProjectionIdeBridge.publishModuleSnapshot(
                "main",
                secondBridgeToken,
                listOf(ideModel("main", "Result_second", "Context_second", "id" to "kotlin.Long?"))
            )
            assertEquals(
                listOf("main:Result_second"),
                KronosProjectionIdeBridge.read().map { "${it.moduleName}:${it.name}" }
            )
        }

    @Test
    fun `module replacement preserves nested generic field type strings`() = withCleanIdeBridgeProperties {
        val nestedType = "kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.Int?>>?"
        replaceIdeModuleSnapshot(
            "main",
            listOf(ideModel("main", "Result_nested", "Context_nested", "nested" to nestedType))
        )

        val model = KronosProjectionIdeBridge.read().single()
        assertEquals(nestedType, model.fields.single().type)
        assertEquals(nestedType, model.contextFields.single().type)
    }

    @Test
    fun `IDE type rendering preserves nested generic nullability and star positions`() {
        val nullableString = renderIdeClassLikeType("kotlin.String", nullable = true)
        val list = renderIdeClassLikeType("kotlin.collections.List", listOf(nullableString))
        val pair = renderIdeClassLikeType("kotlin.Pair", listOf(list, "*"))
        val map = renderIdeClassLikeType(
            "kotlin.collections.Map",
            listOf("kotlin.String", pair),
            nullable = true,
        )

        assertEquals(
            "kotlin.collections.Map<kotlin.String, kotlin.Pair<kotlin.collections.List<kotlin.String?>, *>>?",
            map,
        )
    }

    @Test
    fun `IDE type rendering preserves variance nested nullability and star positions`() {
        val nullableString = renderIdeClassLikeType("kotlin.String", nullable = true)
        val nullableInt = renderIdeClassLikeType("kotlin.Int", nullable = true)
        val list = renderIdeClassLikeType("kotlin.collections.List", listOf(nullableInt))
        val box = renderIdeClassLikeType(
            "sample.Box",
            listOf(
                renderIdeTypeArgument(nullableString, ProjectionKind.IN),
                renderIdeTypeArgument(list, ProjectionKind.OUT),
                renderIdeTypeArgument(null, null),
            ),
        )

        assertEquals(
            "sample.Box<in kotlin.String?, out kotlin.collections.List<kotlin.Int?>, *>",
            box,
        )
        assertEquals("*", renderIdeTypeArgument("kotlin.String", null))
        assertEquals("*", renderIdeTypeArgument(null, ProjectionKind.OUT))
    }

    @Test
    fun `IDE type rendering handles real FIR variance star and flexible types`() {
        val intType = ConeClassLikeTypeImpl(
            StandardClassIds.Int.toLookupTag(),
            emptyArray(),
            false,
        )
        val listType = ConeClassLikeTypeImpl(
            StandardClassIds.List.toLookupTag(),
            arrayOf(
                intType,
                ConeKotlinTypeProjectionIn(intType),
                ConeStarProjection,
                ConeKotlinTypeProjectionOut(intType),
            ),
            true,
        )
        val flexibleType = ConeFlexibleType(intType, intType, isTrivial = true)

        assertEquals(
            "kotlin.collections.List<kotlin.Int, in kotlin.Int, *, out kotlin.Int>?",
            listType.renderIdeType(),
        )
        assertEquals("kotlin.Any?", flexibleType.renderIdeType())
    }

    @Test
    fun `registry starts a clean bridge snapshot when a declaration generator registers`() {
        val source = Files.readString(
            Paths.get("src/main/kotlin/com/kotlinorm/compiler/fir/KronosProjectionRegistry.kt")
        ).replace("\r\n", "\n")

        assertTrue(source.contains("KronosProjectionIdeBridge.beginModuleSession(session.ideModuleName())"))
        assertTrue(source.contains("ideGenerationTokensBySession = WeakHashMap<FirSession, String>()"))
        assertTrue(source.contains("ideGenerationTokensBySession[session] = generationToken"))
    }

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
        val invalidBase64 = listOf("%%%", "bad", "fields", "context", "contextFields").joinToString("|")
        System.setProperty(ProjectionPayloadProperty, "\n$invalidBase64\n$malformedFields\n")

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
    fun `generated projection declarations keep real member source in ide mode`() {
        val source = Files.readString(
            Paths.get("src/main/kotlin/com/kotlinorm/compiler/fir/KronosProjectionDeclarationGenerationExtension.kt")
        ).replace("\r\n", "\n")

        assertTrue(source.contains("private fun KronosProjectionModel.generatedDeclarationSource(): KtSourceElement?"))
        assertTrue(source.contains("private fun KronosProjectionModel.generatedMemberSource(field: KronosProjectionField): KtSourceElement?"))
        assertTrue(source.contains("if (KronosProjectionIdeBridge.isIdeActive()) {\n        sourceDeclaration"))
        assertTrue(source.contains("if (KronosProjectionIdeBridge.isIdeActive()) {\n        field.source ?: anchor"))
        assertFalse(source.contains("fakeElement"))
        assertFalse(source.contains("KtFakeSourceElementKind"))
        assertFalse(source.contains("Class.forName"))
        assertFalse(source.contains("KronosSourceElementCompat"))
        assertFalse(source.contains("import org.jetbrains.kotlin.KtFakePsiSourceElement"))
        assertFalse(source.contains("import org.jetbrains.kotlin.KtPsiSourceElement"))
        assertFalse(source.contains("PluginGenerated"))
        assertFalse(source.contains("DataClassGeneratedMembers"))
        assertFalse(source.contains("SyntheticCall"))
        assertTrue(source.contains("projectionOriginForIde"))
    }

    @Test
    fun `generated projection declarations avoid fake psi source abi descriptors`() {
        val classLoader = KronosProjectionGeneratedDeclarationKey::class.java.classLoader
        val bytecode = checkNotNull(
            classLoader.getResourceAsStream(
                "com/kotlinorm/compiler/fir/KronosProjectionDeclarationGenerationExtensionKt.class"
            )
        ).use { it.readBytes() }
        val constantPoolText = String(bytecode, Charsets.ISO_8859_1)

        assertFalse(
            classLoader.getResource("com/kotlinorm/compiler/fir/KronosSourceElementCompat.class") != null,
            "fake source compatibility helper should not be packaged"
        )
        assertFalse(constantPoolText.contains("org/jetbrains/kotlin/KtPsiSourceElement"))
        assertFalse(constantPoolText.contains("org/jetbrains/kotlin/KtFakePsiSourceElement"))
        assertFalse(constantPoolText.contains("org/jetbrains/kotlin/com/intellij/psi/PsiElement"))
        assertFalse(constantPoolText.contains("com/intellij/psi/PsiElement"))
    }

    @Test
    fun `generated projection source does not reflect into kotlin psi internals`() {
        val source = Files.readString(
            Paths.get("src/main/kotlin/com/kotlinorm/compiler/fir/KronosProjectionDeclarationGenerationExtension.kt")
        ).replace("\r\n", "\n")

        assertFalse(source.contains("getPsi"))
        assertFalse(source.contains("isAccessible = true"))
        assertFalse(source.contains("javaClass.methods"))
        assertFalse(source.contains("constructor.newInstance"))
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
            val ownerKeys = System.getProperties().stringPropertyNames()
                .filter { it.startsWith(ProjectionOwnerPropertyPrefix) }
            val oldValues = keys.associateWith { System.getProperty(it) }
            val oldOwnerValues = ownerKeys.associateWith { System.getProperty(it) }
            try {
                keys.forEach { System.clearProperty(it) }
                ownerKeys.forEach { System.clearProperty(it) }
                block()
            } finally {
                System.getProperties().stringPropertyNames()
                    .filter { it.startsWith(ProjectionOwnerPropertyPrefix) }
                    .forEach { System.clearProperty(it) }
                keys.forEach { key ->
                    val value = oldValues[key]
                    if (value == null) System.clearProperty(key) else System.setProperty(key, value)
                }
                oldOwnerValues.forEach { (key, value) ->
                    if (value != null) System.setProperty(key, value)
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
private const val ProjectionOwnerPropertyPrefix = "com.kotlinorm.kronos.ide.projections.owner."

private fun encodedLine(vararg parts: String): String =
    parts.joinToString("|") { it.encodeUrlBase64() }

private fun encodedFields(vararg fields: Pair<String, String>): String =
    fields.joinToString(",") { (name, type) -> encodedField(name, type) }

private fun encodedField(name: String, type: String): String =
    "${name.encodeUrlBase64()}:${type.encodeUrlBase64()}"

private fun replaceIdeModuleSnapshot(
    moduleName: String,
    models: Collection<KronosIdeProjectionModel>
) {
    val generationToken = KronosProjectionIdeBridge.beginModuleSession(moduleName)
    KronosProjectionIdeBridge.publishModuleSnapshot(moduleName, generationToken, models)
}

private fun ideModel(
    moduleName: String,
    name: String,
    contextName: String,
    vararg fields: Pair<String, String>,
): KronosIdeProjectionModel {
    val ideFields = fields.map { (fieldName, type) -> KronosIdeProjectionField(fieldName, type) }
    return KronosIdeProjectionModel(
        moduleName = moduleName,
        name = name,
        fields = ideFields,
        contextName = contextName,
        contextFields = ideFields,
    )
}

private fun String.encodeUrlBase64(): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(StandardCharsets.UTF_8))
