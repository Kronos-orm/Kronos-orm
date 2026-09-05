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
import kotlin.test.assertNull

class KronosProjectionTypeResolutionTest {
    private val source = projection("KronosSelectResult_source")
    private val selected = projection("KronosSelectResult_selected")
    private val context = projection("KronosSelectContext_selected")

    @Test
    fun `direct result context and nullable shapes resolve as direct rows`() {
        listOf(selected, context, selected.copy(nullable = true), context.copy(nullable = true)).forEach { shape ->
            val discovery = shape.findProjectionType()

            assertEquals(shape.classFqName, discovery?.classFqName, shape.toString())
            assertEquals(listOf(KronosProjectionCarrierKind.Direct), discovery?.carrierPath, shape.toString())
        }
    }

    @Test
    fun `selectable query carriers use their declared Selected argument`() {
        val threeArguments = listOf(source, selected, context)
        val carriers = listOf(
            type("com.kotlinorm.orm.select.SelectClause", threeArguments),
            type("com.kotlinorm.orm.join.JoinedSelectQuery", threeArguments),
            type("com.kotlinorm.beans.dsl.KSelectable", listOf(selected)),
            type("com.kotlinorm.orm.union.UnionClause", listOf(selected)),
            type("com.kotlinorm.orm.pagination.OffsetPageQuery", listOf(selected)),
        )

        carriers.forEach { carrier ->
            val discovery = carrier.findProjectionType()

            assertEquals(selected.classFqName, discovery?.classFqName, carrier.classFqName)
            assertEquals(
                listOf(KronosProjectionCarrierKind.SelectableQuery, KronosProjectionCarrierKind.Direct),
                discovery?.carrierPath,
                carrier.classFqName,
            )
        }
    }

    @Test
    fun `non-selectable known carriers retain their kind for documentation discovery`() {
        val carriers = listOf(
            type("kotlin.collections.List", listOf(selected)) to KronosProjectionCarrierKind.Collection,
            type("com.kotlinorm.orm.pagination.TotalPageQuery", listOf(selected)) to
                KronosProjectionCarrierKind.ExecutionStage,
            type("com.kotlinorm.orm.pagination.CursorPageQuery", listOf(selected)) to
                KronosProjectionCarrierKind.ExecutionStage,
            type("com.kotlinorm.orm.pagination.PageResult", listOf(selected)) to
                KronosProjectionCarrierKind.ResultEnvelope,
            type("com.kotlinorm.orm.pagination.CursorResult", listOf(selected)) to
                KronosProjectionCarrierKind.ResultEnvelope,
        )

        carriers.forEach { (carrier, kind) ->
            val discovery = carrier.findProjectionType()

            assertEquals(selected.classFqName, discovery?.classFqName, carrier.classFqName)
            assertEquals(listOf(kind, KronosProjectionCarrierKind.Direct), discovery?.carrierPath, carrier.classFqName)
        }
    }

    @Test
    fun `supported collection containers and multiple layers preserve the carrier path`() {
        val containers = listOf(
            "kotlin.Array",
            "kotlin.collections.Iterable",
            "kotlin.collections.MutableIterable",
            "kotlin.collections.Collection",
            "kotlin.collections.MutableCollection",
            "kotlin.collections.List",
            "kotlin.collections.MutableList",
            "kotlin.collections.Set",
            "kotlin.collections.MutableSet",
            "kotlin.sequences.Sequence",
        )

        containers.forEach { container ->
            assertEquals(
                listOf(KronosProjectionCarrierKind.Collection, KronosProjectionCarrierKind.Direct),
                type(container, listOf(selected)).findProjectionType()?.carrierPath,
                container,
            )
        }

        val nested = type(
            "kotlin.collections.List",
            listOf(
                type(
                    "kotlin.collections.List",
                    listOf(type("com.kotlinorm.orm.pagination.OffsetPageQuery", listOf(selected))),
                )
            ),
        )
        assertEquals(
            listOf(
                KronosProjectionCarrierKind.Collection,
                KronosProjectionCarrierKind.Collection,
                KronosProjectionCarrierKind.SelectableQuery,
                KronosProjectionCarrierKind.Direct,
            ),
            nested.findProjectionType()?.carrierPath,
        )
    }

