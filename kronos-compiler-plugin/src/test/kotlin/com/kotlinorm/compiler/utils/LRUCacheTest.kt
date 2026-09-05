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

package com.kotlinorm.compiler.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LRUCacheTest {

    @Test
    fun `get returns null for missing key`() {
        val cache = LRUCache<String, Int>(3)
        assertNull(cache["missing"])
    }

    @Test
    fun `set and get basic operation`() {
        val cache = LRUCache<String, Int>(3)
        cache["a"] = 1
        assertEquals(1, cache["a"])
    }

    @Test
    fun `set overwrites existing key`() {
        val cache = LRUCache<String, Int>(3)
        cache["a"] = 1
        cache["a"] = 2
        assertEquals(2, cache["a"])
    }

    @Test
    fun `eviction removes least recently used entry`() {
        val cache = LRUCache<String, Int>(2)
        cache["a"] = 1
        cache["b"] = 2
        cache["c"] = 3 // should evict "a"
        assertNull(cache["a"])
        assertEquals(2, cache["b"])
        assertEquals(3, cache["c"])
    }

    @Test
    fun `access promotes entry and changes eviction order`() {
        val cache = LRUCache<String, Int>(2)
        cache["a"] = 1
        cache["b"] = 2
        cache["a"] // access "a" to promote it
        cache["c"] = 3 // should evict "b" (least recently used), not "a"
        assertEquals(1, cache["a"])
        assertNull(cache["b"])
        assertEquals(3, cache["c"])
    }

    @Test
    fun `getOrPut returns existing value without calling default`() {
        val cache = LRUCache<String, Int>(3)
        cache["a"] = 1
        var called = false
        val result = cache.getOrPut("a") { called = true; 99 }
        assertEquals(1, result)
        assertEquals(false, called)
    }

    @Test
    fun `getOrPut computes and stores value for missing key`() {
        val cache = LRUCache<String, Int>(3)
        val result = cache.getOrPut("a") { 42 }
        assertEquals(42, result)
        assertEquals(42, cache["a"])
    }

    @Test
    fun `capacity of 1 always keeps only last entry`() {
        val cache = LRUCache<String, Int>(1)
        cache["a"] = 1
        cache["b"] = 2
        assertNull(cache["a"])
        assertEquals(2, cache["b"])
    }

    @Test
    fun `multiple evictions in sequence`() {
        val cache = LRUCache<Int, String>(3)
        cache[1] = "one"
        cache[2] = "two"
        cache[3] = "three"
        cache[4] = "four"   // evicts 1
        cache[5] = "five"   // evicts 2
        assertNull(cache[1])
        assertNull(cache[2])
        assertEquals("three", cache[3])
        assertEquals("four", cache[4])
        assertEquals("five", cache[5])
    }

    @Test
    fun `overwrite does not increase size or cause spurious eviction`() {
        val cache = LRUCache<String, Int>(2)
        cache["a"] = 1
        cache["b"] = 2
        cache["a"] = 10 // overwrite, should not evict
        assertEquals(10, cache["a"])
        assertEquals(2, cache["b"])
    }

    @Test
    fun `getOrPut with eviction`() {
        val cache = LRUCache<String, Int>(2)
        cache["a"] = 1
        cache["b"] = 2
        cache.getOrPut("c") { 3 } // should evict "a"
        assertNull(cache["a"])
        assertEquals(2, cache["b"])
        assertEquals(3, cache["c"])
    }

    @Test
    fun `default capacity is 128`() {
        assertEquals(128, LRUCache.DEFAULT_LRU_CACHE_CAPACITY)
    }

    @Test
    fun `works with nullable values`() {
        val cache = LRUCache<String, String?>(3)
        cache["a"] = null
        // get returns null for both missing and null-valued keys
        // but the key should exist in the cache
        assertNull(cache["a"])
    }

    @Test
    fun `integer keys work correctly`() {
        val cache = LRUCache<Int, String>(3)
        cache[1] = "one"
        cache[2] = "two"
        cache[3] = "three"
        assertEquals("one", cache[1])
        assertEquals("two", cache[2])
        assertEquals("three", cache[3])
    }

    @Test
    fun `complex eviction with access pattern`() {
        val cache = LRUCache<String, Int>(3)
        cache["a"] = 1
        cache["b"] = 2
        cache["c"] = 3
        // Access "a" to make it most recently used
        cache["a"]
        // Access "b" to make it most recently used
        cache["b"]
        // Now LRU order is: c (least), a, b (most)
        cache["d"] = 4 // should evict "c"
        assertNull(cache["c"])
        assertEquals(1, cache["a"])
        assertEquals(2, cache["b"])
        assertEquals(4, cache["d"])
    }
}
