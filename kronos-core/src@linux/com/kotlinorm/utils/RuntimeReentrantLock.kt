package com.kotlinorm.utils
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import kotlinx.cinterop.rawValue

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
internal actual class RuntimeReentrantLock actual constructor() {
    private val mutex = kotlinx.cinterop.nativeHeap.alloc<platform.posix.pthread_mutex_t>().ptr.also { mutex ->
        kotlinx.cinterop.memScoped {
            val attr = alloc<platform.posix.pthread_mutexattr_t>()
            check(platform.posix.pthread_mutexattr_init(attr.ptr) == 0)
            try {
                check(platform.posix.pthread_mutexattr_settype(attr.ptr, platform.posix.PTHREAD_MUTEX_RECURSIVE.toInt()) == 0)
                check(platform.posix.pthread_mutex_init(mutex, attr.ptr) == 0)
            } finally { platform.posix.pthread_mutexattr_destroy(attr.ptr) }
        }
    }
    @Suppress("unused")
    private val cleaner = kotlin.native.ref.createCleaner(mutex) {
        platform.posix.pthread_mutex_destroy(it)
        kotlinx.cinterop.nativeHeap.free(it.rawValue)
    }
    actual fun <T> withLock(block: () -> T): T {
        check(platform.posix.pthread_mutex_lock(mutex) == 0)
        return try { block() } finally { platform.posix.pthread_mutex_unlock(mutex) }
    }
}
