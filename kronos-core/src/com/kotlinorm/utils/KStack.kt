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

expect class KStack<T>() : MutableCollection<T> {
    override val size: Int
    override fun iterator(): MutableIterator<T>
    override fun add(element: T): Boolean
    override fun remove(element: T): Boolean
    override fun addAll(elements: Collection<T>): Boolean
    override fun removeAll(elements: Collection<T>): Boolean
    override fun retainAll(elements: Collection<T>): Boolean
    override fun clear()
    override fun isEmpty(): Boolean
    override fun contains(element: T): Boolean
    override fun containsAll(elements: Collection<T>): Boolean

    fun push(element: T)
    fun pop(): T
    fun addLast(element: T)
    fun pollLast(): T?
    companion object { operator fun <T> of(vararg elements: T): KStack<T> }
}
fun <T> KStack<T>.push(element: T) = addLast(element)
fun <T> KStack<T>.pop(): T = pollLast() ?: throw NoSuchElementException("KStack is empty")
