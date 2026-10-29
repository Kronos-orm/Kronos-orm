package com.kotlinorm.utils

import kotlin.test.*

class RuntimeCollectionsTest {
    @Test
    fun sourceIdentityDoesNotCollapseEqualObjects() {
        data class EqualSource(val value: Int = 0)
        val sources = List(5000) { EqualSource() }
        val map = IdentityHashMap<EqualSource, Int>()
        sources.forEachIndexed { index, source -> map[source] = index }
        sources.forEachIndexed { index, source -> assertEquals(index, map[source]) }
        assertEquals(5000, map.size)
        assertEquals(0, map.remove(sources.first()))
        assertFalse(map.containsKey(sources.first()))
        assertEquals(1, map[sources[1]])
        map.clear()
        assertTrue(map.isEmpty())
    }

    @Test
    fun identityMapReplacementAndEntryMutationKeepEqualKeysSeparate() {
        data class Key(val label: String)
        val first = Key("same")
        val second = Key("same")
        val absent = Key("absent")
        val map = IdentityHashMap<Key, Int>()
        assertNull(map.put(first, 1))
        assertNull(map.put(second, 2))
        assertEquals(1, map.put(first, 3))
        assertEquals(2, map.size)
        assertEquals(3, map[first])
        assertEquals(2, map[second])
        assertNull(map[absent])
        assertNull(map.remove(absent))
        val entry = map.entries.single { it.key === first }
        assertEquals(3, entry.setValue(4))
        assertEquals(4, map[first])
        assertTrue(map.containsKey(first))
        assertFalse(map.containsKey(absent))
    }

    @Test
    fun identityMapIteratorRemovesOnlyItsCurrentEntryAndRejectsInvalidMutation() {
        val first = Any()
        val second = Any()
        val map = IdentityHashMap<Any, String>()
        map[first] = "first"
        map[second] = "second"
        val iterator = map.entries.iterator()
        assertFailsWith<IllegalStateException> { iterator.remove() }
        val selected = iterator.next()
        val selectedKey = selected.key
        iterator.remove()
        assertFalse(map.containsKey(selectedKey))
        assertEquals(1, map.entries.size)
        assertFailsWith<IllegalStateException> { iterator.remove() }
        assertTrue(iterator.hasNext())
        val remaining = iterator.next()
        assertEquals(if (selectedKey === first) "second" else "first", remaining.value)
        assertFalse(iterator.hasNext())
        assertFailsWith<IllegalStateException> { map.entries.add(remaining) }
        iterator.remove()
        assertTrue(map.isEmpty())
    }

    @Test
    fun cacheLoaderCanReenterTheSameCache() {
        lateinit var cache: LRUCache<String, String>
        cache = LRUCache(2, defaultValue = { key -> if (key == "root") cache["leaf"] else "value" })
        assertEquals("value", cache["root"])
        assertEquals("value", cache["leaf"])
    }
}
