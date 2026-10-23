package com.kotlinorm.utils
actual class LinkedHashSet<T> private constructor(private val backing: MutableSet<T>) : MutableSet<T> by backing {
    actual constructor() : this(linkedSetOf())
    actual constructor(elements: Collection<T>) : this(elements.toMutableSet())
    actual companion object {
        actual operator fun <T> of(vararg elements: T): LinkedHashSet<T> = LinkedHashSet<T>().apply { addAll(elements) }
    }
}
