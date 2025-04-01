package com.kotlinorm.utils

typealias KStack<T> = ArrayDeque<T>

fun <T> KStack<T>.push(element: T) = addLast(element)

fun <T> KStack<T>.pop() = removeLastOrNull() ?: throw NoSuchElementException("ArrayDeque is empty")