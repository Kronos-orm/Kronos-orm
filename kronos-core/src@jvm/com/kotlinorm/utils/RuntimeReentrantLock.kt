package com.kotlinorm.utils

internal actual class RuntimeReentrantLock actual constructor() {
    private val monitor = Any()
    actual fun <T> withLock(block: () -> T): T = synchronized(monitor) { block() }
}
