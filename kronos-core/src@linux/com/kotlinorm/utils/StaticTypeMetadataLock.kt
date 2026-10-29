/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package com.kotlinorm.utils

import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.rawValue
import kotlin.native.ref.createCleaner
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_unlock

internal actual class StaticTypeMetadataLock {
    private val mutex = nativeHeap.alloc<pthread_mutex_t>().ptr.also {
        check(pthread_mutex_init(it, null) == 0) { "Cannot initialize metadata mutex" }
    }

    @Suppress("unused")
    private val cleaner = createCleaner(mutex) {
        pthread_mutex_destroy(it)
        nativeHeap.free(it.rawValue)
    }

    actual fun <T> withLock(block: () -> T): T {
        check(pthread_mutex_lock(mutex) == 0) { "Cannot lock metadata mutex" }
        return try {
            block()
        } finally {
            pthread_mutex_unlock(mutex)
        }
    }
}