    @Test
    fun `raw JoinSource base and all generated arities are rejected`() {
        val rawJoinSources = listOf("JoinSource") + (2..16).map { "JoinSource$it" }

        rawJoinSources.forEach { className ->
            assertNull(
                type(
                    "com.kotlinorm.orm.join.$className",
                    listOf(source, type("sample.Company"), selected),
                ).findProjectionType(),
                className,
            )
        }
    }

    @Test
    fun `map pair and custom wrappers do not expose nested projections`() {
        val wrappers = listOf(
            type("kotlin.Pair", listOf(type("kotlin.Int"), selected)),
            type("kotlin.collections.Map", listOf(type("kotlin.String"), selected)),
            type("sample.CustomWrapper", listOf(selected)),
        )

        wrappers.forEach { wrapper ->
            assertNull(wrapper.findProjectionType(), wrapper.classFqName)
        }
    }

    @Test
    fun `type renderer preserves star variance and nested nullability`() {
        val nullableListOfStar = KronosProjectionTypeShape(
            classFqName = "kotlin.collections.List",
            typeArguments = listOf(KronosProjectionTypeArgumentShape.Star),
            nullable = true,
        )
        val map = KronosProjectionTypeShape(
            classFqName = "kotlin.collections.Map",
            typeArguments = listOf(
                KronosProjectionTypeArgumentShape.Type(
                    type("kotlin.String", nullable = true),
                    KronosProjectionTypeVariance.In,
                ),
                KronosProjectionTypeArgumentShape.Type(
                    nullableListOfStar,
                    KronosProjectionTypeVariance.Out,
                ),
            ),
            nullable = true,
        )

        val cases = listOf(
            type("kotlin.String") to "kotlin.String",
            type("kotlin.String", nullable = true) to "kotlin.String?",
            nullableListOfStar to "kotlin.collections.List<*>?",
            map to "kotlin.collections.Map<in kotlin.String?, out kotlin.collections.List<*>?>?",
        )

        cases.forEach { (shape, expected) ->
            assertEquals(expected, shape.renderKronosProjectionTypeText(), shape.toString())
        }
    }

    @Test
    fun `bridge model keeps duplicate suffixes and separates Selected from Context`() {
        val selectedFields = listOf(
            KronosIdeProjectionField("id", "kotlin.Int?"),
            KronosIdeProjectionField("id_1", "kotlin.Long?"),
        )
        val contextFields = selectedFields + KronosIdeProjectionField("sourceOnly", "kotlin.String?")
        val model = KronosIdeProjectionModel(
            moduleName = "idea-test",
            name = selected.className,
            contextName = context.className,
            fields = selectedFields,
            contextFields = contextFields,
        )

        assertEquals(listOf("id", "id_1"), model.fieldsForProjectionClass(selected.className)?.map { it.name })
        assertEquals(
            listOf("id", "id_1", "sourceOnly"),
            model.fieldsForProjectionClass(context.className)?.map { it.name }
        )
        assertNull(model.fieldsForProjectionClass(source.className))
    }

    private fun projection(className: String): KronosProjectionTypeShape =
        type("com.kotlinorm.generated.projection.$className")

    private fun type(
        classFqName: String,
        typeArguments: List<KronosProjectionTypeShape?> = emptyList(),
        nullable: Boolean = false,
    ): KronosProjectionTypeShape = KronosProjectionTypeShape(
        classFqName,
        typeArguments.map { argument ->
            argument?.let { KronosProjectionTypeArgumentShape.Type(it) }
                ?: KronosProjectionTypeArgumentShape.Star
        },
        nullable,
    )
}
