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

package com.kotlinorm.utils

/**
 * Utility functions for collection operations
 */
object CollectionUtils {
    /**
     * Check if a value is an empty array or collection
     */
    fun isEmptyArrayOrCollection(value: Any?): Boolean {
        return when (value) {
            null -> true
            is Array<*> -> value.isEmpty()
            is Collection<*> -> value.isEmpty()
            else -> false
        }
    }
}

/**
 * Simple key counter for generating unique keys
 */
class KeyCounter {
    private val counters = mutableMapOf<String, Int>()
    
    fun getNext(key: String): Int {
        val current = counters.getOrDefault(key, 0)
        counters[key] = current + 1
        return current
    }
    
    fun getCurrent(key: String): Int = counters.getOrDefault(key, 0)
}
