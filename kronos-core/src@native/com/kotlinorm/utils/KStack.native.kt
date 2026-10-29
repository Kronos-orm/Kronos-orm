package com.kotlinorm.utils
actual class KStack<T> private constructor(private val backing: ArrayDeque<T>) : MutableCollection<T> by backing {
    actual constructor() : this(ArrayDeque())
    actual fun push(element: T) = backing.addFirst(element)
    actual fun pop(): T = backing.removeFirst()
    actual fun addLast(element: T) = backing.addLast(element)
    actual fun pollLast(): T? = backing.removeLastOrNull()
    actual companion object {
        actual operator fun <T> of(vararg elements: T): KStack<T> = KStack<T>().apply { addAll(elements.reversed()) }
    }
}
