package com.kotlinorm.utils

class LRUCache<T, R>(private val capacity: Int = DEFAULT_LRU_CACHE_CAPACITY, val defaultValue: (T) -> R? = { null }) {

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
            return node.value
        }
        return defaultValue(key)?.also { set(key, it) }
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