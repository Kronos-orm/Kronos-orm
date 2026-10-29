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

/**
 * A platform-neutral physical database value.
 *
 * Database wrappers expose only these storage classes at the cursor boundary.
 * Logical Kotlin conversion, including enum, temporal, serialization, and
 * nullable handling, belongs to the codec layer after this value is read.
 */
sealed interface KronosPhysicalValue {
    /** SQL NULL, distinct from a missing column. */
    data object Null : KronosPhysicalValue

    /** A signed 64-bit database integer. */
    data class Integer(val value: Long) : KronosPhysicalValue

    /** A double-precision database real value. */
    data class Real(val value: Double) : KronosPhysicalValue

    /** A database text value. */
    data class Text(val value: String) : KronosPhysicalValue

    /**
     * A binary database value.
     *
     * The constructor and accessor copy the byte content so a row value stays
     * stable while a caller retains it after a cursor advances.
     */
    class Blob(bytes: ByteArray) : KronosPhysicalValue {
        private val content: ByteArray = bytes.copyOf()

        val bytes: ByteArray
            get() = content.copyOf()

        override fun equals(other: Any?): Boolean =
            other is Blob && content.contentEquals(other.content)

        override fun hashCode(): Int = content.contentHashCode()

        override fun toString(): String = "Blob(size=${content.size})"
    }
}

/**
 * One active physical row supplied by a platform database cursor.
 *
 * Column positions are one-based to match the execution/binding boundary used
 * by current wrappers. Labels are exact database column names or SQL aliases;
 * duplicate labels must be addressed by position. A row is live only while its
 * mapper is running. Implementations set [isActive] to `false` afterwards and
 * all accessors must then throw [IllegalStateException].
 */
interface KronosPhysicalRow {
    /** Zero-based index of this row in the current result. */
    val rowNumber: Int

    /** Number of columns available from this row. */
    val columnCount: Int

    /** Whether this row remains valid for reads. */
    val isActive: Boolean

    /** Returns the exact column name or alias at the one-based [position]. */
    fun columnLabel(position: Int): String

    /** Returns the raw physical value at the one-based [position]. */
    fun get(position: Int): KronosPhysicalValue

    /** Verifies that the row still belongs to its active mapper callback. */
    fun requireActive() {
        check(isActive) { "KronosPhysicalRow is only valid while its mapper is running" }
    }

    /** Verifies that [position] identifies a column in this active row. */
    fun requireColumnPosition(position: Int) {
        requireActive()
        require(position in 1..columnCount) {
            "Column position $position is outside 1..$columnCount"
        }
    }

    /**
     * Resolves one exact [label] to its one-based column position.
     *
     * A label must select exactly one column. This intentionally does not use
     * case-insensitive matching because platform drivers can expose labels that
     * differ only by case.
     */
    fun columnPosition(label: String): Int {
        require(label.isNotBlank()) { "Column label must not be blank" }
        requireActive()
        var match = 0
        for (position in 1..columnCount) {
            if (columnLabel(position) == label) {
                if (match != 0) {
                    throw IllegalArgumentException(
                        "Ambiguous row column label '$label'; use a one-based position"
                    )
                }
                match = position
            }
        }
        require(match != 0) { "No row column with label '$label'" }
        return match
    }

    /** Returns the raw physical value selected by one exact [label]. */
    fun get(label: String): KronosPhysicalValue = get(columnPosition(label))
}
