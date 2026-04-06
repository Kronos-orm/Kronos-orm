package com.kotlinorm.compiler.plugin.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import kotlin.test.Test

class LRUCacheTest {

    @Test
    fun testAddItem() {
        val cache = LRUCache<String, String>(2)
        cache["a"] = "1"
        assertEquals("1", cache["a"])
    }

    @Test
    fun testUpdateItem() {
        val cache = LRUCache<String, String>(2)
        cache["a"] = "1"
        cache["a"] = "2"  // 更新
        assertEquals("2", cache["a"])
    }

    @Test
    fun testEvictOldestItem() {
        val cache = LRUCache<String, String>(2)
        cache["a"] = "1"
        cache["b"] = "2"
        cache["c"] = "3"  // 这将导致 "a" 被移除
        assertNull(cache["a"])  // "a" 应该被移除
        assertEquals("2", cache["b"])
        assertEquals("3", cache["c"])
    }

    @Test
    fun testEvictLeastRecentlyUsed() {
        val cache = LRUCache<String, String>(2)
        cache["a"] = "1"
        cache["b"] = "2"
        cache["a"]  // 访问 "a"
        cache["c"] = "3"  // 这将导致 "b" 被移除
        assertNull(cache["b"])  // "b" 应该被移除
        assertEquals("1", cache["a"])
        assertEquals("3", cache["c"])
    }

    @Test
    fun testCacheSizeLimit() {
        val cache = LRUCache<String, String>(3)
        cache["a"] = "1"
        cache["b"] = "2"
        cache["c"] = "3"
        cache["d"] = "4"  // 这将导致 "a" 被移除
        assertNull(cache["a"])  // "a" 应该被移除
        assertEquals("2", cache["b"])
        assertEquals("3", cache["c"])
        assertEquals("4", cache["d"])
    }

    @Test
    fun testAccessOrder() {
        val cache = LRUCache<String, String>(2)
        cache["a"] = "1"
        cache["b"] = "2"
        cache["a"]  // 访问 "a"
        cache["c"] = "3"  // 这将导致 "b" 被移除
        assertNull(cache["b"])  // "b" 应该被移除
        assertEquals("1", cache["a"])
        assertEquals("3", cache["c"])
    }
}