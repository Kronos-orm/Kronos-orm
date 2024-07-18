package com.kotlinorm.utils

typealias KStack<T> = ArrayDeque<T>

fun <T> ArrayDeque<T>.push(element: T) = addLast(element)

fun <T> ArrayDeque<T>.pop() = removeLastOrNull() ?: throw NoSuchElementException("ArrayDeque is empty")