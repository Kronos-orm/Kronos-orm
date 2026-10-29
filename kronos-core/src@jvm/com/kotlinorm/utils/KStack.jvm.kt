package com.kotlinorm.utils
actual class KStack<T> actual constructor() : java.util.ArrayDeque<T>() {
    actual override fun push(element: T) = super.push(element)
    actual override fun pop(): T = super.pop()
    actual override fun addLast(element: T) = super.addLast(element)
    actual override fun pollLast(): T? = super.pollLast()
    actual companion object {
        actual operator fun <T> of(vararg elements: T): KStack<T> = KStack<T>().apply { addAll(elements.reversed()) }
    }
}
