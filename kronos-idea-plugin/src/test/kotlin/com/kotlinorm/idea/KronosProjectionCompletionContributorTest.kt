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

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers projection completion for derived sources such as SelectSubquerySqlTest's window alias case.
 */
class KronosProjectionCompletionContributorTest {
    @Test
    fun `projection completion supports empty selector receivers`() {
        val source = Files.readString(
            Paths.get("src/main/kotlin/com/kotlinorm/idea/KronosProjectionCompletionContributor.kt")
        ).replace("\r\n", "\n")

        assertTrue(source.contains("private fun CompletionParameters.projectionCompletionReceiver(): KtExpression?"))
        assertTrue(source.contains("qualified.selectorExpression == null"))
        assertTrue(source.contains("qualified.textRange.contains(offset)"))
        assertTrue(source.contains("qualified.receiverExpression"))
    }

    @Test
    fun `projection completion still contributes bridge fields`() {
        val source = Files.readString(
            Paths.get("src/main/kotlin/com/kotlinorm/idea/KronosProjectionCompletionContributor.kt")
        ).replace("\r\n", "\n")

        assertTrue(source.contains("KronosProjectionIdeBridge.read()"))
        assertTrue(source.contains("expressionType?.toKronosProjectionType()"))
        assertTrue(source.contains("projectionType.findCompletionFields()"))
        assertTrue(source.contains("canonicalProjectionClassDeclaration(className)?.fields"))
        assertTrue(source.contains("fields.distinctBy { it.name }"))
        assertTrue(source.contains("result.addElement("))
        assertFalse(source.contains("firstOrNull { it.name == className || it.contextName == className }"))
    }

    @Test
    fun `only direct projection receivers are eligible for row field completion`() {
        val direct = projectionType(KronosProjectionCarrierKind.Direct)
        val nonDirect = listOf(
            projectionType(KronosProjectionCarrierKind.SelectableQuery),
            projectionType(KronosProjectionCarrierKind.Collection),
            projectionType(KronosProjectionCarrierKind.ExecutionStage),
            projectionType(KronosProjectionCarrierKind.ResultEnvelope),
        )

        assertTrue(direct.isDirectCompletionReceiver)
        nonDirect.forEach { projectionType ->
            assertFalse(projectionType.isDirectCompletionReceiver, projectionType.carrierKind.name)
        }
    }

    @Test
    fun `explicit row extraction converts only selectable and collection carriers to direct`() {
        val selectable = projectionType(KronosProjectionCarrierKind.SelectableQuery)
        val collection = projectionType(KronosProjectionCarrierKind.Collection)
        val execution = projectionType(KronosProjectionCarrierKind.ExecutionStage)
        val envelope = projectionType(KronosProjectionCarrierKind.ResultEnvelope)
        val nestedCollection = collection.copy(
            carrierPath = listOf(
                KronosProjectionCarrierKind.Collection,
                KronosProjectionCarrierKind.SelectableQuery,
                KronosProjectionCarrierKind.Direct,
            )
        )

        assertTrue(selectable.canExtractDirectRow)
        assertTrue(collection.canExtractDirectRow)
        assertFalse(execution.canExtractDirectRow)
        assertFalse(envelope.canExtractDirectRow)
        assertFalse(nestedCollection.canExtractDirectRow)

        val extracted = collection.asDirectCompletionReceiver(nullable = true)
        assertTrue(extracted.isDirectCompletionReceiver)
        assertEquals("com.kotlinorm.generated.projection.KronosSelectResult_completion?", extracted.subjectType)
    }

    @Test
    fun `completion source enforces carrier eligibility and explicit extraction`() {
        val source = Files.readString(
            Paths.get("src/main/kotlin/com/kotlinorm/idea/KronosProjectionCompletionContributor.kt")
        ).replace("\r\n", "\n")

        assertTrue(source.contains("discoveredProjectionType()?.takeIf { it.isDirectCompletionReceiver }"))
        assertTrue(source.contains("?.takeIf { it.canExtractDirectRow }"))
        assertTrue(source.contains("receiverType.asDirectCompletionReceiver(nullable)"))
        assertTrue(source.contains("selectorName != \"firstOrNull\""))
        assertTrue(source.contains("selectorName != \"singleOrNull\""))
        assertTrue(source.contains("selectorName != \"first\""))
    }

    private fun projectionType(kind: KronosProjectionCarrierKind): KronosProjectionType =
        KronosProjectionType(
            classFqName = "com.kotlinorm.generated.projection.KronosSelectResult_completion",
            subjectType = "com.kotlinorm.generated.projection.KronosSelectResult_completion",
            carrierPath = if (kind == KronosProjectionCarrierKind.Direct) {
                listOf(kind)
            } else {
                listOf(kind, KronosProjectionCarrierKind.Direct)
            },
        )
}
