package com.kotlinorm.utils
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import kotlinx.cinterop.rawValue

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
internal actual class RuntimeReentrantLock actual constructor() {
    private val mutex = kotlinx.cinterop.nativeHeap.alloc<platform.windows.CRITICAL_SECTION>().ptr.also { platform.windows.InitializeCriticalSection(it) }
    @Suppress("unused")
    private val cleaner = kotlin.native.ref.createCleaner(mutex) {
        platform.windows.DeleteCriticalSection(it)
        kotlinx.cinterop.nativeHeap.free(it.rawValue)
    }
    actual fun <T> withLock(block: () -> T): T {
        platform.windows.EnterCriticalSection(mutex)
        return try { block() } finally { platform.windows.LeaveCriticalSection(mutex) }
    }
}
