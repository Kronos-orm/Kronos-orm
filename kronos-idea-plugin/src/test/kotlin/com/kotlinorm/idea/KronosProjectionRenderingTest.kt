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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KronosProjectionRenderingTest {
    @Test
    fun `projection renderer preserves allocated names exact nullability and nested generic types`() {
        val rendered = renderProjectionClass(
            "KronosSelectResult_allocated",
            listOf(
                field("id", "kotlin.Int"),
                field("id_2", "kotlin.Long?"),
                field("id_1", "kotlin.collections.List<kotlin.String?>"),
            ),
        )

        assertEquals(
            """
            data class KronosSelectResult_allocated(
                var id: kotlin.Int,
                var id_2: kotlin.Long? = null,
                var id_1: kotlin.collections.List<kotlin.String?>,
            ) : com.kotlinorm.interfaces.KPojo
            """.trimIndent(),
            rendered,
        )
        assertFalse(rendered.contains("id_3"))
        assertFalse(rendered.contains("kotlin.Int = null"))
        assertFalse(rendered.contains("kotlin.collections.List<kotlin.String?> = null"))
    }

    @Test
    fun `declaration file separates Selected and Context and deduplicates identical classes`() {
        val selectedFields = listOf(
            field("id", "kotlin.Int"),
            field("id_2", "kotlin.Long?"),
            field("id_1", "kotlin.collections.List<kotlin.String?>"),
        )
        val contextFields = selectedFields + field(
            "sourceOnly",
            "kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.Long?>>?",
        )
        val model = model(
            name = "KronosSelectResult_allocated",
            fields = selectedFields,
            contextName = "KronosSelectContext_allocated",
            contextFields = contextFields,
        )

        val declarations = projectionClassDeclarations(listOf(model, model))
        val rendered = renderProjectionDeclarationFile(listOf(model, model))

        assertEquals(2, declarations.size)
        assertEquals(selectedFields, declarations.single { it.name == model.name }.fields)
        assertEquals(contextFields, declarations.single { it.name == model.contextName }.fields)
        assertEquals(1, rendered.occurrencesOf("data class ${model.name}("))
        assertEquals(1, rendered.occurrencesOf("data class ${model.contextName}("))
        assertEquals(1, rendered.occurrencesOf("var sourceOnly:"))
        assertFalse(rendered.contains("legacyName"))
        assertFalse(rendered.contains("id_3"))
    }

    @Test
    fun `same class name with different shapes renders one deterministic observable declaration`() {
        val current = model(
            name = "KronosSelectResult_shared",
            fields = listOf(field("current", "kotlin.String")),
            contextName = "KronosSelectResult_shared",
            contextFields = listOf(field("current", "kotlin.String")),
        )
        val stale = model(
            name = "KronosSelectResult_shared",
            fields = listOf(field("stale", "kotlin.String?")),
            contextName = "KronosSelectResult_shared",
            contextFields = listOf(field("stale", "kotlin.String?")),
        )

        val forward = renderProjectionDeclarationFile(listOf(current, stale))
        val reversed = renderProjectionDeclarationFile(listOf(stale, current))

        assertEquals(forward, reversed)
        assertEquals(1, forward.occurrencesOf("data class KronosSelectResult_shared("))
        assertTrue(forward.contains("Kronos projection shape conflict"))
        assertTrue(forward.contains("ignored 1 alternative(s)"))
        assertTrue(forward.contains("var current: kotlin.String,"))
        assertFalse(forward.contains("var stale:"))
    }

    @Test
    fun `canonical lookup uses the same deterministic shape as declaration rendering`() {
        val current = model(
            name = "KronosSelectResult_shared",
            fields = listOf(field("current", "kotlin.String")),
            contextName = "KronosSelectContext_shared",
            contextFields = listOf(field("currentContext", "kotlin.Int?")),
        )
        val stale = model(
            name = "KronosSelectResult_shared",
            fields = listOf(field("stale", "kotlin.String?")),
            contextName = "KronosSelectContext_shared",
            contextFields = listOf(field("staleContext", "kotlin.Long?")),
        )
        val forwardModels = listOf(current, stale)
        val reversedModels = forwardModels.reversed()

        val renderedDeclaration = projectionClassDeclarations(forwardModels)
            .single { it.name == "KronosSelectResult_shared" }
        val forwardLookup = forwardModels.canonicalProjectionClassDeclaration("KronosSelectResult_shared")
        val reversedLookup = reversedModels.canonicalProjectionClassDeclaration("KronosSelectResult_shared")

        assertEquals(renderedDeclaration, forwardLookup)
        assertEquals(forwardLookup, reversedLookup)
        assertEquals(listOf(field("current", "kotlin.String")), forwardLookup?.fields)
        assertEquals(2, forwardLookup?.conflictingShapeCount)
        assertEquals(
            listOf(field("currentContext", "kotlin.Int?")),
            forwardModels.canonicalProjectionClassDeclaration("KronosSelectContext_shared")?.fields,
        )
        assertEquals(
            forwardModels.canonicalProjectionClassDeclaration("KronosSelectContext_shared"),
            reversedModels.canonicalProjectionClassDeclaration("KronosSelectContext_shared"),
        )
    }

    private fun field(name: String, type: String): KronosIdeProjectionField =
        KronosIdeProjectionField(name, type)

    private fun model(
        name: String,
        fields: List<KronosIdeProjectionField>,
        contextName: String,
        contextFields: List<KronosIdeProjectionField>,
    ): KronosIdeProjectionModel = KronosIdeProjectionModel(
        moduleName = "idea-rendering-test",
        name = name,
        fields = fields,
        contextName = contextName,
        contextFields = contextFields,
    )

    private fun String.occurrencesOf(value: String): Int = split(value).size - 1
}
