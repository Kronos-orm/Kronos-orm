/**
 * Copyright 2022-2025 kronos-orm
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

package com.kotlinorm.beans.dsl

data class KTableIndex(
    var name: String,
    var columns: Array<String>,
    var type: String = "",
    var method: String = "",
    var concurrently: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KTableIndex

        if (name != other.name) return false
        if (!columns.contentEquals(other.columns)) return false
        if (type != other.type) return false
        if (method != other.method) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + columns.contentHashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + method.hashCode()
        return result
    }

    fun columns(vararg columns: String) {
        this.columns = this.columns.plus(columns)
    }
}