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

package com.kotlinorm.interfaces

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class KronosPhysicalRowTest {
    @Test
    fun `physical values preserve SQLite storage classes and blob ownership`() {
        val source = byteArrayOf(1, 2)
        val blob = KronosPhysicalValue.Blob(source)
        source[0] = 9

        assertEquals(KronosPhysicalValue.Null, KronosPhysicalValue.Null)
        assertEquals(KronosPhysicalValue.Integer(7), KronosPhysicalValue.Integer(7))
        assertEquals(KronosPhysicalValue.Real(1.5), KronosPhysicalValue.Real(1.5))
        assertEquals(KronosPhysicalValue.Text("text"), KronosPhysicalValue.Text("text"))
        assertContentEquals(byteArrayOf(1, 2), blob.bytes)
        assertEquals(KronosPhysicalValue.Blob(byteArrayOf(1, 2)), blob)

        val exposed = blob.bytes
        exposed[1] = 8
        assertContentEquals(byteArrayOf(1, 2), blob.bytes)
    }

    @Test
    fun `physical rows use one based positions and exact unambiguous labels`() {
        val row = TestRow(
            labels = listOf("name", "age"),
            values = listOf(KronosPhysicalValue.Text("Ada"), KronosPhysicalValue.Integer(42))
        )

        assertEquals(0, row.rowNumber)
        assertEquals(2, row.columnCount)
        assertEquals("name", row.columnLabel(1))
        assertEquals(2, row.columnPosition("age"))
        assertEquals(KronosPhysicalValue.Text("Ada"), row.get("name"))
        assertEquals(KronosPhysicalValue.Integer(42), row.get(2))
        assertFailsWith<IllegalArgumentException> { row.get(0) }
        assertFailsWith<IllegalArgumentException> { row.columnPosition("NAME") }
        assertFailsWith<IllegalArgumentException> { row.columnPosition("missing") }
    }

    @Test
    fun `physical rows reject ambiguous labels and reads after mapper lifetime`() {
        val duplicate = TestRow(
            labels = listOf("id", "id"),
            values = listOf(KronosPhysicalValue.Integer(1), KronosPhysicalValue.Integer(2))
        )
        assertFailsWith<IllegalArgumentException> { duplicate.get("id") }

        val row = TestRow(
            labels = listOf("name"),
            values = listOf(KronosPhysicalValue.Text("Ada"))
        )
        row.invalidate()

        assertFalse(row.isActive)
        assertFailsWith<IllegalStateException> { row.get(1) }
        assertFailsWith<IllegalStateException> { row.get("name") }
    }

    private class TestRow(
        private val labels: List<String>,
        private val values: List<KronosPhysicalValue>
    ) : KronosPhysicalRow {
        init {
            require(labels.size == values.size)
        }

        override val rowNumber: Int = 0
        override val columnCount: Int = labels.size
        override var isActive: Boolean = true
            private set

        override fun columnLabel(position: Int): String {
            requireColumnPosition(position)
            return labels[position - 1]
        }

        override fun get(position: Int): KronosPhysicalValue {
            requireColumnPosition(position)
            return values[position - 1]
        }

        fun invalidate() {
            isActive = false
        }
    }
}
