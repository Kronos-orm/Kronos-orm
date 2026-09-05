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

import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectionNameResolutionTest {
    @Test
    fun `suffixes duplicate requested names around reserved output names`() {
        val fields = listOf(
            field("id"),
            field("id"),
            field("id_1"),
            field("id"),
        )

        val resolved = fields.withUniqueProjectionNames()

        assertEquals(listOf("id", "id_2", "id_1", "id_3"), resolved.map { it.name.asString() })
        assertEquals(listOf("property:id", "property:id:output:id_2", "property:id_1", "property:id:output:id_3"), resolved.map { it.signature })
    }

    private fun field(name: String): KronosProjectionField = KronosProjectionField(
        name = Name.identifier(name),
        requestedName = Name.identifier(name),
        type = ConeClassLikeTypeImpl(StandardClassIds.Int.toLookupTag(), emptyArray(), false),
        source = null,
        signature = "property:$name",
    )
}
