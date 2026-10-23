package com.kotlinorm.utils
expect class LinkedHashSet<T> : MutableSet<T> {
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

    constructor()
    constructor(elements: Collection<T>)
    companion object { operator fun <T> of(vararg elements: T): LinkedHashSet<T> }
}
