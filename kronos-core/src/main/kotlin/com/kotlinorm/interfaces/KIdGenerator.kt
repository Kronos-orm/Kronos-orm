package com.kotlinorm.interfaces

interface KIdGenerator<T> {
    fun nextId(): T
}