/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.utils.tableCache

class LRUCacheImpl(private val limit: Int) {
    private var counter: Int
    private val head: Node = Node(null, null)
    private val tail: Node = Node(null, null)
    private val map: HashMap<String, Node?> = HashMap()


    init {
        //initial values
        head.next = tail
        tail.prev = head
        head.prev = null //pointing at nothing
        tail.next = null //pointing at nothing
        counter = 0
    }

    private fun deleteNode(node: Node?) {
        node?.prev?.next = node?.next
        node?.next?.prev = node?.prev
    }

    private fun addToHead(node: Node?) {
        node?.next = head.next
        node?.next?.prev = node
        node?.prev = head
        head.next = node
    }

    operator fun get(key: String): TableObject? {
        if (map[key] != null) {
            val node = map[key]
            deleteNode(node)
            addToHead(node)
            assert(node != null)
            return node!!.value
        }
        return null
    }

    operator fun set(key: String, value: TableObject) {
        if (map[key] != null) {
            val node = map[key]
            node!!.value = value
            deleteNode(node)
            addToHead(node)
        } else {
            val node = Node(key, value)
            map[key] = node
            if (counter < limit) {
                counter++
            } else {
                map.remove(tail.prev!!.key)
                deleteNode(tail.prev)
            }
            addToHead(node)
        }
    }

}