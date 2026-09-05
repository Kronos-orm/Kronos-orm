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

class LRUCache<T, R>(
    private val capacity: Int = DEFAULT_LRU_CACHE_CAPACITY,
    private val keySelector: (T) -> Any? = { it },
    val defaultValue: ((T) -> R?)? = null
) {

    private val lock = Any()
    private val map = hashMapOf<Any?, Node<T, R>>()
    private val head: Node<T, R> = Node()
    private val tail: Node<T, R> = Node()

    init {
        head.next = tail
        tail.prev = head
    }

    operator fun get(key: T): R? = synchronized(lock) {
        val cacheKey = keySelector(key)
        map[cacheKey]?.let { node ->
            promote(node)
            return@synchronized node.value
        }

        val loader = defaultValue ?: return@synchronized null
        val value = loader(key)
        put(cacheKey, key, value)
        value
    }

    @Suppress("UNCHECKED_CAST")
    operator fun get(key: T, defaultValue: ((T) -> R?)): R {
        return synchronized(lock) {
            val cacheKey = keySelector(key)
            map[cacheKey]?.let { node ->
                promote(node)
                return@synchronized node.value as R
            }

            val value = defaultValue(key) ?: error("default value not found")
            put(cacheKey, key, value)
            value
        }
    }

    operator fun set(key: T, value: R) {
        synchronized(lock) {
            put(keySelector(key), key, value)
        }
    }

    private fun put(cacheKey: Any?, key: T, value: R?) {
        map.remove(cacheKey)?.let(::remove)
        val node = Node(key, cacheKey, value)
        addAtEnd(node)
        map[cacheKey] = node
        if (map.size > capacity) {
            val first = head.next!!
            remove(first)
            map.remove(first.cacheKey)
        }
    }

    private fun promote(node: Node<T, R>) {
        remove(node)
        addAtEnd(node)
    }

    private fun remove(node: Node<T, R>) {
        val next = node.next!!
        val prev = node.prev!!
        prev.next = next
        next.prev = prev
    }

    private fun addAtEnd(node: Node<T, R>) {
        val prev = tail.prev!!
        prev.next = node
        node.prev = prev
        node.next = tail
        tail.prev = node
    }

    private class Node<T, R>(
        val key: T? = null,
        val cacheKey: Any? = null,
        val value: R? = null
    ) {
        var next: Node<T, R>? = null
        var prev: Node<T, R>? = null
    }

    companion object {
        const val DEFAULT_LRU_CACHE_CAPACITY = 128
    }
}
