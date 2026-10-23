package com.kotlinorm.utils
actual class LinkedHashSet<T> : java.util.LinkedHashSet<T> {
    actual constructor() : super()
    actual constructor(elements: Collection<T>) : super(elements)
    actual companion object {
        actual operator fun <T> of(vararg elements: T): LinkedHashSet<T> = LinkedHashSet<T>().apply { addAll(elements) }
    }
}
