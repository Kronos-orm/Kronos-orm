package com.kotlinorm.utils

internal actual class RuntimeReentrantLock actual constructor() {
    private val lock = platform.Foundation.NSRecursiveLock()
    actual fun <T> withLock(block: () -> T): T { lock.lock(); return try { block() } finally { lock.unlock() } }
}
