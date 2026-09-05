package com.kotlinorm.utils

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

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
        val cache = LRUCache<String, String?>(2)
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
        val cache = LRUCache<String, String?>(3)
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
        val cache = LRUCache<String, String?>(2)
        cache["a"] = "1"
        cache["b"] = "2"
        cache["a"]  // 访问 "a"
        cache["c"] = "3"  // 这将导致 "b" 被移除
        assertNull(cache["b"])  // "b" 应该被移除
        assertEquals("1", cache["a"])
        assertEquals("3", cache["c"])
    }

    @Test
    fun getReturnsDefaultValueWhenKeyNotPresent() {
        val cache = LRUCache<String, String?>(2)
        val defaultValue = { key: String -> "default_$key" }
        assertEquals("default_a", cache.get("a", defaultValue))
    }

    @Test
    fun getThrowsExceptionWhenDefaultValueIsNull() {
        val cache = LRUCache<String, String?>(2)
        val defaultValue = { _: String -> null }
        assertFailsWith<IllegalStateException> { cache["a", defaultValue] }
    }

    @Test
    fun getReturnsExistingValueWhenKeyPresent() {
        val cache = LRUCache<String, String?>(2)
        cache["a"] = "1"
        val defaultValue = { _: String -> "default" }
        assertEquals("1", cache.get("a", defaultValue))
    }

    @Test
    fun `constructor default is invoked once and nullable results are cached`() {
        val calls = AtomicInteger()
        val cache = LRUCache<String, String?>(
            capacity = 2,
            defaultValue = {
                calls.incrementAndGet()
                null
            }
        )

        assertNull(cache["a"])
        assertNull(cache["a"])
        assertEquals(1, calls.get())
    }

    @Test
    fun `concurrent cache miss computes once and linked access remains consistent`() {
        val calls = AtomicInteger()
        val cache = LRUCache<String, String>(
            capacity = 2,
            keySelector = { it.lowercase() },
            defaultValue = { key ->
                calls.incrementAndGet()
                key.uppercase()
            }
        )
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val futures = (0 until 32).map { index ->
                executor.submit<String?> {
                    start.await()
                    cache[if (index % 2 == 0) "value" else "VALUE"]
                }
            }
            start.countDown()

            assertEquals(List(32) { "VALUE" }, futures.map { it.get() })
            assertEquals(1, calls.get())
            cache["other"] = "OTHER"
            assertEquals("VALUE", cache["VaLuE"])
        } finally {
            executor.shutdownNow()
        }
    }
}
