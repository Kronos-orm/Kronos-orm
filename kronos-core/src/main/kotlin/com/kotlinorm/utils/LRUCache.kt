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

class LRUCache<T, R>(private val capacity: Int = DEFAULT_LRU_CACHE_CAPACITY, val defaultValue: ((T) -> R?)? = null) {

    private val map = hashMapOf<T, Node<T, R>>()
    private val head: Node<T, R> = Node()
    private val tail: Node<T, R> = Node()

    init {
        head.next = tail
        tail.prev = head
    }

    operator fun get(key: T): R? {
        if (map.containsKey(key)) {
            val node = map[key]!!
            remove(node)
            addAtEnd(node)
            if (node.value != null)
                return node.value
        }
        if (defaultValue != null) {
            if (defaultValue(key) == null) {
                return null
            } else {
                val node = Node(key, defaultValue(key))
                addAtEnd(node)
                map[key] = node
                if (map.size > capacity) {
                    val first = head.next!!
                    remove(first)
                    map.remove(first.key)
                }
                return node.value!!
            }
        } else {
            return null
        }
    }

    operator fun get(key: T, defaultValue: ((T) -> R?)): R {
        return get(key) ?: defaultValue(key) ?: throw IllegalStateException("default value not found")
    }

    operator fun set(key: T, value: R) {
        if (map.containsKey(key)) {
            remove(map[key]!!)
        }
        val node = Node(key, value)
        addAtEnd(node)
        map[key] = node
        if (map.size > capacity) {
            val first = head.next!!
            remove(first)
            map.remove(first.key)
        }
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

    data class Node<T, R>(val key: T? = null, val value: R? = null) {
        var next: Node<T, R>? = null
        var prev: Node<T, R>? = null
    }

    companion object{
        const val DEFAULT_LRU_CACHE_CAPACITY = 128
    }
}